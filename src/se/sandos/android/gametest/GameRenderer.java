package se.sandos.android.gametest;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

public class GameRenderer implements GLSurfaceView.Renderer {

	long lastFrame = -1;
	
	final private float[] mProjMatrix		= new float[16];
	final private float[] mViewMatrix		= new float[16];
	final private float[] mMVPMatrix 		= new float[16];
	final private float[] rotationMatrix 	= new float[16];
	final private float[] scratchMatrix 	= new float[16];
	
	private float ratio;
	GLShip ship;
	GLShip ship2;
	
	private GameSimulation gs;
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		ship = new GLShip();
		ship2 = new GLShip();
		
	    // Set the camera position (View matrix)
	    Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		
		ratio = (float) width / height;
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		// this projection matrix is applied to object coordinates
		// in the onDrawFrame() method
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

	    // Calculate the projection and view transformation
	    Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
	    
	    ship.setPos(gs.x(), gs.y());
	    ship.setAngle(gs.r());
		ship.draw(mMVPMatrix);
	    ship2.setPos(0.01f, 0.0f);
//		ship2.draw(mMVPMatrix);
		
		gs.step();
//		long now = System.nanoTime();
//		Log.v("majs", "FPS: " + 1.0/((now - lastFrame)/1000000000.0f));
//		lastFrame = now;
	}

	public void setColor(float r, float g, float b) {
		mRed = r;
		mGreen = g;
		mBlue = b;
	}

	private float mRed;
	private float mGreen;
	private float mBlue;

	public void setSim(GameSimulation sim) {
		gs = sim;
	}

}
