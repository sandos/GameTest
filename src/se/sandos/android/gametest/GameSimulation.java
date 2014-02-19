package se.sandos.android.gametest;

import java.io.IOException;
import java.util.ArrayDeque;

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
	private int timestep;
	
	private final String TAG = "SIM";
	
	//Player
	private int pX, pY, vX, vY, r, vR;
	
	private static final int SHFT = 16;
	private static final float SCALE = 1 << SHFT;
	private static final int MIN_X = -10 << SHFT;
	private static final int MIN_Y = -10 << SHFT;
	private static final int MAX_X = 10 << SHFT;
	private static final int MAX_Y = 10 << SHFT;
	
	
	//Synch data, never sent out!
	private ArrayDeque<Integer> timeOffsets = new ArrayDeque<Integer>();
	private int avgOffset = -1;
	//This is used to control speeding up/down of timestep
	private int stepCheckCounter = 0;
	
	public GameSimulation()
	{
		vX = 24000;
		vY = 31200;
		vR = 400000;
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
		pX += vX;
		pY += vY;
		
		if(pX >= MAX_X) {
			//pX = MAX_X;
			pX -= vX;
			vX = -vX;
			
		} else if(pX <= MIN_X) {
			//pX = MIN_X;
			pX -= vX;
			vX = -vX;
		}
		
		if(pY >= MAX_Y) {
			//pY = MAX_Y;
			pY -= vY;
			vY = -vY;
		} else if(pY <= MIN_Y) {
			//pY = MIN_Y;
			pY -= vY;
			vY = -vY;
		}

		r += vR;
		
		//Drag
//		vX *= 0.9f;
//		vY *= 0.9f;
		
		timestep++;
	}
	
	public void step()
	{	
		stepCheckCounter++;

		if(stepCheckCounter % 13 == 1)
		{
			if(avgOffset < -1) {
				Log.v(TAG, "Running late, small step " + avgOffset);
				actualStep();
			} else if(avgOffset > 3) {
				Log.v(TAG, "Skipping step");
				return;
			}
		}
		
		if(stepCheckCounter % 400 == 1) {
			if(avgOffset < -200)
			{
				Log.v(TAG, "Running late, BIG step: " + avgOffset);
				for(int i=0; i<(-avgOffset+100); i++)
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

	public void absorb(BinaryMessage d) throws IOException
	{
		int readInt = d.readInt();
		
		timeOffsets.add(timestep-readInt);
		if(timeOffsets.size() >= 120)
		{
			
			Integer[] array = timeOffsets.toArray(new Integer[0]);
			long total = 0;
			for(int i=0; i<array.length; i++)
			{
				total += array[i];
			}
			total = total / array.length;
//			Log.v(TAG, "Avg offset is " + total + "|" + (timestep-readInt));
			avgOffset = (int) total;
			Integer poll = timeOffsets.poll();
		}
	}


	public void serialize(BinaryMessage d) throws IOException {
		d.writeInt(timestep);
		d.writeInt(pX).writeInt(pY).writeInt(vX).writeInt(vY).writeInt(r).writeInt(vR);
	}
}
