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
	private static final int HISTORY_LENGTH = 100;
	private static final int STATE_SIZE = 8;
	
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
	private static final int ACTION_MAX = 40;
	private static final int INPUT_DELAY = 10;
	
	//List for internal handling
	private Action[] actionList = new Action[ACTION_MAX];
	//List for sending on to network
	private Action[] actionOutList = new Action[ACTION_MAX];
	
	//Synch data, never sent out!
	private Map<InetAddress, ArrayDeque<Integer>> timeOffsets = new HashMap <InetAddress, ArrayDeque<Integer>>();
	private List<Integer> medians = new ArrayList<Integer>(10);
	private int avgOffset = 0;
	private boolean peers = false;
	//This is used to control speeding up/down of timestep
	private int stepCheckCounter = 0;
	
	private MainActivity act;

	class Action {
		public int timestep = -1;
		public int type;
		public boolean applied = false;
	}
	
	//Player input from network
	private Action[] actionInList = new Action[ACTION_MAX+10];
	
	private int highestTimestepSeen;
	private int highestSynchedTimestep;
	
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
		for(int i=0; i<ACTION_MAX; i++)
		{
			actionOutList[i] = new Action();
			actionList[i] = new Action();
		}
		for(int i=0; i<actionInList.length; i++)
		{
			actionInList[i] = new Action();
		}

	}
	
	public float r()
	{
		return r / SCALE;
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
		boolean clicked = false;
		for(int i=0; i<actionList.length; i++) {
			if(actionList[i].timestep <= timestep && actionList[i].timestep != -1) {
				if(actionList[i].applied) {
					continue;
				}
				if(actionList[i].timestep < timestep) {
					Log.v(TAG, "Action not on this timestep, internal: " + actionList[i].timestep + "|" + timestep);
					restoreHistory(actionList[i]);
				}
				if(timestep == actionList[i].timestep) {
					clicked = true;
					actionList[i].applied = true;
				}
			} else {
				//Future, we wait
			}
		}

		for(int i=0; i<actionInList.length; i++) {
			if(actionInList[i].timestep <= timestep && actionInList[i].timestep != -1) {
				if(actionInList[i].applied) {
					continue;
				}
				if(actionInList[i].timestep < timestep) {
					Log.v(TAG, "Action not on this timestep, external: " + actionInList[i].timestep + "|" + timestep + "|" + i);
					restoreHistory(actionInList[i]);
				}
				if(timestep == actionInList[i].timestep) {
					Log.v(TAG, "Applied action at timestep " + timestep);
					clicked = true;
					actionInList[i].applied = true;
				}
			} else {
				//In the future, we wait
			}
		}

		if(highestTimestepSeen < timestep) {
			highestTimestepSeen = timestep;
		}
		moveHistory();
		oneStep(clicked);
	}

	private void oneStep(boolean clicked) {
		boolean bounce = false;
		
		pX += vX;
		pY += vY;
		
		if(pX >= MAX_X || pX <= MIN_X || clicked) {
			pX -= vX;
			vX = -vX;
			bounce = true;
			clicked = false;
			if(clicked) {
				Log.v(TAG, "User action taken here " + timestep);
			}
		} 
		
		if(pY >= MAX_Y || pY <= MIN_Y) {
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

	private boolean restoreHistory(Action a)
	{
		int offset = timestep - a.timestep;
		offset--;
		if(offset >= 0 && offset < HISTORY_LENGTH)
		{
			pX = history[offset][0];
			pY = history[offset][1];
			vX = history[offset][2];
			vY = history[offset][3];
			r  = history[offset][4];
			vR = history[offset][5];
			int ts = history[offset][7];
			
			//Move history itself
			for(int i=0; i<HISTORY_LENGTH-offset-1; i++)
			{
				history[i] = history[i+offset+1];
			}
			//XXX We just pray that we never end up in the "rewound" part of history, this might contain too old entries
			//We should really just add a timestep field to the history itself
			
			Log.v(TAG, "Rewound to " + a.timestep + " from " + timestep + ", history says we are at " + ts);
			if(a.timestep < highestSynchedTimestep) {
				Log.v(TAG, "WOOOAH! Why did we rewind to BEFORE a verified point in time ?!?!!?");
			}
			if(ts != a.timestep) {
				Log.v(TAG, "AAAAAAGH! We died!");
			}

			timestep = a.timestep;
			a.applied = true;
			//XXX - We need to "un-apply" all the pending actions that we went past going backwards in time
			//so they get applied when we start stepping again

			if(highestTimestepSeen > timestep) {
				highestTimestepSeen = timestep;
			}
			
			return true;
		}
		else
		{
			Log.v(TAG, "Tried restoring history beyond limits: " + a.timestep + "|" + timestep);
			a.timestep = -1;
		}
		
		return false;
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
		history[0][7] = timestep;
	}
	
	public synchronized void step()
	{	
		stepCheckCounter++;

		if(peers && stepCheckCounter % 13 == 1)
		{
			if(avgOffset < -1) {
//				Log.v(TAG, "Running late, small step " + avgOffset);
				actualStep();
			} else if(avgOffset > 10) {
//				Log.v(TAG, "Skipping " + avgOffset);
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
		
		int newActions = d.readInt();
		if(newActions > 0)
		{
//			Log.v(TAG, "Got actions from network: " + newActions + "|" + hash + "|" + peerTimestep + " " + timestep);
			for(int i=0;i<actionInList.length; i++)
			{
				if((timestep - actionInList[i].timestep) > HISTORY_LENGTH*2)
				{
					//Clean this item
					actionInList[i].timestep = -1;
				}
			}
			for(int i=0; i<newActions; i++)
			{
				int ts = d.readInt();
				int type = d.readInt();
				int freeSlot = findUnusedslot(actionInList, ts);
				if(freeSlot != -1) {
					if(actionInList[freeSlot].applied == true && actionInList[freeSlot].timestep == ts) {
						//XXX Conflict resolution here, if there is NEW input, we need to unset applied. 
						//If its identical, just keep on going on
					} else {
						Log.v(TAG, "Adding new external action: " + peerTimestep + "|" + ts + "|[" + highestSynchedTimestep + "|" + timestep);
						actionInList[freeSlot].timestep = ts;
						actionInList[freeSlot].type = type;
						actionInList[freeSlot].applied = false;
					}
				} else {
					Log.v(TAG, "Discarded input, we will desynch");
				}
			}
		}
		
		int peerX = d.readInt();
		int peerY = d.readInt();
		int peervX = d.readInt();
		int peervY = d.readInt();

		if(peerTimestep < timestep) {
			int offset = timestep - peerTimestep;
			offset--;
			if(offset < HISTORY_LENGTH && hash != history[offset][6]) {
				Log.v(TAG, "SyncHIST: " + peerTimestep + "|" + printState(history[offset]) + " [" + peer.getHostAddress() + "] " + highestSynchedTimestep);
				Log.v(TAG, "Peervals: " + peerX +":" + history[offset][0] + "|" + peerY +":" + history[offset][1] + "|" + peervX + ":" + history[offset][2] + "|" + peervY +":" + history[offset][2]);
			} else {
				if(highestSynchedTimestep < peerTimestep) {
					highestSynchedTimestep = peerTimestep;
				}
			}
		}
		else if(peerTimestep == timestep)
		{
			if(hashCode() != hash) {
				Log.v(TAG, "UNSYNCH at current frame: " + timestep + "|" + hash + "!=" + hashCode() + " [" + peer.getHostAddress() + "] " + highestSynchedTimestep);
				Log.v(TAG, "Peervals: " + peerX +":" + pX + "|" + peerY +":" + pY + "|" + vX + ":" + peervX + "|" + peervY +"|" + vY);
			} else {
				if(highestSynchedTimestep < timestep) {
					highestSynchedTimestep = timestep;
				}
			}
		}

//		Log.v(TAG, "Handled message " + peerTimestep + " " + hash);
		
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
		return pX ^ (pX >> 6) ^ (pX << 4) ^ pY ^ (pY >> 6) ^ (pY << 7) ^ vX ^ vY ^ r ^ vR;
	}
	
	public synchronized void serialize(BinaryMessage d) throws IOException {
		d.writeInt(timestep);
		
		d.writeInt(hashCode());

		//Clean old items from actionOutList
		for(int i=0; i<actionOutList.length; i++)
		{
			if(actionOutList[i].timestep != -1) {
				//We want this to be lower than for cleaning incoming items, otherwise the "client"/"receiver" will forget
				//actions and try to re-play them
				if(timestep - actionOutList[i].timestep > HISTORY_LENGTH) {
					actionOutList[i].timestep = -1;
//					Log.v(TAG, "Clearing old out-action at index " + i + "|" + timestep);
				}
			}
		}
		int actionCount = countActions(actionOutList);
		d.writeInt(actionCount);
		for(int i=0; i<actionOutList.length; i++)
		{
			if(actionOutList[i].timestep != -1) {
//				Log.v(TAG, "Writing action to network: " + actionOutList[i].timestep + "|" + timestep + "| Index: " + i);
				d.writeInt(actionOutList[i].timestep);
				d.writeInt(actionOutList[i].type);
			}
		}
		
		d.writeInt(pX);
		d.writeInt(pY);
		d.writeInt(vX);
		d.writeInt(vY);
		
		//numOutActions = 0;
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

	private int countActions(Action[] list) {
		int count = 0;
		for(int i=0; i<list.length; i++)
		{
			if(list[i].timestep != -1) {
				count++;
			}
		}
		return count;
	}

	private int findAction(Action[] list, int t)
	{
		for(int i=0; i<list.length; i++) {
			if(list[i].timestep == t) {
				return i;
			}
		}
		
		return -1;
	}
	
	private int findUnusedslot(Action[] list, int timestep)
	{
		int found = findAction(list, timestep);
		
		if(found != -1) {
			return found;
		}
		
		for(int i=0; i<list.length; i++)
		{
			if(list[i].timestep == -1) {
				return i;
			}
		}
		
		return -1;
	}
	
	public void clicked()
	{
		int targetTS = timestep+1+INPUT_DELAY;
		int freeSlot = findUnusedslot(actionList, targetTS);
		
		if(freeSlot != -1) {
			Log.v(TAG, "Got new internal action " + targetTS + "|" + timestep);
			actionList[freeSlot].timestep = targetTS;
			actionList[freeSlot].type     = 1;
			actionList[freeSlot].applied  = false;
		}
		
		freeSlot = findUnusedslot(actionOutList, targetTS);
		if(freeSlot != -1) {
			Log.v(TAG, "Got new external action " + targetTS + "|" + timestep + "|" + highestSynchedTimestep);
			actionOutList[freeSlot].timestep = targetTS;
			actionOutList[freeSlot].type     = 1;
			actionOutList[freeSlot].applied  = false;
		}
	}

	public int timestep() {
		return timestep;
	}
}
