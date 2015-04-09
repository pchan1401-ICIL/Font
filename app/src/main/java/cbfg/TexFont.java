
package cbfg;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.content.res.AssetManager;
import android.content.Context;
 
public class TexFont{
	Context mContext;
	float xScale, yScale;
	int fntTexWidth, fntTexHeight;
	int fntCellWidth, fntCellHeight;
	int BPP,firstCharOffset,colCount;
	int[] charWidth;
	int texID;
	float redVal, greenVal, blueVal;
	int curX, curY;
	int UVarray[];
	
	// Quad Stuff
	// Vertex array
	private float vertices[] = {  0.0f,   0.0f,  0.0f,  // 0, Bottom Left
			                      1.0f,   0.0f,  0.0f,  // 1, Bottom Right
			                      1.0f,   1.0f,  0.0f,  // 2, Top Right
			                      0.0f,   1.0f,  0.0f,  // 3, Top Left
			                    };
	// Order of connection for the vertices.
	private short[] indices = { 0, 1, 2, 0, 2, 3 };
	
	// UV co-ords
	private float[] uvs = {0,0, 1,0, 1,1, 0,1};
	
	// Our vertex buffer.
	private FloatBuffer vertexBuffer;

	// Our index buffer.
	private ShortBuffer indexBuffer;
	
	// Texture buffer
	private FloatBuffer texBuffer;
	
	
	
	public TexFont(Context context, GL10 gl)
	{
		// Get handle on assets
		mContext = context;
		
		// Initialise parameters
		redVal = greenVal = blueVal = 1.0f;
		curX = curY = 0;
		
		// Set scale to neutral
		xScale = yScale = 1.0f;
		
		// Array to hold character width data
		charWidth = new int[256];
		
		// UV Array for crop rectangle
		UVarray = new int[4];
		
		// Generate GL texture ID
		int temp[] = new int[1];
		gl.glGenTextures(1, temp, 0);
		texID = temp[0];
		
	    gl.glBindTexture(GL10.GL_TEXTURE_2D, texID);
	    
	    // Set texture parameters
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
	    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
	    
	    // Initialise quad buffers
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexBuffer = vbb.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

		ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
		ibb.order(ByteOrder.nativeOrder());
		indexBuffer = ibb.asShortBuffer();
		indexBuffer.put(indices);
		indexBuffer.position(0);
		
		// Texture buffer
        ByteBuffer tbb = ByteBuffer.allocateDirect(4 * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());
        texBuffer = tbb.asFloatBuffer();
        texBuffer.put(uvs);
        texBuffer.position(0);
	    
	}
	
	// BFF files should be placed in the assets folder to be picked up by this function
	public Boolean LoadFontAlt(String fontName, GL10 gl) throws IOException
	{
		int bytesRead;
		
		// Array for file header
		byte[] head = new byte[20];
		
		try
		{
			AssetManager am = mContext.getAssets();
			InputStream uStream = am.open(fontName, AssetManager.ACCESS_BUFFER);
		 
			// Read header
			bytesRead = uStream.read(head, 0, 20);
		 
			// Check header size is correct
			if(bytesRead < 20)
			{
				throw new IOException("Header read failed");
			}
		 
			ByteBuffer headBuf = ByteBuffer.wrap(head);

			// Check header
			int h0 = getUnsignedByteVal(headBuf.get());
			int h1 = getUnsignedByteVal(headBuf.get());
		 
			// Check header signature
			if(h0 != 0xBF || h1 != 0xF2)
			{
				uStream.close();
				throw new IOException("Bad header signature");
			}
		 
			// Get image width and height
			fntTexWidth  = flipEndian(headBuf.getInt());
			fntTexHeight = flipEndian(headBuf.getInt());	
			
			// Get cell dimensions
			fntCellWidth  = flipEndian(headBuf.getInt());
			fntCellHeight = flipEndian(headBuf.getInt());
			
			// Sanity check (prevent divide by zero)
			if(fntCellWidth <= 0 || fntCellHeight <= 0)
			{
				throw new IOException("Invalid header content");
			}
			
			// Pre-calculate column count
			colCount = fntTexWidth / fntCellWidth;
			
			// Get colour depth
			BPP = getUnsignedByteVal(headBuf.get());
					
			// Get base offset
			firstCharOffset = getUnsignedByteVal(headBuf.get());
			
			// Read width information
			for(int wLoop = 0; wLoop < 256; ++wLoop)
			{
				charWidth[wLoop] = uStream.read(); // read() returns an unsigned byte, unlike the overloaded versions.  :s
			}			
			
			// Get bitmap
			int bitLen = (fntTexHeight * fntTexWidth) * (BPP / 8);
			byte bits[] = new byte[bitLen];
		
			uStream.read(bits, 0, bitLen);
				
			uStream.close();	
			
			// Flip image scanlines and wrap in Bytebuffer for glTexImage2D
			ByteBuffer pix = ByteBuffer.allocate(bits.length);
			int lineLen = fntTexWidth * (BPP / 8);
			
			for(int lines = fntTexHeight-1; lines>0 ; --lines)
			{
			 pix.put(bits, lines * lineLen, lineLen);
			}
		
			// Place bitmap in texture
			gl.glBindTexture(GL10.GL_TEXTURE_2D, texID);
			
			switch(BPP)
			{
			case 8: // Alpha channel info only
				gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_ALPHA, fntTexWidth, fntTexHeight, 0, GL10.GL_ALPHA, GL10.GL_UNSIGNED_BYTE, pix);
				break;
				
			case 24: // RGB Texture
				gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGB, fntTexWidth, fntTexHeight, 0, GL10.GL_RGB, GL10.GL_UNSIGNED_BYTE, pix);
				break;
				
			case 32: // RGBA Texture
				gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, fntTexWidth, fntTexHeight, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pix);
				break;
			}

		}
		catch (java.io.EOFException e)
		{
			throw e;
		}
		catch (IOException e)
		{
			throw e;			
		}
		
		return true;
	}

	
	// BFF files should be placed in the assets folder to be picked up by this function
	public Boolean LoadFont(String fontName, GL10 gl) throws IOException
	{
		AssetManager am = mContext.getAssets();
		InputStream uStream = am.open(fontName, AssetManager.ACCESS_BUFFER);
		DataInputStream in = new DataInputStream(uStream); // Get around Java's stupid unsigned bytes 
		
		// Check header - should read 0xBF 0xF2 (BFF2)
  		int h0 = in.readUnsignedByte();
  		int h1 = in.readUnsignedByte();
		
		if(h0 != 0xBF || h1 != 0xF2)
		{
			in.close();
			return false;
		}
		
		// Get image dimensions
		fntTexWidth = flipEndian(in.readInt());
		fntTexHeight = flipEndian(in.readInt());
		
		// Get cell dimensions
		fntCellWidth = flipEndian(in.readInt());
		fntCellHeight = flipEndian(in.readInt());	
		
		// Sanity check (prevent divide by zero)
		if(fntCellWidth <= 0 || fntCellHeight <= 0)
		{
			throw new IOException("Invalid header content");
		}
		
		// Pre-calculate column count
		colCount = fntTexWidth / fntCellWidth;
		
		// Get colour depth
		BPP = in.readUnsignedByte();
				
		// Get base offset
		firstCharOffset = in.readUnsignedByte();
		
		// Read width information
		for(int wLoop = 0; wLoop < 256; ++wLoop)
		{
			charWidth[wLoop] = in.readUnsignedByte();
		}

		// Get bitmap
		int bitLen = (fntTexHeight * fntTexWidth) * (BPP / 8);
		byte bits[] = new byte[bitLen];
	
		in.read(bits, 0, bitLen);
			
		in.close();	
		
		// Flip bits and wrap in Bytebuffer for glTexImage2D
		ByteBuffer pix = ByteBuffer.allocate(bits.length);
		int lineLen = fntTexWidth * (BPP / 8);
		
		for(int lines = fntTexHeight-1; lines>0 ; --lines)
		{
		 pix.put(bits, lines * lineLen, lineLen);
		}
	
		// Place bitmap in texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texID);
		
		switch(BPP)
		{
		case 8: // Alpha channel info only
			gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_ALPHA, fntTexWidth, fntTexHeight, 0, GL10.GL_ALPHA, GL10.GL_UNSIGNED_BYTE, pix);
			break;
			
		case 24: // RGB Texture
			gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGB, fntTexWidth, fntTexHeight, 0, GL10.GL_RGB, GL10.GL_UNSIGNED_BYTE, pix);
			break;
			
		case 32: // RGBA Texture
			gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, fntTexWidth, fntTexHeight, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pix);
			break;
		}
        
 		return true;
	}
	
	// Flip endian-ness of a 32 bit integer value. (Stupid Java, no cake for you)
	int flipEndian(int val)
	{
		return  (val >>> 24) | (val << 24) | ((val << 8) & 0x00FF0000) | ((val >> 8) & 0x0000FF00);
	}
	
	// Bodge to get unsigned byte values
	int getUnsignedByteVal(byte val)
	{
		if(val < 0)
			return 256 + val;
		else
			return val;		
	}
	
	// Set a general scale for text
	public void SetScale(float scaleVal)
	{
		xScale = yScale = scaleVal;
	}
	
	// Set text width and height scale independently
	public void SetScale(float xScaleVal, float yScaleVal)
	{
		xScale = xScaleVal;
		yScale = yScaleVal;	
	}
	
	// Set colour for text quads.
	public void SetPolyColor(float red, float green, float blue)
	{
		redVal = red;
		greenVal = green;
		blueVal = blue;
	}
	
	// Set cursor position
	public void SetCursor(int x, int y)
	{
		curX = x;
		curY = y;		
	}
	
	// Print a line of text at current cursor position
	public void Print(GL10 gl, String text)
	{
		float xPos = curX;
		
		// Calculate quad size from scaling factors
		float cellWidth = fntCellWidth * xScale;
		float cellHeight = fntCellHeight * yScale;
		
		// The height and width values in the crop rect never change
		UVarray[2]=fntCellWidth;
		UVarray[3]=fntCellHeight;
		
		// Set up GL for rendering the text
		gl.glColor4f(redVal, greenVal, blueVal, 1.0f);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texID);
		
		// Loop through each character of the string
		for(int index = 0 ; index != text.length(); ++index )
		{
			// Calculate glyph position within texture
			int glyph = (int)text.charAt(index) - firstCharOffset;
			int col = glyph % colCount;
			int row = (glyph / colCount) + 1;
			
			// Update the crop rect
			UVarray[0] = col*fntCellWidth;
			UVarray[1] = fntTexHeight - (row*fntCellHeight);
			
			// Set crop area
			((GL11) gl).glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES,UVarray,0);
			// Draw texture
			((GL11Ext) gl).glDrawTexfOES(xPos,(float)curY,0.0f,cellWidth,cellHeight);
			
			// Add character width to offset for next glyph
			xPos += (xScale * charWidth[glyph + firstCharOffset]);
		}
		
		// Update cursor position
		curX = (int)xPos;
	}
	
	// Print a line of text to screen at specified co-ords
	public void PrintAt(GL10 gl, String text, int x, int y)
	{
		SetCursor(x, y);
		Print(gl, text);
	}
	
	public void Print3D(GL10 gl, String text)
	{
		int glyph, col, row;
		float xPos = 0.0f;
		float U,V,U1,V1;
		
		// Calculate quad size from scaling factors
		float cellWidth = fntCellWidth * xScale;
		float cellHeight = fntCellHeight * yScale;
		
		// Set the quad buffer
		vertices[3] = vertices[6] = cellWidth;
		vertices[7] = vertices[10] = cellHeight;
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);
		
		// Calculate UV params
		int RowPitch=fntTexWidth/fntCellWidth;
		float ColFactor=(float)fntCellWidth/(float)fntTexWidth;
		float RowFactor=(float)fntCellHeight/(float)fntTexHeight;
		
		// Set up GL for rendering the text
		gl.glColor4f(redVal, greenVal, blueVal, 1.0f);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texID);
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		 
	    gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	    gl.glActiveTexture(GL10.GL_TEXTURE0);
	    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);
		
		// Loop through each character of the string
		for(int index = 0 ; index != text.length(); ++index )
		{
			// Calculate glyph position within texture
			glyph = (int)text.charAt(index) - firstCharOffset;
			
		    row=(int) (glyph/RowPitch);
		    col=(int) (glyph-row*RowPitch);			
			
			// Set UV area
			U = col*ColFactor;
			V = 1.0f - (row*RowFactor);
			U1 = U+ColFactor;
			V1 = V-RowFactor;
			
			uvs[0] = U;
			uvs[1] = V1;
			
			uvs[2] = U1;
			uvs[3] = V1;
			
			uvs[4] = U1;
			uvs[5] = V;
			
			uvs[6] = U;
			uvs[7] = V;
			
	        texBuffer.put(uvs);
	        texBuffer.position(0);
	        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuffer);/**/
			
			// Draw Quad
			gl.glDrawElements(GL10.GL_TRIANGLES, indices.length, GL10.GL_UNSIGNED_SHORT, indexBuffer);
		
			// Add character width to offset for next glyph
			xPos = (xScale * charWidth[glyph + firstCharOffset]);
			gl.glTranslatef(xPos,0.0f,0.0f);
			curX += (int)xPos;
			
		}
		
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
	}
	
	// Return the length (pixels) of a string
	public int GetTextLength(String text)
	{
		float len = 0.0f;
		
		for(int index = 0 ; index != text.length(); ++index )
		{
			len += (xScale * charWidth[(int)text.charAt(index)]);
		}
		
		return (int)len;
	}
	
	// Return the height of text
	public int GetTextHeight()
	{
		return (int)(fntCellHeight * yScale);
	}
	
}
