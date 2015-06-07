package ru.meefik.linuxdeploy;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.h6ah4i.android.compat.preference.MultiSelectListPreferenceCompat;

public class PropertiesActivity extends SherlockPreferenceActivity implements
		Preference.OnPreferenceClickListener, OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PreferenceManager prefMgr = this.getPreferenceManager();
		prefMgr.setSharedPreferencesName(PrefStore.CURRENT_PROFILE);

		Bundle b = getIntent().getExtras();
		int pref = 0;
		if (b != null)
			pref = b.getInt("pref");
		switch (pref) {
		case 1:
			this.addPreferencesFromResource(R.xml.properties_ssh);
			break;
		case 2:
			this.addPreferencesFromResource(R.xml.properties_vnc);
			break;
		case 3:
			this.addPreferencesFromResource(R.xml.properties_xserver);
			break;
		case 4:
			this.addPreferencesFromResource(R.xml.properties_framebuffer);
			break;
		default:
			this.addPreferencesFromResource(R.xml.properties);
		}

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
		if (preference.getKey().equals("sshproperties")) {
			Intent intent = new Intent(getApplicationContext(),
					PropertiesActivity.class);
			Bundle b = new Bundle();
			b.putInt("pref", 1);
			intent.putExtras(b);
			startActivity(intent);
		}
		if (preference.getKey().equals("guiproperties")) {
			ListPreference guitype = (ListPreference) this
					.findPreference("guitype");
			if (guitype.getValue().equals("vnc")) {
				Intent intent = new Intent(getApplicationContext(),
						PropertiesActivity.class);
				Bundle b = new Bundle();
				b.putInt("pref", 2);
				intent.putExtras(b);
				startActivity(intent);
			}
			if (guitype.getValue().equals("xserver")) {
				Intent intent = new Intent(getApplicationContext(),
						PropertiesActivity.class);
				Bundle b = new Bundle();
				b.putInt("pref", 3);
				intent.putExtras(b);
				startActivity(intent);
			}
			if (guitype.getValue().equals("framebuffer")) {
				Intent intent = new Intent(getApplicationContext(),
						PropertiesActivity.class);
				Bundle b = new Bundle();
				b.putInt("pref", 4);
				intent.putExtras(b);
				startActivity(intent);
			}
		}
		if (preference.getKey().equals("scriptseditor")) {
			Intent intent = new Intent(getApplicationContext(),
					ScriptsActivity.class);
			startActivity(intent);
		}
		if (preference.getKey().equals("mountseditor")) {
			Intent intent = new Intent(getApplicationContext(),
					MountsActivity.class);
			startActivity(intent);
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
												.execScript("configure");
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
												.execScript("install");
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
			if (editPref.getText().equals("{replace}")) {
				if (editPref.getKey().equals("diskimage")) {
					File extStore = Environment.getExternalStorageDirectory();
					String imgFile = extStore.getAbsolutePath() + "/linux.img";
					((EditTextPreference) pref).setText(imgFile);
					((EditTextPreference) pref).setSummary(imgFile);
				}
				if (editPref.getKey().equals("vncwidth")) {
					String vncWidth = String.valueOf(PrefStore
							.getWidth(getApplicationContext()));
					((EditTextPreference) pref).setText(vncWidth);
					((EditTextPreference) pref).setSummary(vncWidth);
				}
				if (editPref.getKey().equals("vncheight")) {
					String vncHeight = String.valueOf(PrefStore
							.getHeight(getApplicationContext()));
					((EditTextPreference) pref).setText(vncHeight);
					((EditTextPreference) pref).setSummary(vncHeight);
				}
			}
		}

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());

			if (listPref.getKey().equals("distribution")) {
				ListPreference suite = (ListPreference) this
						.findPreference("suite");
				ListPreference architecture = (ListPreference) this
						.findPreference("architecture");
				EditTextPreference mirror = (EditTextPreference) this
						.findPreference("mirror");
				MultiSelectListPreferenceCompat components = (MultiSelectListPreferenceCompat) this
						.findPreference("xcomponents");
		
				String distributionStr = listPref.getValue();
				
				// suite
				int suiteValuesId = PrefStore.getResourceId(this,
						distributionStr + "_suite_values", "array");
				if (suiteValuesId != -1) {
					suite.setEntries(suiteValuesId);
					suite.setEntryValues(suiteValuesId);
				}
				if (init) {
					int suiteId = PrefStore.getResourceId(this, distributionStr
							+ "_suite", "string");
					if (suiteId != -1) {
						String suiteStr = getString(suiteId);
						if (suiteStr.length() > 0) suite.setValue(suiteStr);
					}
				}
				suite.setSummary(suite.getEntry());
				suite.setEnabled(true);

				// architecture
				int architectureValuesId = PrefStore.getResourceId(this,
						distributionStr + "_architecture_values", "array");
				if (suiteValuesId != -1) {
					architecture.setEntries(architectureValuesId);
					architecture.setEntryValues(architectureValuesId);
				}
				if (init || architecture.getValue().equals("{replace}")) {
					int architectureId = PrefStore.getResourceId(this,
							PrefStore.MARCH + "_" + distributionStr
									+ "_architecture", "string");
					if (architectureId != -1) {
						String architectureStr = getString(architectureId);
						if (architectureStr.length() > 0) architecture.setValue(architectureStr);
					}
				}
				architecture.setSummary(architecture.getEntry());
				architecture.setEnabled(true);

				// mirror
				if (init || mirror.getText().equals("{replace}")) {
					int mirrorId = PrefStore
							.getResourceId(this, PrefStore.MARCH + "_"
									+ distributionStr + "_mirror", "string");
					if (mirrorId != -1) {
						mirror.setText(getString(mirrorId));
					}
				}
				mirror.setSummary(mirror.getText());
				mirror.setEnabled(true);

				// components
				int componentsEntriesId = PrefStore.getResourceId(this,
						distributionStr + "_components_entries", "array");
				int componentsValuesId = PrefStore.getResourceId(this,
						distributionStr + "_components_values", "array");
				if (componentsEntriesId != -1 && componentsValuesId != -1) {
					components.setEntries(componentsEntriesId);
					components.setEntryValues(componentsValuesId);
				}
				if (init) {
					components.setValues(new HashSet<String>(Arrays
							.asList(getResources().getStringArray(
									R.array.default_components))));
				}
				components.setEnabled(true);
				
				// RootFS
				if (distributionStr.equals("rootfs")) {
					// suite
					suite.setEnabled(false);
					// architecture
					architecture.setEnabled(false);
					// mirror
					if (init) {
						File extStore = Environment
								.getExternalStorageDirectory();
						String archiveFile = extStore.getAbsolutePath()
								+ "/linux-rootfs.tar.gz";
						mirror.setText(archiveFile);
					}
					mirror.setSummary(mirror.getText());
					mirror.setEnabled(true);
					// components
					components.setEnabled(false);
				}
			}
			if (listPref.getKey().equals("architecture") && init) {
				ListPreference distribution = (ListPreference) this
						.findPreference("distribution");
				EditTextPreference mirror = (EditTextPreference) this
						.findPreference("mirror");

				String architectureStr = PrefStore.getArch(listPref.getValue());
				String distributionStr = distribution.getValue();
				
				int mirrorId = PrefStore
						.getResourceId(this, architectureStr + "_"
								+ distributionStr + "_mirror", "string");
				if (mirrorId != -1) {
					mirror.setText(getString(mirrorId));
				}
				
				mirror.setSummary(mirror.getText());
			}
			if (listPref.getKey().equals("deploytype")) {
				EditTextPreference diskimage = (EditTextPreference) this
						.findPreference("diskimage");
				EditTextPreference disksize = (EditTextPreference) this
						.findPreference("disksize");
				ListPreference fstype = (ListPreference) this
						.findPreference("fstype");

				switch (listPref.getValue()) {
				case "file":
					if (init) {
						diskimage.setText(PrefStore.EXTERNAL_STORAGE + "/linux.img");
					}
					disksize.setEnabled(true);
					fstype.setEnabled(true);
					break;
				case "partition":
					if (init) {
						diskimage.setText("/dev/block/mmcblkXpY");
					}
					disksize.setEnabled(false);
					fstype.setEnabled(true);
					break;
				case "ram":
					if (init) {
						diskimage.setText("/data/local/tmp");
					}
					disksize.setEnabled(true);
					fstype.setEnabled(false);
					break;
				default:
					if (init) {
						diskimage.setText("/data/local/linux");
					}
					disksize.setEnabled(false);
					fstype.setEnabled(false);
				}
			}
		}

	}

}
