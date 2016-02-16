package ru.meefik.linuxdeploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;

import com.h6ah4i.android.compat.content.SharedPreferenceCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.view.Display;
import android.view.WindowManager;

public class PrefStore {

    public static Boolean CONF_CHANGE = false;
    public static final String APP_PREF_NAME = "app_settings";
    public static final String PROFILES_PREF_NAME = "profiles";

    /**
     * Get string with values by resource id
     * 
     * @param c context
     * @param resId resource id with variables
     * @return string with values
     */
    public static String getValues(Context c, int resId) {
        return c.getString(resId).replace("${ENV_DIR}", 
                getEnvDir(c)).replace("${EXTERNAL_STORAGE}", getStorage());
    }
    
    /**
     * Get string with values by string
     * 
     * @param c context
     * @param text string with variables
     * @return string with values
     */
    public static String getValues(Context c, String text) {
        return text.replace("${ENV_DIR}", 
                getEnvDir(c)).replace("${EXTERNAL_STORAGE}", getStorage());
    }
    
    /**
     * Get application version
     * 
     * @param c context
     * @return version, format versionName-versionCode
     */
    public static String getVersion(Context c) {
        String version = "";
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(
                    c.getPackageName(), 0);
            version = pi.versionName + "-" + pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    /**
     * Get external storage path
     * 
     * @return path
     */
    public static String getStorage() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * Get environment directory
     * 
     * @param c context
     * @return path, e.g. /data/data/com.example.app/files
     */
    public static String getEnvDir(Context c) {
        return c.getFilesDir().getAbsolutePath();
    }
    
    /**
     * Get language code
     * 
     * @param c context
     * @return language code, e.g. "en"
     */
    public static String getLanguage(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        String language = pref.getString("language", c
                .getString(R.string.language));
        if (language.length() == 0) {
            String countryCode = Locale.getDefault().getLanguage();
            switch (countryCode) {
            case "ru":
                language = countryCode;
                break;
            default:
                language = "en";
            }
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("language", language);
            editor.commit();
        }
        return language;
    }

    /**
     * Get application theme resource id
     * 
     * @param c context
     * @return resource id
     */
    public static int getTheme(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        String theme = pref.getString("theme", c.getString(R.string.theme));
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
    public static int getFontSize(Context c) {
        Integer fontSizeInt;
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        String fontSize = pref.getString("fontsize", c
                .getString(R.string.fontsize));
        try {
            fontSizeInt = Integer.parseInt(fontSize);
        } catch (Exception e) {
            fontSize = c.getString(R.string.fontsize);
            fontSizeInt = Integer.parseInt(fontSize);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("fontsize", fontSize);
            editor.commit();
        }
        return fontSizeInt;
    }

    /**
     * Get maximum limit to scroll
     * 
     * @param c context
     * @return number of lines
     */
    public static int getMaxLines(Context c) {
        Integer maxLinesInt;
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        String maxLines = pref.getString("maxlines", c
                .getString(R.string.maxlines));
        try {
            maxLinesInt = Integer.parseInt(maxLines);
        } catch (Exception e) {
            maxLines = c.getString(R.string.maxlines);
            maxLinesInt = Integer.parseInt(maxLines);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("maxlines", maxLines);
            editor.commit();
        }
        return maxLinesInt;
    }

    /**
     * Timestamp is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isTimestamp(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("timestamp", c.getString(R.string.timestamp)
                .equals("true"));
    }

    /**
     * Debug mode is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isDebugMode(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("debug", c.getString(R.string.debug).equals(
                "true"));
    }

    /**
     * Trace mode is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isTraceMode(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("debug", c.getString(R.string.debug).equals(
                "true"))
                && pref.getBoolean("trace", c.getString(R.string.trace).equals(
                        "true"));
    }

    /**
     * Logging is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isLogger(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("logger", c.getString(R.string.logger).equals(
                "true"));
    }

    /**
     * Get path of log file
     * 
     * @param c context
     * @return path
     */
    public static String getLogFile(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return getValues(c, pref.getString("logfile", c.getString(R.string.logfile)));
    }

    /**
     * Screen lock is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isScreenLock(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("screenlock", c.getString(R.string.screenlock)
                .equals("true"));
    }

    /**
     * Wifi lock is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isWifiLock(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("wifilock", c.getString(R.string.wifilock)
                .equals("true"));
    }
    

    /**
     * Application autostart is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isAutostart(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("autostart", c.getString(R.string.autostart)
                .equals("true"));
    }

    /**
     * Show icon is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isShowIcon(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("appicon", c.getString(R.string.appicon).equals(
                "true"));
    }

    /**
     * Get BusyBox directory
     * 
     * @param c context
     * @return directory path
     */
    public static String getBusyboxDir(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getString("busyboxdir", c.getString(R.string.busyboxdir));
    }
    
    /**
     * Get terminal script
     * 
     * @param c context
     * @return script
     */
    public static String getTerminalCmd(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return getValues(c, pref.getString("terminalcmd", c.getString(R.string.terminalcmd)));
    }

    /**
     * Get shell
     * 
     * @param c context
     * @return path to shell, e.g. /system/xbin/ash
     */
    public static String getShell(Context c) {
        return getBusyboxDir(c) + "/ash";
    }

    /**
     * CLI is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isUseCli(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref.getBoolean("usecli", c.getString(R.string.usecli).equals(
                "true"));
    }

    /**
     * Get chroot directory
     * 
     * @param c context
     * @return path
     */
    public static String getChrootDir(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("mountdir", c.getString(R.string.mountdir));
    }

    /**
     * Get target path
     * 
     * @param c context
     * @return path
     */
    public static String getTargetPath(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return getValues(c, pref.getString("diskimage", c.getString(R.string.diskimage)));
    }

    /**
     * Get deploy type
     * 
     * @param c context
     * @return type
     */
    public static String getTargetType(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("deploytype", c.getString(R.string.deploytype));
    }

    /**
     * Get image size
     * 
     * @param c context
     * @return size in Mb
     */
    public static String getDiskSize(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("disksize", c.getString(R.string.disksize));
    }

    /**
     * Get filesystem
     * 
     * @param c context
     * @return filesystem: ext2, ext3, ext4 or auto 
     */
    public static String getFilesystem(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("fstype", c.getString(R.string.fstype));
    }

    /**
     * Get GNU/Linux distribution name
     * 
     * @param c context
     * @return name, e.g. debian, ubuntu etc.
     */
    public static String getDistribution(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("distribution", c
                .getString(R.string.distribution));
    }

    /**
     * Get distribution suite
     * 
     * @param c context
     * @return suite, e.g. jessie, percise etc.
     */
    public static String getSuite(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("suite", c.getString(R.string.suite));
    }

    /**
     * Get source path
     * 
     * @param c context
     * @return path
     */
    public static String getSourcePath(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("mirror", c.getString(R.string.mirror));
    }

    /**
     * Get distribution architecture
     * 
     * @param c context
     * @return architecture, e.g. armel, armhf, i386 etc.
     */
    public static String getArchitecture(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("architecture", c
                .getString(R.string.architecture));
    }

    /**
     * Get user name
     * 
     * @param c context
     * @return name in lower case
     */
    public static String getUserName(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("username", c.getString(R.string.username)).trim();
    }

    /**
     * Get user password
     * 
     * @param c context
     * @return plain password
     */
    public static String getUserPassword(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("password", c.getString(R.string.password));
    }

    /**
     * Get DNS server
     * 
     * @param c context
     * @return one or more IP separated by a space
     */
    public static String getServerDns(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("serverdns", c.getString(R.string.serverdns));
    }

    /**
     * Get distribution locale
     * 
     * @param c context
     * @return locale, e.g. ru_RU.UTF-8
     */
    public static String getLocale(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("locale", c.getString(R.string.locale));
    }
    
    /**
     * Get desktop environment
     * 
     * @param c context
     * @return environment name, e.g. xterm, lxde, xfce, gnome, kde etc.
     */
    public static String getDesktopEnv(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("desktopenv", c.getString(R.string.desktopenv));
    }

    /**
     * Get used components
     * 
     * @param c context
     * @return list of components separated by a space, e.g. ssh, vnc etc.
     */
    public static String getUseComponents(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        Set<String> defcomp = new HashSet<String>(Arrays.asList(c
                .getResources().getStringArray(R.array.default_components)));
        if (!pref.contains("xcomponents")) {
            SharedPreferences.Editor editor = pref.edit();
            // set default components
            SharedPreferenceCompat.EditorCompat.putStringSet(editor,
                    "xcomponents", defcomp);
            editor.commit();
        }
        Set<String> comp_set = SharedPreferenceCompat.getStringSet(pref,
                "xcomponents", defcomp);
        String components = "";
        for (String str : comp_set) {
            components += str + " ";
        }
        return components.trim();
    }

    /**
     * Get list of startup
     * 
     * @param c context
     * @return list of startup separated by a space, e.g. ssh, vnc etc.
     */
    public static String getStartup(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        String startup = "";
        if (pref.getBoolean("sshstartup", c.getString(R.string.sshstartup)
                .equals("true"))) {
            startup += " ssh";
        }
        if (pref.getBoolean("guistartup", c.getString(R.string.guistartup)
                .equals("true"))) {
            startup += " "
                    + pref.getString("guitype", c.getString(R.string.guitype));
        }
        if (pref.getBoolean("customstartup", c
                .getString(R.string.customstartup).equals("true"))) {
            startup += " custom";
        }
        return startup.trim();
    }

    /**
     * Get list of custom scripts
     * 
     * @param c context
     * @return list of scripts separated by a space
     */
    public static String getCustomScripts(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        String startup = "";
        if (pref.getBoolean("customstartup", c
                .getString(R.string.customstartup).equals("true"))) {
            startup = pref.getString("scripts", c.getString(R.string.scripts));
        }
        return startup.trim();
    }

    /**
     * Get list of custom mounts
     * 
     * @param c context
     * @return list of mount points separated by a space
     */
    public static String getCustomMounts(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        String mountPoints = "";
        if (pref.getBoolean("custommounts", c.getString(R.string.custommount)
                .equals("true"))) {
            mountPoints = pref.getString("mounts", getStorage());
        }
        return mountPoints.trim();
    }

    /**
     * Get SSH server port
     * 
     * @param c context
     * @return port, e.g. 22
     */
    public static String getSshPort(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("sshport", c.getString(R.string.sshport));
    }

    /**
     * Get VNC display
     * 
     * @param c context
     * @return display, e.g. 0
     */
    public static String getVncDisplay(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("vncdisplay", c.getString(R.string.vncdisplay));
    }

    /**
     * Get VNC color depth
     * 
     * @param c context
     * @return depth, e.g. 16
     */
    public static String getVncDepth(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("vncdepth", c.getString(R.string.vncdepth));
    }

    /**
     * Get VNC DPI
     * 
     * @param c context
     * @return dpi, e.g. 100
     */
    public static String getVncDpi(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("vncdpi", c.getString(R.string.vncdpi));
    }

    /**
     * Get VNC geometry WxH
     * 
     * @param c context
     * @return geometry, e.g. 800x400
     */
    public static String getVncGeometry(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        String vncWidth = pref.getString("vncwidth", c
                .getString(R.string.vncwidth));
        if (vncWidth.length() == 0) {
            vncWidth = String.valueOf(getScreenWidth(c));
            editor.putString("vncwidth", vncWidth);
            editor.commit();
        }
        String vncHeight = pref.getString("vncheight", c
                .getString(R.string.vncheight));
        if (vncHeight.length() == 0) {
            vncHeight = String.valueOf(getScreenHeight(c));
            editor.putString("vncheight", vncHeight);
            editor.commit();
        }
        return vncWidth + "x" + vncHeight;
    }

    /**
     * Get VNC arguments
     * 
     * @param c context
     * @return arguments
     */
    public static String getVncArgs(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("vncargs", c.getString(R.string.vncargs));
    }

    /**
     * Get X server display
     * 
     * @param c context
     * @return display, e.g. 0
     */
    public static String getXserverDisplay(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("xdisplay", c.getString(R.string.xdisplay));
    }

    /**
     * Get X server host
     * 
     * @param c context
     * @return host name or IP
     */
    public static String getXserverHost(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("xhost", c.getString(R.string.xhost));
    }

    /**
     * XServer XSDL is enabled
     * 
     * @param c context
     * @return true if enabled
     */
    public static Boolean isXserverXsdl(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getBoolean("xserverxsdl", c.getString(R.string.xserverxsdl)
                .equals("true"));
    }

    /**
     * Get X display (framebuffer mode)
     * 
     * @param c context
     * @return display, e.g. 0
     */
    public static String getFbDisplay(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("fbdisplay", c.getString(R.string.fbdisplay));
    }

    /**
     * Get X DPI (framebuffer mode)
     * 
     * @param c context
     * @return DPI, e.g. 100
     */
    public static String getFbDpi(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("fbdpi", c.getString(R.string.fbdpi));
    }

    /**
     * Get fbdev
     * 
     * @param c context
     * @return device, e.g. /dev/graphics/fb0
     */
    public static String getFbDev(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("fbdev", c.getString(R.string.fbdev));
    }

    /**
     * Get fb input device
     * 
     * @param c context
     * @return device, e.g. /dev/input/event0
     */
    public static String getFbInput(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("fbinput", c.getString(R.string.fbinput));
    }

    /**
     * Get X arguments (framebuffer mode)
     * 
     * @param c context
     * @return arguments
     */
    public static String getFbArgs(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("fbargs", c.getString(R.string.fbargs));
    }
    
    /**
     * Get fb refresh mode
     * 
     * @param c context
     * @return 0 or 1
     */
    public static String getFbRefreshMode(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getBoolean("fbrefresh", c.getString(R.string.fbrefresh).equals("true")) ? "1" : "0";
    }

    /**
     * Get Android freeze mode
     * 
     * @param c context
     * @return mode, e.g. pause, stop or none.
     */
    public static String getFbFreezeMode(Context c) {
        SharedPreferences pref = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        return pref.getString("fbfreeze", c.getString(R.string.fbfreeze));
    }

    /**
     * Get hardware architecture
     * 
     * @param arch unformated architecture
     * @return intel, arm or mips
     */
    public static String getArch(String arch) {
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
    public static String getArch() {
        return getArch(System.getProperty("os.arch"));
    }

    /**
     * Get width of device screen
     * 
     * @param c context
     * @return screen width
     */
    @SuppressLint("NewApi")
    public static Integer getScreenWidth(Context c) {
        int width = 0;
        WindowManager wm = (WindowManager) c
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT > 12) {
            Point size = new Point();
            display.getSize(size);
            width = size.x;
        } else {
            width = display.getWidth(); // deprecated
        }
        return width;
    }

    /**
     * Get height of device screen
     * 
     * @param c context
     * @return screen height
     */
    @SuppressLint("NewApi")
    public static Integer getScreenHeight(Context c) {
        int height = 0;
        WindowManager wm = (WindowManager) c
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT > 12) {
            Point size = new Point();
            display.getSize(size);
            height = size.y;
        } else {
            height = display.getHeight(); // deprecated
        }
        return height;
    }

    /**
     * Get local IP address
     * 
     * @return ip address
     */
    public static String getLocalIpAddress() {
        String ip = "127.0.0.1";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && !inetAddress.isLinkLocalAddress()) {
                        ip = inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ip;
    }
    
    /**
     * Get current profile
     * 
     * @param c context
     * @return profile
     */
    public static String getCurrentProfile(Context c) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        String profile = pref.getString("profile", null);
        if (profile == null) {
            profile = String.valueOf(System.currentTimeMillis());
            setCurrentProfile(c, profile);
        }
        return profile;
    }
    
    /**
     * Set current profile
     * 
     * @param c context
     * @return profile
     */
    public static void setCurrentProfile(Context c, String profile) {
        SharedPreferences pref = c.getSharedPreferences(APP_PREF_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("profile", profile);
        editor.commit();
    }

    /**
     * Get current profile title
     * 
     * @param c context
     * @return profile title
     */
    public static String getCurrentProfileTitle(Context c) {
        SharedPreferences pref = c.getSharedPreferences(PROFILES_PREF_NAME,
                Context.MODE_PRIVATE);
        return pref
                .getString(getCurrentProfile(c), c.getString(R.string.profile));
    }

    /**
     * Get list of profiles
     * 
     * @param c context
     * @return list of profiles
     */
    public static List<Profile<String, String>> getProfiles(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PROFILES_PREF_NAME,
                Context.MODE_PRIVATE);
        List<Profile<String, String>> p = new ArrayList<Profile<String, String>>();
        for (Entry<String, ?> entry : sp.getAll().entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            p.add(new Profile<String, String>(key, value));
        }
        return p;
    }

    /**
     * Set list of profiles
     * 
     * @param c context
     * @param p list of profiles
     */
    public static void setProfiles(Context c, List<Profile<String, String>> p) {
        SharedPreferences sp = c.getSharedPreferences(PROFILES_PREF_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sp.edit();
        prefEditor.clear();
        prefEditor.commit();
        for (Profile<String, String> item : p) {
            String key = item.getKey();
            String value = item.getValue();
            prefEditor.putString(key, value);
        }
        prefEditor.commit();
    }

    /**
     * Delete profile
     * 
     * @param c context
     * @param key profile name
     */
    public static void deleteProfile(Context c, String key) {
        SharedPreferences sp = c
                .getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sp.edit();
        prefEditor.clear();
        prefEditor.commit();

        String fPref = "../shared_prefs/" + key + ".xml";
        File f = new File(c.getFilesDir(), fPref);
        if (f.exists()) f.delete();
    }

    /**
     * Import profile
     * 
     * @param c context
     * @param key profile name
     * @param src file name
     * @return true if no errors
     */
    public static Boolean importProfile(Context c, String key, String src) {
        String fPref = "../shared_prefs/" + key + ".xml";
        File destFile = new File(c.getFilesDir(), fPref);
        File sourceFile = new File(src);
        if (sourceFile.exists()) {
            try {
                copyFile(sourceFile, destFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Export profile
     * 
     * @param c context
     * @param key profile name
     * @param dst file name
     * @return true if no errors
     */
    public static Boolean exportProfile(Context c, String key, String dst) {
        String fPref = "../shared_prefs/" + key + ".xml";
        File sourceFile = new File(c.getFilesDir(), fPref);
        File destFile = new File(dst);
        if (sourceFile.exists()) {
            try {
                copyFile(sourceFile, destFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Load list of scripts
     * 
     * @param c context
     * @return list of scripts
     */
    public static List<String> getScriptsList(Context c) {
        SharedPreferences sp = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        String str = sp.getString("scripts", c.getString(R.string.scripts));
        List<String> list = new ArrayList<String>();
        for (String i : str.split(" ")) {
            list.add(i);
        }
        return list;
    }

    /**
     * Save list of scripts
     * 
     * @param c context
     * @param list list of scripts
     */
    public static void setScriptsList(Context c, List<String> list) {
        SharedPreferences sp = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sp.edit();
        String str = "";
        for (String i : list) {
            str += i + " ";
        }
        prefEditor.putString("scripts", str.trim());
        prefEditor.commit();
        CONF_CHANGE = true;
    }

    /**
     * Load list of mount points
     * 
     * @param c context
     * @return list of mount points
     */
    public static List<String> getMountsList(Context c) {
        SharedPreferences sp = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        String str = getValues(c, sp.getString("mounts", c.getString(R.string.mounts)));
        List<String> list = new ArrayList<String>();
        for (String i : str.split(" ")) {
            list.add(i);
        }
        return list;
    }

    /**
     * Save list of mount points
     * 
     * @param c context
     * @param list list of mount points
     */
    public static void setMountsList(Context c, List<String> list) {
        SharedPreferences sp = c.getSharedPreferences(getCurrentProfile(c),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sp.edit();
        String str = "";
        for (String i : list) {
            str += i + " ";
        }
        prefEditor.putString("mounts", str.trim());
        prefEditor.commit();
        CONF_CHANGE = true;
    }

    /**
     * Copy file
     * 
     * @param sourceFile source file
     * @param destFile destination file
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile)
            throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Set application locale
     * 
     * @param c context
     */
    public static void setLocale(Context c) {
        String language = getLanguage(c);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        c.getResources().updateConfiguration(config,
                c.getResources().getDisplayMetrics());
    }

    /**
     * Get Android resource id
     * 
     * @param c context
     * @param variableName
     * @param resourceName
     * @return
     */
    public static int getResourceId(Context c, String variableName,
            String resourceName) {
        try {
            return c.getResources().getIdentifier(variableName, resourceName,
                    c.getPackageName());
        } catch (Exception e) {
            return -1;
        }
    }

}
