package com.foggyciti.macremote;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/* controls view for entering passcode and for main menu */
public class MainMenuActivity extends Activity {
	private static final Integer PASSCODE_LEN = 4;
	private boolean alertedWifi = false;
	private ButtonStateManager buttonStateManager;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		String passcode = PersistenceHandler.getPasscode(this);
		buttonStateManager = new ButtonStateManager(this);
		if (null == passcode)
			startPasscodeInitialization();
		else
			startMainMenu();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_settings:
			goToSettings();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private WifiManager getWifiManager() {
		return (WifiManager)getSystemService(Context.WIFI_SERVICE);
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
		buttonStateManager.setImageButtonStates(R.id.continue_button, R.drawable.pressed, R.drawable.hover);

		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				displayPasscodeDialog();
			}
		}, 1200);
	}
	
	private void displayWifiDialog() {
		alertedWifi = true;
		DialogService.displaySingleOptionDialog(this, R.string.wifi_not_connected_msg, R.string.OK, new Callback() {
			public void callback() {
				openWirelessSettings();
			}
		});
	}
	
	private void startMainMenu() {
		setTheme(R.style.CustomTheme);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main_menu);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

		boolean wifiSet = getWifiManager().setWifiEnabled(true);
		if (!wifiSet) {
			displayWifiDialog();
		}
	}

	public void openWirelessSettings() {
		startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
	}
	
	public void goToSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
	public void onRemoteButtonClick(View v) {
		if (!alertedWifi && WifiManager.WIFI_STATE_ENABLED != getWifiManager().getWifiState()) {
			displayWifiDialog();
			return;
		}
		if (PersistenceHandler.isSuperuser(this)) {
			Intent intent = new Intent(this, DebugRemoteControlActivity.class);
			startActivity(intent);
		} else {
			Intent intent = new Intent(this, RemoteControlActivity.class);
			startActivity(intent);
		}
	}
	
	private void displayPasscodeDialog() {
		DialogService.displaySingleOptionDialog(this, R.string.passcode_info_msg, R.string.OK, null);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 2)
			finish();
	}
	
	public void onContinueButtonClick(View v) {
		EditText txt = (EditText)findViewById(R.id.passcode);
		String passcode = txt.getText().toString();
		if (passcode.length() != PASSCODE_LEN) {
			displayPasscodeDialog();
		} else {
			PersistenceHandler.savePasscode(this, passcode);
			
			Intent intent = new Intent(this, MainMenuActivity.class);
			startActivityForResult(intent, 2);
			//startMainMenu();
		}
	}
}
