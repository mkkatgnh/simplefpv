package de.mobile2power.simplefpv.vehicle;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import de.mobile2power.simplefpv.groundcontrol.uavcam.Preview;

class CamPreview extends SurfaceView implements SurfaceHolder.Callback { 
	
	private static final int PREVIEW_WIDTH = 267;
	private static final int PREVIEW_HEIGHT = 216;
	private static final int WAIT_MILLIS_TILL_NEXTPIC = 100;
	SurfaceHolder mHolder; // <2>
	private Camera camera = null; // <3>
	private Preview preview;
	private int compressionValue = 50;
	private Activity parentActivity;
	private int previewWidth = 0;
	private int previewHeight = 0;
	private int[] previewFPS = {0,0};
	private Rect rect;

	CamPreview(Activity context) {
		super(context);

		parentActivity = context;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder(); // <4>
		mHolder.addCallback(this); // <5>
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // <6>
	}

	public void setPreviewDTO(Preview previewDTO) {
		this.preview = previewDTO;
	}

	// Called once the holder is ready
	public void surfaceCreated(SurfaceHolder holder) { // <7>
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		if (camera == null) {
			camera = Camera.open(); // <8>
			chooseCamPreviewSize();
			chooseCamPreviewFPS();
			installCamPreviewBuffer();
			try {
				camera.setPreviewDisplay(holder); // <9>

				preparePreviewCallbackOnCam();
			} catch (IOException e) { // <13>
				camera.release();
				camera = null;
			}
		}
	}

	public void preparePreviewCallbackOnCam() {

			camera.setPreviewCallbackWithBuffer(new PreviewCallback() { // <10>
				// Called for each frame previewed
				public void onPreviewFrame(byte[] data, Camera camera) { // <11>
					Camera.Parameters parameters = camera.getParameters();

					int width = parameters.getPreviewSize().width;
					int height = parameters.getPreviewSize().height;

					ByteArrayOutputStream outstr = new ByteArrayOutputStream();
					YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21,
							width, height, null);
					yuvimage.compressToJpeg(rect, compressionValue, outstr);

					preview.setJpegImage(outstr.toByteArray());
					camera.addCallbackBuffer(data);
					// CamPreview.this.invalidate(); // <12>
				}
			});
//		}
	}

	private void chooseCamPreviewSize() {
		List<Size> sizes = camera.getParameters().getSupportedPreviewSizes();
		Iterator<Size> sizesIter = sizes.iterator();
		Size size = null;
		do {
		 size = sizesIter.next();
		} while (sizesIter.hasNext() && size.width < PREVIEW_WIDTH);
		previewWidth = size.width;
		previewHeight = size.height;
		rect = new Rect(0, 0, previewWidth, previewHeight);
	}

	private void chooseCamPreviewFPS() {
		Camera.Parameters parameters = camera.getParameters();
		List<int[]> fpss = new ArrayList<int[]>();
		fpss.addAll(parameters.getSupportedPreviewFpsRange());
		if (fpss != null) {
			previewFPS = fpss.get(fpss.size() - 1);
		}
	}
	
	private void installCamPreviewBuffer() {
		final int BITS_PER_BYTE = 8;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(camera.getParameters().getPreviewFormat()) / BITS_PER_BYTE;
        // XXX: According to the documentation the buffer size can be
        // calculated by width * height * bytesPerPixel. However, this
        // returned an error saying it was too small. It always needed
        // to be exactly 1.5 times larger.
        int mPreviewBufferSize = previewWidth * previewHeight * bytesPerPixel * 3 / 2 + 1;
        camera.addCallbackBuffer(new byte[mPreviewBufferSize]);
	}
	
	// Called when the holder is destroyed
	public void surfaceDestroyed(SurfaceHolder holder) { // <14>
		if (camera != null) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
			camera = null;
		}
	}

	public void startPreview() {
		camera.startPreview();
	}

	// Called when holder has changed
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) { // <15>
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(previewWidth, previewHeight);
		parameters.setPreviewFpsRange(previewFPS[0], previewFPS[1]);
		camera.setParameters(parameters);
		startPreview();
	}

	// Called when shutter is opened
	ShutterCallback shutterCallback = new ShutterCallback() { // <6>
		public void onShutter() {
		}
	};

	// Handles data for raw picture
	PictureCallback rawCallback = new PictureCallback() { // <7>
		public void onPictureTaken(byte[] data, Camera camera) {
		}
	};

	// Handles data for jpeg picture
	PictureCallback jpegCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			
		} // <8>

	};

	public void setCompressionValue(int compressionValue) {
		this.compressionValue = compressionValue;
	}
}
