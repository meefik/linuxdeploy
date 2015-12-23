package ru.meefik.linuxdeploy;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
		super.onCreate(savedInstanceState);
		PrefStore.setLocale(this);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(PrefStore.APP_PREF_NAME);
		addPreferencesFromResource(R.xml.settings);

		initSummaries(getPreferenceScreen());
	}
	
    @Override
    public void setTheme(int resid) {
        super.setTheme(PrefStore.getTheme(this));
    }

	@Override
	public void onResume() {
		super.onResume();

		setTitle(R.string.title_activity_settings);
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
			updateEnvDialog();
		}
		if (preference.getKey().equals("removeenv")) {
			removeEnvDialog();
		}
		return true;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Preference pref = findPreference(key);
		setSummary(pref, true);
	}

	private void initSummaries(PreferenceGroup pg) {
		for (int i = 0; i < pg.getPreferenceCount(); ++i) {
			Preference p = pg.getPreference(i);
			if (p instanceof PreferenceGroup)
				initSummaries((PreferenceGroup) p);
			else
				setSummary(p, false);
			if (p instanceof PreferenceScreen)
				p.setOnPreferenceClickListener(this);
		}
	}

	private void setSummary(Preference pref, boolean init) {
		if (pref instanceof EditTextPreference) {
			EditTextPreference editPref = (EditTextPreference) pref;
			pref.setSummary(editPref.getText());

            if (editPref.getKey().equals("terminalcmd") && !init) {
                editPref.setText(PrefStore.getTerminalCmd(this));
                pref.setSummary(editPref.getText());
            }
			
            if (editPref.getKey().equals("logfile") && !init) {
                editPref.setText(PrefStore.getLogFile(this));
                pref.setSummary(editPref.getText());
            }
		}

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
	}

	private void updateEnvDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_installenv_preference)
				.setMessage(R.string.message_installenv_confirm_dialog)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								new ExecScript(getApplicationContext(),
										"update").start();
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
								new ExecScript(getApplicationContext(),
										"remove").start();
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
