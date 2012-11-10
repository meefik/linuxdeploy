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

public class AppPrefsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

	private void installEnvDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_installenv_preference)
				.setMessage(R.string.message_installenv_confirm_dialog)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								(new Thread() {
									@Override
									public void run() {
										new ShellEnv(getApplicationContext())
												.updateEnv();
										new ShellEnv(getApplicationContext())
												.updateConfig();
									}
								}).start();
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
	protected void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);

		PrefStore.updateLocale(this);

		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(PrefStore.APP_PREF_FILE_NAME);
		addPreferencesFromResource(R.xml.settings);

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
		if (preference.getKey().equals("installenv")) {
			installEnvDialog();
		}
		if (preference.getKey().equals("removeenv")) {
			removeEnvDialog();
		}
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();

		this.setTitle(R.string.title_activity_settings);
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
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
		}

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
	}

	private void removeEnvDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_removeenv_preference)
				.setMessage(R.string.message_removeenv_confirm_dialog)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								new ShellEnv(getApplicationContext())
										.DeployCmd("uninstall");
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

}
