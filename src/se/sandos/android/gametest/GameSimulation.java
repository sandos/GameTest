package se.sandos.android.gametest;

import java.io.IOException;

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
	
	//Player
	private int pX, pY, vX, vY, r, vR;
	
	private static final int SHFT = 16;
	private static final float SCALE = 1 << SHFT;
	private static final int MIN_X = -10 << SHFT;
	private static final int MIN_Y = -10 << SHFT;
	private static final int MAX_X = 10 << SHFT;
	private static final int MAX_Y = 10 << SHFT;
	
	public GameSimulation()
	{
		vX = 14000;
		vY = 21200;
		vR = 100000;
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
	
	public void step()
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
	}




	public void serialize(BinaryMessage d) throws IOException {
		d.writeInt(timestep);
	}
}
