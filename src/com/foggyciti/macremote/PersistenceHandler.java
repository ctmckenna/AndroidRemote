package com.foggyciti.macremote;

import android.content.Context;
import android.content.SharedPreferences;

public class PersistenceHandler {
	private static final String PREFS_NAME="DefaultPrefs";
	
	private static final String passcodeKey = "@string/passcodeKey";

	private static SharedPreferences.Editor getEditor(Context c, String name) {
		return c.getSharedPreferences(name, Context.MODE_PRIVATE).edit();
	}
	
	private static SharedPreferences getPrefs(Context c, String name) {
		return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}
	
	public static void savePasscode(Context c, String passcode) {
		SharedPreferences.Editor editor = getEditor(c, PREFS_NAME);
		editor.putString(passcodeKey, passcode);
		editor.commit();
	}
	
	public static String getPasscode(Context c) {
		SharedPreferences prefs = getPrefs(c, PREFS_NAME);
		return prefs.getString(passcodeKey, null);
	}
}
