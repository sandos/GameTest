package se.sandos.android.gametest.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;

public class GLShip {
	private FloatBuffer vertexBuffer;
	private ShortBuffer drawListBuffer;

	private int mProgram;
	private int mPositionHandle;
	private int mColorHandle;
	private int mMVPMatrixHandle;
	
	private float[] rotationMatrix = new float[16];
	private float[] scratchMatrix = new float[16];
	private float[] scratchMatrix2 = new float[16];
	
	private float x;
	private float y;
	private float angle;
	
	public void setPos(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void setAngle(float a)
	{
		angle = a;
	}
	
	private final String vertexShaderCode =
		    "attribute vec4 vPosition;" +
			"uniform mat4 uMVPMatrix;" +
		    "void main() {" +
		    "  gl_Position = uMVPMatrix * vPosition;" +
		    "}";

		private final String fragmentShaderCode =
		    "precision mediump float;" +
		    "uniform vec4 vColor;" +
		    "void main() {" +
		    "  vec2 pos = mod(gl_FragCoord.xy, vec2(150.0)) - vec2(75.0);" +
		    "  float dist_squared = dot(pos, pos);"	+
//		    " gl_FragColor = (dist_squared < 400.0) ?" +
//		    "     vec4(.90, .90, 1.0) :" + 
//		    "     vec4(.20, .20, .10);" + 
			"  gl_FragColor = vColor;" +
			"  gl_FragColor.r = sin(gl_FragCoord.x/10.0)*0.5+0.5;" +
			"  gl_FragColor.g = cos(gl_FragCoord.y/30.0)*0.5+0.5;" +
		    "}";
	
	static final int COORDS_PER_VERTEX = 3;
	static float triangleCoords[] = { // in counterclockwise order:
			 0.0f,  0.622008459f, 0.0f, // top
			-0.5f, -0.311004243f, 0.0f, // bottom left
			 0.5f, -0.311004243f, 0.0f, // bottom right
			 0.5f,  0.311004243f, 0.0f  // bottom right
	};

	private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

	// Set color with red, green, blue and alpha (opacity) values
	float color[] = { 0.99671875f, 0.99953125f, 0.99265625f, 1.0f };

	public GLShip() {
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
        
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables

	}
	
	public void draw(float[] m) {
		System.arraycopy(m, 0, scratchMatrix2, 0, 16);
		
	    Matrix.setRotateM(rotationMatrix, 0, angle, 0, 0, -1.0f);
//	    Matrix.setRotateM(rotationMatrix, 0, angle, 0, 0.0f, 1.0f);
		
		Matrix.scaleM(scratchMatrix2, 0, 0.1f, 0.1f, 0.1f);
		Matrix.translateM(scratchMatrix2, 0, x, y, 0.0f);
	    Matrix.multiplyMM(scratchMatrix, 0, scratchMatrix2, 0, rotationMatrix, 0);

	    // Add program to OpenGL ES environment
	    GLES20.glUseProgram(mProgram);

	    // get handle to vertex shader's vPosition member
	    mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

	    // Enable a handle to the triangle vertices
	    GLES20.glEnableVertexAttribArray(mPositionHandle);

	    // Prepare the triangle coordinate data
	    GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
	                                 GLES20.GL_FLOAT, false,
	                                 0, vertexBuffer);

	    // get handle to fragment shader's vColor member
	    mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

	    // Set color for drawing the triangle
	    GLES20.glUniform4fv(mColorHandle, 1, color, 0);

	    // get handle to shape's transformation matrix
	    mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
	    	
	    // Pass the projection and view transformation to the shader
	    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, scratchMatrix, 0);
	    
	    // Draw the triangle
	    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

	    // Disable vertex array
	    GLES20.glDisableVertexAttribArray(mPositionHandle);
	}

	
	public static int loadShader(int type, String shaderCode){
	    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    int shader = GLES20.glCreateShader(type);

	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
	    GLES20.glCompileShader(shader);

	    return shader;
	}
}
