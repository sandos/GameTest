package se.sandos.android.gametest;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;

/**
 * Deterministic game simulation with discrete time steps
 * 
 * Rewind-and-merge
 * 
 * @author sandos
 *
 */
public class GameSimulation {
	private static final int MEDIAN_NUMBER = 23;
	private static final int ENEMY_MAX = 10;
	private static final int SHOTS_MAX = 10;
	private static final int HISTORY_LENGTH = 30;
	private static final int STATE_SIZE = 7;
	
	private int timestep;
	
	private final String TAG = "SIM";
	
	//Player
	private int pX, pY, vX, vY, r, vR;
	private int[][] history = new int[HISTORY_LENGTH][STATE_SIZE];
	
	
	class Enemy {
		int x, y, vX, vY, r, vR;
		boolean alive;
		
		public void serialize(BinaryMessage b) throws IOException
		{
			b.writeInt(x).writeInt(y).writeInt(vX).writeInt(vY).writeInt(r).writeInt(vR);
			b.writeBoolean(alive);
		}
	}
	
	private Enemy[] enemies = new Enemy[ENEMY_MAX];

	class Shot {
		int x, y, vX, vY;
		boolean alive;
		
		public void serialize(BinaryMessage b) throws IOException
		{
			b.writeInt(x).writeInt(y).writeInt(vX).writeInt(vY);
			b.writeBoolean(alive);
		}
	}
	
	private Shot[] shots = new Shot[SHOTS_MAX];
	
	private static final int SHFT = 16;
	private static final float SCALE = 1 << SHFT;
	private static final int MIN_X = -10 << SHFT;
	private static final int MIN_Y = -10 << SHFT;
	private static final int MAX_X = 10 << SHFT;
	private static final int MAX_Y = 10 << SHFT;
	private static final int ACTION_MAX = 10;
	
	//List for internal handling
	private int[] actionList = new int[ACTION_MAX];
	private int numActions = 0;
	//List for sending on to network
	private int[] actionOutList = new int[ACTION_MAX];
	private int numOutActions = 0;
	
	//Synch data, never sent out!
	private Map<InetAddress, ArrayDeque<Integer>> timeOffsets = new HashMap <InetAddress, ArrayDeque<Integer>>();
	private List<Integer> medians = new ArrayList<Integer>(10);
	private int avgOffset = 0;
	private boolean peers = false;
	//This is used to control speeding up/down of timestep
	private int stepCheckCounter = 0;
	
	private MainActivity act;

	//Player input from network
	private int[] actionInList = new int[ACTION_MAX];
	private int numInActions = 0;
	
	
	public GameSimulation(MainActivity activity)
	{
		act = activity;
		
		vX = 14000;
		vY = 51200;
		vR = 700000;
		
		for(int i=0; i<ENEMY_MAX; i++)
		{
			enemies[i] = new Enemy();
		}
		for(int i=0; i<SHOTS_MAX; i++)
		{
			shots[i] = new Shot();
		}
	}
	
	public float r()
	{
		return r/SCALE;
	}
	
	public float x()
	{
		return pX / SCALE;
	}
	
	public float y()
	{
		return pY / SCALE;
	}
	
	private void actualStep()
	{
		moveHistory();
		
		pX += vX;
		pY += vY;
		
		boolean bounce = false;
		
		boolean clicked = false;
		if(numActions > 0) {
			for(int i=0; i<numActions; i++) {
				if(actionList[i] != timestep) {
					Log.v(TAG, "Action not on this timestep: " + actionList[i]);
				}
				else
				{
					clicked = true;
				}
			}
			
			numActions = 0;
		}
		
		if(pX >= MAX_X || pX <= MIN_X || clicked) {
			//pX = MAX_X;
			pX -= vX;
			vX = -vX;
			bounce = true;
			clicked = false;
		} 
		
		if(pY >= MAX_Y || pY <= MIN_Y) {
			//pY = MAX_Y;
			pY -= vY;
			vY = -vY;
			bounce = true;
		} 

		if(bounce && avgOffset > -100 && avgOffset < 10) {
			act.soundPool.play(act.clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
		}
		
		r += vR;
		
		//Drag
//		vX *= 0.9f;
//		vY *= 0.9f;
		
		timestep++;
	}

	private void moveHistory() {
		//Move history
		for(int i=HISTORY_LENGTH-2; i>=0; i--)
		{
			for(int j=0; j<STATE_SIZE; j++)
			{
				history[i+1][j] = history[i][j];
			}
		}
		
		history[0][0] = pX;
		history[0][1] = pY;
		history[0][2] = vX;
		history[0][3] = vY;
		history[0][4] = r;
		history[0][5] = vR;
		history[0][6] = hashCode();
	}
	
	public synchronized void step()
	{	
		stepCheckCounter++;

		if(peers && stepCheckCounter % 13 == 1)
		{
			if(avgOffset < -1) {
				Log.v(TAG, "Running late, small step " + avgOffset);
				actualStep();
			} else if(avgOffset > 10) {
				Log.v(TAG, "Skipping " + avgOffset);
				return;
			}
		}
		
		if(peers && stepCheckCounter % 60 == 1) {
			if(avgOffset < -60)
			{
				Log.v(TAG, "Running late, BIG step: " + avgOffset);
				for(int i=0; i<(-avgOffset); i++)
				{
					actualStep();
				}
			}
		}
		else
		{
			actualStep();
		}
	}

	//Handle timestamp+hash
	public synchronized void absorb(BinaryMessage d, InetAddress peer) throws IOException
	{
		peers = true;
		int peerTimestep = d.readInt();
		
		if(!timeOffsets.containsKey(peer))
		{
			timeOffsets.put(peer, new ArrayDeque<Integer>(MEDIAN_NUMBER));
		}
		
		final ArrayDeque<Integer> deque = timeOffsets.get(peer);;
		
		deque.add(timestep-peerTimestep);
		if(deque.size() >= MEDIAN_NUMBER)
		{
			deque.poll();
		}
		
		medians.clear();
		for(InetAddress p : timeOffsets.keySet())
		{
			medians.add(medianForPeer(peer));
		}
		Collections.sort(medians);
		avgOffset = medians.get(medians.size()/2);
		
		int hash = d.readInt();
		if(peerTimestep < timestep) {
			int offset = timestep - peerTimestep;
			offset--;
			if(offset < HISTORY_LENGTH && hash != history[offset][6]) {
				Log.v(TAG, "SyncHIST: " + offset + "|" + printState(history[offset]) + "|" + timestep + "|" + peerTimestep);
//				Log.v(TAG, "UNSYCNCH: ");
			}
		}
		else if(peerTimestep == timestep)
		{
			if(hashCode() != hash) {
				Log.v(TAG, "UNSYNCH at current frame: " + timestep + "|" + hash + "!=" + hashCode());
			}
		}
		
		int newActions = d.readInt();
		if(newActions > 0)
		{
			Log.v(TAG, "Got actions from network: " + newActions + "|" + hash + "|" + peerTimestep + " " + timestep);
		}
		if(newActions > 2) {
			Log.v(TAG, "About to crash");
		}
		for(int i=0; i<newActions; i++)
		{
			int timestep = d.readInt();
			if(numInActions < ACTION_MAX) {
				actionInList[numInActions++] = timestep;
			}
		}
		
//		Log.v(TAG, "Median of medians: " + m);
	}

	private int medianForPeer(InetAddress peer)
	{
		if(timeOffsets.size() == 0 || !timeOffsets.containsKey(peer))
		{
			return -1;
		}
		
		final ArrayDeque<Integer> deque = timeOffsets.get(peer);
		
		List<Integer> sorted = new LinkedList<Integer>(deque);
		Collections.sort(sorted);

		Integer median = sorted.get(sorted.size()/2);
		return median.intValue();
	}

	private String printState(int[] s)
	{
		return "XY: " + s[0] +":" + s[1] + " vXY: " + s[2] +":" + s[3] + " rVr: " + s[4] + "|" + s[5] + " h: " + s[6];
	}
	
	private int computeHash(int[] s)
	{
		return s[0] ^ (s[0] >> 6) ^ (s[0] << 4) ^ s[1] ^ (s[1] >> 6) ^ (s[1] << 7) ^ s[2] ^ s[3] ^ s[4] ^ s[5]; 
	}
	
	public int hashCode()
	{
		//return pX ^ (pX >> 6) ^ (pX << 4) ^ pY ^ (pY >> 6) ^ (pY << 7) ^ vX ^ vY ^ r ^ vR; 
		return pX; 
	}
			
	
	public synchronized void serialize(BinaryMessage d) throws IOException {
		d.writeInt(timestep);
		
		d.writeInt(hashCode());
		
		d.writeInt(numOutActions);
		for(int i=0; i<numOutActions; i++)
		{
			d.writeInt(actionOutList[i]);
		}
//		d.writeInt(pX).writeInt(pY).writeInt(vX).writeInt(vY).writeInt(r).writeInt(vR);
//		
//		for(int i=0; i<ENEMY_MAX; i++)
//		{
//			enemies[i].serialize(d);
//		}
//		
//		for(int i=0; i<SHOTS_MAX; i++)
//		{
//			shots[i].serialize(d);
//		}
	}

	public void clicked()
	{
		if(numActions < ACTION_MAX) {
			actionList[numActions++] = timestep;
		}
		
		if(numOutActions < ACTION_MAX) {
			actionOutList[numOutActions++] = timestep;
		}
	}
}
