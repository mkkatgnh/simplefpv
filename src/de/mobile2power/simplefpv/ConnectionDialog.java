package de.mobile2power.simplefpv;

import android.app.ProgressDialog;
import android.content.Context;

public class ConnectionDialog {

	public static ProgressDialog openDialog(String message, Context context) {
		ProgressDialog progressBar = new ProgressDialog(context);
		progressBar.setCancelable(false);
		progressBar.setMessage(message);
		progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressBar.setProgress(0);
		progressBar.setMax(100);
		progressBar.show();
		return progressBar;
	}
}
