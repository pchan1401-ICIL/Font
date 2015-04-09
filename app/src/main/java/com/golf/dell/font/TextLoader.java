package com.golf.dell.font;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.opengl.GLUtils;

public class TextLoader {

	// Get a new texture id:
	private static int newTextureID(GL10 gl) {
	    int[] temp = new int[1];
	    gl.glGenTextures(1, temp, 0);
	    return temp[0];        
	}

	// Will load a texture out of a drawable resource file, and return an OpenGL texture ID:
	public static int loadTexture(GL10 gl, Context context, int resource) {
	    
	    // In which ID will we be storing this texture?
	    int id = newTextureID(gl);
	    
	    // We need to flip the textures vertically:
	    Matrix flip = new Matrix();
	    flip.postScale(1f, -1f);
	    
	    // Load up, and flip the texture:
	    Bitmap temp = BitmapFactory.decodeResource(context.getResources(), resource);
	    Bitmap bmp = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), flip, true);
	    temp.recycle();
	    
	    gl.glBindTexture(GL10.GL_TEXTURE_2D, id);
	    
	    // Set all of our texture parameters:
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR_MIPMAP_LINEAR);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR_MIPMAP_LINEAR);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
	    
	    // Generate, and load up all of the mipmaps:
	    for(int level=0, height = bmp.getHeight(), width = bmp.getWidth(); true; level++) {
	        // Push the bitmap onto the GPU:
	        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, level, bmp, 0);
	        
	        // We need to stop when the texture is 1x1:
	        if(height==1 && width==1) break;
	        
	        // Resize, and let's go again:
	        width >>= 1; height >>= 1;
	        if(width<1)  width = 1;
	        if(height<1) height = 1;
	        
	        Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, width, height, true);
	        bmp.recycle();
	        bmp = bmp2;
	    }
	    
	    bmp.recycle();
	    
	    return id;
	}
	
	
}
