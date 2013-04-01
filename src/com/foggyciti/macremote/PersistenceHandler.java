package com.foggyciti.macremote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PersistenceHandler {
	
	private static SharedPreferences.Editor getEditor(Context c) {
		return getPrefs(c).edit();
	}
	
	private static SharedPreferences getPrefs(Context c) {
		return PreferenceManager.getDefaultSharedPreferences(c);
	}
	
	public static String getPasscodeKey(Context c) {
		return c.getResources().getString(R.string.passcodeKey);
	}
	
	public static void savePasscode(Context c, String passcode) {
		SharedPreferences.Editor editor = getEditor(c);
		editor.putString(getPasscodeKey(c), passcode);
		editor.commit();
	}
	
	public static String getPasscode(Context c) {
		SharedPreferences prefs = getPrefs(c);
		return prefs.getString(getPasscodeKey(c), null);
	}
}
