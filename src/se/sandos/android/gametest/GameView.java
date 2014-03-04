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
		Log.v(TAG, "Got " + num + " pointers");
		counter++;
//		PointerCoords pc = new PointerCoords();
		for(int i=0; i<num; i++) {
//			if(event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_MOVE)
			{
//				render.clicked = true;
				
//				event.getPointerCoords(i, pc);
//				render.clickX = event.getX(i) / getWidth();
//				render.clickY = event.getY(i) / getHeight();
				if(counter % num == 0) {
					render.clicked(event.getX(i) / getWidth(), event.getY(i) / getHeight());
				}
//				render.clickX = pc.x / getWidth();
//				render.clickY = pc.y / getHeight();
			}
		}
//		queueEvent(new Runnable() {
//			public void run() {
//				render.setColor(event.getX() / getWidth(), event.getY()
//						/ getHeight(), 0.0f);
//			}
//		});
		return true;
	}

}
