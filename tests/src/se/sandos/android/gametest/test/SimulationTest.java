package se.sandos.android.gametest.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Random;

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
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
		BinaryMessage binaryMessage = new BinaryMessage();
		
 		for(int x=0; x<ITERATIONS; x++) {
			sim1.clicked();

			stepSynched(sim1, sim2, binaryMessage);
		}
	}

	public void testDuplex2Peers() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
		BinaryMessage binaryMessage = new BinaryMessage();
		
 		for(int x=0; x<ITERATIONS; x++) {
			if(sim1.timestep() % 10 == 3) {
				sim1.clicked();
			}
			if(sim1.timestep() % 10 == 4) {
				sim2.clicked();
			}

			stepSynched(sim1, sim2, binaryMessage);
		}
	}
	
	public void testDelaySingleClick() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
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
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
 		for(int x=0; x<ITERATIONS; x++) {
 			if(x % 8 == 2) {
 				sim1.clicked();
 			}
 			
 			Assert.assertTrue(sim2.compareHistory(sim1, 10));
 			
 			try {
 				step(sim1, sim2, 5, x > 100, 1.0f);
 			} catch(RuntimeException e) {
 				Log.d("test", "Failed at " + x);
 				throw e;
 			}
 		}
	}
	
	public void testDelayLossComplexSimplex2Peers() throws UnknownHostException, IOException
	{
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
 		for(int x=0; x<ITERATIONS; x++) {
 			if(x % 3 == 2 && x < 300) {
 				sim1.clicked();
 			}
 			
 			Assert.assertTrue(sim2.compareHistory(sim1, 10));
 			
 			if(x > ITERATIONS*0.9) {
 				Assert.assertTrue("Simulation drift: " + sim1.timestep() + "|" + sim2.timestep(), Math.abs(sim1.timestep() - sim2.timestep()) < 5);
 			}
 			
 			try {
 				step(sim1, sim2, 5, x > 500, 0.5f);
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
					Assert.assertEquals(sim1.hashCode(), sim2.hashCode());
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
	private void stepSynched(GameSimulation sim1, GameSimulation sim2, BinaryMessage binaryMessage) throws IOException,
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
		Assert.assertEquals(sim1.hashCode(), sim2.hashCode());

		sim1.step();
		sim2.step();
	}
}
