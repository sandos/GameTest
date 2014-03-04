package se.sandos.android.gametest;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import se.sandos.android.gametest.GameSimulation.Shot;
import se.sandos.android.gametest.gl.GLFps;
import se.sandos.android.gametest.gl.GLShip;
import se.sandos.android.gametest.gl.GLShot;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

public class GameRenderer implements GLSurfaceView.Renderer {

	private static final String TAG = "Rend";

	long lastFrame = -1;
	
	final private float[] mProjMatrix		= new float[16];
	final private float[] mViewMatrix		= new float[16];
	final private float[] mMVPMatrix 		= new float[16];
	final private float[] scratchMatrix 	= new float[16];
	
	private float ratio;
	GLShip ship;
	GLShip ship2;
	GLFps fps;
	
	private GameSimulation gs;
	private GLShot[] shots = new GLShot[GameSimulation.SHOTS_MAX];
	
	public volatile boolean clicked  = false;
	public float clickX;
	public float clickY;
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		ship = new GLShip();
		ship2 = new GLShip();
		fps = new GLFps();
		
	    // Set the camera position (View matrix)
	    Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
	    
	    for(int i=0; i<shots.length; i++) {
	    	if(shots[i] == null) {
	    		shots[i] = new GLShot();
	    	}
	    }
	    
	    GLES20.glEnable(GLES20.GL_BLEND);
	    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		
		ratio = (float) width / height;
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(0, 0, 0, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		// this projection matrix is applied to object coordinates
		// in the onDrawFrame() method
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

	    // Calculate the projection and view transformation
	    Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
	    
	    ship.setPos(gs.x(), gs.y());
	    ship.setAngle(gs.r());
	    copyArray(scratchMatrix, mMVPMatrix);
		ship.draw(scratchMatrix);
		ship2.draw(mMVPMatrix);
		
		Shot[] shs = gs.getShots();
		for(int i=0; i<shs.length; i++) {
			shots[i].alive = shs[i].alive;
			if(shots[i].alive) {
				shots[i].x = shs[i].x / GameSimulation.SCALE;
				shots[i].y = shs[i].y / GameSimulation.SCALE;
				
				shots[i].draw(mMVPMatrix);
			}
		}
		
		fps.draw(mMVPMatrix);
		
 		if(clicked) {
			gs.clicked(clickX, clickY);
			clicked = false;
		}
		gs.step();
//		long now = System.nanoTime();
//		Log.v("majs", "FPS: " + 1.0/((now - lastFrame)/1000000000.0f));
//		lastFrame = now;
	}

	private void copyArray(float[] to, float[] from)
	{
		to[0] = from[0];
		to[1] = from[1];
		to[2] = from[2];
		to[3] = from[3];
		to[4] = from[4];
		to[5] = from[5];
		to[6] = from[6];
		to[7] = from[7];
		to[8] = from[8];
		to[9] = from[9];
		to[10] = from[10];
		to[11] = from[11];
		to[12] = from[12];
		to[13] = from[13];
		to[14] = from[14];
		to[15] = from[15];
	}
	
	public void setColor(float r, float g, float b) {
		ship2.setPos(-r*20+10, -g*20+10);
	}

	public void setSim(GameSimulation sim) {
		gs = sim;
	}

}
