package se.sandos.android.gametest.test;

import junit.framework.Assert;
import junit.framework.TestCase;
import se.sandos.android.gametest.History;

public class HistoryTest extends TestCase {
	public void testFill() {
		History<int[]> h = new History<int[]>(int[].class, 5, 2, 3);
		
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

	private void addCheck(int age, int ts, int size, History<int[]> h) {
		h.addNewRecord(new int[1], ts);
		checkAge(age, size, h);
	}
	
	private void checkAge(int age, int size, History<?> h) {
		Assert.assertEquals("Oldest history age was incorrect", age, h.oldestHistory());
		Assert.assertEquals("Size was incorrect", size, h.size());
	}
}
