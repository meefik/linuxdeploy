package ru.meefik.linuxdeploy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class DeployPrefsActivity extends PreferenceActivity implements
		Preference.OnPreferenceClickListener, OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);

		PrefStore.updateLocale(this);

		PreferenceManager prefMgr = this.getPreferenceManager();
		prefMgr.setSharedPreferencesName(PrefStore.CURRENT_PROFILE);
		this.addPreferencesFromResource(R.xml.properties);

		this.initSummaries(this.getPreferenceScreen());
	}

	@Override
	public void onPause() {
		super.onPause();

		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals("reconfigure")) {
			reconfigureDialog();
		}
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();

		String titleMsg = this.getString(R.string.title_activity_properties)
				+ ": " + PrefStore.getCurrentProfile(getApplicationContext());
		this.setTitle(titleMsg);

		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

	}

	private void reconfigureDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_reconfigure_preference)
				.setMessage(R.string.message_reconfigure_confirm_dialog)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								new ShellEnv(getApplicationContext())
										.DeployCmd("configure");
								finish();
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						}).show();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Preference pref = this.findPreference(key);
		this.setSummary(pref, true);
	}

	private void initSummaries(PreferenceGroup pg) {
		for (int i = 0; i < pg.getPreferenceCount(); ++i) {
			Preference p = pg.getPreference(i);
			if (p instanceof PreferenceGroup)
				this.initSummaries((PreferenceGroup) p);
			else
				this.setSummary(p, false);
			if (p instanceof PreferenceScreen)
				p.setOnPreferenceClickListener(this);
		}
	}

	private void setSummary(Preference pref, boolean init) {
		if (pref instanceof EditTextPreference) {
			EditTextPreference editPref = (EditTextPreference) pref;
			pref.setSummary(editPref.getText());

			if (editPref.getKey().equals("serverdns")
					&& editPref.getText().length() == 0) {
				pref.setSummary(this
						.getString(R.string.summary_serverdns_preference));
			}
		}

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());

			if (listPref.getKey().equals("distribution")) {
				if (listPref.getEntry().equals("Debian")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.debian_suite_values);
					suite.setEntryValues(R.array.debian_suite_values);
					if (init)
						suite.setValue(getString(R.string.debian_suite));
					suite.setSummary(suite.getEntry());
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.debian_mirror));
					mirror.setSummary(mirror.getText());
					// architecture
					ListPreference architecture = (ListPreference) this
							.findPreference("architecture");
					architecture.setEntries(R.array.debian_architecture_values);
					architecture
							.setEntryValues(R.array.debian_architecture_values);
					if (init)
						architecture
								.setValue(getString(R.string.debian_architecture));
					architecture.setSummary(architecture.getEntry());
					// desktopenv
					ListPreference desktopenv = (ListPreference) this
							.findPreference("desktopenv");
					desktopenv.setEntries(R.array.debian_desktopenv_values);
					desktopenv.setEntryValues(R.array.debian_desktopenv_values);
					if (init)
						desktopenv.setValue(getString(R.string.desktopenv));
					desktopenv.setSummary(desktopenv.getEntry());
				}
				if (listPref.getEntry().equals("Ubuntu")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.ubuntu_suite_values);
					suite.setEntryValues(R.array.ubuntu_suite_values);
					if (init)
						suite.setValue(getString(R.string.ubuntu_suite));
					suite.setSummary(suite.getEntry());
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.ubuntu_mirror));
					mirror.setSummary(mirror.getText());
					// architecture
					ListPreference architecture = (ListPreference) this
							.findPreference("architecture");
					architecture.setEntries(R.array.debian_architecture_values);
					architecture
							.setEntryValues(R.array.debian_architecture_values);
					if (init)
						architecture
								.setValue(getString(R.string.debian_architecture));
					architecture.setSummary(architecture.getEntry());
					// desktopenv
					ListPreference desktopenv = (ListPreference) this
							.findPreference("desktopenv");
					desktopenv.setEntries(R.array.ubuntu_desktopenv_values);
					desktopenv.setEntryValues(R.array.ubuntu_desktopenv_values);
					if (init)
						desktopenv.setValue(getString(R.string.desktopenv));
					desktopenv.setSummary(desktopenv.getEntry());
				}
			}
			if (listPref.getKey().equals("deploytype")) {
				EditTextPreference disksize = (EditTextPreference) this
						.findPreference("disksize");
				ListPreference fstype = (ListPreference) this
						.findPreference("fstype");
				if (listPref.getValue().equals("image"))
					disksize.setEnabled(true);
				else
					disksize.setEnabled(false);
				if (listPref.getValue().equals("directory"))
					fstype.setEnabled(false);
				else
					fstype.setEnabled(true);
			}
		}
	}

}
