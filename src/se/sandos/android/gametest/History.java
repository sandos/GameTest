package se.sandos.android.gametest;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * History class with two different density arrays
 * 
 * @author sandos
 * 
 * @param <T>
 */
public class History<T> {
	private Queue<T> hQ;
	private int hSize;
	private int stride;
	
	private Queue<T> lQ;
	private int lSize;
	private int lTimestamp = -1;

	// Timestamp at end
	private int timestamp = -1;

	public History(Class<T> clz, int highDensitySize, int lowDensitySize,
			int lowDensityStride) {
		stride = lowDensityStride;
		hQ = new ArrayDeque<T>();
		lQ = new ArrayDeque<T>();
		
		hSize = highDensitySize;
		lSize = lowDensitySize;
	}

	public int size() {
		return hQ.size() + lQ.size();
	}

	public void addNewRecord(T t, int ts) {
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
				if(newTS == lTimestamp + stride) {
					lQ.offer(removed);
					lTimestamp = newTS;
					if(lQ.size() > lSize) {
						lQ.poll();
					}
				}
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
}
