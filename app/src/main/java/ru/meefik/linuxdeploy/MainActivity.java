package ru.meefik.linuxdeploy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
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

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_WRITE_STORAGE = 112;
    private static TextView output;
    private static ScrollView scroll;
    private static WifiLock wifiLock;
    private static PowerManager.WakeLock wakeLock;

    /**
     * Show message in TextView, used from Logger
     *
     * @param log message
     */
    public static void showLog(final String log) {
        if (output == null || scroll == null) return;
        // show log in TextView
        output.post(() -> {
            output.setText(log);
            // scroll TextView to bottom
            scroll.post(() -> {
                scroll.fullScroll(View.FOCUS_DOWN);
                scroll.clearFocus();
            });
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        output = findViewById(R.id.outputView);
        scroll = findViewById(R.id.scrollView);

        output.setMovementMethod(LinkMovementMethod.getInstance());

        // WiFi lock init
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, getPackageName());

        // Wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName());

        if (EnvUtils.isLatestVersion(this)) {
            // start services
            EnvUtils.execServices(getBaseContext(), new String[]{"telnetd", "httpd"}, "start");
        } else {
            // Update ENV
            new UpdateEnvTask(this).execute();
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
                containerExportWithRequestPermissions();
                break;
            case R.id.menu_status:
                containerStatus();
                break;
            case R.id.menu_clear:
                clearLog();
                break;
            case android.R.id.home:
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
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
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

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
                String uri = "http://127.0.0.1:" + PrefStore.getHttpPort(this) +
                        "/cgi-bin/terminal?size=" + PrefStore.getFontSize(this);
                // Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                // startActivity(browserIntent);
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                if (PrefStore.getTheme(this) == R.style.LightTheme) {
                    builder.setToolbarColor(Color.LTGRAY);
                } else {
                    builder.setToolbarColor(Color.DKGRAY);
                }
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(this, Uri.parse(uri));
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
                if (wifiLock.isHeld()) wifiLock.release();
                if (wakeLock.isHeld()) wakeLock.release();
                EnvUtils.execServices(getBaseContext(), new String[]{"telnetd", "httpd"}, "stop");
                PrefStore.hideNotification(getBaseContext());
                finish();
                break;
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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
        } else {
            if (wifiLock.isHeld()) wifiLock.release();
        }

        // Wake lock
        if (PrefStore.isWakeLock(this)) {
            if (!wakeLock.isHeld()) wakeLock.acquire();
        } else {
            if (wakeLock.isHeld()) wakeLock.release();
        }
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
                        (dialog, id) -> {
                            // actions
                            Handler h = new Handler();
                            if (PrefStore.isXserver(getApplicationContext())
                                    && PrefStore.isXsdl(getApplicationContext())) {
                                PackageManager pm = getPackageManager();
                                Intent intent = pm.getLaunchIntentForPackage("x.org.server");
                                if (intent != null) startActivity(intent);
                                h.postDelayed(() -> EnvUtils.execService(getBaseContext(), "start", "-m"), PrefStore.getXsdlDelay(getApplicationContext()));
                            } else if (PrefStore.isFramebuffer(getApplicationContext())) {
                                EnvUtils.execService(getBaseContext(), "start", "-m");
                                h.postDelayed(() -> {
                                    Intent intent = new Intent(getApplicationContext(),
                                            FullscreenActivity.class);
                                    startActivity(intent);
                                }, 1500);
                            } else {
                                EnvUtils.execService(getBaseContext(), "start", "-m");
                            }
                        }).setNegativeButton(android.R.string.no,
                (dialog, id) -> dialog.cancel()).show();
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
                        (dialog, id) -> EnvUtils.execService(getBaseContext(), "stop", "-u")).setNegativeButton(android.R.string.no,
                (dialog, id) -> dialog.cancel()).show();
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
                        (dialog, id) -> EnvUtils.execService(getApplicationContext(), "deploy", null))
                .setNegativeButton(android.R.string.no,
                        (dialog, id) -> dialog.cancel()).show();
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
                        (dialog, id) -> EnvUtils.execService(getBaseContext(), "deploy", "-m -n bootstrap"))
                .setNegativeButton(android.R.string.no,
                        (dialog, id) -> dialog.cancel()).show();
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
                .setView(input)
                .setPositiveButton(android.R.string.yes,
                        (dialog, id) -> EnvUtils.execService(getBaseContext(), "export", input.getText().toString()))
                .setNegativeButton(android.R.string.no,
                        (dialog, id) -> dialog.cancel()).show();
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
        Intent intent = new Intent(this, RepositoryActivity.class);
        startActivity(intent);
    }

    /**
     * Request permission for write to storage
     */
    private void containerExportWithRequestPermissions() {
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        } else {
            containerExport();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    containerExport();
                } else {
                    Toast.makeText(this, getString(R.string.write_permissions_disallow), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
