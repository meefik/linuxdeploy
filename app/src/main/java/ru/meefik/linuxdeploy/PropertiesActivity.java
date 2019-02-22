package ru.meefik.linuxdeploy;

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
import android.view.MenuItem;

public class PropertiesActivity extends AppCompatPreferenceActivity implements
        Preference.OnPreferenceClickListener, OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(PrefStore.getPropertiesSharedName());

        // Restore from conf file if open from main activity
        if (getIntent().getBooleanExtra("restore", false)) {
            PrefStore.restoreProperties(this);
        }

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
                addPreferencesFromResource(R.xml.properties_x11);
                break;
            case 4:
                addPreferencesFromResource(R.xml.properties_fb);
                break;
            case 5:
                addPreferencesFromResource(R.xml.properties_run_parts);
                break;
            case 6:
                addPreferencesFromResource(R.xml.properties_sysv);
                break;
            case 7:
                addPreferencesFromResource(R.xml.properties_pulse);
                break;
            default:
                addPreferencesFromResource(R.xml.properties);
        }

        initSummaries(getPreferenceScreen());
    }

    @Override
    public void setTheme(int resId) {
        super.setTheme(PrefStore.getTheme(this));
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
    public void onResume() {
        super.onResume();

        String titleMsg = getString(R.string.title_activity_properties)
                + ": " + PrefStore.getProfileName(this);
        setTitle(titleMsg);

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        // update configuration file
        PrefStore.dumpProperties(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("ssh_properties")) {
            Intent intent = new Intent(this, PropertiesActivity.class);
            Bundle b = new Bundle();
            b.putInt("pref", 1);
            intent.putExtras(b);
            startActivity(intent);
        }
        if (preference.getKey().equals("gui_properties")) {
            ListPreference graphics = (ListPreference) findPreference("graphics");
            Intent intent = new Intent(this, PropertiesActivity.class);
            Bundle b = new Bundle();
            if (graphics.getValue().equals("vnc")) {
                b.putInt("pref", 2);
            }
            if (graphics.getValue().equals("x11")) {
                b.putInt("pref", 3);
            }
            if (graphics.getValue().equals("fb")) {
                b.putInt("pref", 4);
            }
            intent.putExtras(b);
            startActivity(intent);
        }
        if (preference.getKey().equals("init_properties")) {
            ListPreference init = (ListPreference) findPreference("init");
            Intent intent = new Intent(this, PropertiesActivity.class);
            Bundle b = new Bundle();
            if (init.getValue().equals("run-parts")) {
                b.putInt("pref", 5);
            }
            if (init.getValue().equals("sysv")) {
                b.putInt("pref", 6);
            }
            intent.putExtras(b);
            startActivity(intent);
        }
        if (preference.getKey().equals("pulse_properties")) {
            Intent intent = new Intent(this, PropertiesActivity.class);
            Bundle b = new Bundle();
            b.putInt("pref", 7);
            intent.putExtras(b);
            startActivity(intent);
        }
        if (preference.getKey().equals("mounts_editor")) {
            Intent intent = new Intent(this, MountsActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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

            if (editPref.getKey().equals("dns")
                    && editPref.getText().isEmpty()) {
                pref.setSummary(getString(R.string.summary_dns_preference));
            }
            if (editPref.getKey().equals("disk_size")
                    && editPref.getText().equals("0")) {
                pref.setSummary(getString(R.string.summary_disk_size_preference));
            }
            if (editPref.getKey().equals("user_password") &&
                    editPref.getText().isEmpty()) {
                editPref.setText(PrefStore.generatePassword());
                pref.setSummary(editPref.getText());
            }
        }

        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());

            if (listPref.getKey().equals("distrib")) {
                ListPreference suite = (ListPreference) findPreference("suite");
                ListPreference architecture = (ListPreference) findPreference("arch");
                EditTextPreference sourcepath = (EditTextPreference) findPreference("source_path");

                String distributionStr = listPref.getValue();

                // suite
                int suiteValuesId = PrefStore.getResourceId(this,
                        distributionStr + "_suite_values", "array");
                if (suiteValuesId > 0) {
                    suite.setEntries(suiteValuesId);
                    suite.setEntryValues(suiteValuesId);
                }
                if (init) {
                    int suiteId = PrefStore.getResourceId(this, distributionStr
                            + "_suite", "string");
                    if (suiteId > 0) {
                        String suiteStr = getString(suiteId);
                        if (suiteStr.length() > 0)
                            suite.setValue(suiteStr);
                    }
                }
                suite.setSummary(suite.getEntry());
                suite.setEnabled(true);

                // architecture
                int architectureValuesId = PrefStore.getResourceId(this,
                        distributionStr + "_arch_values", "array");
                if (suiteValuesId > 0) {
                    architecture.setEntries(architectureValuesId);
                    architecture.setEntryValues(architectureValuesId);
                }
                if (init || architecture.getValue().length() == 0) {
                    int architectureId = PrefStore.getResourceId(this,
                            PrefStore.getArch() + "_" + distributionStr
                                    + "_arch", "string");
                    if (architectureId > 0) {
                        String architectureStr = getString(architectureId);
                        if (architectureStr.length() > 0)
                            architecture.setValue(architectureStr);
                    }
                }
                architecture.setSummary(architecture.getEntry());
                architecture.setEnabled(true);

                // source path
                if (init || sourcepath.getText().length() == 0) {
                    int sourcepathId = PrefStore
                            .getResourceId(this, PrefStore.getArch() + "_"
                                    + distributionStr + "_source_path", "string");
                    if (sourcepathId > 0) {
                        sourcepath.setText(getString(sourcepathId));
                    }
                }
                sourcepath.setSummary(sourcepath.getText());
                sourcepath.setEnabled(true);

                // RootFS
                if (distributionStr.equals("rootfs")) {
                    // suite
                    suite.setEnabled(false);
                    // architecture
                    architecture.setEnabled(false);
                    // source path
                    if (init) {
                        String archiveFile = getString(R.string.rootfs_archive);
                        sourcepath.setText(archiveFile);
                    }
                    sourcepath.setSummary(sourcepath.getText());
                    sourcepath.setEnabled(true);
                }
            }
            if (listPref.getKey().equals("arch") && init) {
                ListPreference distribution = (ListPreference) findPreference("distrib");
                EditTextPreference sourcepath = (EditTextPreference) findPreference("source_path");

                String architectureStr = PrefStore.getArch(listPref.getValue());
                String distributionStr = distribution.getValue();

                int sourcePathId = PrefStore.getResourceId(this, architectureStr
                        + "_" + distributionStr + "_source_path", "string");
                if (sourcePathId > 0) {
                    sourcepath.setText(getString(sourcePathId));
                }

                sourcepath.setSummary(sourcepath.getText());
            }
            if (listPref.getKey().equals("target_type")) {
                EditTextPreference targetpath = (EditTextPreference) findPreference("target_path");
                EditTextPreference disksize = (EditTextPreference) findPreference("disk_size");
                ListPreference fstype = (ListPreference) findPreference("fs_type");

                switch (listPref.getValue()) {
                    case "file":
                        if (init) {
                            targetpath.setText(getString(R.string.target_path_file));
                        }
                        disksize.setEnabled(true);
                        fstype.setEnabled(true);
                        break;
                    case "directory":
                        if (init) {
                            targetpath.setText(getString(R.string.target_path_directory));
                        }
                        disksize.setEnabled(false);
                        fstype.setEnabled(false);
                        break;
                    case "partition":
                        if (init) {
                            targetpath.setText(getString(R.string.target_path_partition));
                        }
                        disksize.setEnabled(false);
                        fstype.setEnabled(true);
                        break;
                    case "ram":
                        if (init) {
                            targetpath.setText(getString(R.string.target_path_ram));
                        }
                        disksize.setEnabled(true);
                        fstype.setEnabled(false);
                        break;
                    case "custom":
                        if (init) {
                            targetpath.setText(getString(R.string.target_path_custom));
                        }
                        disksize.setEnabled(false);
                        fstype.setEnabled(false);
                        break;
                }
            }
        }
    }
}
