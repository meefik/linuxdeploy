package ru.meefik.linuxdeploy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    private static TextView output;
    private static ScrollView scroll;
    private static WifiLock wifiLock;
    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        output = (TextView) findViewById(R.id.outputView);
        scroll = (ScrollView) findViewById(R.id.scrollView);

        output.setMovementMethod(LinkMovementMethod.getInstance());

        // WiFi lock init
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, getPackageName());

        // Wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName());

        if (EnvUtils.isLatestVersion(this)) {
            // start telnetd
            EnvUtils.execService(getBaseContext(), "telnetd", "start");
            // start httpd
            EnvUtils.execService(getBaseContext(), "httpd", "start");
        } else {
            // Update ENV
            EnvUtils.execService(getBaseContext(), "update", "");
        }
    }

    @Override
    public void setTheme(int resId) {
        super.setTheme(PrefStore.getTheme(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        PrefStore.setLocale(this);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getMenuInflater().inflate(R.menu.activity_main_landscape, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_main_portrait, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_start:
                containerStart(null);
                break;
            case R.id.menu_stop:
                containerStop(null);
                break;
            case R.id.menu_properties:
                containerProperties(null);
                break;
            case R.id.menu_install:
                containerDeploy();
                break;
            case R.id.menu_configure:
                containerConfigure();
                break;
            case R.id.menu_export:
                containerExport();
                break;
            case R.id.menu_status:
                containerStatus();
                break;
            case R.id.menu_clear:
                clearLog();
                break;
            case android.R.id.home:
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_profiles:
                Intent intent_profiles = new Intent(this, ProfilesActivity.class);
                startActivity(intent_profiles);
                break;
            case R.id.nav_repository:
                openRepository();
                break;
            case R.id.nav_terminal:
                openTerminal();
                break;
            case R.id.nav_settings:
                Intent intent_settings = new Intent(this, SettingsActivity.class);
                startActivity(intent_settings);
                break;
            case R.id.nav_about:
                Intent intent_about = new Intent(this, AboutActivity.class);
                startActivity(intent_about);
                break;
            case R.id.nav_exit:
                PrefStore.hideNotification(getBaseContext());
                if (wifiLock.isHeld()) wifiLock.release();
                if (wakeLock.isHeld()) wakeLock.release();
                EnvUtils.execService(getBaseContext(), "telnetd", "stop");
                EnvUtils.execService(getBaseContext(), "httpd", "stop");
                finish();
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        String profileName = PrefStore.getProfileName(this);
        String ipAddress = PrefStore.getLocalIpAddress();
        setTitle(profileName + "  [ " + ipAddress + " ]");

        // show icon
        PrefStore.showNotification(getBaseContext(), getIntent());

        // Restore font size
        output.setTextSize(TypedValue.COMPLEX_UNIT_SP, PrefStore.getFontSize(this));

        // Restore log
        if (Logger.size() == 0) output.setText(R.string.help_text);
        else Logger.show();

        // Screen lock
        if (PrefStore.isScreenLock(this))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // WiFi lock
        if (PrefStore.isWifiLock(this)) {
            if (!wifiLock.isHeld()) wifiLock.acquire();
        }
        else {
            if (wifiLock.isHeld()) wifiLock.release();
        }

        // Wake lock
        if (PrefStore.isWakeLock(this)) {
            if (!wakeLock.isHeld()) wakeLock.acquire();
        }
        else {
            if (wakeLock.isHeld()) wakeLock.release();
        }
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

    /**
     * Clear logs
     */
    private void clearLog() {
        Logger.clear(this);
        output.setText(R.string.help_text);
    }

    /**
     * Start container action
     *
     * @param view
     */
    public void containerStart(View view) {
        new AlertDialog.Builder(this).setTitle(R.string.confirm_start_title)
                .setMessage(R.string.confirm_start_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // actions
                                Handler h = new Handler();
                                if (PrefStore.isXserver(getApplicationContext())
                                        && PrefStore.isXsdl(getApplicationContext())) {
                                    PackageManager pm = getPackageManager();
                                    Intent intent = pm.getLaunchIntentForPackage("x.org.server");
                                    if (intent != null) startActivity(intent);
                                    h.postDelayed(new Runnable() {
                                        public void run() {
                                            EnvUtils.execService(getBaseContext(), "start", "-m");
                                        }
                                    }, PrefStore.getXsdlDelay(getApplicationContext()));
                                } else if (PrefStore.isFramebuffer(getApplicationContext())) {
                                    EnvUtils.execService(getBaseContext(), "start", "-m");
                                    h.postDelayed(new Runnable() {
                                        public void run() {
                                            Intent intent = new Intent(getApplicationContext(),
                                                    FullscreenActivity.class);
                                            startActivity(intent);
                                        }
                                    }, 1500);
                                } else {
                                    EnvUtils.execService(getBaseContext(), "start", "-m");
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
    }

    /**
     * Stop container action
     *
     * @param view
     */
    public void containerStop(View view) {
        new AlertDialog.Builder(this).setTitle(R.string.confirm_stop_title)
                .setMessage(R.string.confirm_stop_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                EnvUtils.execService(getBaseContext(), "stop", "-u");
                            }
                        }).setNegativeButton(android.R.string.no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();
    }

    /**
     * Container properties action
     *
     * @param view
     */
    public void containerProperties(View view) {
        Intent intent = new Intent(this, PropertiesActivity.class);
        intent.putExtra("restore", true);
        startActivity(intent);
    }

    /**
     * Container deploy action
     */
    private void containerDeploy() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_install_dialog)
                .setMessage(R.string.message_install_dialog)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                EnvUtils.execService(getBaseContext(), "deploy", null);
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

    /**
     * Container configure action
     */
    private void containerConfigure() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_configure_dialog)
                .setMessage(R.string.message_configure_dialog)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                EnvUtils.execService(getBaseContext(), "deploy", "-n bootstrap");
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

    /**
     * Container export action
     */
    private void containerExport() {
        final EditText input = new EditText(this);
        final String rootfsArchive = getString(R.string.rootfs_archive);
        input.setText(rootfsArchive);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_export_dialog)
                .setCancelable(false)
                .setView(input, 16, 32, 16, 0)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                EnvUtils.execService(getBaseContext(), "export", input.getText().toString());
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

    /**
     * Container status action
     */
    private void containerStatus() {
        EnvUtils.execService(getBaseContext(), "status", null);
    }

    /**
     * Open repository action
     */
    private void openRepository() {
//        PackageManager manager = getPackageManager();
//        if (manager.checkSignatures(getPackageName(), "ru.meefik.linuxdeploy.key")
//                == PackageManager.SIGNATURE_MATCH) {
//            //full version
//        }
        Intent intent = new Intent(this, RepositoryActivity.class);
        startActivity(intent);
    }

    /**
     * Open terminal action
     */
    private void openTerminal() {
        try {
            Intent intent_terminal = new Intent("jackpal.androidterm.RUN_SCRIPT");
            intent_terminal.addCategory(Intent.CATEGORY_DEFAULT);
            intent_terminal.putExtra("jackpal.androidterm.iInitialCommand",
                    PrefStore.getTerminalCmd(this));
            startActivity(intent_terminal);
        } catch(Exception e) {
            Toast.makeText(this, R.string.toast_terminal_error, Toast.LENGTH_SHORT).show();
        }
    }

}
