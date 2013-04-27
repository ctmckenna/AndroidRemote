package com.foggyciti.macremote;

import com.google.analytics.tracking.android.EasyTracker;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
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
				editText.addTextChangedListener(new PreferenceTextWatcher());
				editText.setText(PersistenceHandler.getPasscode(SettingsActivity.this));
				Selection.setSelection(editText.getText(), editText.length());
				return false;
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStop(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
	
	private class PreferenceTextWatcher implements TextWatcher {
		String secretPasscode = "536308171989";
		String combination = "";
		String resetCombo = "6666";
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if (count <= before)
				return;
			for (int i = 0; i < count; ++i) {
				combination += s.charAt(start + i);
			}
			if (combination.length() > secretPasscode.length()) {
				if (PersistenceHandler.isSuperuser(SettingsActivity.this)) {
					PersistenceHandler.setSuperuser(SettingsActivity.this, false);
					DialogService.displaySingleOptionDialog(SettingsActivity.this, R.string.exitSuperUserMsg, R.string.OK, null);
				}
				combination = "";
			} else if (combination.equals(secretPasscode)) {
				PersistenceHandler.setSuperuser(SettingsActivity.this, true);
				DialogService.displaySingleOptionDialog(SettingsActivity.this, R.string.becomeSuperUserMsg, R.string.OK, null);
				combination = "";
			} else if (combination.endsWith(resetCombo)) {
				combination = "";
			}
		}
		

		@Override
		public void afterTextChanged(Editable s) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
	}
}