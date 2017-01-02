package de.mobile2power.simplefpv.vehicle;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class WiFiScanReceiver extends BroadcastReceiver {
	
	private WifiManager wifi = null;
	
	public WiFiScanReceiver(WifiManager wifi) {
		this.wifi = wifi;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
	    List<ScanResult> results = wifi.getScanResults();
	    ScanResult bestSignal = null;
	    for (ScanResult result : results) {
	      if (bestSignal == null
	          || WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0)
	        bestSignal = result;
	    }

	    String message = String.format("%s networks found. %s is the strongest.",
	        results.size(), bestSignal.SSID);
//	    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

}
