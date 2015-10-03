package ru.meefik.linuxdeploy;

import java.util.Arrays;
import java.util.HashSet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.EditText;

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

		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(PrefStore.CURRENT_PROFILE);

		Bundle b = getIntent().getExtras();
		int pref = 0;
		if (b != null)
			pref = b.getInt("pref");
		switch (pref) {
		case 1:
			addPreferencesFromResource(R.xml.properties_ssh);
			break;
		case 2:
			addPreferencesFromResource(R.xml.properties_vnc);
			break;
		case 3:
			addPreferencesFromResource(R.xml.properties_xserver);
			break;
		case 4:
			addPreferencesFromResource(R.xml.properties_framebuffer);
			break;
		default:
			addPreferencesFromResource(R.xml.properties);
		}

		initSummaries(getPreferenceScreen());
	}

	@Override
	public void onResume() {
		super.onResume();

		String titleMsg = getString(R.string.title_activity_properties)
				+ ": " + PrefStore.getCurrentProfile(getApplicationContext());
		setTitle(titleMsg);

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
		if (preference.getKey().equals("export")) {
			exportDialog();
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
			ListPreference guitype = (ListPreference) findPreference("guitype");
			Intent intent = new Intent(getApplicationContext(),
					PropertiesActivity.class);
			Bundle b = new Bundle();
			if (guitype.getValue().equals("vnc")) {
				b.putInt("pref", 2);
			}
			if (guitype.getValue().equals("xserver")) {
				b.putInt("pref", 3);
			}
			if (guitype.getValue().equals("framebuffer")) {
				b.putInt("pref", 4);
			}
			intent.putExtras(b);
			startActivity(intent);
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

	private void installDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_install_preference)
				.setMessage(R.string.message_install_confirm_dialog)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								PrefStore.CONF_CHANGE = false;
								new ExecScript(getApplicationContext(),
										"install").start();
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

	private void reconfigureDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_reconfigure_preference)
				.setMessage(R.string.message_reconfigure_confirm_dialog)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								PrefStore.CONF_CHANGE = false;
								new ExecScript(getApplicationContext(),
										"configure").start();
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

	private void exportDialog() {
		final EditText input = new EditText(this);
		final String rootfsArchive = PrefStore.EXTERNAL_STORAGE
				+ "/linux-rootfs.tar.gz";
		input.setText(rootfsArchive);
		new AlertDialog.Builder(this)
				.setTitle(R.string.title_export_preference)
				.setCancelable(false)
				.setView(input)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								PrefStore.CONF_CHANGE = false;
								new ExecScript(getApplicationContext(),
										"export " + input.getText()).start();
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
		Preference pref = findPreference(key);
		setSummary(pref, true);
		PrefStore.CONF_CHANGE = true;
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

			if (editPref.getKey().equals("serverdns")
					&& editPref.getText().length() == 0) {
				pref.setSummary(getString(R.string.summary_serverdns_preference));
			}
			if (editPref.getKey().equals("disksize")
					&& editPref.getText().equals("0")) {
				pref.setSummary(getString(R.string.summary_disksize_preference));
			}
		}

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());

			if (listPref.getKey().equals("distribution")) {
				ListPreference suite = (ListPreference) findPreference("suite");
				ListPreference architecture = (ListPreference) findPreference("architecture");
				EditTextPreference mirror = (EditTextPreference) findPreference("mirror");
				MultiSelectListPreferenceCompat components = (MultiSelectListPreferenceCompat) 
						findPreference("xcomponents");

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
						if (suiteStr.length() > 0)
							suite.setValue(suiteStr);
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
				if (init || architecture.getValue().length() == 0) {
					int architectureId = PrefStore.getResourceId(this,
							PrefStore.MARCH + "_" + distributionStr
									+ "_architecture", "string");
					if (architectureId != -1) {
						String architectureStr = getString(architectureId);
						if (architectureStr.length() > 0)
							architecture.setValue(architectureStr);
					}
				}
				architecture.setSummary(architecture.getEntry());
				architecture.setEnabled(true);

				// mirror
				if (init || mirror.getText().length() == 0) {
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
						String archiveFile = PrefStore.EXTERNAL_STORAGE + "/linux-rootfs.tar.gz";
						mirror.setText(archiveFile);
					}
					mirror.setSummary(mirror.getText());
					mirror.setEnabled(true);
					// components
					components.setEnabled(false);
				}
			}
			if (listPref.getKey().equals("architecture") && init) {
				ListPreference distribution = (ListPreference) findPreference("distribution");
				EditTextPreference mirror = (EditTextPreference) findPreference("mirror");

				String architectureStr = PrefStore.getArch(listPref.getValue());
				String distributionStr = distribution.getValue();

				int mirrorId = PrefStore.getResourceId(this, architectureStr
						+ "_" + distributionStr + "_mirror", "string");
				if (mirrorId != -1) {
					mirror.setText(getString(mirrorId));
				}

				mirror.setSummary(mirror.getText());
			}
			if (listPref.getKey().equals("deploytype")) {
				EditTextPreference diskimage = (EditTextPreference) findPreference("diskimage");
				EditTextPreference disksize = (EditTextPreference) findPreference("disksize");
				ListPreference fstype = (ListPreference) findPreference("fstype");

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
						diskimage.setText("/data/local/ram");
					}
					disksize.setEnabled(true);
					fstype.setEnabled(false);
					break;
				default:
					if (init) {
						diskimage.setText(PrefStore.EXTERNAL_STORAGE);
					}
					disksize.setEnabled(false);
					fstype.setEnabled(false);
				}
			}
		}

	}

}
