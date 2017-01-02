package de.mobile2power.simplefpv.vehicle;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.mobile2power.simplefpv.ConfigurationActivity;
import de.mobile2power.simplefpv.ConnectionDialog;
import de.mobile2power.simplefpv.Constants;
import de.mobile2power.simplefpv.IPointCommunication;
import de.mobile2power.simplefpv.R;
import de.mobile2power.simplefpv.SendToOppositeDeviceTask;
import de.mobile2power.simplefpv.SimpleFPVApp;
import de.mobile2power.simplefpv.external.SerialMWC;
import de.mobile2power.simplefpv.gps.GPSCallback;
import de.mobile2power.simplefpv.gps.GPSManager;
import de.mobile2power.simplefpv.groundcontrol.ObjectConverter;
import de.mobile2power.simplefpv.groundcontrol.VehicleEventType;
import de.mobile2power.simplefpv.groundcontrol.remotecontrol.ControlChannels;
import de.mobile2power.simplefpv.groundcontrol.remotecontrol.VehicleEvent;
import de.mobile2power.simplefpv.groundcontrol.uavcam.Preview;
import de.mobile2power.simplefpv.groundcontrol.uavlocation.UavOrientation;
import de.mobile2power.simplefpv.groundcontrol.uavlocation.UavPosition;
import de.mobile2power.simplefpv.rc.ControlPadActivity;
import eu.mightyfrog.udpcomm.node.DatagrammNode;
import eu.mightyfrog.udpcomm.node.IDatagrammNodeCallback;
import eu.mightyfrog.udpcomm.node.NoConnectionException;

public class VehicleActivity extends Activity implements
		IDatagrammNodeCallback, IPointCommunication, GPSCallback, SensorEventListener {

	SimpleFPVApp mobileFPVApp;

	private WifiManager wifi;
	private BroadcastReceiver receiver;

	private GPSManager gpsManager;

	private CamPreview camPreview; // <1>

	private WakeLock mWakeLock;
	private PowerManager mPowerManager;

	private Preview preview = null;

	private OutputStream bluetoothOutStream = null;
	private boolean bluetoothConnectionEstablished = false;
	private boolean udpConnectionEstablished = false;

	private SendToOppositeDeviceTask sendToGroundstationTask;
	DatagrammNode datagrammNode;
	private long currentTime = 0;

	private ControlChannels channels = null;
	private int[] channelArray = new int[8];

	private VehicleEventType event;
	final Activity parentActivity = this;
	private ProgressDialog dialog;

	private static final long MICROSEC_1FPS = 1000;
	private long time1fps = 0;
	private static final long MICROSEC_FPS[] = { 1000 / 10, 1000 / 15, 1000 / 20, 1000 / 25, 1000 / 30 };
	private long timeFps = MICROSEC_FPS[MICROSEC_FPS.length-1];
	private long time15fps = 0;
	private static final long MICROSEC_30FPS = 1000 / 30;
	private long time30fps = 0;
	private byte[] serializeObjectToByteArray;

	private boolean previewHasToRestart = false;
	
	private UavPosition currentPosition = new UavPosition();
	private UavOrientation currentOrientation = new UavOrientation();
	
	private SensorManager mSensorManager;
	private Sensor mRotationVectorSensor;
	private final float[] mRotationMatrix = new float[16];
	
	private boolean hasCamera = false;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PackageManager pm = getPackageManager();
		hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);

		setContentView(R.layout.activity_vehicle);

		mobileFPVApp = (SimpleFPVApp) getApplicationContext();

		/*
		//------------------------------
	      // Setup WiFi
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        
        // Get WiFi status
        WifiInfo info = wifi.getConnectionInfo();
		int ipAddress = info.getIpAddress();

		String ipString = String.format("%d.%d.%d.%d", (ipAddress & 0xff),
				(ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
				(ipAddress >> 24 & 0xff));

        // List available networks
        List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
//            textStatus.append("\n\n" + config.toString());
        }

        // Register Broadcast Receiver
        if (receiver == null)
            receiver = new WiFiScanReceiver(wifi);

        registerReceiver(receiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		//------------------------------
		*/
		
		preview = new Preview();
		if (mobileFPVApp.isTransmitLivePreview() && hasCamera) {
			initCamPreview();
		}

		datagrammNode = new DatagrammNode();
		datagrammNode.setCallbackListener(this);

		channels = new ControlChannels();
		if (mobileFPVApp.isMiddlePositionIsZero()) {
			channels.setThrust(127 + 64);
		} else {
			channels.setThrust(127 + 0);
		}
		channels.setNick(127);
		channels.setRoll(127);
		channels.setYaw(127);

		// Get an instance of the PowerManager
		mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
		// Create a bright wake lock
		mWakeLock = mPowerManager.newWakeLock(
				PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());

		if (mobileFPVApp.isGPSActive()) {
			gpsManager = new GPSManager();
			gpsManager.startListening(this);
			gpsManager.setGPSCallback(this);
		}

		timeFps = MICROSEC_FPS[mobileFPVApp.getFpsValue()];

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		mRotationVectorSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		
		setupNetwork();
		initButtons();
	}
	
	private void initButtons() {
		final Intent serverIntentConfigurationActivity = new Intent(this, ConfigurationActivity.class);

        final Button buttonSetting = (Button) findViewById(R.id.buttonSetting);
        buttonSetting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
    			startActivityForResult(serverIntentConfigurationActivity,
    					Constants.OPEN_SETTINGS_ACTIVITY);
          }
        });

        final Button buttonConnectAsVehicle = (Button) findViewById(R.id.buttonConnectAsVehicle);
        buttonConnectAsVehicle.setClickable(hasCamera);
        buttonConnectAsVehicle.setEnabled(hasCamera);
        if (hasCamera) {
			buttonConnectAsVehicle
					.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							connectAsVehicle();
							buttonConnectAsVehicle.setClickable(false);
							buttonConnectAsVehicle.setEnabled(false);
						}

					});
		}

		final Intent serverIntentControlPadActivity = new Intent(this, ControlPadActivity.class);

        final Button buttonConnectAsCC = (Button) findViewById(R.id.buttonConnectAsCC);
        buttonConnectAsCC.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
    			startActivity(serverIntentControlPadActivity);
    			finish();
           }
        });
 
        final Button buttonConnectBtDevice = (Button) findViewById(R.id.buttonConnectBtDevice);
        buttonConnectBtDevice.setClickable(hasCamera);
        buttonConnectBtDevice.setEnabled(hasCamera);
        buttonConnectBtDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
    			connectBTDevice();
          }

        });
	}

	public void connectAsVehicle() {
		dialog = ConnectionDialog.openDialog(getString(R.string.connect_to_rc), this);
		setupNetwork();
		initConnection();
	}

	public void connectBTDevice() {
		bluetoothConnectionEstablished = mobileFPVApp.getBluetoothManager()
				.connectToDevice(mobileFPVApp.getBluetoothNameAndAddress());
		TextView bluetoothConnectionStatus = (TextView) findViewById(R.id.connectionStateBT);
		if (bluetoothConnectionEstablished) {
			bluetoothOutStream = mobileFPVApp.getBluetoothManager()
					.getOutputStream();
			bluetoothConnectionStatus
					.setText(getString(R.string.connection_state_bt_established));
			transferChannelsToBluetooth();
		} else {
			Toast.makeText(this, "Cannot connect to bluetooth device",
					Toast.LENGTH_LONG).show();
			bluetoothConnectionStatus.setText(getString(R.string.connection_state_bt_not_established));
		}
	}

	private void setupNetwork() {
		sendToGroundstationTask = (SendToOppositeDeviceTask) getLastNonConfigurationInstance();
		if (sendToGroundstationTask == null
				|| !sendToGroundstationTask.isAlive()) {
			datagrammNode = new DatagrammNode();
			sendToGroundstationTask = new SendToOppositeDeviceTask(this,
					datagrammNode);
			sendToGroundstationTask.setDatagrammNode(datagrammNode);
			sendToGroundstationTask.start();
		} else {
			datagrammNode = sendToGroundstationTask.getDatagrammNode();
		}
		sendToGroundstationTask.setPointCommunication(this);
		datagrammNode.setCallbackListener(this);
	}

	private void closeNetwork() {
		if (sendToGroundstationTask != null) {
			sendToGroundstationTask.stopTask();
			while (sendToGroundstationTask.isAlive()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// do nothing
				}
			}
			sendToGroundstationTask = null;
		}
	}

	private void initCamPreview() {
		camPreview = new CamPreview(this); // <3>
		((FrameLayout) findViewById(R.id.preview)).addView(camPreview); // <4>
		camPreview.setPreviewDTO(preview);
		int compressionValue[] = { 2, 5, 10, 20, 30, 40, 50, 60, 70, 80 };
		camPreview.setCompressionValue(compressionValue[mobileFPVApp.getCompressionValue()]);
	}

	@Override
	public void receivedDatagramm(byte[] content) {
		Object objContent = null;
//		Log.d("vec", "received " + content.length + " bytes");
		try {
			objContent = ObjectConverter.deserializeByteArrayToObject(content);
		} catch (StreamCorruptedException e1) {
			this.showMessage("decode object error (stream corrupted)");
		} catch (IOException e1) {
			this.showMessage("decode object error (io)");
		} catch (ClassNotFoundException e1) {
			this.showMessage("receive unknown object");
		}
		if (objContent instanceof ControlChannels) {
			channels = (ControlChannels) objContent;
			transferChannelsToBluetooth();
		} else if (objContent instanceof VehicleEvent) {
			event = ((VehicleEvent) objContent).getType();
			switch (event) {
			case TAKE_PICTURE:
				break;
			default:
				break;
			}
		}
		currentTime = System.currentTimeMillis();
	}

	private void transferChannelsToBluetooth() {
		channelArray[Constants.YAW] = channels.getYaw();
		channelArray[Constants.THRUST] = channels.getThrust();
		channelArray[Constants.ROLL] = channels.getRoll();
		channelArray[Constants.NICK] = channels.getNick();
		channelArray[Constants.MOVE_YAW] = channels.getmYaw();
		channelArray[Constants.MOVE_ROLL] = channels.getmRoll();
		channelArray[Constants.MOVE_NICK] = channels.getmNick();
		byte[] controlSerialTransferArray = SerialMWC
				.transformControlCommands(channelArray);
		if (bluetoothConnectionEstablished && bluetoothOutStream != null) {
			try {
				bluetoothOutStream.write(controlSerialTransferArray);
				bluetoothOutStream.flush();
			} catch (IOException e) {
				// What do we do in case of connection lost to QC communication?
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
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
		// and release our wake-lock
		mWakeLock.release();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (this.isFinishing()) {
			if (gpsManager != null) {
				gpsManager.stopListening();
				gpsManager.setGPSCallback(null);

				gpsManager = null;
			}
			
			datagrammNode.setCallbackListener(null);
			closeNetwork();
			System.runFinalizersOnExit(true);
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return sendToGroundstationTask;
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
						.getByName(mobileFPVApp.getConnectionServerHostname()),9876);
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
			if (sendToGroundstationTask != null) {
				sendToGroundstationTask
						.setUdpConnectionEstablished(udpConnectionEstablished);
			}
		}
	}

	public void initConnection() {
		ConnectOtherPointTask task = new ConnectOtherPointTask();
		task.execute();
	}

	@Override
	public void communicateTaskCaller(long currentTime) throws IOException {
		if (currentTime > (time1fps + MICROSEC_1FPS)) {
			time1fps = currentTime;
			if (mobileFPVApp.isGPSActive() && currentPosition.isAccuracy()) {
				serializeObjectToByteArray = ObjectConverter
						.serializeObjectToByteArray(currentPosition);
				datagrammNode.sendToNode(serializeObjectToByteArray);
			}
		}
		if (currentTime > (time30fps + MICROSEC_30FPS)) {
			time30fps = currentTime;
			serializeObjectToByteArray = ObjectConverter.serializeObjectToByteArray(currentOrientation);
			datagrammNode.sendToNode(serializeObjectToByteArray);
		}
		if (currentTime > (time15fps + timeFps)) {
			time15fps = currentTime;
//			if (mobileRCApp.isTransmitLivePreview())
//				Log.d("vec", "transmitting");
//			if (preview.getJpegImage() != null)
//				Log.d("vec", "jpeg size: " + preview.getJpegImage().length);
			if (mobileFPVApp.isTransmitLivePreview()
			        && preview.getJpegImage() != null
					&& preview.getJpegImage().length > 0) {
//				serializeObjectToByteArray = ObjectConverter
//						.serializeObjectToByteArray(preview);
//				Log.d("vec", "send pic " + serializeObjectToByteArray.length);
				datagrammNode.sendToNode(preview.getJpegImage());
//				datagrammNode.sendToNode(serializeObjectToByteArray);
			}
		}
	}

	@Override
	public void onGPSUpdate(Location location) {
		currentPosition.setLongitude(location.getLongitude());
		currentPosition.setLatitude(location.getLatitude());
		currentPosition.setAltitude(location.getAltitude());
		currentPosition.setDirection(location.getBearing());
		currentPosition.setSpeed(location.getSpeed());
		if (location.hasAccuracy() && location.getAccuracy() < 12.0) {
			currentPosition.setAccuracy(true);
		} else {
			currentPosition.setAccuracy(false);
		}
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		// we received a sensor event. it is a good practice to check
		// that we received the proper event
		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			// convert the rotation-vector to a 4x4 matrix. the matrix
			// is interpreted by Open GL as the inverse of the
			// rotation-vector, which is what we want.
			// SensorManager.getRotationMatrixFromVector(
			// mRotationMatrix , event.values);
			// / R[ 0] R[ 1] R[ 2] 0 \
			// | R[ 4] R[ 5] R[ 6] 0 |
			// | R[ 8] R[ 9] R[10] 0 |
			// \ 0 0 0 1 /
	        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
	        float[] remapCoords = new float[16];
	        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X, remapCoords);
	        float[] orientationVals = new float[3];
			SensorManager.getOrientation(remapCoords, orientationVals );
	        
			int yaw = (int) Math.toDegrees(orientationVals[0]);
			int pitch = (int) Math.toDegrees(orientationVals[1]);
			int roll = (int) Math.toDegrees(orientationVals[2]);

//			Log.d("orientation", "R:"+roll+" P:"+pitch+" Y:"+yaw);

			currentOrientation.setYaw(yaw);
			currentOrientation.setPitch(pitch);
			currentOrientation.setRoll(roll);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	
}
