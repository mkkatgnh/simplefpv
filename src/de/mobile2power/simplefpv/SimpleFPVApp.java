package de.mobile2power.simplefpv;

import java.util.ArrayList;
import java.util.List;

import de.mobile2power.simplefpv.external.BluetoothManager;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;

public class SimpleFPVApp extends Application {
	private static final String PREFS_NAME = "MobileFPVAppPrefV1";
	private static final String HOSTNAME = "hostname";
	private static final String HOSTPORT = "hostport";
	private static final String CONNECTION_ID = "connectionID";
	private static final String BLUETOOTH_NAME_AND_ADDRESS = "bluetoothNameAndAddress";
	private static final String SAVE_DATA_FOLDER = "saveDataFolder";
	private static final String TRANSMIT_LIVE_PREVIEW = "transmitLivePreview";
	private static final String MIDDLE_POSITION_IS_ZERO = "middlePositionIsZero";
	private static final String GPS_IS_ACTIVE = "gpsIsActive";
	private static final String ATTITUDE_IS_ACTIVE = "attitudeIsActive";
	private static final String STEREO_VIEW = "stereoView";
	private static final String STEREO_VIEW_SMALL = "stereoViewSmall";
	private static final String COMPRESSION_VALUE = "compressionValue";
	private static final String FPS_VALUE = "fpsValue";

	private String connectionServerHostname;
	private int connectionServerPort;
	private String connectionIdentifier;
	private String bluetoothNameAndAddress;
	private int compressionValue;
	private int fpsValue;
	private BluetoothManager bluetoothManager;
	private String saveDataFolder;
	private boolean middlePositionIsZero;
	private boolean transmitLivePreview;
	private boolean gpsActive;
	private boolean attitudeActive;
	private boolean stereoView;
	private boolean stereoViewSmall;
	private List<SettingsChanged> listener = new ArrayList<SettingsChanged>();

	@Override
	public void onCreate() {
		// Here you could pull values from a config file in res/raw or somewhere
		// else
		// but for simplicity's sake, we'll just hardcode values
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		connectionServerHostname = settings.getString(HOSTNAME, Constants.DEFAULT_P2P_IP);
		connectionServerPort = settings.getInt(HOSTPORT, Constants.DEFAULT_P2P_SERVERPORT);
		compressionValue = settings.getInt(COMPRESSION_VALUE, Constants.DEFAULT_COMPRESSION);
		connectionIdentifier = settings.getString(CONNECTION_ID, Constants.DEFAULT_P2P_ID);
		bluetoothNameAndAddress = settings.getString(BLUETOOTH_NAME_AND_ADDRESS, ",");
		saveDataFolder = settings.getString(SAVE_DATA_FOLDER, Environment.getExternalStorageDirectory().getPath());
		transmitLivePreview = settings.getBoolean(TRANSMIT_LIVE_PREVIEW, true);
		middlePositionIsZero = settings.getBoolean(MIDDLE_POSITION_IS_ZERO, false);
		fpsValue = settings.getInt(FPS_VALUE, Constants.DEFAULT_FPS);
		gpsActive = settings.getBoolean(GPS_IS_ACTIVE, false);
		attitudeActive = settings.getBoolean(ATTITUDE_IS_ACTIVE, false);
		stereoView = settings.getBoolean(STEREO_VIEW, true);
		stereoViewSmall = settings.getBoolean(STEREO_VIEW_SMALL, false);
		
		bluetoothManager = new BluetoothManager();
		super.onCreate();
	}

	public void addListener(SettingsChanged listener) {
		this.listener.add(listener);
	}
	
	public void removeListener(SettingsChanged listener) {
		this.listener.remove(listener);
	}
	
	private void informListeners() {
		for (SettingsChanged changed : this.listener)
			changed.settingsChangedListener();
	}
	
	public void persistPreferences() {

		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(HOSTNAME, connectionServerHostname);
		editor.putInt(HOSTPORT, connectionServerPort);
		editor.putString(CONNECTION_ID, connectionIdentifier);
		editor.putString(BLUETOOTH_NAME_AND_ADDRESS, bluetoothNameAndAddress);
		editor.putString(SAVE_DATA_FOLDER, saveDataFolder);
		editor.putBoolean(TRANSMIT_LIVE_PREVIEW, transmitLivePreview);
		editor.putBoolean(MIDDLE_POSITION_IS_ZERO, middlePositionIsZero);
		editor.putInt(COMPRESSION_VALUE, compressionValue);
		editor.putInt(FPS_VALUE, fpsValue);
		editor.putBoolean(GPS_IS_ACTIVE, gpsActive);
		editor.putBoolean(ATTITUDE_IS_ACTIVE, attitudeActive);
		editor.putBoolean(STEREO_VIEW, stereoView);
		editor.putBoolean(STEREO_VIEW_SMALL, stereoViewSmall);

		// Commit the edits!
		editor.commit();
	}

	public String getConnectionServerHostname() {
		return connectionServerHostname;
	}

	public void setConnectionServerHostname(String connectionServerHostname) {
		this.connectionServerHostname = connectionServerHostname;
		informListeners();
	}

	public int getConnectionServerPort() {
		return connectionServerPort;
	}

	public void setConnectionServerPort(int connectionServerPort) {
		this.connectionServerPort = connectionServerPort;
		informListeners();
	}

	public String getConnectionIdentifier() {
		return connectionIdentifier;
	}

	public void setConnectionIdentifier(String connectionIdentifier) {
		this.connectionIdentifier = connectionIdentifier;
		informListeners();
	}

	public String getBluetoothNameAndAddress() {
		return bluetoothNameAndAddress;
	}

	public void setBluetoothNameAndAddress(String bluetoothNameAndAddress) {
		this.bluetoothNameAndAddress = bluetoothNameAndAddress;
		informListeners();
	}

	public BluetoothManager getBluetoothManager() {
		return bluetoothManager;
	}

	public String getSaveDataFolder() {
		return saveDataFolder;
	}

	public void setSaveDataFolder(String saveDataFolder) {
		this.saveDataFolder = saveDataFolder;
		informListeners();
	}

	public boolean isTransmitLivePreview() {
		return transmitLivePreview;
	}

	public void setTransmitLivePreview(boolean transmitLivePreview) {
		this.transmitLivePreview = transmitLivePreview;
	}

	public boolean isMiddlePositionIsZero() {
		return middlePositionIsZero;
	}

	public void setMiddlePositionIsZero(boolean mittlePositionIsZero) {
		this.middlePositionIsZero = mittlePositionIsZero;
		informListeners();
	}

	public boolean isGPSActive() {
		return gpsActive;
	}

	public void setGPSActive(boolean gpsActive) {
		this.gpsActive = gpsActive;
		informListeners();
	}

	public boolean isAttitudeActive() {
		return attitudeActive;
	}

	public void setAttitudeActive(boolean attitudeActive) {
		this.attitudeActive = attitudeActive;
		informListeners();
	}

	public boolean isStereoView() {
		return stereoView;
	}
	
	public void setStereoView(boolean stereoView) {
		this.stereoView = stereoView;
		informListeners();
	}

	public boolean isStereoViewSmall() {
		return stereoViewSmall;
	}
	
	public void setStereoViewSmall(boolean stereoViewSmall) {
		this.stereoViewSmall = stereoViewSmall;
		informListeners();
	}

	public int getCompressionValue() {
		return compressionValue;
	}

	public void setCompressionValue(int compressionValue) {
		this.compressionValue = compressionValue;
		informListeners();
	}

	public int getFpsValue() {
		return fpsValue;
	}

	public void setFpsValue(int fpsValue) {
		this.fpsValue = fpsValue;
	}

}
