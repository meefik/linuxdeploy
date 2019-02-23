package ru.meefik.linuxdeploy;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import static ru.meefik.linuxdeploy.App.SERVICE_CHANNEL_ID;

public class PrefStore {

    private final static SettingsStore SETTINGS = new SettingsStore();
    private final static PropertiesStore PROPERTIES = new PropertiesStore();
    private final static int NOTIFY_ID = 1;

    /**
     * Get application version
     *
     * @param c context
     * @return version, format versionName-versionCode
     */
    static String getVersion(Context c) {
        return BuildConfig.VERSION_NAME + "-" + BuildConfig.VERSION_CODE;
    }

    /**
     * Get environment directory
     *
     * @param c context
     * @return path, e.g. /data/data/package/files
     */
    static String getEnvDir(Context c) {
        String envDir = SETTINGS.get(c, "env_dir");
        if (envDir.isEmpty()) {
            envDir = c.getFilesDir().getAbsolutePath();
        }
        return envDir;
    }

    /**
     * Get config directory
     *
     * @param c context
     * @return path, e.g. ${ENV_DIR}/config
     */
    static String getConfigDir(Context c) {
        return getEnvDir(c) + "/config";
    }

    /**
     * Get bin directory
     *
     * @param c context
     * @return path, e.g. ${ENV_DIR}/bin
     */
    static String getBinDir(Context c) {
        return getEnvDir(c) + "/bin";
    }

    /**
     * Get tmp directory
     *
     * @param c context
     * @return path, e.g. ${ENV_DIR}/tmp
     */
    static String getTmpDir(Context c) {
        return getEnvDir(c) + "/tmp";
    }

    /**
     * Get web directory
     *
     * @param c context
     * @return path, e.g. ${ENV_DIR}/web
     */
    static String getWebDir(Context c) {
        return getEnvDir(c) + "/web";
    }

    /**
     * Dump settings to configuration file
     *
     * @param c context
     * @return true if success
     */
    static boolean dumpSettings(Context c) {
        return SETTINGS.dump(c, getSettingsConfFile(c));
    }

    /**
     * Restore settings from configuration file
     *
     * @param c context
     * @return true if success
     */
    static boolean restoreSettings(Context c) {
        return SETTINGS.restore(c, getSettingsConfFile(c));
    }

    /**
     * Dump profile to configuration file
     *
     * @param c context
     * @return true if success
     */
    static boolean dumpProperties(Context c) {
        return PROPERTIES.dump(c, getPropertiesConfFile(c));
    }

    /**
     * Restore profile from configuration file
     *
     * @param c context
     * @return true if success
     */
    static boolean restoreProperties(Context c) {
        PROPERTIES.clear(c, true);
        return PROPERTIES.restore(c, getPropertiesConfFile(c));
    }

    /**
     * Get shared name for settings
     *
     * @return name
     */
    static String getSettingsSharedName() {
        return SettingsStore.name;
    }

    /**
     * Get shared name for properties
     *
     * @return name
     */
    static String getPropertiesSharedName() {
        return PropertiesStore.name;
    }

    /**
     * Check root is required for current profile
     *
     * @return true if required
     */
    static boolean isRootRequired(Context c) {
        return PROPERTIES.get(c, "method").equals("chroot");
    }

    /**
     * Get language code
     *
     * @param c context
     * @return language code, e.g. "en"
     */
    private static String getLanguage(Context c) {
        String language = SETTINGS.get(c, "language");
        if (language.length() == 0) {
            String countryCode = Locale.getDefault().getLanguage();
            switch (countryCode) {
                case "de":
                case "es":
                case "fr":
                case "in":
                case "it":
                case "ko":
                case "pl":
                case "pt":
                case "ru":
                case "sk":
                case "vi":
                case "zh":
                    language = countryCode;
                    break;
                default:
                    language = "en";
            }
            SETTINGS.set(c, "language", language);
        }
        return language;
    }

    /**
     * Get application theme resource id
     *
     * @param c context
     * @return resource id
     */
    static int getTheme(Context c) {
        String theme = SETTINGS.get(c, "theme");
        int themeId = R.style.DarkTheme;
        switch (theme) {
            case "dark":
                themeId = R.style.DarkTheme;
                break;
            case "light":
                themeId = R.style.LightTheme;
                break;
        }
        return themeId;
    }

    /**
     * Get font size
     *
     * @param c context
     * @return font size
     */
    static int getFontSize(Context c) {
        int fontSizeInt;
        String fontSize = SETTINGS.get(c, "fontsize");
        try {
            fontSizeInt = Integer.parseInt(fontSize);
        } catch (Exception e) {
            fontSize = c.getString(R.string.fontsize);
            fontSizeInt = Integer.parseInt(fontSize);
            SETTINGS.set(c, "fontsize", fontSize);
        }
        return fontSizeInt;
    }

    /**
     * Get maximum limit to scroll
     *
     * @param c context
     * @return number of lines
     */
    static int getMaxLines(Context c) {
        int maxLinesInt;
        String maxLines = SETTINGS.get(c, "maxlines");
        try {
            maxLinesInt = Integer.parseInt(maxLines);
        } catch (Exception e) {
            maxLines = c.getString(R.string.maxlines);
            maxLinesInt = Integer.parseInt(maxLines);
            SETTINGS.set(c, "maxlines", maxLines);
        }
        return maxLinesInt;
    }

    /**
     * Timestamp is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isTimestamp(Context c) {
        return SETTINGS.get(c, "timestamp").equals("true");
    }

    /**
     * Debug mode is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isDebugMode(Context c) {
        return SETTINGS.get(c, "debug_mode").equals("true");
    }

    /**
     * Trace mode is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isTraceMode(Context c) {
        return SETTINGS.get(c, "trace_mode").equals("true");
    }

    /**
     * Logging is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isLogger(Context c) {
        return SETTINGS.get(c, "logger").equals("true");
    }

    /**
     * Get path of log file
     *
     * @param c context
     * @return path
     */
    static String getLogFile(Context c) {
        String logfile = SETTINGS.get(c, "logfile");
        if (!logfile.contains("/")) logfile = getEnvDir(c) + "/" + logfile;
        return logfile;
    }

    /**
     * Screen lock is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isScreenLock(Context c) {
        return SETTINGS.get(c, "screenlock").equals("true");
    }

    /**
     * Wifi lock is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isWifiLock(Context c) {
        return SETTINGS.get(c, "wifilock").equals("true");
    }

    /**
     * Wake lock is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isWakeLock(Context c) {
        return SETTINGS.get(c, "wakelock").equals("true");
    }

    /**
     * Application autostart is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isAutostart(Context c) {
        return SETTINGS.get(c, "autostart").equals("true");
    }

    /**
     * Get Auto start delay
     *
     * @param c context
     * @return Auto start delay in seconds
     */
    static Integer getAutostartDelay(Context c) {
        try {
            return Integer.parseInt(SETTINGS.get(c, "autostart_delay"));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Track changes of the network status is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isTrackNetwork(Context c) {
        return SETTINGS.get(c, "nettrack").equals("true");
    }

    /**
     * Show icon is enabled
     *
     * @param c context
     * @return true if enabled
     */
    private static Boolean isNotification(Context c) {
        return SETTINGS.get(c, "appicon").equals("true");
    }

    /**
     * Stealth mode
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isStealth(Context c) {
        return SETTINGS.get(c, "stealth").equals("true");
    }

    /**
     * Get PATH variable
     *
     * @param c context
     * @return path, e.g. ${ENV_DIR}/bin
     */
    static String getPath(Context c) {
        String binDir = getBinDir(c);
        String path = SETTINGS.get(c, "path");
        if (path.isEmpty()) path = binDir;
        else path = path + ":" + binDir;
        return path;
    }

    /**
     * Which path to SH
     *
     * @param c context
     * @return path, e.g. /system/bin/sh
     */
    static String getShell(Context c) {
        String[] path = getPath(c).split(":");
        String shell = "/system/bin/sh";
        for (String p : path) {
            shell = p + "/sh";
            File f = new File(shell);
            if (f.exists()) break;
        }
        return shell;
    }

    /**
     * Get repository URL
     *
     * @param c context
     * @return url
     */
    static String getRepositoryUrl(Context c) {
        return SETTINGS.get(c, "repository_url");
    }

    /**
     * Set repository URL
     *
     * @param c context
     */
    static void setRepositoryUrl(Context c, String url) {
        SETTINGS.set(c, "repository_url", url);
    }

    /**
     * CLI symlink is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isCliSymlink(Context c) {
        return SETTINGS.get(c, "is_cli").equals("true");
    }

    /**
     * Telnet is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isTelnet(Context c) {
        return SETTINGS.get(c, "is_telnet").equals("true");
    }

    /**
     * Get telnetd port
     *
     * @param c context
     * @return port
     */
    static String getTelnetPort(Context c) {
        return SETTINGS.get(c, "telnet_port");
    }

    /**
     * Is telnetd localhost
     *
     * @param c context
     * @return true if localhost
     */
    static boolean isTelnetLocalhost(Context c) {
        return SETTINGS.get(c, "telnet_localhost").equals("true");
    }

    /**
     * HTTP is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static Boolean isHttp(Context c) {
        return SETTINGS.get(c, "is_http").equals("true");
    }

    /**
     * Get http port
     *
     * @param c context
     * @return port
     */
    static String getHttpPort(Context c) {
        return SETTINGS.get(c, "http_port");
    }

    /**
     * Get http authentication
     *
     * @param c context
     * @return authentication string, e.g. /:user:password (for crypt password use httpd -m password)
     */
    static String getHttpConf(Context c) {
        String auth = SETTINGS.get(c, "http_conf");
        if (auth.isEmpty()) auth = "/:android:" + generatePassword();
        return auth;
    }

    /**
     * X server is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static boolean isXserver(Context c) {
        return PROPERTIES.get(c, "is_gui").equals("true") &&
                PROPERTIES.get(c, "graphics").equals("x11");
    }

    /**
     * Framebuffer is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static boolean isFramebuffer(Context c) {
        return PROPERTIES.get(c, "is_gui").equals("true") &&
                PROPERTIES.get(c, "graphics").equals("fb");
    }

    /**
     * XServer XSDL is enabled
     *
     * @param c context
     * @return true if enabled
     */
    static boolean isXsdl(Context c) {
        return PROPERTIES.get(c, "x11_sdl").equals("true");
    }

    /**
     * Get XServer XSDL opening delay
     *
     * @param c context
     * @return delay in ms
     */
    static int getXsdlDelay(Context c) {
        int deplayInt;
        String delay = PROPERTIES.get(c, "x11_sdl_delay");
        try {
            deplayInt = Integer.parseInt(delay);
        } catch (Exception e) {
            delay = c.getString(R.string.x11_sdl_delay);
            deplayInt = Integer.parseInt(delay);
            PROPERTIES.set(c, "x11_sdl_delay", delay);
        }
        return deplayInt * 1000;
    }

    /**
     * Get global configuration file path
     *
     * @param c context
     * @return path of cli.conf
     */
    static File getSettingsConfFile(Context c) {
        return new File(getEnvDir(c) + "/cli.conf");
    }

    /**
     * Get profile configuration path
     *
     * @param c context
     * @return path of profile
     */
    static File getPropertiesConfFile(Context c) {
        return new File(getConfigDir(c) + "/" + getProfileName(c) + ".conf");
    }

    /**
     * Get current profile
     *
     * @param c context
     * @return profile
     */
    static String getProfileName(Context c) {
        return SETTINGS.get(c, "profile");
    }

    /**
     * Set current profile
     *
     * @param c context
     */
    static void changeProfile(Context c, String profile) {
        SETTINGS.set(c, "profile", profile);
        dumpSettings(c);
        File confFile = getPropertiesConfFile(c);
        if (!confFile.exists()) {
            PROPERTIES.clear(c, true);
            PROPERTIES.dump(c, confFile);
        }
    }

    /**
     * Set application locale
     *
     * @param c context
     */
    static void setLocale(Context c) {
        String language = getLanguage(c);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        c.getResources().updateConfiguration(config, c.getResources().getDisplayMetrics());
    }

    /**
     * Load list of mount points
     *
     * @param c context
     * @return list of mount points
     */
    static List<String> getMountsList(Context c) {
        String str = PROPERTIES.get(c, "mounts");
        List<String> list = new ArrayList<>();
        if (!str.isEmpty()) Collections.addAll(list, str.split(" "));
        return list;
    }

    /**
     * Save list of mount points
     *
     * @param c    context
     * @param list list of mount points
     */
    static void setMountsList(Context c, List<String> list) {
        PROPERTIES.set(c, "mounts", TextUtils.join(" ", list));
    }

    /**
     * Generate password
     *
     * @return plain password
     */
    static String generatePassword() {
        return Long.toHexString(Double.doubleToLongBits(Math.random())).substring(8);
    }

    /**
     * Get hardware architecture
     *
     * @param arch unformated architecture
     * @return intel, arm or mips
     */
    static String getArch(String arch) {
        String march = "unknown";
        if (arch.length() > 0) {
            char a = arch.toLowerCase().charAt(0);
            switch (a) {
                case 'a':
                    if (arch.equals("amd64"))
                        march = "intel";
                    else march = "arm";
                    break;
                case 'm':
                    march = "mips";
                    break;
                case 'i':
                case 'x':
                    march = "intel";
                    break;
            }
        }
        return march;
    }

    /**
     * Get current hardware architecture
     *
     * @return intel, arm or mips
     */
    static String getArch() {
        return getArch(System.getProperty("os.arch"));
    }

    /**
     * Get width of device screen
     *
     * @param c context
     * @return screen width
     */
    static Integer getScreenWidth(Context c) {
        int width;
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        return width;
    }

    /**
     * Get height of device screen
     *
     * @param c context
     * @return screen height
     */
    static Integer getScreenHeight(Context c) {
        int height;
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        height = size.y;
        return height;
    }

    /**
     * Get local IP address
     *
     * @return ip address
     */
    static String getLocalIpAddress() {
        String ip = "127.0.0.1";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ip = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ip;
    }

    /**
     * Get Android resource id
     *
     * @param c            context
     * @param resourceName variable name
     * @param resourceType resource type
     * @return resource id
     */
    static int getResourceId(Context c, String resourceName, String resourceType) {
        try {
            return c.getResources().getIdentifier(resourceName, resourceType, c.getPackageName());
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Show icon on notification bar
     *
     * @param context context
     * @param intent  intent
     */
    static void showNotification(Context context, Intent intent) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (isNotification(context)) {
            setLocale(context);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notification_current_profile)
                            + ": " + getProfileName(context));

            if (isStealth(context)) {
                Intent stealthReceive = new Intent();
                stealthReceive.setAction("ru.meefik.linuxdeploy.BROADCAST_ACTION");
                stealthReceive.putExtra("show", true);
                PendingIntent pendingIntentStealth = PendingIntent.getBroadcast(context, 2, stealthReceive, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(pendingIntentStealth);
            } else {
                Intent resultIntent = intent;
                if (resultIntent == null) resultIntent = new Intent(context, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                Intent startReceive = new Intent();
                startReceive.setAction("ru.meefik.linuxdeploy.BROADCAST_ACTION");
                startReceive.putExtra("start", true);
                PendingIntent pendingIntentStart = PendingIntent.getBroadcast(context, 3, startReceive, PendingIntent.FLAG_UPDATE_CURRENT);
                int startIcon = SETTINGS.get(context, "theme").equals("dark") ? R.drawable.ic_action_start_dark : R.drawable.ic_action_start_light;
                mBuilder.addAction(startIcon, context.getString(R.string.menu_start), pendingIntentStart);

                Intent stopReceive = new Intent();
                stopReceive.setAction("ru.meefik.linuxdeploy.BROADCAST_ACTION");
                stopReceive.putExtra("stop", true);
                PendingIntent pendingIntentStop = PendingIntent.getBroadcast(context, 4, stopReceive, PendingIntent.FLAG_UPDATE_CURRENT);
                int stopIcon = SETTINGS.get(context, "theme").equals("dark") ? R.drawable.ic_action_stop_dark : R.drawable.ic_action_stop_light;
                mBuilder.addAction(stopIcon, context.getString(R.string.menu_stop), pendingIntentStop);
            }
            mBuilder.setOngoing(true);
            mBuilder.setWhen(0);
            mNotificationManager.notify(NOTIFY_ID, mBuilder.build());
        } else {
            mNotificationManager.cancel(NOTIFY_ID);
        }
    }

    /**
     * Hide icon from notification bar
     *
     * @param context context
     */
    static void hideNotification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFY_ID);
    }
}
