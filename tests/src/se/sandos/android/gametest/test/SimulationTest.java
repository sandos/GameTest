package se.sandos.android.gametest.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import junit.framework.Assert;
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
	}
	
	public SimulationTest()
	{
		super(MainActivity.class);
	}
	
	public SimulationTest(Class<MainActivity> activityClass) {
		super(activityClass);
	}

	public void testSimplexSuccess() throws IOException
	{
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
		BinaryMessage binaryMessage = new BinaryMessage();

		
		for(int x=0; x<ITERATIONS; x++) {
			if(sim1.timestep() < 113) {
				sim1.clicked();
			}

			stepSynched(sim1, sim2, binaryMessage);
		}
	}

	public void testSimplexFail() throws IOException
	{
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
		BinaryMessage binaryMessage = new BinaryMessage();
		
 		for(int x=0; x<ITERATIONS; x++) {
			if(sim1.timestep() < 114) {
				sim1.clicked();
			}

			stepSynched(sim1, sim2, binaryMessage);
		}
	}

	public void testDuplexSuccess() throws IOException
	{
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
		BinaryMessage binaryMessage = new BinaryMessage();
		
 		for(int x=0; x<ITERATIONS; x++) {
			if(sim1.timestep() < 403) {
				if(sim1.timestep() % 10 == 3) {
					sim1.clicked();
				}
				if(sim1.timestep() % 10 == 4) {
					sim2.clicked();
				}
			}

			stepSynched(sim1, sim2, binaryMessage);
		}
	}

	public void testDuplexFail() throws Throwable
	{
		GameSimulation sim1 = new GameSimulation(act);
		GameSimulation sim2 = new GameSimulation(act);
		
		BinaryMessage binaryMessage = new BinaryMessage();
		
 		for(int x=0; x<ITERATIONS; x++) {
			if(sim1.timestep() < 404) {
				if(sim1.timestep() % 10 == 3) {
					sim1.clicked();
				}
				if(sim1.timestep() % 10 == 4) {
					sim2.clicked();
				}
			}

			try {
				stepSynched(sim1, sim2, binaryMessage);
			} catch(Throwable e) {
				Log.d("XXXX", "We are at " + sim1.timestep());
				throw e;
			}
		}
	}
	
	
	//Simulate perfect network, no PL or delay
	private void stepSynched(GameSimulation sim1, GameSimulation sim2,
			BinaryMessage binaryMessage) throws IOException,
			UnknownHostException {
		
		binaryMessage.reset();
		sim1.serialize(binaryMessage);
		binaryMessage.parseFrom(binaryMessage.getWritten());
		sim2.absorb(binaryMessage, addr);

		binaryMessage.reset();
		sim2.serialize(binaryMessage);
		binaryMessage.parseFrom(binaryMessage.getWritten());
		sim1.absorb(binaryMessage, addr);
		
		sim1.step();
		sim2.step();
		
		Assert.assertEquals(sim1.hashCode(), sim2.hashCode());
	}
}
