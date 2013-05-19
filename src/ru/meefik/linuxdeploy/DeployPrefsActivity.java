package ru.meefik.linuxdeploy;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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

public class DeployPrefsActivity extends SherlockPreferenceActivity implements
		Preference.OnPreferenceClickListener, OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);

		PrefStore.updateLocale(this);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PreferenceManager prefMgr = this.getPreferenceManager();
		prefMgr.setSharedPreferencesName(PrefStore.CURRENT_PROFILE);
		this.addPreferencesFromResource(R.xml.properties);

		this.initSummaries(this.getPreferenceScreen());
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
	
	@Override
	public void onPause() {
		super.onPause();

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
		if (preference.getKey().equals("install")) {
			installDialog();
		}
		if (preference.getKey().equals("reconfigure")) {
			reconfigureDialog();
		}
		return true;
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
								PrefStore.PREF_CHANGE = false;
								(new Thread() {
									@Override
									public void run() {
										new ShellEnv(getApplicationContext())
												.updateConfig();
										new ShellEnv(getApplicationContext())
												.deployCmd("configure");
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

	private void installDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_install_preference)
				.setMessage(R.string.message_install_confirm_dialog)
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
												.updateConfig();
										new ShellEnv(getApplicationContext())
												.deployCmd("install");
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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Preference pref = this.findPreference(key);
		this.setSummary(pref, true);
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

			if (editPref.getKey().equals("serverdns")
					&& editPref.getText().length() == 0) {
				pref.setSummary(this
						.getString(R.string.summary_serverdns_preference));
			}
			if (editPref.getKey().equals("disksize")
					&& editPref.getText().equals("0")) {
				pref.setSummary(this
						.getString(R.string.summary_disksize_preference));
			}
			if (editPref.getKey().equals("diskimage")
					&& editPref.getText().equals("{replace}")) {
				File extStore = Environment.getExternalStorageDirectory();
				String imgFile = extStore.getAbsolutePath()+"/linux.img";
				((EditTextPreference) pref).setText(imgFile);
				((EditTextPreference) pref).setSummary(imgFile);
			}
			if (editPref.getKey().equals("logfile")
					&& editPref.getText().equals("{replace}")) {
				File extStore = Environment.getExternalStorageDirectory();
				String logFile = extStore.getAbsolutePath()+"/linuxdeploy.log";
				((EditTextPreference) pref).setText(logFile);
				((EditTextPreference) pref).setSummary(logFile);
			}
			if (editPref.getKey().equals("mountpath")
					&& editPref.getText().equals("{replace}")) {
				File extStore = Environment.getExternalStorageDirectory();
				String mntPath = extStore.getAbsolutePath();
				((EditTextPreference) pref).setText(mntPath);
				((EditTextPreference) pref).setSummary(mntPath);
			}
		}

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());

			if (listPref.getKey().equals("distribution")) {
				if (listPref.getValue().equals("debian")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.debian_suite_values);
					suite.setEntryValues(R.array.debian_suite_values);
					if (init)
						suite.setValue(getString(R.string.debian_suite));
					suite.setSummary(suite.getEntry());
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
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.debian_mirror));
					mirror.setSummary(mirror.getText());
				}
				if (listPref.getValue().equals("ubuntu")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.ubuntu_suite_values);
					suite.setEntryValues(R.array.ubuntu_suite_values);
					if (init)
						suite.setValue(getString(R.string.ubuntu_suite));
					suite.setSummary(suite.getEntry());
					// architecture
					ListPreference architecture = (ListPreference) this
							.findPreference("architecture");
					architecture.setEntries(R.array.ubuntu_architecture_values);
					architecture
							.setEntryValues(R.array.ubuntu_architecture_values);
					if (init)
						architecture
								.setValue(getString(R.string.ubuntu_architecture));
					architecture.setSummary(architecture.getEntry());
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.ubuntu_mirror));
					mirror.setSummary(mirror.getText());
				}
				if (listPref.getValue().equals("archlinux")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.archlinux_suite_values);
					suite.setEntryValues(R.array.archlinux_suite_values);
					if (init)
						suite.setValue(getString(R.string.archlinux_suite));
					suite.setSummary(suite.getEntry());
					// architecture
					ListPreference architecture = (ListPreference) this
							.findPreference("architecture");
					architecture
							.setEntries(R.array.archlinux_architecture_values);
					architecture
							.setEntryValues(R.array.archlinux_architecture_values);
					if (init)
						architecture
								.setValue(getString(R.string.archlinux_architecture));
					architecture.setSummary(architecture.getEntry());
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.archlinux_mirror));
					mirror.setSummary(mirror.getText());
				}
				if (listPref.getValue().equals("fedora")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.fedora_suite_values);
					suite.setEntryValues(R.array.fedora_suite_values);
					if (init)
						suite.setValue(getString(R.string.fedora_suite));
					suite.setSummary(suite.getEntry());
					// architecture
					ListPreference architecture = (ListPreference) this
							.findPreference("architecture");
					architecture.setEntries(R.array.fedora_architecture_values);
					architecture
							.setEntryValues(R.array.fedora_architecture_values);
					if (init)
						architecture
								.setValue(getString(R.string.fedora_architecture));
					architecture.setSummary(architecture.getEntry());
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.fedora_mirror));
					mirror.setSummary(mirror.getText());
				}
				if (listPref.getValue().equals("opensuse")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.opensuse_suite_values);
					suite.setEntryValues(R.array.opensuse_suite_values);
					if (init)
						suite.setValue(getString(R.string.opensuse_suite));
					suite.setSummary(suite.getEntry());
					// architecture
					ListPreference architecture = (ListPreference) this
							.findPreference("architecture");
					architecture
							.setEntries(R.array.opensuse_architecture_values);
					architecture
							.setEntryValues(R.array.opensuse_architecture_values);
					if (init)
						architecture
								.setValue(getString(R.string.opensuse_architecture));
					architecture.setSummary(architecture.getEntry());
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.opensuse_mirror));
					mirror.setSummary(mirror.getText());
				}
				if (listPref.getValue().equals("kali")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.kali_suite_values);
					suite.setEntryValues(R.array.kali_suite_values);
					if (init)
						suite.setValue(getString(R.string.kali_suite));
					suite.setSummary(suite.getEntry());
					// architecture
					ListPreference architecture = (ListPreference) this
							.findPreference("architecture");
					architecture.setEntries(R.array.kali_architecture_values);
					architecture
							.setEntryValues(R.array.kali_architecture_values);
					if (init)
						architecture
								.setValue(getString(R.string.kali_architecture));
					architecture.setSummary(architecture.getEntry());
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.kali_mirror));
					mirror.setSummary(mirror.getText());
				}
				if (listPref.getValue().equals("gentoo")) {
					// suite
					ListPreference suite = (ListPreference) this
							.findPreference("suite");
					suite.setEntries(R.array.gentoo_suite_values);
					suite.setEntryValues(R.array.gentoo_suite_values);
					if (init)
						suite.setValue(getString(R.string.gentoo_suite));
					suite.setSummary(suite.getEntry());
					// architecture
					ListPreference architecture = (ListPreference) this
							.findPreference("architecture");
					architecture.setEntries(R.array.gentoo_architecture_values);
					architecture
							.setEntryValues(R.array.gentoo_architecture_values);
					if (init)
						architecture
								.setValue(getString(R.string.gentoo_architecture));
					architecture.setSummary(architecture.getEntry());
					// mirror
					EditTextPreference mirror = (EditTextPreference) this
							.findPreference("mirror");
					if (init)
						mirror.setText(getString(R.string.gentoo_mirror));
					mirror.setSummary(mirror.getText());
				}
			}
			if (listPref.getKey().equals("deploytype")) {
				EditTextPreference disksize = (EditTextPreference) this
						.findPreference("disksize");
				ListPreference fstype = (ListPreference) this
						.findPreference("fstype");
				
				// for compatibility with version < 1.3.9
				if (listPref.getValue().equals("image"))
					listPref.setValue("file");
				
				if (listPref.getValue().equals("file"))
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
