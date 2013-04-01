package com.foggyciti.macremote;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Selection;
import android.widget.EditText;

public class SettingsActivity extends PreferenceActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		EditTextPreference passcodePreference = (EditTextPreference)findPreference(PersistenceHandler.getPasscodeKey(this));
		passcodePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				EditText editText = ((EditTextPreference)preference).getEditText();
				editText.setText(PersistenceHandler.getPasscode(SettingsActivity.this));
				Selection.setSelection(editText.getText(), editText.length());
				return false;
			}
		});
	}
}
