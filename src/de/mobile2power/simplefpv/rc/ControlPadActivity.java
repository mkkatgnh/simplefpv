package de.mobile2power.simplefpv.rc;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import de.mobile2power.simplefpv.ConnectionDialog;
import de.mobile2power.simplefpv.IPointCommunication;
import de.mobile2power.simplefpv.R;
import de.mobile2power.simplefpv.SendToOppositeDeviceTask;
import de.mobile2power.simplefpv.SettingsChanged;
import de.mobile2power.simplefpv.SimpleFPVApp;
import de.mobile2power.simplefpv.groundcontrol.ObjectConverter;
import de.mobile2power.simplefpv.groundcontrol.remotecontrol.ControlChannels;
import de.mobile2power.simplefpv.groundcontrol.uavcam.Preview;
import de.mobile2power.simplefpv.groundcontrol.uavlocation.UavOrientation;
import de.mobile2power.simplefpv.groundcontrol.uavlocation.UavPosition;
import eu.mightyfrog.udpcomm.node.DatagrammNode;
import eu.mightyfrog.udpcomm.node.IDatagrammNodeCallback;
import eu.mightyfrog.udpcomm.node.NoConnectionException;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ControlPadActivity extends Activity implements
		SensorEventListener, IDatagrammNodeCallback, IPointCommunication,
		InputManager.InputDeviceListener, SettingsChanged {

	private Location vehicleLocation = new Location("vehicle");
	private Location referenceLocation = null;

	private InputManager mInputManager;
	private SparseArray<InputDeviceState> mInputDeviceStates;

    private UavOrientation orientation = new UavOrientation();

	SimpleFPVApp mobileFPVApp;
	ControlChannels controlChannels;
	// TakePictureState pictureTakeState;
	// byte[] takePictureEventData;

	int screenHeight;
	int screenWidth;

	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private float initalDirection = Float.MIN_VALUE;

	private BluetoothSocket btSocket = null;

	private ControlView mySurfaceView;
	// private ControlStick leftStick = new ControlStick();
	// private ControlStick rightStick = new ControlStick();
	private PreviewManager previewManager = new PreviewManager();

	private SendToOppositeDeviceTask sendToVehicleTask;
	DatagrammNode datagrammNode;
	private Preview preview = null;
	private boolean udpConnectionEstablished = false;
	final Activity parentActivity = this;
	private ProgressDialog dialog;

	private static final long MICROSEC_40FPS = 1000 / 40;
	private long time40fps = 0;
	private byte[] serializeObjectToByteArray;

	private long currentTime = 0;

	private WakeLock mWakeLock;
	private PowerManager mPowerManager;
	
	private boolean hasCamera;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mobile_rc);

		mobileFPVApp = (SimpleFPVApp) getApplicationContext();
		mobileFPVApp.addListener(this);

		mInputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);

		mInputDeviceStates = new SparseArray<InputDeviceState>();

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mySurfaceView = new ControlView(this);
		setContentView(mySurfaceView);

		PackageManager pm = getPackageManager();
		hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);

		initPreviewManager();

		controlChannels = new ControlChannels();
		preview = new Preview();

		// try {
		// takePictureEventData = ObjectConverter
		// .serializeObjectToByteArray(new VehicleEvent(
		// VehicleEventType.TAKE_PICTURE));
		// } catch (IOException e) {
		// Toast.makeText(this, "Take picture event could not be created",
		// Toast.LENGTH_LONG).show();
		// }
		// pictureTakeState = new TakePictureState();
		// pictureTakeState.reset();

		// Get an instance of the PowerManager
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		// Create a bright wake lock
		mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

		setupNetwork();
		
		dialog = ConnectionDialog.openDialog(
				getString(R.string.connect_to_vehicle), this);
		setupNetwork();
		udpConnectionEstablished = false;
		initConnection();
	}

	public void initPreviewManager() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		screenHeight = metrics.heightPixels;
		screenWidth = metrics.widthPixels;
		previewManager.setScreenSize(screenWidth, screenHeight);
		if (mobileFPVApp.isStereoView()) {
			previewManager.set(screenWidth / 2, screenHeight / 2, mobileFPVApp.isStereoView());
		}
		else
			previewManager.set(screenWidth, screenHeight, mobileFPVApp.isStereoView());
	}

	@Override
	public void settingsChangedListener() {
		initPreviewManager();
	}

	private void setupNetwork() {
		sendToVehicleTask = (SendToOppositeDeviceTask) getLastNonConfigurationInstance();
		if (sendToVehicleTask == null || !sendToVehicleTask.isAlive()) {
			datagrammNode = new DatagrammNode();
			sendToVehicleTask = new SendToOppositeDeviceTask(this,
					datagrammNode);
			sendToVehicleTask.setDatagrammNode(datagrammNode);
			sendToVehicleTask.start();
		} else {
			datagrammNode = sendToVehicleTask.getDatagrammNode();
		}
		sendToVehicleTask.setPointCommunication(this);
		datagrammNode.setCallbackListener(this);
	}

	private void closeNetwork() {
		if (sendToVehicleTask != null) {
			sendToVehicleTask.stopTask();
			while (sendToVehicleTask.isAlive()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// do nothing
				}
			}
			sendToVehicleTask = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, magnetometer,
				SensorManager.SENSOR_DELAY_GAME);
		mySurfaceView.onResumeMySurfaceView();
		if (this.isFinishing()) {
			freeResources();
		}

		// Register an input device listener to watch when input devices are
		// added, removed or reconfigured.
		mInputManager.registerInputDeviceListener(this, null);

		// Query all input devices.
		// We do this so that we can see them in the log as they are enumerated.
		int[] ids = mInputManager.getInputDeviceIds();
		for (int i = 0; i < ids.length; i++) {
			getInputDeviceState(ids[i]);
		}

		/*
		 * when the activity is resumed, we acquire a wake-lock so that the
		 * screen stays on, since the user will likely not be fiddling with the
		 * screen or buttons.
		 */
		mWakeLock.acquire();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		mySurfaceView.onPauseMySurfaceView();

		// Remove the input device listener when the activity is paused.
		mInputManager.unregisterInputDeviceListener(this);
		// and release our wake-lock
		mWakeLock.release();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.isFinishing()) {
			freeResources();
			closeNetwork();
			System.runFinalizersOnExit(true);
		}
	}

	private void freeResources() {
		mySurfaceView.onDestroy();
		mSensorManager.unregisterListener(this);
		if (btSocket != null) {
			try {
				btSocket.close();
			} catch (IOException e) {
				Toast.makeText(this, "cannot close bluetooth connection",
						Toast.LENGTH_LONG).show();
			}
		}
		datagrammNode.setCallbackListener(null);
	}

	private float[] mGravity;
	private float[] mGeomagnetic;

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mGravity = event.values;
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mGeomagnetic = event.values;
		}
		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
					mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);

				controlChannels.setmNick(trimStickValues((int) Math
						.toDegrees(orientation[2]) * 2 + 192));
				controlChannels.setmRoll(trimStickValues((int) Math
						.toDegrees(-orientation[1]) * 2 + 192));
				if (initalDirection == Float.MIN_VALUE) {
					initalDirection = orientation[0];
				}
				controlChannels.setmYaw(trimStickValues((int) Math
						.toDegrees(orientation[0] - initalDirection) + 192));
			}
		}
	}

	private int trimStickValues(int value) {
		int trimmedValue = value;
		trimmedValue = trimmedValue > 255 ? 255 : trimmedValue;
		trimmedValue = trimmedValue < 127 ? 127 : trimmedValue;
		return trimmedValue;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	class ControlView extends SurfaceView implements Runnable {

		final int CONCURRENT_TOUCH = 2;

		private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		float[] x = new float[CONCURRENT_TOUCH];
		float[] y = new float[CONCURRENT_TOUCH];
		boolean[] isTouch = new boolean[CONCURRENT_TOUCH];

		float[] x_last = new float[CONCURRENT_TOUCH];
		float[] y_last = new float[CONCURRENT_TOUCH];
		boolean[] isTouch_last = new boolean[CONCURRENT_TOUCH];

		Thread thread = null;
		SurfaceHolder surfaceHolder;
		volatile boolean running = false;

		volatile boolean touched = false;
		volatile float touched_x, touched_y;
		
		public ControlView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
			surfaceHolder = getHolder();
			paint.setTextAlign(Align.CENTER);
			paint.setTextSize(20);
		}

		public void onResumeMySurfaceView() {
			running = true;
			thread = new Thread(this);
			thread.start();
		}

		public void onDestroy() {
			running = false;
		}

		public void onPauseMySurfaceView() {
			boolean retry = true;
			running = false;
			while (retry) {
				try {
					thread.join();
					retry = false;
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}

		@Override
		public void run() {
			while (running) {
				if (surfaceHolder.getSurface().isValid()) {
					Canvas canvas = surfaceHolder.lockCanvas();
					// ... actual drawing on canvas
					previewManager.draw(canvas, preview, mobileFPVApp.isStereoView(), mobileFPVApp.isStereoViewSmall());
					showGPSinformations(canvas);
					try {
						Thread.sleep(17); // 1000/25 = 40 Control inputs/minute
											// at max
					} catch (InterruptedException e) {
						// do nothing
					}
					surfaceHolder.unlockCanvasAndPost(canvas);
				}

			}
		}

		public void showGPSinformations(Canvas canvas) {
			paint.setStyle(Paint.Style.FILL);
			paint.setStrokeWidth(0);
			paint.setColor(Color.YELLOW);
			if (mobileFPVApp.isGPSActive()) {

				if (referenceLocation != null) {
					String distanceStr = String
							.format("%d", (int) referenceLocation
									.distanceTo(vehicleLocation))
							+ " m";
					drawStereoText(canvas, distanceStr, screenWidth / 8,
							screenHeight * 3 / 4);

					String altitudeStr = String
							.format("%d",
									(int) (vehicleLocation.getAltitude() - referenceLocation
											.getAltitude()))
							+ " m";
					drawStereoText(canvas, altitudeStr, screenWidth * 2 / 8,
							screenHeight * 3 / 4);

					String speedStr = String.format("%d",
							(int) vehicleLocation.getSpeed())
							+ " m/s";
					drawStereoText(canvas, speedStr, screenWidth * 3 / 8,
							screenHeight * 3 / 4);
				} else {
					drawStereoText(canvas, getString(R.string.gps_wait_for_reference), screenWidth / 4,
							screenHeight * 3 / 4);
				}
			}
			if (mobileFPVApp.isAttitudeActive()) {
				paint.setStrokeWidth(3.0f);
				
				drawStereoOrientation(canvas);			
			}
		}
		
		public void drawStereoOrientation(Canvas canvas) {
			displayOrientation(canvas, 0);
			if (mobileFPVApp.isStereoView()) {
				displayOrientation(canvas, screenWidth / 2);
			}
		}
	
		public void displayOrientation(Canvas canvas, int offset) {
			int factor = 2;
			if (mobileFPVApp.isStereoView()) {
				factor = 4;
			}
			displayRoll(canvas, offset, factor);

			displayYaw(canvas, offset, factor);
			
			displayPitch(canvas, offset, factor);
		}

		public void displayPitch(Canvas canvas, int offset, int factor) {
			canvas.setMatrix(null);
			int middlePitch = offset + (screenWidth / factor) + (screenWidth / (factor*2));
			canvas.rotate(- orientation.getPitch(), middlePitch, screenHeight * 3 / 5);
			float[] pts = {middlePitch - screenWidth / (factor*4), screenHeight * 3 / 5,
					middlePitch + screenWidth / (factor*4), screenHeight * 3 / 5,
					middlePitch + screenWidth / (factor*4), screenHeight * 3 / 5,
					middlePitch + screenWidth / (factor*4), screenHeight * 3 / 5 - screenHeight / 16,
					middlePitch + screenWidth / (factor*4), screenHeight * 3 / 5 - screenHeight / 16,
					middlePitch + screenWidth / (factor*4) - screenHeight / 16, screenHeight * 3 / 5};
			canvas.drawLines(pts, paint);
		}

		public void displayYaw(Canvas canvas, int offset, int factor) {
			canvas.setMatrix(null);
			int middleYaw = offset + (screenWidth / factor);
			canvas.rotate(orientation.getYaw(),
					middleYaw, screenHeight * 3 / 5);
			float[] yawFigure = { middleYaw - screenWidth / (factor*16), screenHeight * 3 / 5 + screenHeight / 16,
					middleYaw + screenWidth / (factor*16), screenHeight * 3 / 5 + screenHeight / 16,
					middleYaw - screenWidth / (factor*16), screenHeight * 3 / 5 + screenHeight / 16,
					middleYaw, screenHeight * 3 / 5 - screenHeight / 16,
					middleYaw + screenWidth / (factor*16), screenHeight * 3 / 5 + screenHeight / 16,
					middleYaw, screenHeight * 3 / 5 - screenHeight / 16};
			canvas.drawLines(yawFigure, paint);
		}

		public void displayRoll(Canvas canvas, int offset, int factor) {
			canvas.setMatrix(null);
			int middleRoll = offset + (screenWidth / factor) - (screenWidth / (factor*2));
			canvas.rotate(orientation.getRoll(),
					middleRoll, screenHeight * 3 / 5);
			float[] rollFigure = { middleRoll - screenWidth / (factor*4), screenHeight * 3 / 5,
					middleRoll + screenWidth / (factor*4), screenHeight * 3 / 5,
					middleRoll, screenHeight * 3 / 5,
					middleRoll, screenHeight * 3 / 5  - screenHeight / 16};
			canvas.drawLines(rollFigure, paint);
		}

		private void drawStereoText(Canvas canvas, String string, float xPos, float yPos) {
			if (mobileFPVApp.isStereoView()) {
				paint.setTextSize(20);
				canvas.drawText(string, xPos, yPos, paint);
				canvas.drawText(string, screenWidth / 2 + xPos, yPos, paint);
			} else {
				paint.setTextSize(25);
				canvas.drawText(string, xPos * 2, yPos , paint);				
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent motionEvent) {
			int pointerIndex = ((motionEvent.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			int pointerId = motionEvent.getPointerId(pointerIndex);
			int action = (motionEvent.getAction() & MotionEvent.ACTION_MASK);
			int pointCnt = motionEvent.getPointerCount();

			if (pointCnt <= CONCURRENT_TOUCH) {
				if (pointerIndex <= CONCURRENT_TOUCH - 1) {

					for (int i = 0; i < pointCnt; i++) {
						int id = motionEvent.getPointerId(i);
						isTouch_last[id] = isTouch[id];
						x[id] = motionEvent.getX(i);
						y[id] = motionEvent.getY(i);
					}

					switch (action) {
					case MotionEvent.ACTION_DOWN:
						isTouch[pointerId] = true;
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						isTouch[pointerId] = true;
						break;
					case MotionEvent.ACTION_MOVE:
						isTouch[pointerId] = true;
						break;
					case MotionEvent.ACTION_UP:
						isTouch[pointerId] = false;
						isTouch_last[pointerId] = false;
						break;
					case MotionEvent.ACTION_POINTER_UP:
						isTouch[pointerId] = false;
						isTouch_last[pointerId] = false;
						break;
					case MotionEvent.ACTION_CANCEL:
						isTouch[pointerId] = false;
						isTouch_last[pointerId] = false;
						break;
					default:
						isTouch[pointerId] = false;
						isTouch_last[pointerId] = false;
					}
				}
			}
			return true;
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return sendToVehicleTask;
	}

	private void showMessage(final String message) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(parentActivity, message, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	private class ConnectOtherPointTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... urls) {
			try {
				datagrammNode.initDirectIPCommunication(InetAddress
						.getByName(mobileFPVApp.getConnectionServerHostname()),
						9876);
				udpConnectionEstablished = true;
			} catch (UnknownHostException e1) {
				udpConnectionEstablished = false;
				showMessage("unknown host");
			} catch (NoConnectionException e1) {
				udpConnectionEstablished = false;
				showMessage("no udp connection");
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void nothing) {
			if (dialog != null) {
				dialog.dismiss();
			}
			if (sendToVehicleTask != null) {
				sendToVehicleTask
						.setUdpConnectionEstablished(udpConnectionEstablished);
			}
		}
	}

	public void initConnection() {
		ConnectOtherPointTask task = new ConnectOtherPointTask();
		task.execute();
	}

	@Override
	public void receivedDatagramm(byte[] content) {
		Object objContent = null;
		// Log.d("cp", "received " + content.length + " bytes");
		currentTime = System.currentTimeMillis();
		if (content[0] == -1 && content[1] == -40) {
			preview = new Preview();
			preview.setJpegImage(content);
		} else {
			try {
				objContent = ObjectConverter
						.deserializeByteArrayToObject(content);
			} catch (StreamCorruptedException e1) {
				this.showMessage("preview decoding error (stream corrupted)");
			} catch (IOException e1) {
				this.showMessage("preview decoding error (io)");
			} catch (ClassNotFoundException e1) {
				this.showMessage("receive unkown object");
			}
			if (objContent instanceof UavPosition) {
				UavPosition uavPos = (UavPosition) objContent;
				vehicleLocation.setLatitude(uavPos.getLatitude());
				vehicleLocation.setLongitude(uavPos.getLongitude());
				vehicleLocation.setAltitude(uavPos.getAltitude());
				vehicleLocation.setSpeed(uavPos.getSpeed());
				vehicleLocation.setBearing(uavPos.getDirection());
				if (referenceLocation == null) {
					referenceLocation = new Location("reference");
					referenceLocation.set(vehicleLocation);
				}
			} else if (objContent instanceof UavOrientation) {
				orientation = (UavOrientation)objContent;
			}
		}
	}

	@Override
	public void communicateTaskCaller(long currentTime) throws IOException {
		if (currentTime > (time40fps + MICROSEC_40FPS)) {
			time40fps = currentTime;
			serializeObjectToByteArray = ObjectConverter
					.serializeObjectToByteArray(controlChannels);
			datagrammNode.sendToNode(serializeObjectToByteArray);
		}
	}

	@Override
	public void onInputDeviceAdded(int deviceId) {
		InputDeviceState state = getInputDeviceState(deviceId);
		// Log.i(TAG, "Device added: " + state.mDevice);
	}

	// Implementation of InputManager.InputDeviceListener.onInputDeviceChanged()
	@Override
	public void onInputDeviceChanged(int deviceId) {
		InputDeviceState state = mInputDeviceStates.get(deviceId);
		if (state != null) {
			mInputDeviceStates.remove(deviceId);
			state = getInputDeviceState(deviceId);
			// Log.i(TAG, "Device changed: " + state.mDevice);
		}
	}

	// Implementation of InputManager.InputDeviceListener.onInputDeviceRemoved()
	@Override
	public void onInputDeviceRemoved(int deviceId) {
		InputDeviceState state = mInputDeviceStates.get(deviceId);
		if (state != null) {
			// Log.i(TAG, "Device removed: " + state.mDevice);
			mInputDeviceStates.remove(deviceId);
		}
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		// Check that the event came from a joystick since a generic motion
		// event
		// could be almost anything.
		if (isJoystick(event.getSource())
				&& event.getAction() == MotionEvent.ACTION_MOVE) {
			// Update device state for visualization and logging.
			InputDeviceState state = getInputDeviceState(event.getDeviceId());
			if (state != null && state.onJoystickMotion(event)) {

				controlChannels.setYaw(normalizePwm(192 + (int) (event
						.getAxisValue(MotionEvent.AXIS_X) * 72)));
				controlChannels.setThrust(normalizePwm(192 - (int) (event
						.getAxisValue(MotionEvent.AXIS_Y) * 72)));
				controlChannels.setRoll(normalizePwm(192 + (int) (event
						.getAxisValue(MotionEvent.AXIS_RX) * 72)));
				controlChannels.setNick(normalizePwm(192 - (int) (event
						.getAxisValue(MotionEvent.AXIS_RY) * 72)));
				// Log.d("fpv", ""+controlChannels.toString());
				// mSummaryAdapter.show(state);
			}
		}
		return super.dispatchGenericMotionEvent(event);
	}

	private int normalizePwm(int value) {
		if (value < 127)
			return 127;
		if (value > 255)
			return 255;
		return value;
	}

	private static boolean isJoystick(int source) {
		return (source & InputDevice.SOURCE_CLASS_JOYSTICK) != 0;
	}

	private InputDeviceState getInputDeviceState(int deviceId) {
		InputDeviceState state = mInputDeviceStates.get(deviceId);
		if (state == null) {
			final InputDevice device = mInputManager.getInputDevice(deviceId);
			if (device == null) {
				return null;
			}
			state = new InputDeviceState(device);
			mInputDeviceStates.put(deviceId, state);
			// Log.i(TAG, "Device enumerated: " + state.mDevice);
		}
		return state;
	}

	/**
	 * Tracks the state of joystick axes and game controller buttons for a
	 * particular input device for diagnostic purposes.
	 */
	private static class InputDeviceState {
		private final InputDevice mDevice;
		private final int[] mAxes;
		private final float[] mAxisValues;
		private final SparseIntArray mKeys;

		public InputDeviceState(InputDevice device) {
			mDevice = device;

			int numAxes = 0;
			final List<MotionRange> ranges = device.getMotionRanges();
			for (MotionRange range : ranges) {
				if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
					numAxes += 1;
				}
			}

			mAxes = new int[numAxes];
			mAxisValues = new float[numAxes];
			int i = 0;
			for (MotionRange range : ranges) {
				if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
					mAxes[i++] = range.getAxis();
				}
			}

			mKeys = new SparseIntArray();
		}

		public InputDevice getDevice() {
			return mDevice;
		}

		public int getAxisCount() {
			return mAxes.length;
		}

		public int getAxis(int axisIndex) {
			return mAxes[axisIndex];
		}

		public float getAxisValue(int axisIndex) {
			return mAxisValues[axisIndex];
		}

		public int getKeyCount() {
			return mKeys.size();
		}

		public int getKeyCode(int keyIndex) {
			return mKeys.keyAt(keyIndex);
		}

		public boolean isKeyPressed(int keyIndex) {
			return mKeys.valueAt(keyIndex) != 0;
		}

		public boolean onKeyDown(KeyEvent event) {
			final int keyCode = event.getKeyCode();
			if (isGameKey(keyCode)) {
				if (event.getRepeatCount() == 0) {
					final String symbolicName = KeyEvent
							.keyCodeToString(keyCode);
					mKeys.put(keyCode, 1);
					// Log.i(TAG, mDevice.getName() + " - Key Down: "
					// + symbolicName);
				}
				return true;
			}
			return false;
		}

		public boolean onKeyUp(KeyEvent event) {
			final int keyCode = event.getKeyCode();
			if (isGameKey(keyCode)) {
				int index = mKeys.indexOfKey(keyCode);
				if (index >= 0) {
					final String symbolicName = KeyEvent
							.keyCodeToString(keyCode);
					mKeys.put(keyCode, 0);
					// Log.i(TAG, mDevice.getName() + " - Key Up: " +
					// symbolicName);
				}
				return true;
			}
			return false;
		}

		/**
		 * idroid:con - Joystick Motion: AXIS_X: 0.003921628 ... yaw -1.0 bis
		 * 1.0 AXIS_Y: 0.003921628 .. thrust 1.0 bis -1.0 AXIS_Z: 0.06666672
		 * AXIS_RX: 0.003921628 .. roll -1.0 bis 1.0 AXIS_RY: 0.003921628 ..
		 * pitch 1.0 bis -1.0 AXIS_HAT_X: 0.0 AXIS_HAT_Y: -1.0 AXIS_GENERIC_1:
		 * 0.0
		 */
		public boolean onJoystickMotion(MotionEvent event) {
			StringBuilder message = new StringBuilder();
			message.append(mDevice.getName()).append(" - Joystick Motion:\n");

			final int historySize = event.getHistorySize();
			for (int i = 0; i < mAxes.length; i++) {
				final int axis = mAxes[i];
				final float value = event.getAxisValue(axis);
				mAxisValues[i] = value;
				message.append("  ").append(MotionEvent.axisToString(axis))
						.append(": ");

				// Append all historical values in the batch.
				for (int historyPos = 0; historyPos < historySize; historyPos++) {
					message.append(event.getHistoricalAxisValue(axis,
							historyPos));
					message.append(", ");
				}

				// Append the current value.
				message.append(value);
				message.append("\n");
			}
			// Log.i(TAG, message.toString());
			return true;
		}

		// Check whether this is a key we care about.
		// In a real game, we would probably let the user configure which keys
		// to use
		// instead of hardcoding the keys like this.
		private static boolean isGameKey(int keyCode) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_SPACE:
				return true;
			default:
				return KeyEvent.isGamepadButton(keyCode);
			}
		}
	}
}
