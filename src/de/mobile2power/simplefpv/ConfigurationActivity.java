package de.mobile2power.simplefpv;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigurationActivity extends Activity {

	private SimpleFPVApp mobileFPVApp;
	private Spinner bluetoothDeviceSpinner;
	private Spinner compressionSpinner;
	private Spinner fpsSpinner;

	// private CheckBox transmitLivePreviewCheckBox;
	private CheckBox middlePositionIsZeroCheckBox;
	private CheckBox gpsActivationCheckBox;
	private CheckBox attitudeActivationCheckBox;
	
	private CheckBox stereoViewCheckBox;
	private CheckBox stereoViewSmallCheckBox;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_configuration);

		mobileFPVApp = (SimpleFPVApp) getApplicationContext();
		// transmitLivePreviewCheckBox = (CheckBox)
		// findViewById(R.id.videoIsTransmitted);
		middlePositionIsZeroCheckBox = (CheckBox)findViewById(R.id.thrustZeroIsMiddle);
		gpsActivationCheckBox = (CheckBox)findViewById(R.id.gpsActive);
		attitudeActivationCheckBox = (CheckBox)findViewById(R.id.attitudeActive);
		stereoViewCheckBox = (CheckBox)findViewById(R.id.stereoView);
		stereoViewSmallCheckBox = (CheckBox)findViewById(R.id.stereoViewSmall);

		addListenerOnTransmitLivePreviewCheckBox();
		addListenerOnMiddlePositionIsZeroCheckBox();
		addListenerOnGPSActivationCheckBox();
		addListenerOnAttitudeActivationCheckBox();
		addListenerOnStereoViewCheckBox();
		addListenerOnStereoViewSmallCheckBox();

		fillBluetoothSpiner();
		
		setCompressionSpiner();
		setFpsSpiner();

		setInputFromValues();
				
		String ownIpAddress;
		try {
			ownIpAddress = getIPAddresses("p2p");
			if (ownIpAddress == null) {
				ownIpAddress = getIPAddresses("wlan");
			}
		} catch (SocketException e) {
			ownIpAddress = "no p2p orwlan";
		}
		TextView textView = (TextView) findViewById(R.id.thisIP);
		textView.setText(ownIpAddress);
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();
		getValuesFromInput();
		mobileFPVApp.persistPreferences();
	}

	public void addListenerOnTransmitLivePreviewCheckBox() {
		/*
		 * transmitLivePreviewCheckBox.setOnClickListener(new OnClickListener()
		 * {
		 * 
		 * @Override public void onClick(View v) { if (((CheckBox)
		 * v).isChecked()) { mobileRCApp.setTransmitLivePreview(true); } else {
		 * mobileRCApp.setTransmitLivePreview(false); }
		 * 
		 * }
		 * 
		 * });
		 */
	}

	public void addListenerOnMiddlePositionIsZeroCheckBox() {
		middlePositionIsZeroCheckBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					mobileFPVApp.setMiddlePositionIsZero(true);
				} else {
					mobileFPVApp.setMiddlePositionIsZero(false);
				}
			}
		});
	}

	public void addListenerOnGPSActivationCheckBox() {
		gpsActivationCheckBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					mobileFPVApp.setGPSActive(true);
				} else {
					mobileFPVApp.setGPSActive(false);
				}
			}
		});
	}

	public void addListenerOnAttitudeActivationCheckBox() {
		attitudeActivationCheckBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					mobileFPVApp.setAttitudeActive(true);
				} else {
					mobileFPVApp.setAttitudeActive(false);
				}
			}
		});
	}

	public void addListenerOnStereoViewCheckBox() {
		stereoViewCheckBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					mobileFPVApp.setStereoView(true);
				} else {
					mobileFPVApp.setStereoView(false);
				}
			}
		});
	}

	private void setCompressionSpiner() {
		compressionSpinner = (Spinner) findViewById(R.id.compressionPercent);
		compressionSpinner.setSelection(mobileFPVApp.getCompressionValue());
	}

	private void setFpsSpiner() {
		fpsSpinner = (Spinner) findViewById(R.id.framesPerSecond);
		fpsSpinner.setSelection(mobileFPVApp.getFpsValue());
	}

	public void addListenerOnStereoViewSmallCheckBox() {
		stereoViewSmallCheckBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					mobileFPVApp.setStereoViewSmall(true);
				} else {
					mobileFPVApp.setStereoViewSmall(false);
				}
			}
		});
	}
	
	private void fillBluetoothSpiner() {
		List<String> spinnerArray = new ArrayList<String>();
		String[] boundedDevices = mobileFPVApp.getBluetoothManager()
				.getBoundedDevices();
		int selectedDevice = -1;
		int i = 0;
		if (boundedDevices != null) {
			for (String device : boundedDevices) {
				spinnerArray.add(device.split(",")[0]);
				if (device.equals(mobileFPVApp.getBluetoothNameAndAddress())) {
					selectedDevice = i;
				}
				i++;
			}
		} else {
			Toast.makeText(this,
					"There are no bounded bluetooth devices available",
					Toast.LENGTH_LONG).show();
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spinnerArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		bluetoothDeviceSpinner = (Spinner) findViewById(R.id.bluetoothSpinner);
		bluetoothDeviceSpinner.setAdapter(adapter);
		bluetoothDeviceSpinner.setSelection(selectedDevice);
	}

	private void getValuesFromInput() {
		EditText et = (EditText) findViewById(R.id.connectionHostname);
		mobileFPVApp.setConnectionServerHostname(et.getText().toString());
		String[] boundedDevices = mobileFPVApp.getBluetoothManager()
				.getBoundedDevices();
		if (bluetoothDeviceSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION) {
			mobileFPVApp
					.setBluetoothNameAndAddress(boundedDevices[bluetoothDeviceSpinner
							.getSelectedItemPosition()]);
		}
		if (compressionSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION) {
			mobileFPVApp.setCompressionValue(compressionSpinner
					.getSelectedItemPosition());
		}
		if (fpsSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION) {
			mobileFPVApp.setFpsValue(fpsSpinner
					.getSelectedItemPosition());
		}		
	}

	private void setInputFromValues() {
		EditText et = (EditText) findViewById(R.id.connectionHostname);
		et.setText(mobileFPVApp.getConnectionServerHostname());
		middlePositionIsZeroCheckBox.setChecked(mobileFPVApp.isMiddlePositionIsZero());
		gpsActivationCheckBox.setChecked(mobileFPVApp.isGPSActive());
		attitudeActivationCheckBox.setChecked(mobileFPVApp.isAttitudeActive());
		stereoViewCheckBox.setChecked(mobileFPVApp.isStereoView());
		stereoViewSmallCheckBox.setChecked(mobileFPVApp.isStereoViewSmall());
//		transmitLivePreviewCheckBox.setChecked(mobileRCApp
//				.isTransmitLivePreview());
	}

	public String getIPAddresses(String type) throws SocketException  {
		for (Enumeration<NetworkInterface> en = NetworkInterface
	            .getNetworkInterfaces(); en.hasMoreElements();) {
	        NetworkInterface intf = en.nextElement();
	        if (intf.getName().indexOf(type) >= 0) {
	        	for (Enumeration<InetAddress> enumIpAddr = intf
	        			.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	        		InetAddress inetAddress = enumIpAddr.nextElement();
	                if (inetAddress instanceof Inet4Address) {
	                    return inetAddress.getHostAddress();
	                }
	        	}
	        	
	        }
		}
		return null;
	}	
}
