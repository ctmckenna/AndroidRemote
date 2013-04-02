package com.foggyciti.macremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/* controls view for entering passcode and for main menu */
public class MainMenuActivity extends Activity {
	private static final Integer PASSCODE_LEN = 4;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		String passcode = PersistenceHandler.getPasscode(this);
		if (null == passcode)
			startPasscodeInitialization();
		else
			startMainMenu();
	}
	
	private void startPasscodeInitialization() {
		setContentView(R.layout.passcode);
		LinearLayout ll = (LinearLayout)findViewById(R.id.passcode_ll);
		ll.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				EditText passcodeInput = (EditText)findViewById(R.id.passcode);
				if (passcodeInput.isFocused()) {
					passcodeInput.clearFocus();
					InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
				return false;
			}
		});
		ImageButton continueButton = (ImageButton)findViewById(R.id.continue_button);
		continueButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				ImageButton b = (ImageButton)v;
				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					b.setImageDrawable(v.getContext().getResources().getDrawable(R.drawable.pressed));
					break;
				case MotionEvent.ACTION_UP:
					b.setImageDrawable(v.getContext().getResources().getDrawable(R.drawable.hover));
					break;
				}
				return false;
			}
			
		});
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
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
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
