package ru.meefik.linuxdeploy;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Environment;

public class PrefStore {

	// to application
	public static Boolean SCREEN_LOCK;
	public static Boolean WIFI_LOCK;
	public static String LANGUAGE;
	public static Integer FONT_SIZE;
	public static Integer MAX_LINE;
	public static String THEME;
	public static String ENV_DIR;
	public static Boolean SYMLINK;

	// to debug
	public static String DEBUG_MODE;
	public static String TRACE_MODE;
	public static Boolean LOGGING;
	public static String LOG_FILE;

	// to deploy
	public static String IMG_TARGET;
	public static String DEPLOY_TYPE;
	public static String IMG_SIZE;
	public static String FS_TYPE;
	public static String DISTRIB;
	public static String SUITE;
	public static String MIRROR;
	public static String ARCH;
	public static String USER_NAME;
	public static String SERVER_DNS;
	public static String LOCALE;
	public static String INSTALL_GUI;
	public static String DESKTOP_ENV;

	// to startup
	public static String CUSTOM_STARTUP;
	public static String CUSTOM_MOUNT;
	public static String SSH_START;
	public static String SSH_PORT;
	public static String VNC_START;
	public static String VNC_DISPLAY;
	public static String VNC_DEPTH;
	public static String VNC_DPI;
	public static String VNC_GEOMETRY;
	public static String XSERVER_START;
	public static String XSERVER_DISPLAY;
	public static String XSERVER_HOST;
	public static String FB_START;
	public static String FB_DISPLAY;
	public static String FB_DEV;
	public static String FB_INPUT;
	public static String FB_ANDROID;

	// miscellaneous
	public static String CURRENT_PROFILE;
	public static Boolean PREF_CHANGE = false;
	public static String VERSION = "unknown";
	public static final String ROOT_ASSETS = "home";
	public static final String APP_PREF_FILE_NAME = "app_settings";
	public static final String PROFILES_FILE_NAME = "profiles";

	// get preferences
	public static void get(Context c) {
		File extStore = Environment.getExternalStorageDirectory();
		
		SharedPreferences sp = c.getSharedPreferences(APP_PREF_FILE_NAME,
				Context.MODE_PRIVATE);

		SCREEN_LOCK = sp.getBoolean("screenlock",
				c.getString(R.string.screenlock).equals("true") ? true : false);
		WIFI_LOCK = sp.getBoolean("wifilock",
				c.getString(R.string.wifilock).equals("true") ? true : false);
		FONT_SIZE = Integer.parseInt(sp.getString("fontsize",
				c.getString(R.string.fontsize)));
		MAX_LINE = Integer.parseInt(sp.getString("maxline",
				c.getString(R.string.maxline)));
		LANGUAGE = sp.getString("language", c.getString(R.string.language));
		THEME = sp.getString("theme", c.getString(R.string.theme));
		ENV_DIR = sp.getString("installdir", c.getString(R.string.envdir));
		SYMLINK = sp.getBoolean("symlink",
				c.getString(R.string.symlink).equals("true") ? true : false);
		CURRENT_PROFILE = sp.getString("profile", null);
		if (CURRENT_PROFILE == null)
			setCurrentProfile(c, String.valueOf(System.currentTimeMillis()));

		DEBUG_MODE = sp.getBoolean("debug",
				c.getString(R.string.debug).equals("true") ? true : false) ? "y"
				: "n";
		TRACE_MODE = sp.getBoolean("debug",
				c.getString(R.string.debug).equals("true") ? true : false)
				&& sp.getBoolean("trace",
						c.getString(R.string.trace).equals("true") ? true : false) ? "y"
				: "n";
		LOGGING = sp.getBoolean("logs",
				c.getString(R.string.logs).equals("true") ? true : false);
		LOG_FILE = sp.getString("logfile", extStore.getAbsolutePath()+"/linuxdeploy.log");

		sp = c.getSharedPreferences(CURRENT_PROFILE, Context.MODE_PRIVATE);

		IMG_TARGET = sp.getString("diskimage", extStore.getAbsolutePath()+"/linux.img");
		DEPLOY_TYPE = sp.getString("deploytype",
				c.getString(R.string.deploytype));
		IMG_SIZE = sp.getString("disksize", c.getString(R.string.disksize));
		FS_TYPE = sp.getString("fstype", c.getString(R.string.fstype));
		DISTRIB = sp.getString("distribution",
				c.getString(R.string.distribution));
		SUITE = sp.getString("suite", c.getString(R.string.suite));
		MIRROR = sp.getString("mirror", c.getString(R.string.mirror));
		ARCH = sp.getString("architecture", c.getString(R.string.architecture));
		USER_NAME = sp.getString("username", c.getString(R.string.username))
				.toLowerCase(Locale.ENGLISH);
		SERVER_DNS = sp.getString("serverdns", c.getString(R.string.serverdns));
		LOCALE = sp.getString("locale", c.getString(R.string.locale));
		INSTALL_GUI = sp.getBoolean("installgui",
				c.getString(R.string.installgui).equals("true") ? true : false) ? "y"
				: "n";
		DESKTOP_ENV = sp.getString("desktopenv",
				c.getString(R.string.desktopenv));

		CUSTOM_STARTUP = sp.getBoolean("customstartup",
				c.getString(R.string.customstartup).equals("true") ? true : false) ? sp
				.getString("customscript", c.getString(R.string.customscript))
				: "";
		CUSTOM_MOUNT = sp.getBoolean("mountcustom",
				c.getString(R.string.mountcustom).equals("true") ? true : false) ? sp
				.getString("mountpath", extStore.getAbsolutePath()) : "";
		SSH_START = sp.getBoolean("sshstartup",
				c.getString(R.string.sshstartup).equals("true") ? true : false) ? "y"
				: "n";
		SSH_PORT = sp.getString("sshport", c.getString(R.string.sshport));
		VNC_START = sp.getBoolean("vncstartup",
				c.getString(R.string.vncstartup).equals("true") ? true : false) ? "y"
				: "n";
		VNC_DISPLAY = sp.getString("vncdisplay",
				c.getString(R.string.vncdisplay));
		VNC_DEPTH = sp.getString("vncdepth", c.getString(R.string.vncdepth));
		VNC_DPI = sp.getString("vncdpi", c.getString(R.string.vncdpi));
		VNC_GEOMETRY = sp.getString("vncwidth", c.getString(R.string.vncwidth))
				+ "x"
				+ sp.getString("vncheight", c.getString(R.string.vncheight));

		XSERVER_START = sp.getBoolean("xstartup",
				c.getString(R.string.xstartup).equals("true") ? true : false) ? "y"
				: "n";
		XSERVER_DISPLAY = sp.getString("xdisplay",
				c.getString(R.string.xdisplay));
		XSERVER_HOST = sp.getString("xhost", c.getString(R.string.xhost));
		
		FB_START = sp.getBoolean("fbstartup",
				c.getString(R.string.fbstartup).equals("true") ? true : false) ? "y"
				: "n";
		FB_DISPLAY = sp.getString("fbdisplay",
				c.getString(R.string.fbdisplay));
		FB_DEV = sp.getString("fbdev", c.getString(R.string.fbdev));
		FB_INPUT = sp.getString("fbinput", c.getString(R.string.fbinput));
		FB_ANDROID = sp.getBoolean("fbandroid",
				c.getString(R.string.fbandroid).equals("true") ? true : false) ? "y"
				: "n";
		
		try {
			VERSION = c.getPackageManager().getPackageInfo(c.getPackageName(),
					0).versionName
					+ "-"
					+ String.valueOf(c.getPackageManager().getPackageInfo(
							c.getPackageName(), 0).versionCode);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
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
	
	public static boolean isAutostart(Context c) {
		SharedPreferences sp = c.getSharedPreferences(
				PrefStore.APP_PREF_FILE_NAME, Context.MODE_PRIVATE);
		boolean isAutostart = sp.getBoolean("autostart",
				c.getString(R.string.autostart).equals("true") ? true : false);
		return isAutostart;
	}
	
	public static boolean isShowIcon(Context c) {
		SharedPreferences sp = c.getSharedPreferences(
				PrefStore.APP_PREF_FILE_NAME, Context.MODE_PRIVATE);
		boolean isAutostart = sp.getBoolean("appicon",
				c.getString(R.string.appicon).equals("true") ? true : false);
		return isAutostart;
	}

	// get current profile name
	public static String getCurrentProfile(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PROFILES_FILE_NAME,
				Context.MODE_PRIVATE);
		return sp.getString(CURRENT_PROFILE, c.getString(R.string.profile));
	}

	// set current profile
	public static void setCurrentProfile(Context c, String key) {
		SharedPreferences sp = c.getSharedPreferences(APP_PREF_FILE_NAME,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = sp.edit();
		prefEditor.putString("profile", key);
		prefEditor.commit();
		CURRENT_PROFILE = key;
	}

	// get profiles list
	public static ArrayList<Profile<String, String>> getProfiles(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PROFILES_FILE_NAME,
				Context.MODE_PRIVATE);
		ArrayList<Profile<String, String>> p = new ArrayList<Profile<String, String>>();
		for (Entry<String, ?> entry : sp.getAll().entrySet()) {
			String key = entry.getKey();
			String value = (String) entry.getValue();
			p.add(new Profile<String, String>(key, value));
		}
		return p;
	}

	// set profiles list
	public static void setProfiles(Context c,
			ArrayList<Profile<String, String>> p) {
		SharedPreferences sp = c.getSharedPreferences(PROFILES_FILE_NAME,
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

	// delete profile
	public static void deleteProfile(Context c, String key) {
		SharedPreferences sp = c
				.getSharedPreferences(key, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = sp.edit();
		prefEditor.clear();
		prefEditor.commit();

		String fPref = "../shared_prefs/" + key + ".xml";
		File f = new File(c.getFilesDir(), fPref);
		if (f.exists())
			f.delete();
	}

	// multilanguage support
	public static void updateLocale(Context c) {
		SharedPreferences sp = c.getSharedPreferences(APP_PREF_FILE_NAME,
				Context.MODE_PRIVATE);
		LANGUAGE = sp.getString("language", c.getString(R.string.language));
		Locale locale = new Locale(LANGUAGE);
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		c.getResources().updateConfiguration(config,
				c.getResources().getDisplayMetrics());
	}

	// themes support
	public static void updateTheme(Context c) {
		SharedPreferences sp = c.getSharedPreferences(APP_PREF_FILE_NAME,
				Context.MODE_PRIVATE);
		THEME = sp.getString("theme", c.getString(R.string.theme));

		if (THEME
				.equals(c.getResources().getStringArray(R.array.theme_values)[0]))
			c.setTheme(R.style.BlackTheme);
		if (THEME
				.equals(c.getResources().getStringArray(R.array.theme_values)[1]))
			c.setTheme(R.style.LightTheme);
	}

}
