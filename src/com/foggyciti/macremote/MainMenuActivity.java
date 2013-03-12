package com.foggyciti.macremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.EditText;

/* controls view for entering passcode and for main menu */
public class MainMenuActivity extends Activity {
	private static final Integer PASSCODE_LEN = 4;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		String passcode = PersistenceHandler.getPasscode(this);
		if (null == passcode)
			setContentView(R.layout.passcode);
		else
			startMainMenu();
	}
	
	private void startMainMenu() {
		setContentView(R.layout.main_menu);
		
		boolean wifiSet = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
		if (!wifiSet) {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setMessage(R.string.wifi_not_connected_msg);
			alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					openWirelessSettings();
				}
			});
			alertDialogBuilder.create().show();
		}
	}
	
	public void openWirelessSettings() {
		startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
	}
	
	public void onSettingsButtonClick(View v) {
		setContentView(R.layout.passcode);
	}
	
	public void onRemoteButtonClick(View v) {
		Intent intent = new Intent(this, RemoteControlActivity.class);
		startActivity(intent);
	}
	
	public void onContinueButtonClick(View v) {
		EditText txt = (EditText)findViewById(R.id.passcode);
		String passcode = txt.getText().toString();
		if (passcode.length() != PASSCODE_LEN) {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setMessage(R.string.invalid_passcode_msg);
			alertDialogBuilder.setPositiveButton("OK", null);
			alertDialogBuilder.create().show();
		} else {
			PersistenceHandler.savePasscode(this, passcode);
			startMainMenu();
		}
	}
}
