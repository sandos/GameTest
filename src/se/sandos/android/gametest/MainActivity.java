package se.sandos.android.gametest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final int PKT_HELLO = 123456;
	private static final int PKT_PING  = 987655;
	private static final int PKT_PONG  = 345678;
	
	private static final String TAG = "majs";
	private static final int UDP_PORT = 49152;
	private static final int UDP_MAX_SIZE = 1000;
	
	private DatagramSocket serverSocketUDP;
	private int pingCounter = 0;
	private MulticastLock ml;
	
	private InetAddress ip;
	private InetAddress broadcast;
	
	private Thread recvThread;
	private Thread sendThread;
	private final BlockingQueue<DatagramPacket> sendQueue = new ArrayBlockingQueue<DatagramPacket>(10);
	
	private Thread paceThread;
	
	private DatagramSocket sendSocket;
	private DatagramPacket helloWorldPacket;
	final private BinaryMessage binaryMessage = new BinaryMessage();
	private DatagramPacket dataPacket;
	
	final private PeerPair pairTemp = new PeerPair();
	
	private ConcurrentHashMap<PeerPair, Latency> latencies = new ConcurrentHashMap<PeerPair, Latency>(10, 0.5f, 2);
	
	private ConcurrentHashMap<InetAddress, Boolean> peersBacking = new ConcurrentHashMap<InetAddress, Boolean>(10, 0.5f, 2);
	private Set<InetAddress> peers = Collections.newSetFromMap(peersBacking);
	
	private GameView gameView;
	
	private GameSimulation sim;
	
	public SoundPool soundPool;
	public int clickSoundId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gameView = new GameView(getApplicationContext());
		GameRenderer gr = new GameRenderer();
		gameView.setRenderer(gr);
		setContentView(gameView);
		
		sim = new GameSimulation(this);
		gr.setSim(sim);
		
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		clickSoundId = soundPool.load(this, R.raw.click, 1);
		
		soundPool.play(clickSoundId, 1.0f, 1.0f, 1, -1, 1.0f);
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		Log.v(TAG, "RESUME");
		
        ip = getIpAddress();
        broadcast = getBroadcast(ip);
        binaryMessage.reset();
		binaryMessage.writeInt(PKT_HELLO);
        byte[] message = binaryMessage.getWrittenCopy();
        Log.v(TAG, "Offset: " + binaryMessage.getOffset() + "|" + binaryMessage.limit() + "|" + binaryMessage.capa());
        
		helloWorldPacket = new DatagramPacket(message, binaryMessage.writtenLength(), broadcast, UDP_PORT);
		byte[] buffer = new byte[100];
		dataPacket = new DatagramPacket(buffer, buffer.length, broadcast, UDP_PORT);
        try {
			sendSocket = new DatagramSocket();
			sendSocket.setBroadcast(true);
		} catch (SocketException e1) {
			Log.v(TAG, "Could not create socket: " + e1.getMessage(), e1);
			finish();
		}
        
//		setStrictMode();

        recvThread = new Thread(new Runnable() {
			public void run() {
				try {
					start_UDP();
				} catch (IOException e) {
					Log.v(TAG, e.getMessage());
				}
			}
		});
        recvThread.setName("UDPreceiver");
        recvThread.setPriority(Thread.MAX_PRIORITY);
        recvThread.start();
		
		sendThread = new Thread("UDPsender") {
			public void run() {
				while(!isInterrupted()) {
					try {
						sendUDPMessage(sendQueue.take());
					} catch (InterruptedException e) {
						Log.v(TAG, "Exception when taking from queue: " + e.getMessage(), e);
					}
				}
			}
		};
		sendThread.setPriority(Thread.MAX_PRIORITY);
		sendThread.start();
		
		paceThread = new Thread("PaceThread") {
			public void run() {
				int cnt = 0;
				while(!isInterrupted()) {
					SystemClock.sleep(40);
					if((cnt++) % 100 == 10) {
						sendUDPMessage(helloWorldPacket);
					}
					else
					{
						synchronized (binaryMessage) {
							for(InetAddress ia : peers) {
								binaryMessage.reset();
								try {
									binaryMessage.writeInt(PKT_PING).writeInt(pingCounter).writeLong(System.nanoTime());
									sim.serialize(binaryMessage);
									sendUDPMessage(packet(binaryMessage.getWritten(), ia));
								} catch (IOException e) {
									Log.v(TAG, "Problem sending package: " + e.getMessage());
								}
							}
						}
					}
				}
				Log.v(TAG, "Interrupted pacer thread");
			}
		};
		paceThread.setPriority(Thread.MAX_PRIORITY);
		paceThread.start();
		
		WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if(wm != null) {
			ml = wm.createMulticastLock("");
			ml.acquire();
		}	
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	        super.onWindowFocusChanged(hasFocus);
	    if (hasFocus && Build.VERSION.SDK_INT >= 19) {
	        gameView.setSystemUiVisibility(
	                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	                | View.SYSTEM_UI_FLAG_FULLSCREEN
	                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
	}
	
	private DatagramPacket packet(byte[] msg, InetAddress address)
	{
		dataPacket.setData(msg);
		dataPacket.setAddress(address);
		return dataPacket;
	}
	
	@SuppressLint("NewApi")
	private void setStrictMode() {
		//Strict mode
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads()
        .detectDiskWrites()
        .detectNetwork()   // or .detectAll() for all detectable problems
        .penaltyLog()
        .build());
        
        if (Build.VERSION.SDK_INT >= 11) {
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
			        .detectLeakedSqlLiteObjects()
			        .detectLeakedClosableObjects()
			        .penaltyLog()
			        .penaltyDeath()
			        .build());
        }
	}
	
	@Override
	public void onPause()
	{
		super.onPause();

		Log.v(TAG, "PAUSE");
		
		paceThread.interrupt();
		recvThread.interrupt();
		
		ml.release();
		
		if(serverSocketUDP != null) {
			serverSocketUDP.close();
		}

		if(sendSocket != null) {
			sendSocket.close();
		}
	}
	
	@Override
	public void onStop()
	{
		super.onStop();	
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	final List<InetAddress> rowHeaders = new LinkedList<InetAddress>();
	final List<InetAddress> colHeaders = new LinkedList<InetAddress>();
	final byte[] receiveData = new byte[UDP_MAX_SIZE];
	final DatagramPacket receivePacket = new DatagramPacket(
			receiveData, receiveData.length);
	
	private Map<PeerPair, TextView> viewCache = new HashMap<PeerPair, TextView>(); 
	
	private void start_UDP() throws IOException {
		try {
			serverSocketUDP = new DatagramSocket(UDP_PORT);
		} catch (Exception e) {
			Log.w(TAG, "Exception opening DatagramSocket UDP: " + e.getMessage());
			return;
		}

		while (true) {
			serverSocketUDP.receive(receivePacket);

			if (!receivePacket.getAddress().equals(ip)) {
				synchronized (binaryMessage) {
					binaryMessage.parseFrom(receivePacket.getData());
					int firstInt = binaryMessage.readInt();
					if(firstInt == PKT_HELLO) {
						if(!peers.contains(receivePacket.getAddress())) {
							Log.v(TAG, "Got package from new PEER: " + receivePacket.getAddress());
							peers.add(receivePacket.getAddress());
						}
					}
					else if(firstInt == PKT_PING)
					{
						final long now = System.nanoTime();
						int which = binaryMessage.readInt();
						long timer = binaryMessage.readLong();
						
						sim.absorb(binaryMessage, receivePacket.getAddress());
						
						binaryMessage.reset();
						binaryMessage.writeInt(PKT_PONG).writeInt(which).writeLong(timer).writeLong(now);
						
						sendUDPMessage(packet(binaryMessage.getWritten(), receivePacket.getAddress()));
					}
	
					if(firstInt == PKT_PONG && pairTemp != null) {
						final long sent = binaryMessage.readLong();
						
						final long now = System.nanoTime();
						pairTemp.setPeer1(ip);
						pairTemp.setPeer2(receivePacket.getAddress());
						final PeerPair pp = pairTemp;
						if(!latencies.containsKey(pairTemp))
						{
							PeerPair newKey = new PeerPair(ip, receivePacket.getAddress());
							Latency l = new Latency();
							l.to = (now-sent)/1000000;
							latencies.put(newKey,  l);
							updateUI(sent, now, newKey);
						}
						else
						{
							latencies.get(pp).to = (now-sent)/1000000;
							updateUI(sent, now, pp);
						}
					}
				}//synchronized block
			} else {
			}
		}// while ends
	}// method ends

	private void updateUI(final long sent, final long now, final PeerPair pp) {
	}

	private void sendUDPMessage(DatagramPacket packet) {
		try {
			sendSocket.send(packet);
		} catch (Exception e) {
			Log.d(TAG, "Exception packet broadcast: " + e.getMessage() +"|" + packet, e);
		}
	}

	public InetAddress getIpAddress() {
		try {

			InetAddress inetAddress = null;
			InetAddress myAddr = null;

			for (Enumeration<NetworkInterface> networkInterface = NetworkInterface
					.getNetworkInterfaces(); networkInterface.hasMoreElements();) {

				NetworkInterface singleInterface = networkInterface
						.nextElement();

				for (Enumeration<InetAddress> IpAddresses = singleInterface
						.getInetAddresses(); IpAddresses.hasMoreElements();) {
					inetAddress = IpAddresses.nextElement();

					if (!inetAddress.isLoopbackAddress()
							&& (singleInterface.getDisplayName().contains(
									"wlan0") || singleInterface
									.getDisplayName().contains("eth0"))) {

						myAddr = inetAddress;
					}
				}
			}
			Log.v(TAG, "My ip is " + myAddr);
			return myAddr;

		} catch (SocketException ex) {
			Log.e(TAG, ex.toString());
		}
		return null;
	}

	public InetAddress getBroadcast(InetAddress inetAddr) {

		Log.v(TAG, "getBroadcast");
		NetworkInterface temp;
		InetAddress iAddr = null;
		try {
			temp = NetworkInterface.getByInetAddress(inetAddr);
			List<InterfaceAddress> addresses = temp.getInterfaceAddresses();

			for (InterfaceAddress inetAddress : addresses) {
				iAddr = inetAddress.getBroadcast();
			}
			Log.d(TAG, "iAddr=" + iAddr);
			return iAddr;

		} catch (SocketException e) {

			e.printStackTrace();
			Log.d(TAG, "getBroadcast" + e.getMessage());
		}
		return null;
	}
}
