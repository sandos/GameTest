package se.sandos.android.gametest.test;

import junit.framework.Assert;
import junit.framework.TestCase;
import se.sandos.android.gametest.History;

public class HistoryTest extends TestCase {
	public void testFill() {
		History<int[]> h = new History<int[]>(int[].class, 5, 2, 3, new int[1]);
		
		checkAge(-1, 0, h);
		
		addCheck(1, 1, 1, h);
		addCheck(1, 2, 2, h);
		addCheck(1, 3, 3, h);
		addCheck(1, 4, 4, h);
		addCheck(1, 5, 5, h);
		addCheck(1, 6, 6, h);
		addCheck(1, 7, 6, h);
		addCheck(1, 8, 6, h);
		addCheck(1, 9, 7, h);
		addCheck(1, 10, 7, h);
		addCheck(1, 11, 7, h);
		addCheck(4, 12, 7, h);
		addCheck(4, 13, 7, h);
		addCheck(4, 14, 7, h);
		addCheck(7, 15, 7, h);
		addCheck(7, 16, 7, h);
		addCheck(7, 17, 7, h);
	}

	public void testRewind() {
		History<int[]> h = new History<int[]>(int[].class, 5, 2, 3, new int[1]);
		
		checkAge(-1, 0, h);
		
		addCheck(1, 1, 1, h);
		addCheck(1, 2, 2, h);
		addCheck(1, 3, 3, h);
		addCheck(1, 4, 4, h);
		
		int[] rewind = h.rewind(2);
		Assert.assertEquals(2, rewind[0]);
	}
	
	public void testRewindLow() {
		History<int[]> h = new History<int[]>(int[].class, 5, 2, 3, new int[1]);
		
		checkAge(-1, 0, h);
		
		addCheck(1, 1, 1, h);
		addCheck(1, 2, 2, h);
		addCheck(1, 3, 3, h);
		addCheck(1, 4, 4, h);
		addCheck(1, 5, 5, h);
		addCheck(1, 6, 6, h);
		addCheck(1, 7, 6, h);
		addCheck(1, 8, 6, h);
		addCheck(1, 9, 7, h);
		
		int[] rewind = h.rewind(4);
		Assert.assertEquals(4, rewind[0]);
		
		rewind = h.rewind(3);
		Assert.assertEquals(1, rewind[0]);

		rewind = h.rewind(1);
		Assert.assertNull(rewind);
	}
	
	public void testGetAt() {
		History<int[]> h = new History<int[]>(int[].class, 5, 30, 5, new int[1]);
		
		int timestamp = 1;
		for(int i=0; i<500; i++) {
			int[] t = new int[1];
			t[0] = timestamp;
			h.addNewRecord(t, timestamp++);
		}
		
		int[] at = h.getAt(timestamp-1);
		Assert.assertNotNull(at);
		Assert.assertEquals(timestamp-1, at[0]);
		
		at = h.getAt(timestamp-2);
		Assert.assertNotNull(at);
		Assert.assertEquals(timestamp-2, at[0]);

		at = h.getAt(timestamp-3);
		Assert.assertNotNull(at);
		Assert.assertEquals(timestamp-3, at[0]);

		at = h.getAt(timestamp-4);
		Assert.assertNotNull(at);
		Assert.assertEquals(timestamp-4, at[0]);
		
		at = h.getAt(timestamp-5);
		Assert.assertNotNull(at);
		Assert.assertEquals(timestamp-5, at[0]);

		at = h.getAt(timestamp-6);
		Assert.assertNull(at);

		at = h.getAt(timestamp-7);
		Assert.assertNull(at);

		at = h.getAt(timestamp-8);
		Assert.assertNull(at);

		at = h.getAt(timestamp-9);
		Assert.assertNull(at);

		at = h.getAt(timestamp-10);
		Assert.assertNotNull(at);
		Assert.assertEquals(timestamp-10, at[0]);
	}
	
	public void testPerformance() {
		History<int[]> h = new History<int[]>(int[].class, 5, 30, 5, new int[1]);
		int timestamp = 1;
		int counter = 1;
		for(int i=0; i<500000; i++) {
			int[] t = new int[1];
			t[0] = timestamp;
			h.addNewRecord(t, timestamp);
			if((counter % 31) == 0) {
				if((counter % 124) == 0) {
					int[] rewind = h.rewind(timestamp - 7);
					Assert.assertTrue(timestamp - 7 >= rewind[0]);
					timestamp -= 7;
				} else {
					int[] rewind = h.rewind(timestamp - 3);
					Assert.assertTrue(timestamp - 3 >= rewind[0]);
					timestamp -= 3;
				}
			}
			timestamp++;
			counter++;
		}
	}
	
	private void addCheck(int age, int ts, int size, History<int[]> h) {
		int[] t = new int[1];
		t[0] = ts;
		h.addNewRecord(t, ts);
		checkAge(age, size, h);
	}
	
	private void checkAge(int age, int size, History<?> h) {
		Assert.assertEquals("Oldest history age was incorrect", age, h.oldestHistory());
		Assert.assertEquals("Size was incorrect", size, h.size());
	}
}
