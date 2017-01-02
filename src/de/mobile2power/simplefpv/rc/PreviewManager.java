package de.mobile2power.simplefpv.rc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import de.mobile2power.simplefpv.groundcontrol.uavcam.Preview;

public class PreviewManager {
	
	private int upperLeftX, upperLeftY, middleX;
	private int width = 178 * 2;
	private int height = 144 * 2;
	private float scrWidth = 0;
	private float scrHeight = 0;
	private Paint paint = new Paint();
	
	public void setScreenSize(float width, float height) {
		scrWidth = width;
		scrHeight = height;
	}
	
	public void set(int middleX, int topY, boolean stereo) {
		this.middleX = middleX;
		width = middleX;
		height = (width * 3) / 4;
		if (stereo)
			this.upperLeftY = topY - (height/2);
		else 
			this.upperLeftY = 0;
	}
	
//	public boolean isWithin(float x, float y, float border) {
//		boolean within = x > (upperLeftX - border) &&
//				x < (upperLeftX + width + border) &&
//				y > (upperLeftY - border) &&
//				y < (upperLeftY + height + border);
//		return within;
//	}

	public void draw(Canvas canvas, Preview preview, boolean stereo, boolean small) {
		if (small) {
			drawWithBorder(canvas, preview, stereo);
		} else {
			drawFullscreen(canvas, preview, stereo);
		}
		
	}
	
	public void drawWithBorder(Canvas canvas, Preview preview, boolean stereo) {
		byte[] jpegData = preview.getJpegImage();
		upperLeftX = middleX - (width / 2);
		if (jpegData != null && jpegData.length > 0) {
			Bitmap previewFrame = BitmapFactory.decodeByteArray(
					jpegData, 0, jpegData.length);
			if (stereo) {
				Bitmap scaledBitmap = Bitmap.createScaledBitmap(
						previewFrame, width * 8 / 10,
						height * 8 / 10, false);
				canvas.drawBitmap(scaledBitmap,
						width / 10, upperLeftY + height / 10, null);
				canvas.drawRect(0f, 0f, scrWidth, upperLeftY - 1f + height / 10, paint);
				canvas.drawRect(0f, upperLeftY + height - height / 10, scrWidth, scrHeight, paint);
				canvas.drawBitmap(scaledBitmap, middleX + width / 10, upperLeftY + height / 10, null);
			} else {
				Bitmap scaledBitmap = Bitmap.createScaledBitmap(previewFrame,
						width, height, false);
				canvas.drawBitmap(scaledBitmap, 0, upperLeftY, null);				
			}
		}
	}
	
	public void drawFullscreen(Canvas canvas, Preview preview, boolean stereo) {
		byte[] jpegData = preview.getJpegImage();
		upperLeftX = middleX - (width / 2);
		if (jpegData != null && jpegData.length > 0) {
			Bitmap previewFrame = BitmapFactory.decodeByteArray(
					jpegData, 0, jpegData.length);
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(
					previewFrame, width,
					height, false);
			canvas.drawBitmap(scaledBitmap,
					0, upperLeftY, null);
			if (stereo) {
				canvas.drawRect(0f, 0f, scrWidth, upperLeftY - 1f, paint);
				canvas.drawRect(0f, upperLeftY + height, scrWidth, scrHeight, paint);
				canvas.drawBitmap(scaledBitmap, middleX, upperLeftY, null);
			}
		}
	}

	public void drawBorder(Canvas canvas, Paint paint) {
		upperLeftX = middleX - (width / 2);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5);
		paint.setColor(Color.GRAY);
		canvas.drawRect(upperLeftX, upperLeftY, upperLeftX+width, upperLeftY+height, paint);
	}
}
