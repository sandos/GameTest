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
	
	private int counter = 0;
	private float ratio;
	GLShip ship;
	private long angle;
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		ship = new GLShip();
		
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
		
		counter+=50;
		if(counter > 1000) {
			counter = 0;
		}

		// this projection matrix is applied to object coordinates
		// in the onDrawFrame() method
		float scale = counter/1000.0f;
		scale = (float) Math.sin(scale*3.1415926);
		scale *= 0.2+mRed*3;
		Matrix.frustumM(mProjMatrix, 0, -ratio * (0.5f+scale), ratio * (0.5f+scale), -1, 1, 3, 7);

	    // Calculate the projection and view transformation
	    Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);
	    
	    //Rotation
	    // Create a rotation transformation for the triangle
	    angle++;
	    long time = angle;
//	    float angle = 4.0f * ((int) time);
	    float angle = (mGreen*7.0f+1.0f) * ((int) time);
	    
	    Matrix.setRotateM(rotationMatrix, 0, angle, 0, 0, -1.0f);

	    Matrix.multiplyMM(scratchMatrix, 0, mMVPMatrix, 0, rotationMatrix, 0);
		
		ship.draw(scratchMatrix);

		
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

}
