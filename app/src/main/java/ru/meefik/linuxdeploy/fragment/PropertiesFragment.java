package ru.meefik.linuxdeploy.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import ru.meefik.linuxdeploy.PrefStore;
import ru.meefik.linuxdeploy.R;
import ru.meefik.linuxdeploy.activity.MountsActivity;
import ru.meefik.linuxdeploy.activity.PropertiesActivity;

public class PropertiesFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceClickListener, OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(PrefStore.getPropertiesSharedName());

        Intent i = getActivity().getIntent();
        if (i != null) {
            switch (i.getIntExtra("pref", 0)) {
                case 1:
                    setPreferencesFromResource(R.xml.properties_ssh, rootKey);
                    break;
                case 2:
                    setPreferencesFromResource(R.xml.properties_vnc, rootKey);
                    break;
                case 3:
                    setPreferencesFromResource(R.xml.properties_x11, rootKey);
                    break;
                case 4:
                    setPreferencesFromResource(R.xml.properties_fb, rootKey);
                    break;
                case 5:
                    setPreferencesFromResource(R.xml.properties_run_parts, rootKey);
                    break;
                case 6:
                    setPreferencesFromResource(R.xml.properties_sysv, rootKey);
                    break;
                case 7:
                    setPreferencesFromResource(R.xml.properties_pulse, rootKey);
                    break;
                default:
                    setPreferencesFromResource(R.xml.properties, rootKey);
            }
        }

        initSummaries(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "ssh_properties": {
                Intent intent = new Intent(getContext(), PropertiesActivity.class);
                intent.putExtra("pref", 1);
                startActivity(intent);
                break;
            }
            case "gui_properties": {
                Intent intent = new Intent(getContext(), PropertiesActivity.class);

                ListPreference graphics = findPreference("graphics");
                switch (graphics.getValue()) {
                    case "vnc":
                        intent.putExtra("pref", 2);
                        break;
                    case "x11":
                        intent.putExtra("pref", 3);
                        break;
                    case "fb":
                        intent.putExtra("pref", 4);
                        break;
                }

                startActivity(intent);
                break;
            }
            case "init_properties": {
                Intent intent = new Intent(getContext(), PropertiesActivity.class);

                ListPreference init = findPreference("init");
                switch (init.getValue()) {
                    case "run-parts":
                        intent.putExtra("pref", 5);
                        break;
                    case "sysv":
                        intent.putExtra("pref", 6);
                        break;
                }

                startActivity(intent);
                break;
            }
            case "pulse_properties": {
                Intent intent = new Intent(getContext(), PropertiesActivity.class);
                intent.putExtra("pref", 7);
                startActivity(intent);
                break;
            }
            case "mounts_editor": {
                Intent intent = new Intent(getContext(), MountsActivity.class);
                startActivity(intent);
                break;
            }
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
            if (editPref.getKey().equals("user_name")) {
                String userName = editPref.getText();
                String privilegedUsers = getString(R.string.privileged_users).replaceAll("android", userName);
                EditTextPreference editPrivilegedUsers = findPreference("privileged_users");
                editPrivilegedUsers.setText(privilegedUsers);
                editPrivilegedUsers.setSummary(privilegedUsers);
            }
        }

        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());

            if (listPref.getKey().equals("distrib")) {
                ListPreference suite = findPreference("suite");
                ListPreference architecture = findPreference("arch");
                EditTextPreference sourcepath = findPreference("source_path");

                String distributionStr = listPref.getValue();

                // suite
                int suiteValuesId = PrefStore.getResourceId(getContext(),
                        distributionStr + "_suite_values", "array");
                if (suiteValuesId > 0) {
                    suite.setEntries(suiteValuesId);
                    suite.setEntryValues(suiteValuesId);
                }
                if (init) {
                    int suiteId = PrefStore.getResourceId(getContext(), distributionStr
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
                int architectureValuesId = PrefStore.getResourceId(getContext(),
                        distributionStr + "_arch_values", "array");
                if (suiteValuesId > 0) {
                    architecture.setEntries(architectureValuesId);
                    architecture.setEntryValues(architectureValuesId);
                }
                if (init || architecture.getValue().length() == 0) {
                    int architectureId = PrefStore.getResourceId(getContext(),
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
                            .getResourceId(getContext(), PrefStore.getArch() + "_"
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
                ListPreference distribution = findPreference("distrib");
                EditTextPreference sourcepath = findPreference("source_path");

                String architectureStr = PrefStore.getArch(listPref.getValue());
                String distributionStr = distribution.getValue();

                int sourcePathId = PrefStore.getResourceId(getContext(), architectureStr
                        + "_" + distributionStr + "_source_path", "string");
                if (sourcePathId > 0) {
                    sourcepath.setText(getString(sourcePathId));
                }

                sourcepath.setSummary(sourcepath.getText());
            }
            if (listPref.getKey().equals("target_type")) {
                EditTextPreference targetpath = findPreference("target_path");
                EditTextPreference disksize = findPreference("disk_size");
                ListPreference fstype = findPreference("fs_type");

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
