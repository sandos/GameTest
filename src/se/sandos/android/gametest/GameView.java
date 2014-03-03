package se.sandos.android.gametest;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.view.MotionEvent;

public class GameView extends GLSurfaceView {

	GameRenderer render;
	GameSimulation sim;
	
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
		if(event.getActionMasked() == MotionEvent.ACTION_DOWN)
		{
			render.clicked = true;
			
			render.clickX = event.getX() / getWidth();
			render.clickY = event.getY() / getHeight();
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
