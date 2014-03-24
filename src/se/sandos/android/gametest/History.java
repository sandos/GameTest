package se.sandos.android.gametest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import android.util.Log;

/**
 * History class with two different density arrays
 * 
 * @author sandos
 * 
 * @param <T>
 */
public class History<T> {
	private static final int DEL_SIZE = 100;
	private Deque<T> hQ;
	private int hSize;
	private int stride;
	
	private Deque<T> lQ;
	private int lSize;
	private int lTimestamp = -1;

	private Deque<T> deleted;
	
	// Timestamp at end
	private int timestamp = -1;

	private T empty;
	
	public History(Class<T> clz, int highDensitySize, int lowDensitySize,
			int lowDensityStride, T empty) {
		stride = lowDensityStride;
		hQ = new ArrayDeque<T>(highDensitySize);
		lQ = new ArrayDeque<T>(lowDensitySize);
		
		hSize = highDensitySize;
		lSize = lowDensitySize;
		this.empty = empty;
		deleted = new ArrayDeque<T>(DEL_SIZE);
	}

	public T getDeleted() {
		if(deleted != null && !deleted.isEmpty()) {
			while(deleted.size() > DEL_SIZE) {
				deleted.poll();
			}
			return deleted.poll();
		}
		return null;
	}
	
	public int size() {
		return hQ.size() + lQ.size();
	}

	public int getTimestamp() {
		return timestamp;
	}
	
	public void addNewRecord(T t, int ts) {
		if(ts <= timestamp) {
			Log.w("History", "Adding over old data");
			return;
		}
		//Add null data...
		while(timestamp != -1 && timestamp != ts-1) {
			hQ.offer(empty);
			timestamp++;
		}
		hQ.offer(t);
		timestamp = ts;
		if(hQ.size() > hSize) {
			T removed = hQ.poll();
			if(lTimestamp == -1) {
				//Empty low-density, just add
				lQ.offer(removed);
				lTimestamp = ts - hSize;
			} else {
				int newTS = ts - hQ.size();
				while(newTS > lTimestamp + stride) {
					lQ.offer(empty);
					lTimestamp += stride;
				}
				if(newTS == lTimestamp + stride) {
					lQ.offer(removed);
					lTimestamp = newTS;
					if(lQ.size() > lSize) {
						lQ.poll();
					}
				} else {
					deleted.offer(removed);
				}
			}
			//Clean out any empties
			while(hQ.peek() == empty) {
				hQ.poll();
			}
		}
	}
	
	public boolean isEmpty() {
		return size() == 0;
	}
	
	public int oldestHistory() {
		if(lTimestamp != -1) {
			return lTimestamp - (lQ.size()-1)*stride;
		} else {
			if(!hQ.isEmpty() && timestamp != -1) {
				return timestamp - (hQ.size()-1);
			} else {
				return -1;
			}
		}
	}
	
	public T rewind(int ts) {
		if(oldestHistory() > ts || (timestamp != -1 && ts > timestamp)) {
			return null;
		}

		//Just clean deleted list a bit here
		while(deleted.size() > DEL_SIZE) {
			deleted.poll();
		}

		//We do a "simple" rollback, we never re-mix low-density and high-density queues again:
		//It is simply not possible to recreate the high-density data from low-density data
		//We simply "remove" the data that is too new
		if(timestamp == -1 || timestamp - hQ.size() >= ts) {
			hQ.clear();
			timestamp = -1;
			
			if(lQ.isEmpty()) {
				lTimestamp = -1;
				return null;
			}
			
			while(lTimestamp > ts) {
				deleted.offer(lQ.pollLast());
				lTimestamp -= stride;
			}
			lTimestamp -= stride;
			return lQ.pollLast();
		} else {
			while(timestamp > ts && !hQ.isEmpty()) {
				deleted.offer(hQ.pollLast());
				timestamp--;
			}
			
			while(hQ.peekLast() == empty) {
				timestamp--;
				deleted.offer(hQ.pollLast());
			}
			T r = hQ.pollLast();
			timestamp--;
			if(hQ.size() == 0) {
				timestamp = -1;
			}
			return r;
		}
	}
	
	public T getAt(int timestamp) {
		if(timestamp > this.timestamp || timestamp < oldestHistory()) {
			return null;
		}
		
		if(timestamp > lTimestamp && timestamp >= this.timestamp-hQ.size()+1) {
			int ts = this.timestamp - hQ.size() + 1;
			Iterator<T> it;
			for(it = hQ.iterator(); ts<timestamp && it.hasNext(); it.next()) {
				ts++;
			}
			return it.next();
		} else {
			if(timestamp > lTimestamp) {
				return null;
			}
			int ts = lTimestamp - (lQ.size()-1)*stride;
			Iterator<T> it;
			for(it = lQ.iterator(); ts<timestamp && it.hasNext(); it.next()) {
				ts += stride;
			}
			if(ts == timestamp) {
				return it.next();
			} else {
				return null;
			}
		}
	}

	@Override
	public String toString() {
		return "History [hQ=" + hQ + ", hSize=" + hSize + ", stride=" + stride
				+ ", lQ=" + lQ + ", lSize=" + lSize + ", lTimestamp="
				+ lTimestamp + ", deleted=" + deleted + ", timestamp="
				+ timestamp + ", empty=" + empty + ", latest element=" + hQ.peekLast() + "]";
	}
	
	
}
