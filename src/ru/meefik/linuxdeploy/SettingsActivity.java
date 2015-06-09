package ru.meefik.linuxdeploy;

import java.io.File;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class SettingsActivity extends SherlockPreferenceActivity implements
		OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(PrefStore.APP_PREF_FILE_NAME);
		addPreferencesFromResource(R.xml.settings);

		this.initSummaries(this.getPreferenceScreen());
	}

	@Override
	public void onResume() {
		super.onResume();

		this.setTitle(R.string.title_activity_settings);
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		// set autostart settings
		int flag = (PrefStore.isAutostart(getApplicationContext()) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
				: PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
		ComponentName component = new ComponentName(this, EventsReceiver.class);
		getPackageManager().setComponentEnabledSetting(component, flag,
				PackageManager.DONT_KILL_APP);

		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		return false;
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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Preference pref = this.findPreference(key);
		this.setSummary(pref, true);
		if (pref.getKey().equals("debug") || pref.getKey().equals("trace"))
			PrefStore.PREF_CHANGE = true;
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

			if (editPref.getKey().equals("logfile")
					&& editPref.getText().equals("{replace}")) {
				File extStore = Environment.getExternalStorageDirectory();
				String logFile = extStore.getAbsolutePath()
						+ "/linuxdeploy.log";
				((EditTextPreference) pref).setText(logFile);
				((EditTextPreference) pref).setSummary(logFile);
			}
		}

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
	}

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
								PrefStore.PREF_CHANGE = false;
								(new Thread() {
									@Override
									public void run() {
										ShellEnv sh = new ShellEnv(
												getApplicationContext());
										sh.updateEnv();
										sh.updateConfig();
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
								PrefStore.PREF_CHANGE = false;
								(new Thread() {
									@Override
									public void run() {
										new ShellEnv(getApplicationContext())
												.execScript("uninstall");
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

}
