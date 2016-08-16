package ru.meefik.linuxdeploy;

import android.app.Activity;
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
import android.support.v7.app.AlertDialog;

public class SettingsActivity extends AppCompatPreferenceActivity implements
        OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);

        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(PrefStore.getSettingsSharedName());

        // Restore from conf file
        PrefStore.restoreSettings(this);

        addPreferencesFromResource(R.xml.settings);

        initSummaries(getPreferenceScreen());
    }

    @Override
    public void setTheme(int resId) {
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

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

        // update configuration file
        PrefStore.dumpSettings(this);
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        setSummary(pref, true);
        switch (key) {
            case "is_telnet":
                // start/stop telnetd
                EnvUtils.execService(getBaseContext(), "telnetd", null);
                break;
            case "telnet_port":
                // restart httpd
                EnvUtils.execService(getBaseContext(), "httpd", "restart");
            case "telnet_localhost":
                // restart telnetd
                EnvUtils.execService(getBaseContext(), "telnetd", "restart");
                break;
            case "is_http":
                // start/stop httpd
                EnvUtils.execService(getBaseContext(), "httpd", null);
                break;
            case "http_port":
            case "http_conf":
                // restart httpd
                EnvUtils.execService(getBaseContext(), "httpd", "restart");
                break;
            case "autostart":
                // set autostart settings
                int autostartFlag = (PrefStore.isAutostart(this) ?
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                ComponentName bootComponent = new ComponentName(this, BootReceiver.class);
                getPackageManager().setComponentEnabledSetting(bootComponent, autostartFlag,
                        PackageManager.DONT_KILL_APP);
                break;
            case "nettrack":
                // set handler for network change action
                int nettrackFlag = (PrefStore.isTrackNetwork(this) ?
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                ComponentName networkComponent = new ComponentName(this, NetworkReceiver.class);
                getPackageManager().setComponentEnabledSetting(networkComponent, nettrackFlag,
                        PackageManager.DONT_KILL_APP);
                break;
            case "stealth":
                // set stealth mode
                // Run app without launcher: am start -n ru.meefik.linuxdeploy/.MainActivity
                int stealthFlag = (PrefStore.isStealth(this) ?
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
                ComponentName mainComponent = new ComponentName(getPackageName(), getPackageName() + ".Launcher");
                getPackageManager().setComponentEnabledSetting(mainComponent, stealthFlag,
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

            if (editPref.getKey().equals("env_dir") && !init) {
                editPref.setText(PrefStore.getEnvDir(this));
                pref.setSummary(editPref.getText());
            }

            if (editPref.getKey().equals("http_conf") &&
                    editPref.getText().isEmpty()) {
                editPref.setText(PrefStore.getHttpConf(this));
                pref.setSummary(editPref.getText());
            }
        }

        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());
        }
    }

    private void updateEnvDialog() {
        final Activity activity = this;
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_installenv_preference)
                .setMessage(R.string.message_installenv_confirm_dialog)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                new UpdateEnvTask(activity).execute();
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
        final Activity activity = this;
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_removeenv_preference)
                .setMessage(R.string.message_removeenv_confirm_dialog)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                new RemoveEnvTask(activity).execute();
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
