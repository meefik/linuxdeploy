package ru.meefik.linuxdeploy.fragment;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import ru.meefik.linuxdeploy.EnvUtils;
import ru.meefik.linuxdeploy.PrefStore;
import ru.meefik.linuxdeploy.R;
import ru.meefik.linuxdeploy.RemoveEnvTask;
import ru.meefik.linuxdeploy.UpdateEnvTask;
import ru.meefik.linuxdeploy.receiver.BootReceiver;

public class SettingsFragment extends PreferenceFragmentCompat implements
        OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(PrefStore.getSettingsSharedName());
        setPreferencesFromResource(R.xml.settings, rootKey);
        initSummaries(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();

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
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "installenv":
                updateEnvDialog();
                return true;
            case "removeenv":
                removeEnvDialog();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        setSummary(pref, true);
        switch (key) {
            case "is_telnet":
                // start/stop telnetd
                EnvUtils.execService(getContext(), "telnetd", null);
                break;
            case "telnet_port":
                // restart telnetd
                EnvUtils.execService(getContext(), "telnetd", "restart");
                // restart httpd
                EnvUtils.execService(getContext(), "httpd", "restart");
                break;
            case "telnet_localhost":
                // restart telnetd
                EnvUtils.execService(getContext(), "telnetd", "restart");
                break;
            case "is_http":
                // start/stop httpd
                EnvUtils.execService(getContext(), "httpd", null);
                break;
            case "http_port":
            case "http_conf":
                // restart httpd
                EnvUtils.execService(getContext(), "httpd", "restart");
                break;
            case "autostart":
                // set autostart settings
                int autostartFlag = (PrefStore.isAutostart(getContext()) ?
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                ComponentName bootComponent = new ComponentName(getContext(), BootReceiver.class);
                getContext().getPackageManager().setComponentEnabledSetting(bootComponent, autostartFlag,
                        PackageManager.DONT_KILL_APP);
                break;
            case "stealth":
                // set stealth mode
                // Run app without launcher: am start -n ru.meefik.linuxdeploy/.MainActivity
                int stealthFlag = PrefStore.isStealth(getContext()) ?
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                ComponentName mainComponent = new ComponentName(getContext().getPackageName(), getContext().getPackageName() + ".Launcher");
                getContext().getPackageManager().setComponentEnabledSetting(mainComponent, stealthFlag,
                        PackageManager.DONT_KILL_APP);
                break;
        }
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

            switch (editPref.getKey()) {
                case "env_dir":
                    if (!init) {
                        editPref.setText(PrefStore.getEnvDir(getContext()));
                        pref.setSummary(editPref.getText());
                    }
                    break;
                case "http_conf":
                    if (editPref.getText().isEmpty()) {
                        editPref.setText(PrefStore.getHttpConf(getContext()));
                        pref.setSummary(editPref.getText());
                    }
                    break;
                case "logfile":
                    if (!init) {
                        editPref.setText(PrefStore.getLogFile(getContext()));
                        pref.setSummary(editPref.getText());
                    }
                    break;
            }
        }

        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());
        }

        if (pref instanceof CheckBoxPreference) {
            CheckBoxPreference checkPref = (CheckBoxPreference) pref;

            if (checkPref.getKey().equals("logger") && checkPref.isChecked() && init) {
                requestWritePermissions();
            }
        }
    }

    private void updateEnvDialog() {
        final Context context = getContext();
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_installenv_preference)
                .setMessage(R.string.message_installenv_confirm_dialog)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        (dialog, id) -> new UpdateEnvTask(context).execute())
                .setNegativeButton(android.R.string.no,
                        (dialog, id) -> dialog.cancel()).show();
    }

    private void removeEnvDialog() {
        final Context context = getContext();
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_removeenv_preference)
                .setMessage(R.string.message_removeenv_confirm_dialog)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        (dialog, id) -> new RemoveEnvTask(context).execute())
                .setNegativeButton(android.R.string.no,
                        (dialog, id) -> dialog.cancel()).show();
    }

    /**
     * Request permission for write to storage
     */
    private void requestWritePermissions() {
        int REQUEST_WRITE_STORAGE = 112;
        boolean hasPermission = (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }
}
