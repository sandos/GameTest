package se.sandos.android.gametest.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

public class GLFps {
	private FloatBuffer vertexBuffer;
	private ShortBuffer drawListBuffer;
	private FloatBuffer texCoordBuffer;

	private int mProgram;
	private int mPositionHandle;
	private int mMVPMatrixHandle;
	
	private float[] rotationMatrix = new float[16];
	private float[] scratchMatrix = new float[16];
	float[] mvpMatrix = new float[16];
	
	public float x = 0.0f;
	public float y = 0.0f;
	public float angle = 180;
	
	public boolean alive;
	
	private int fps;
	
	int[] textures = new int[100];
	private int counter;

	
	 private final String vertexShaderCode = 
		 "attribute vec4 vPosition;" +
		 "attribute vec2 inputTextureCoordinate;"+
		 "uniform mat4 uMVPMatrix;"+
		 "varying vec2 textureCoordinate;"+
		 "void main() {" +
		 "  gl_Position = uMVPMatrix*vPosition;" +
		 "  textureCoordinate = inputTextureCoordinate;" +
 		 "}";

	 private final String fragmentShaderCode =
		 "varying highp vec2 textureCoordinate;" +
		 "uniform sampler2D sampler;"+
		 "void main() { "+
		 "    gl_FragColor = texture2D(sampler, textureCoordinate);" +
		 "}";
	 
	static final int COORDS_PER_VERTEX = 3;
	static float triangleCoords[] = { // in counterclockwise order:
			-1.0f,  1.0f, 0.0f, // top
			-1.0f, -1.0f, 0.0f, // bottom left
			 1.0f, -1.0f, 0.0f, // bottom right
			 1.0f,  1.0f, 0.0f  // bottom right
	};

	final float[] textureCoordinates =
	{
	        0.0f, 1.0f,
	        0.0f, 0.0f,
	        1.0f, 0.0f,
	        1.0f, 1.0f,
	};

	private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices
	
	public GLFps() {
		//Generate one texture pointer...
		GLES20.glGenTextures(100, textures, 0);

		for(int i=0; i<100; i++) {
			drawText(i);
		}
		
		ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
		bb.order(ByteOrder.nativeOrder());

		vertexBuffer = bb.asFloatBuffer();
		vertexBuffer.put(triangleCoords);
		vertexBuffer.position(0);
		
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);
        

        ByteBuffer tcbb = ByteBuffer.allocateDirect(textureCoordinates.length * 4);
        tcbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tcbb.asFloatBuffer();
        texCoordBuffer.put(textureCoordinates);
        texCoordBuffer.position(0);
        
        int vertexShader = GLShip.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLShip.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables

	}
	
	public void setFps(int f)
	{
		fps = f;
	}

	public void drawText(int i) {
		// Create an empty, mutable bitmap
		Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
		// get a canvas to paint over the bitmap
		Canvas canvas = new Canvas(bitmap);
		bitmap.eraseColor(0);

		// Draw the text
		Typeface tf = Typeface.create("Arial",Typeface.BOLD);
		Paint textPaint = new Paint();
		textPaint.setTypeface(tf);
		textPaint.setTextSize(98);
		textPaint.setAntiAlias(true);
		textPaint.setARGB(0x7f, 0xff, 0xff, 0xff);
		// draw the text centered
		canvas.drawText("" + i, 16,112, textPaint);

		//...and bind it to our array
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[i]);

		//Create Nearest Filtered Texture
		GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		GLES20.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

		//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		//Clean up
		bitmap.recycle();
	}
	
	public void draw(float[] m) {
		System.arraycopy(m, 0, mvpMatrix, 0, 16);
	    Matrix.setRotateM(rotationMatrix, 0, angle, 0, 0, 1.0f);
		
	    counter++;
		Matrix.translateM(mvpMatrix, 0, .1f, -.61f + (.3f*(counter % 6)), 0.0f);
		Matrix.scaleM(mvpMatrix, 0, .5f, .2f, 1.0f);
	    Matrix.multiplyMM(scratchMatrix, 0, mvpMatrix, 0, rotationMatrix, 0);

	    // Add program to OpenGL ES environment
	    GLES20.glUseProgram(mProgram);

	    // get handle to vertex shader's vPosition member
	    mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

	    // Enable a handle to the triangle vertices
	    GLES20.glEnableVertexAttribArray(mPositionHandle);

	    int handle = GLES20.glGetAttribLocation(mProgram, "position");
	    GLES20.glUniform3f(handle, x, y, 0.0f);
	    
	    // Prepare the triangle coordinate data
	    GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
	                                 GLES20.GL_FLOAT, false,
	                                 COORDS_PER_VERTEX*4, vertexBuffer);

		GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[Math.min(99, fps)]);

	    
	    int mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
	    GLES20.glEnableVertexAttribArray(mTexCoordHandle);
	    GLES20.glVertexAttribPointer(mTexCoordHandle, 2,
                GLES20.GL_FLOAT, false,
                2*4, texCoordBuffer);
	    
	    // get handle to shape's transformation matrix
	    mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
	    	
	    // Pass the projection and view transformation to the shader
	    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, scratchMatrix, 0);
	    
	    // Draw the triangle
	    GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

	    // Disable vertex array
	    GLES20.glDisableVertexAttribArray(mPositionHandle);
	}

}
