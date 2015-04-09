package com.golf.dell.font;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

public class GLRenderer implements GLSurfaceView.Renderer {
	
    private TexFont fnt;
    private Context mContext;
    private Square qd;
    private int tex;
    
	public GLRenderer(Context context)
	{
		mContext = context;
		qd = new Square();
	}
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) 
	{
     gl.glDisable(GL10.GL_DITHER);
     gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

     gl.glClearColor(.0f, .0f, .0f, 1);
     gl.glShadeModel(GL10.GL_SMOOTH);
     gl.glDisable(GL10.GL_DEPTH_TEST);
     gl.glEnable(GL10.GL_TEXTURE_2D);
        

     fnt = new TexFont(mContext,gl);
     
     // Load font file from Assets
     try
     {
    	 fnt.LoadFontAlt("fonts/ArialBig.bff",gl);
     } 
     catch (IOException e) 
     {
      e.printStackTrace();
     }
     
     // Load texture for backgroud quad
     tex = TextLoader.loadTexture(gl, mContext, R.drawable.golf);
                
	}    
    
	public void onDrawFrame(GL10 gl)
	 {
      gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

      gl.glMatrixMode(GL10.GL_MODELVIEW);
      gl.glLoadIdentity();
      
      gl.glDisable(GL10.GL_CULL_FACE);
        
      gl.glBindTexture(GL10.GL_TEXTURE_2D, tex);
        
      gl.glTranslatef(0.0f, 0.0f, -250.0f);

      gl.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);

        
      qd.draw(gl);
      
      
      fnt.SetPolyColor(1.0f, 1.0f, 1.0f);///Set font colour
      fnt.SetScale(1.0f);///set font size
      fnt.PrintAt(gl, "Golf_Hit", 50, 390); ///////////Set the value of x and y axis

         fnt.PrintAt(gl, "Golf_Court", 50, 200);
      
	 }

	public void onSurfaceChanged(GL10 gl, int width, int height) 
	{
	 gl.glViewport(0, 0, width, height);


	 gl.glMatrixMode(GL10.GL_PROJECTION);
     gl.glLoadIdentity();
	

     GLU.gluPerspective(gl, 45.0f, height / width, 0.01f, 500.0f);

	 gl.glMatrixMode(GL10.GL_MODELVIEW);
	 gl.glLoadIdentity();
		
	}



}
