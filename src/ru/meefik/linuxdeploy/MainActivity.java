package ru.meefik.linuxdeploy;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockActivity {

    private static TextView output;
    private static ScrollView scroll;
    private static WifiLock wifiLock;
    final static int NOTIFY_ID = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        output = (TextView) findViewById(R.id.LogView);
        scroll = (ScrollView) findViewById(R.id.LogScrollView);

        output.setMovementMethod(LinkMovementMethod.getInstance());

        // WiFi lock init
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,
                "linuxdeploy");
    }

    @Override
    public void setTheme(int resid) {
        super.setTheme(PrefStore.getTheme(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        PrefStore.setLocale(this);
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);

        boolean isLight = PrefStore.getTheme(this) == R.style.LightTheme;

        menu.findItem(R.id.menu_properties).setIcon(
                isLight ? R.drawable.ic_action_properties_light
                        : R.drawable.ic_action_properties_dark);

        int ot = getResources().getConfiguration().orientation;
        if (ot == Configuration.ORIENTATION_LANDSCAPE) {
            menu.findItem(R.id.menu_start).setIcon(
                    isLight ? R.drawable.ic_action_start_light
                            : R.drawable.ic_action_start_dark);
            menu.findItem(R.id.menu_stop).setIcon(
                    isLight ? R.drawable.ic_action_stop_light
                            : R.drawable.ic_action_stop_dark);
        }

        super.onCreateOptionsMenu(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_start:
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_start_title).setMessage(
                            R.string.confirm_start_message).setIcon(
                            android.R.drawable.ic_dialog_alert).setCancelable(
                            false).setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    new ExecScript(getApplicationContext(),
                                            "start").start();
                                    // actions
                                    Handler h = new Handler();
                                    if (PrefStore.getStartup(
                                            getApplicationContext()).contains(
                                            "xserver")
                                            && PrefStore
                                                    .isXserverXsdl(getApplicationContext())) {
                                        h.postDelayed(new Runnable() {
                                            public void run() {
                                                PackageManager pm = getPackageManager();
                                                Intent intent = pm
                                                        .getLaunchIntentForPackage("x.org.server");
                                                if (intent != null)
                                                    startActivity(intent);
                                            }
                                        }, 1000);
                                    }
                                    if (PrefStore.getStartup(
                                            getApplicationContext()).contains(
                                            "framebuffer")) {
                                        h.postDelayed(new Runnable() {
                                            public void run() {
                                                Intent intent = new Intent(
                                                        getApplicationContext(),
                                                        FullscreenActivity.class);
                                                if (intent != null)
                                                    startActivity(intent);
                                            }
                                        }, 1000);
                                    }
                                }
                            }).setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.cancel();
                                }
                            }).show();
            break;
        case R.id.menu_stop:
            new AlertDialog.Builder(this).setTitle(R.string.confirm_stop_title)
                    .setMessage(R.string.confirm_stop_message).setIcon(
                            android.R.drawable.ic_dialog_alert).setCancelable(
                            false).setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    new ExecScript(getApplicationContext(),
                                            "stop").start();
                                }
                            }).setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.cancel();
                                }
                            }).show();
            break;
        case R.id.menu_status:
            new ExecScript(getApplicationContext(), "status").start();
            break;
        case R.id.menu_properties:
            Intent intent_properties = new Intent(this,
                    PropertiesActivity.class);
            startActivity(intent_properties);
            break;
        case R.id.menu_settings:
            Intent intent_settings = new Intent(this, SettingsActivity.class);
            startActivity(intent_settings);
            break;
        case R.id.menu_about:
            Intent intent_about = new Intent(this, AboutActivity.class);
            startActivity(intent_about);
            break;
        case R.id.menu_clear:
            clearLog();
            break;
        case R.id.menu_exit:
            notification(getApplicationContext());
            if (wifiLock.isHeld()) wifiLock.release();
            finish();
            break;
        case android.R.id.home:
            Intent intent_profiles = new Intent(this, ProfilesActivity.class);
            startActivity(intent_profiles);
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        String profileName = PrefStore
                .getCurrentProfileTitle(getApplicationContext());
        String ipaddress = PrefStore.getLocalIpAddress();
        setTitle(profileName + "  [ " + ipaddress + " ]");

        // show icon
        notification(getApplicationContext(), getIntent());

        // Restore font size
        output.setTextSize(TypedValue.COMPLEX_UNIT_SP, PrefStore
                .getFontSize(this));

        // Restore log
        if (Logger.get().length() == 0) {
            output.setText(R.string.help_text);
        } else {
            showLog(Logger.get());
        }

        // Screen lock
        if (PrefStore.isScreenLock(this))
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // WiFi lock
        if (PrefStore.isWifiLock(this))
            wifiLock.acquire();
        else if (wifiLock.isHeld()) wifiLock.release();

        // update configuration file
        if (PrefStore.CONF_CHANGE) {
            PrefStore.CONF_CHANGE = false;
            // update config file
            EnvUtils.updateConf(this);
        }
    }

    private static void clearLog() {
        Logger.clear();
        output.setText(R.string.help_text);
    }

    /**
     * Show message in TextView, used from Logger
     * 
     * @param log message
     */
    public static void showLog(final String log) {
        if (output == null || scroll == null) return;
        // show log in TextView
        output.post(new Runnable() {
            @Override
            public void run() {
                output.setText(log);
                // scroll TextView to bottom
                scroll.post(new Runnable() {
                    @Override
                    public void run() {
                        scroll.fullScroll(View.FOCUS_DOWN);
                        scroll.clearFocus();
                    }
                });
            }
        });
    }

    public static void notification(Context context, Intent intent) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (PrefStore.isShowIcon(context)) {
            PrefStore.setLocale(context);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    context)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(
                            context.getString(R.string.notification_current_profile)
                                    + ": "
                                    + PrefStore.getCurrentProfileTitle(context));
            Intent resultIntent = intent;
            if (resultIntent == null)
                resultIntent = new Intent(context, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                    0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            mBuilder.setOngoing(true);
            mBuilder.setWhen(0);
            mNotificationManager.notify(NOTIFY_ID, mBuilder.build());
        } else {
            mNotificationManager.cancel(NOTIFY_ID);
        }
    }

    public static void notification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFY_ID);
    }

}
