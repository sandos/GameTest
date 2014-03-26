package se.sandos.android.gametest.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import se.sandos.android.gametest.BinaryMessage;
import se.sandos.android.gametest.GameSimulation;
import se.sandos.android.gametest.MainActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SimulationTest extends android.test.ActivityUnitTestCase<MainActivity> {

	private static final int ITERATIONS = 1000;

	private static InetAddress addr; 
	
	private MainActivity act;
	private Random r;
	
	private ArrayDeque<BinaryMessage> queue1;
	private ArrayDeque<BinaryMessage> queue2;
	
	public void setUp()
	{
		try {
			super.setUp();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		startActivity(new Intent(), new Bundle(), null);
		act = getActivity();
		
		try {
			addr = InetAddress.getByName("1.2.3.4");
		} catch (UnknownHostException e) {
		}
		
		r = new Random(10203398);
		
		queue1 = new ArrayDeque<BinaryMessage>();
		queue2 = new ArrayDeque<BinaryMessage>();
	}
	
	public SimulationTest()
	{
		super(MainActivity.class);
	}
	
	public SimulationTest(Class<MainActivity> activityClass) {
		super(activityClass);
	}

	public void testSimplex2Peers() throws IOException
	{
		GameSimulation sim1 = new GameSimulation(act, "sim1");
		GameSimulation sim2 = new GameSimulation(act, "sim2");
		sim1.silence(true);
		sim2.silence(true);
		
		BinaryMessage binaryMessage = new BinaryMessage();
		
 		for(int x=0; x<ITERATIONS; x++) {
			sim1.clicked();

			stepSynched(sim1, sim2, binaryMessage, true);
		}
	}

	public void testDuplex2Peers() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act, "sim1");
		GameSimulation sim2 = new GameSimulation(act, "sim2");
		sim1.silence(true);
		sim2.silence(true);
		
		BinaryMessage binaryMessage = new BinaryMessage();
		
 		for(int x=0; x<ITERATIONS; x++) {
			if(sim1.timestep() % 10 == 3) {
				sim1.clicked();
			}
			if(sim1.timestep() % 10 == 4) {
				sim2.clicked();
			}

			stepSynched(sim1, sim2, binaryMessage, true);
		}
	}
	
	public void testDelaySingleClick() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act, "sim1");
		GameSimulation sim2 = new GameSimulation(act, "sim2");
		sim1.silence(true);
		sim2.silence(true);

 		for(int x=0; x<ITERATIONS; x++) {
 			if(x == 10) {
 				sim1.clicked();
 			}
 			try {
 				step(sim1, sim2, 5, x > 100, 1.0f);
 			} catch(RuntimeException e) {
 				Log.d("test", "Failed at " + x);
 				throw e;
 			}
 		}
	}
	
	public void testDelayComplexSimplex2Peers() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act, "sim1");
		GameSimulation sim2 = new GameSimulation(act, "sim2");
		sim1.silence(true);
		sim2.silence(true);
		
 		for(int x=0; x<ITERATIONS; x++) {
 			if(x % 8 == 2) {
 				sim1.clicked();
 			}
 			
 			Assert.assertEquals("Failed history compare at " + x, -1, sim2.compareHistory(sim1, 10));
 			
 			
 			try {
 				step(sim1, sim2, 5, false, 1.0f);
 			} catch(RuntimeException e) {
 				Log.d("test", "Failed at " + x);
 				throw e;
 			}
 		}
	}
	
	public void testDelayLossComplexSimplex2Peers() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act, "sim1");
		GameSimulation sim2 = new GameSimulation(act, "sim2");
		sim1.silence(true);
		sim2.silence(true);
		
 		for(int x=0; x<ITERATIONS; x++) {
 			if(x % 3 == 2 && x < 300) {
 				sim1.clicked();
 			}
 			
// 			if(!sim2.checkHistory()) {
// 				Assert.fail("gaah");
// 			}
 			
 			Assert.assertEquals(-1, sim2.compareHistory(sim1, 10));
 			
 			if(x > ITERATIONS*0.9) {
 				Assert.assertTrue("Simulation drift: " + sim1.timestep() + "|" + sim2.timestep(), Math.abs(sim1.timestep() - sim2.timestep()) < 5);
 			}
 			
 			try {
 				step(sim1, sim2, 5, x > 500, 0.7f);
 			} catch(RuntimeException e) {
 				Log.d("test", "Failed at " + x);
 				throw e;
 			}
 		}
	}
	
	public void testOutOfOrderPackets() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act, "sim1");
		GameSimulation sim2 = new GameSimulation(act, "sim2");
		sim1.silence(true);
		sim2.silence(true);
		
		BinaryMessage temp = new BinaryMessage();
		
		sim1.clicked();
		stepSynched(sim1, sim2, temp, false);
		stepSynched(sim1, sim2, temp, false);
		stepSynched(sim1, sim2, temp, false);
		Assert.assertEquals(-1, sim1.compareHistory(sim2, 1));
		
		sim1.clicked();
		sim1.step();
		sim1.step();
		sim1.step();
		sim1.step();
		sim1.step();
		
		sim2.step();
		sim2.step();
		sim2.step();
		sim2.step();
		sim2.step();
		
		//We diverge here, network is desynched
		//This also tests that compareHistory works!
		Assert.assertEquals(6, sim1.compareHistory(sim2, 0));

		stepSynched(sim1, sim2, temp, false);

		Assert.assertEquals(-1, sim1.compareHistory(sim2, 2));
		Assert.assertEquals("Simulator lagging due to input", 8,  sim2.timestep());
	}
	
	public void testN() throws IOException
	{
		Map<GameSimulation, ArrayDeque<BinaryMessage>> sims = new HashMap<GameSimulation, ArrayDeque<BinaryMessage>>();
		
		for(int count=3; count<7; count++) {
			sims.clear();
			for(int j=0; j<count; j++) {
				GameSimulation gs = new GameSimulation(act, "sim" + count);
				sims.put(gs, new ArrayDeque<BinaryMessage>(10));
				gs.silence(true);
			}
			
			for(int x=0; x<ITERATIONS; x++) {
				//Clicker
				if(x < 200) {
					int index = 0;
					for(GameSimulation gs : sims.keySet()) {
						index++;
						if((index+x)%20 == 2) {
							gs.clicked();
						}
					}
				}
			
				Set<GameSimulation> done = new HashSet<GameSimulation>();
				for(GameSimulation gs : sims.keySet()) {
					for(GameSimulation peer : sims.keySet()) {
						if(done.contains(peer)) {
							continue;
						}
			 			Assert.assertEquals("Histories diverge at " + gs.timestep() + "|" + peer.timestep() + " Count is " + count + " " + gs + " " + peer, -1, peer.compareHistory(gs, 30));
					}
					done.add(gs);
				}

				
				//Send network data
				for(Entry<GameSimulation, ArrayDeque<BinaryMessage>> e : sims.entrySet()) {
					GameSimulation s = e.getKey();
					ArrayDeque<BinaryMessage> queue = e.getValue();

					BinaryMessage bm = new BinaryMessage();
					s.serialize(bm);
					queue.add(bm);
					
					if(queue.size() >= 5) {
						BinaryMessage poll = queue.poll();
						Set<GameSimulation> others = sims.keySet();
						others.remove(s);
						for(GameSimulation gs : others) {
							BinaryMessage b = new BinaryMessage();
							b.parseFrom(poll.getWritten());
							gs.absorb(b, addr);
						}
					}
				}
				
				//Step
				for(GameSimulation gs : sims.keySet()) {
					gs.step();
				}
				
			}
		}
	}
	
	public void testDelayLossComplexDuplex2Peers() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act, "sim1");
		GameSimulation sim2 = new GameSimulation(act, "sim2");
		sim1.silence(true);
		sim2.silence(true);
			
 		for(int x=0; x<ITERATIONS; x++) {
 			if(x < 300) {
	 			if(x % 10 == 2) {
	 				sim1.clicked();
	 			}
	 			if(x % 26 == 2) {
	 				sim2.clicked();
	 			}
 			}
 			
 			Assert.assertEquals("Histories diverge at " + sim1.timestep() + "|" + sim2.timestep(), -1, sim2.compareHistory(sim1, 30));
 			
 			if(x > ITERATIONS*0.9) {
 				Assert.assertTrue("Simulation drift: " + sim1.timestep() + "|" + sim2.timestep(), Math.abs(sim1.timestep() - sim2.timestep()) < 5);
 			}
 			
 			try {
 				step(sim1, sim2, 5, x > 600, 0.7f);
 			} catch(RuntimeException e) {
 				Log.d("test", "Failed at " + x);
 				throw e;
 			}
 		}
	}

	
	private void step(GameSimulation sim1, GameSimulation sim2, int delay, boolean check, float packetLoss) throws IOException,
			UnknownHostException {

		BinaryMessage bm = new BinaryMessage();
		sim1.serialize(bm);
		BinaryMessage bm2 = new BinaryMessage();
		sim2.serialize(bm2);
		
		if(r.nextFloat() < packetLoss) {
			queue1.add(bm);
		}
		if(r.nextFloat() < packetLoss) {
			queue2.add(bm2);
		}

		if(queue1.size() > delay) {
			BinaryMessage p = queue1.poll();
			p.parseFrom(p.getWritten());
			sim2.absorb(p, addr);
		}
		if(queue2.size() > delay) {
			BinaryMessage p = queue2.poll();
			p.parseFrom(p.getWritten());
			sim1.absorb(p, addr);
		}

		
		//Run the assert on the old, so we get the debug log output when failing
		if(check) {
			try {
				if(sim1.timestep() == sim2.timestep()) {
					Assert.assertEquals("Simualations desynched at " + sim1.timestep() + " " + sim1 + "|" + sim2, sim1.hashCode(), sim2.hashCode());
				}
			} catch(AssertionFailedError e) {
				//Run them anyway, we want to see the detailed info
				sim1.step();
				sim2.step();
				
				throw e;
			}
		}
		
		sim1.step();
		sim2.step();
	}
	
	//Simulate perfect network, no PL or delay
	private void stepSynched(GameSimulation sim1, GameSimulation sim2, BinaryMessage binaryMessage, boolean check) throws IOException,
			UnknownHostException {
		
		binaryMessage.reset();
		sim1.serialize(binaryMessage);
		binaryMessage.parseFrom(binaryMessage.getWritten());
		sim2.absorb(binaryMessage, addr);

		binaryMessage.reset();
		sim2.serialize(binaryMessage);
		binaryMessage.parseFrom(binaryMessage.getWritten());
		sim1.absorb(binaryMessage, addr);

		//Run the assert on the old, so we get the debug log output when failing
		if(sim1.timestep() == sim2.timestep() && check) {
			Assert.assertEquals(sim1.hashCode(), sim2.hashCode());
		}
		
		sim1.step();
		sim2.step();
	}
}
