package se.sandos.android.gametest;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.view.MotionEvent;

public class GameView extends GLSurfaceView {

	GameRenderer render;
	
	public GameView(Context context) {
		super(context);
		
		setEGLContextClientVersion(2);
	}
	
	public void setRenderer(Renderer renderer)
	{
		super.setRenderer(renderer);
		this.render = (GameRenderer) renderer;
	}

	public boolean onTouchEvent(final MotionEvent event) {
		queueEvent(new Runnable() {
			public void run() {
				render.setColor(event.getX() / getWidth(), event.getY()
						/ getHeight(), 0.0f);
			}
		});
		return true;
	}

}
