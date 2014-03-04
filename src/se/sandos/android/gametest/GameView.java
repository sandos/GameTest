package se.sandos.android.gametest;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

public class GameView extends GLSurfaceView {

	GameRenderer render;
	GameSimulation sim;
	
	private final static String TAG = "GameView";
	
	private int counter;
	
	public GameView(Context context) {
		super(context);
		
		setEGLContextClientVersion(2);
		
//		setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
	}
	
	public void setRenderer(Renderer renderer)
	{
		super.setRenderer(renderer);
		this.render = (GameRenderer) renderer;
	}
	
	public void setSim(GameSimulation s)
	{
		sim = s;
	}

	public boolean onTouchEvent(final MotionEvent event) {
		int num = event.getPointerCount();
		counter++;
		for(int i=0; i<num; i++) {
			if(counter % num == i) {
				render.clicked(event.getX(i) / getWidth(), event.getY(i) / getHeight());
			}
		}
		return true;
	}

}
