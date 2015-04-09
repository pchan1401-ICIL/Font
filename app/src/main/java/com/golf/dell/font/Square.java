package com.golf.dell.font;



import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Square {
	// Our vertices.
	private float vertices[] = {
		        -140.0f,  -100.0f,  0.0f,
		        -140.0f,   100.0f,  0.0f,
		         140.0f,   100.0f,  0.0f,
		         140.0f,  -100.0f,  0.0f,
		};


	private short[] indices = { 0, 1, 2, 0, 2, 3 };
	

	private float[] uvs = {0,0, 0,1, 1,1, 1,0};


	private FloatBuffer vertexBuffer;


	private ShortBuffer indexBuffer;
	

	private FloatBuffer texBuffer; 
	
	public Square() {

		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexBuffer = vbb.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

		// short is 2 bytes, therefore we multiply the number if
		// vertices with 2.
		ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
		ibb.order(ByteOrder.nativeOrder());
		indexBuffer = ibb.asShortBuffer();
		indexBuffer.put(indices);
		indexBuffer.position(0);
		

        ByteBuffer tbb = ByteBuffer.allocateDirect(4 * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());
        texBuffer = tbb.asFloatBuffer();
        texBuffer.put(uvs);
        texBuffer.position(0);
        
	}
	
	public void draw(GL10 gl) 
	{
	 gl.glDisable(GL10.GL_DEPTH_TEST);
	 
	 gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	 gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);


     gl.glEnable(GL10.GL_TEXTURE_2D);
     gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
     gl.glActiveTexture(GL10.GL_TEXTURE0);
     gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);
        
     gl.glEnable(GL10.GL_BLEND);
     gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
	 gl.glDrawElements(GL10.GL_TRIANGLES, indices.length, GL10.GL_UNSIGNED_SHORT, indexBuffer);


	 gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

}