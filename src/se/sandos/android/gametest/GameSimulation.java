package se.sandos.android.gametest;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
	private static final int SHOTS_ALIVE_TIME = 90;

	private static final int MEDIAN_NUMBER = 23;
	private static final int ENEMY_MAX = 10;
	public static final int SHOTS_MAX = 150;
	private static final int HISTORY_LENGTH = 100;
	
	private int timestep;
	
	private final String TAG = "SIM";
	
	//Player
	private int pX, pY, vX, vY, r, vR;
	//private int[][] history = new int[HISTORY_LENGTH][STATE_SIZE];
	History<State> history = new History<State>(State.class, 10, 40, 5, new State(this));
	
	public static class State {
		public int timestep, pX, pY, vX, vY, r, vR;
		
		public Shot[] shots;
		
		public State(GameSimulation sim) {
			pX        = sim.pX;
			pY        = sim.pY;
			vX        = sim.vX;
			vY        = sim.vY;
			r         = sim.r;
			vR 		  = sim.vR;
			timestep = sim.timestep;
		
			if(sim.shots != null) {
				shots = new Shot[sim.shots.length];
				for(int i=0; i<shots.length; i++) {
					shots[i] = new Shot(sim.shots[i]);
				}
			}
		}
		
		public void export(GameSimulation sim) {
			sim.pX = pX;
			sim.pY = pY;
			sim.vX = vX;
			sim.vY = vY;
			sim.r = r;
			sim.vR = vR;
			sim.timestep = timestep;
			for(int i=0; i<shots.length; i++) {
				sim.shots[i] = shots[i];
			}
		}

		public void inport(GameSimulation sim) {
			pX        = sim.pX;
			pY        = sim.pY;
			vX        = sim.vX;
			vY        = sim.vY;
			r         = sim.r;
			vR 		  = sim.vR;
			timestep = sim.timestep;
		
			if(sim.shots != null) {
				shots = new Shot[sim.shots.length];
				for(int i=0; i<shots.length; i++) {
					shots[i] = new Shot(sim.shots[i]);
				}
			}
		}
		
		public boolean differs(State at) {
			if(at.timestep != timestep) {
				return false;
			}
			
			if(at.hashCode() == hashCode()) {
				return false;
			}
			
			if(pX != at.pX) {
				Log.w("GS", "pX differs: " + pX + "|" + at.pX);
				return true;
			}

			if(pY != at.pY) {
				Log.w("GS", "pY differs: " + pY + "|" + at.pY);
				return true;
			}
			
			if(vY != at.vY) {
				Log.w("GS", "vY differs: " + vY + "|" + at.vY);
				return true;
			}
			
			if(vX != at.vX) {
				Log.w("GS", "vX differs: " + vX + "|" + at.vX);
				return true;
			}
			
			for(int i=0; i<shots.length; i++) {
				if(shots[i].equals(at.shots[i])) {
					continue;
				}
				Log.w("GS", "Differing shot: " + shots[i] + "|" + at.shots[i]);
				return false;
			}

			Log.w("GS", "Differ on sth else");
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pX;
			result = prime * result + pY;
			result = prime * result + r;
			result = prime * result + Arrays.hashCode(shots);
			result = prime * result + timestep;
			result = prime * result + vR;
			result = prime * result + vX;
			result = prime * result + vY;
			return result;
		}

		@Override
		public String toString() {
			return "State [timestep=" + timestep + ", pX=" + pX + ", pY=" + pY
					+ ", vX=" + vX + ", vY=" + vY + ", r=" + r + ", vR=" + vR
					+ ", shots=" + Arrays.toString(shots) + "]";
		}
	}
	
	public static class Enemy {
		int x, y, vX, vY, r, vR;
		boolean alive;
		
		public void serialize(BinaryMessage b) throws IOException
		{
			b.writeInt(x).writeInt(y).writeInt(vX).writeInt(vY).writeInt(r).writeInt(vR);
			b.writeBoolean(alive);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (alive ? 1231 : 1237);
			result = prime * result + r;
			result = prime * result + vR;
			result = prime * result + vX;
			result = prime * result + vY;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Enemy other = (Enemy) obj;
			if (alive != other.alive)
				return false;
			if (r != other.r)
				return false;
			if (vR != other.vR)
				return false;
			if (vX != other.vX)
				return false;
			if (vY != other.vY)
				return false;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}
	}
	
	private Enemy[] enemies = new Enemy[ENEMY_MAX];

	public static class Shot {
		int x, y, vX, vY;
		int aliveCounter;
		boolean alive;
		
		public Shot() {
		}
		
		public Shot(Shot s) {
			x = s.x;
			y = s.y;
			vX = s.vX;
			vY = s.vY;
			aliveCounter = s.aliveCounter;
			alive = s.alive;
		}
		
		public void serialize(BinaryMessage b) throws IOException
		{
			b.writeInt(x).writeInt(y).writeInt(vX).writeInt(vY).writeInt(aliveCounter);
			b.writeBoolean(alive);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (alive ? 1231 : 1237);
			result = prime * result + aliveCounter;
			result = prime * result + vX;
			result = prime * result + vY;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Shot other = (Shot) obj;
			if (alive != other.alive)
				return false;
			if (aliveCounter != other.aliveCounter)
				return false;
			if (vX != other.vX)
				return false;
			if (vY != other.vY)
				return false;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}
	}
	
	private Shot[] shots = new Shot[SHOTS_MAX];
	
	public static final int SHFT = 16;
	public static final float SCALE = 1 << SHFT;
	private static final int MIN_X = -10 << SHFT;
	private static final int MIN_Y = -10 << SHFT;
	private static final int MAX_X = 10 << SHFT;
	private static final int MAX_Y = 10 << SHFT;
	private static final int ACTION_MAX = 86;
	private static final int INPUT_DELAY = 2;
	
	//List for internal handling
	private Action[] actionList = new Action[ACTION_MAX];
	//List for sending on to network
	private Action[] actionOutList = new Action[ACTION_MAX];
	
	//Synch data, never sent out!
	private Map<InetAddress, ArrayDeque<Integer>> timeOffsets = new HashMap <InetAddress, ArrayDeque<Integer>>();
	private List<Integer> medians = new ArrayList<Integer>(10);
	private final List<Integer> sorted = new LinkedList<Integer>();
	private int avgOffset = 0;
	private boolean peers = false;
	//This is used to control speeding up/down of timestep
	private int stepCheckCounter = 0;
	
	private MainActivity act;
	
	private String name;
	private boolean playSound = true;
	
	public static class Action {
		public int timestep = -1;
		public int type;
		public int x, y;
		public boolean applied = false;
		@Override
		public String toString() {
			return "Action [timestep=" + timestep + ", type=" + type + ", x="
					+ x + ", y=" + y + ", applied=" + applied + "]";
		}
	}
	
	//Player input from network
	private Action[] actionInList = new Action[ACTION_MAX*2];
	
	private int highestTimestepSeen;
	private int highestSynchedTimestep;
	
	
	public void silence(boolean a) {
		playSound = !a;
	}
	
	public GameSimulation(MainActivity activity, String name)
	{
		this.name = name;
		
		act = activity;
		
		vX = 14000;
		vY = 11200;
		vR = 70000;
		
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
//		for(int i=0; i<HISTORY_LENGTH; i++) {
//			history[i][HISTORY_TIMESTEP_COL] = -1;
//		}

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
		boolean shot = false;
		int shotVX = 0, shotVY = 0;
		
		//XXX This is sort of all fucked up. We sometimes iterate two times, to reach all actions
		for(int i=0; i<actionList.length; i++) {
			if(actionList[i].timestep <= timestep && actionList[i].timestep != -1) {
				if(actionList[i].applied) {
					continue;
				}
				if(actionList[i].timestep < timestep) {
					Log.v(TAG, "Action not on this timestep, internal: " + actionList[i].timestep + " now: " + timestep + "|" + highestSynchedTimestep + " >>" + name);
					restoreHistory(actionList[i]);
				}
				if(timestep == actionList[i].timestep) {
					Log.v(TAG, "Applied (own) action at timestep " + timestep + "|" + highestSynchedTimestep + " >>" + name);
					if(actionList[i].type == 1) {
						clicked = true;
					} else {
						shot = true;
						shotVX = actionList[i].x;
						shotVY = actionList[i].y;
					}
					actionList[i].applied = true;
				}
			} else {
				//Future, we wait
			}
		}

		//XXX duplicated code
		for(int i=0; i<actionInList.length; i++) {
			if(actionInList[i].timestep <= timestep && actionInList[i].timestep != -1) {
				if(actionInList[i].applied) {
					continue;
				}
				//Only move back in time, only un-apply actions. This is a one-way street
				if(actionInList[i].timestep < timestep) {
					Log.v(TAG, "Action not on this timestep, external: " + actionInList[i].timestep + "|" + i + " now: " + timestep + "|" + highestSynchedTimestep + " >>" + name);
					restoreHistory(actionInList[i]);
				}
				if(timestep == actionInList[i].timestep) {
					Log.v(TAG, "Applied action at timestep " + timestep + "|" + highestSynchedTimestep + " >>" + name);
					
					if(actionInList[i].type == 1) {
						clicked = true;
					} else {
						shot = true;
						shotVX = actionInList[i].x;
						shotVY = actionInList[i].y;
					}
					actionInList[i].applied = true;
				}
			} else {
				//In the future, we wait
			}
		}

		moveHistory();
		oneStep(clicked, shot, shotVX, shotVY);
		if(highestTimestepSeen < timestep) {
			highestTimestepSeen = timestep;
		}
	}

	private void oneStep(boolean clicked, boolean shot, int shotVX, int shotVY) {
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
			vX = (vX * 60000) >> SHFT;
 		} 
		
		if(pY >= MAX_Y || pY <= MIN_Y) {
			pY -= vY;
			vY = -vY;
			bounce = true;
			vY = (vY * 60000) >> SHFT;
		} 

		if(bounce && avgOffset > -100 && avgOffset < 10 && playSound) {
			act.soundPool.play(act.clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
		}
		
		if(shot) {
			int free = -1;
			for(int i=0; i<SHOTS_MAX; i++) {
				if(!shots[i].alive) {
					free = i;
				}
			}
			if(free != -1) {
				shots[free].alive = true;
				shots[free].x = pX;
				shots[free].y = pY;
				shots[free].vX = -shotVX;
				shots[free].vY = -shotVY;
				shots[free].aliveCounter = 0;
			} else {
				Log.v(TAG, "Shot memory is full now: " + timestep + "|" + highestSynchedTimestep + " >>" + name);
			}
		}
		
		for(int i=0; i<shots.length; i++) {
			if(shots[i].alive) {
				shots[i].x += shots[i].vX;
				shots[i].y += shots[i].vY;
				if(shots[i].aliveCounter++ > SHOTS_ALIVE_TIME) {
					shots[i].alive = false;
				}
			}
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
			State rewind = history.rewind(a.timestep);
			Log.v(TAG, "Rewound to " + a.timestep + " from " + timestep + ", history says we are at " + rewind.timestep + " >>" + name);
			int oldTS = timestep;
			rewind.export(this);
			timestep = oldTS;

			//XXX We just pray that we never end up in the "rewound" part of history, this might contain too old entries
			//We should really just add a timestep field to the history itself
			
			if(a.timestep < highestSynchedTimestep) {
				Log.v(TAG, "WOOOAH! Why did we rewind to BEFORE a verified point in time ?!?!!?");
			}
			if(rewind.timestep != a.timestep) {
				Log.v(TAG, "AAAAAAGH! We died!");
			}

			a.applied = true;
			//XXX - We need to "un-apply" all the pending actions that we went past going backwards in time
			//so they get applied when we start stepping again
			for(int i=0; i<actionList.length; i++) {
				Action r = actionList[i];
				if(r.applied) {
					if(r.timestep <= timestep && r.timestep >= a.timestep) {
						Log.v(TAG, "Unapplied action at " + r.timestep + " now: " + timestep + "|" + highestSynchedTimestep + " >>" + name);
						r.applied = false;
					}
				}
			}
			for(int i=0; i<actionInList.length; i++) {
				Action r = actionInList[i];
				if(r.applied) {
					if(r.timestep <= timestep && r.timestep >= a.timestep) {
						Log.v(TAG, "Unapplied in-action at " + r.timestep + " now: " + timestep + "|" + highestSynchedTimestep + " >>" + name);
						r.applied = false;
					}
				}
			}
			
			//We want to move one step ahead of our last step (as is normal) in this iteration, before returning from step()
			highestTimestepSeen++;
			//timestep = a.timestep;
			rewind.export(this);
			
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
		State state = history.getDeleted();
		
		if(state == null) {
			state = new State(this);
		} else {
			state.inport(this);
		}
		
		history.addNewRecord(state, timestep);
	}
	
	public synchronized void step()
	{	
		stepCheckCounter++;

		if(peers && stepCheckCounter % 13 == 1)
		{
			if(avgOffset < -10) {
				actualStep();
			}
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
		
		//Correct for rewinding history
		while(highestTimestepSeen > timestep) {
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
			medians.add(medianForPeer(p));
		}
		Collections.sort(medians);
		avgOffset = medians.get(medians.size()/2);
		
		int hash = d.readInt();
		
		int newActions = d.readInt();
		cleanIncomingActionLists();
		if(newActions > 0)
		{
//			Log.v(TAG, "Got actions from network: " + newActions + "|" + hash + "|" + peerTimestep + " " + timestep);
			for(int i=0; i<newActions; i++)
			{
				int ts = d.readInt();
				int type = d.readInt();
				int x = d.readInt();
				int y = d.readInt();
				int freeSlot = findUnusedslot(actionInList, ts);
				if(freeSlot != -1) {
					if(actionInList[freeSlot].applied == true && actionInList[freeSlot].timestep == ts) {
						//XXX Conflict resolution here, if there is NEW input, we need to unset applied. 
						//If its identical, just keep on going on
					} else {
						Log.v(TAG, "Adding new external action at " + ts + "|" + peerTimestep + " now: " + timestep + "|" + highestSynchedTimestep + " >" + name);
						actionInList[freeSlot].timestep = ts;
						actionInList[freeSlot].type = type;
						actionInList[freeSlot].x = x;
						actionInList[freeSlot].y = y;
						actionInList[freeSlot].applied = false;
					}
				} else {
					Log.v(TAG, "Discarded input, we will desynch now: " + timestep + "|" + highestSynchedTimestep + " >>" + name);
				}
			}
		}
		
		int peerX = d.readInt();
		int peerY = d.readInt();
		int peervX = d.readInt();
		int peervY = d.readInt();

		if(peerTimestep < timestep) {
//			int offset = timestep - peerTimestep;
//			offset--;
//			if(offset < HISTORY_LENGTH && hash != history[offset][6] && history[offset][7] != -1) {
//				//Log.v(TAG, "SyncHIST: " + peerTimestep + "|" + printState(history[offset]) + " [" + peer.getHostAddress() + "] " + highestSynchedTimestep);
//				Log.v(TAG, "Synchist: " + peerX +":" + history[offset][0] + "|" + peerY +":" + history[offset][1] + "|" + peervX + ":" + history[offset][2] + "|" + peervY +":" + history[offset][3] + " ts:" +history[offset][HISTORY_TIMESTEP_COL] + " now: " + timestep + "|" + highestSynchedTimestep + " >>" + name);
//			} else {
//				if(highestSynchedTimestep < peerTimestep) {
//					highestSynchedTimestep = peerTimestep;
//				}
//			}
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
	}

	private void cleanIncomingActionLists() {
		for(int i=0;i<actionInList.length; i++) {
			if((timestep - actionInList[i].timestep) > HISTORY_LENGTH*1.5) {
				//Clean this item
				actionInList[i].timestep = -1;
			}
		}
		for(int i=0;i<actionList.length; i++) {
			if((timestep - actionList[i].timestep) > HISTORY_LENGTH*1.5) {
				//Clean this item
				actionList[i].timestep = -1;
			}
		}
	}

	private int medianForPeer(InetAddress peer)
	{
		if(timeOffsets.size() == 0 || !timeOffsets.containsKey(peer))
		{
			return -1;
		}
		
		final ArrayDeque<Integer> deque = timeOffsets.get(peer);
		
		sorted.clear();
		sorted.addAll(deque);
		Collections.sort(sorted);

		Integer median = sorted.get(sorted.size()/2);
		return median.intValue();
	}

	public int hashCode()
	{
		return pX ^ pY ^ vX ^ vY ^ r ^ vR ^ Arrays.deepHashCode(shots) ^ Arrays.hashCode(enemies);
	}
	
	public synchronized void serialize(BinaryMessage d) throws IOException {
		d.writeInt(timestep);
		
		d.writeInt(hashCode());

		cleanActionOutList();
		int actionCount = countActions(actionOutList);
		d.writeInt(actionCount);
		for(int i=0; i<actionOutList.length; i++)
		{
			if(actionOutList[i].timestep != -1) {
				d.writeInt(actionOutList[i].timestep);
				d.writeInt(actionOutList[i].type);
				d.writeInt(actionOutList[i].x);
				d.writeInt(actionOutList[i].y);
			}
		}
		
		d.writeInt(pX);
		d.writeInt(pY);
		d.writeInt(vX);
		d.writeInt(vY);
		
	}

	private void cleanActionOutList() {
		//Clean old items from actionOutList
		for(int i=0; i<actionOutList.length; i++)
		{
			if(actionOutList[i].timestep != -1) {
				//We want this to be lower than for cleaning incoming items, otherwise the "client"/"receiver" will forget
				//actions and try to re-play them
				if(timestep - actionOutList[i].timestep > 10) {
					actionOutList[i].timestep = -1;
//					Log.v(TAG, "Clearing old out-action at index " + i + "|" + timestep);
				}
			}
		}
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
		clicked(0.0f, 0.0f);
	}
	
	public void clicked(float x, float y)
	{
		int targetTS = timestep+1+INPUT_DELAY;
		int freeSlot = findUnusedslot(actionList, targetTS);
		int outfreeSlot = findUnusedslot(actionOutList, targetTS);

		cleanIncomingActionLists();
		cleanActionOutList();
		if(freeSlot != -1 && outfreeSlot != -1) {
			actionList[freeSlot].timestep = targetTS;
			actionList[freeSlot].type     = 1;
			actionList[freeSlot].applied  = false;
			
			Log.v(TAG, "Generated action at " + targetTS + " now:" + timestep + "|" + highestSynchedTimestep + " >" + name);
			actionOutList[outfreeSlot].timestep = targetTS;
			actionOutList[outfreeSlot].type     = 1;
			actionOutList[outfreeSlot].applied  = false;
			
			if((x < -0.0001 || x > 0.0001f) && (y < -0.0001f || y > 0.0001f)) {
				actionList[freeSlot].type = 2;
				actionList[freeSlot].x = (int) ((x-0.5f)*SCALE);
				actionList[freeSlot].y = (int) ((y-0.5f)*SCALE);
				actionOutList[outfreeSlot].type = 2;
				actionOutList[outfreeSlot].x = (int) ((x-0.5f)*SCALE);
				actionOutList[outfreeSlot].y = (int) ((y-0.5f)*SCALE);
			}
		}
		else
		{
			Log.v(TAG, "Discarding click: " + freeSlot + "|" + outfreeSlot);
		}
	}

	public int timestep() {
		return timestep;
	}
	
	@Override
	public String toString() {
		return "GameSimulation [timestep=" + timestep
				+ ", pX=" + pX + ", pY=" + pY + ", vX=" + vX + ", vY=" + vY
				+ ", r=" + r + ", vR=" + vR + ", name=" + name
				+ ", highestTimestepSeen=" + highestTimestepSeen
				+ ", highestSynchedTimestep=" + highestSynchedTimestep + "]";
	}
	
	public int compareHistory(GameSimulation gs, int age) {
		for(int i=timestep-1-age; i>=history.oldestHistory(); i--) {
			State at = history.getAt(i);

			State at2 = gs.history.getAt(i);
			if(at2 != null && at != null && at.differs(at2)) {
				return i;
			}
		}
		return -1;
	}
	
	public Shot[] getShots()
	{
		return shots;
	}
}
