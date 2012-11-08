package ru.meefik.linuxdeploy;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

public class PrefStore {

	// to application
	public static Boolean SCREEN_LOCK;
	public static String LANGUAGE;
	public static Integer FONT_SIZE;
	public static String THEME;
	public static String HOME_DIR;

	// to debug
	public static Boolean DEBUG_MODE;
	public static String TRACE_MODE;
	public static Boolean LOGGING;
	public static String LOG_FILE;

	// to deploy
	public static String MNT_TARGET;
	public static String IMG_TARGET;
	public static String LOOP_DEV;
	public static String IMG_SIZE;
	public static String FS_TYPE;
	public static String DISTRIB;
	public static String ARCH;
	public static String SUITE;
	public static String MIRROR;
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
	public static String VNC_GEOMETRY;
	public static String XSERVER_START;
	public static String XSERVER_DISPLAY;
	public static String XSERVER_HOST;

	// miscellaneous
	public static String CURRENT_PROFILE;
	public static final String ROOT_ASSETS = "home";
	public static final String APP_PREF_FILE_NAME = "app_settings";
	public static final String PROFILES_FILE_NAME = "profiles";

	// get preferences
	public static void get(Context c) {
		SharedPreferences sp = c.getSharedPreferences(APP_PREF_FILE_NAME,
				Context.MODE_PRIVATE);

		SCREEN_LOCK = sp.getBoolean("screenlock",
				c.getString(R.string.screenlock) == "true" ? true : false);
		FONT_SIZE = Integer.parseInt(sp.getString("fontsize",
				c.getString(R.string.fontsize)));
		LANGUAGE = sp.getString("language", c.getString(R.string.language));
		THEME = sp.getString("theme", c.getString(R.string.theme));
		HOME_DIR = sp.getString("installdir", c.getString(R.string.installdir));
		CURRENT_PROFILE = sp.getString("profile", null);
		if (CURRENT_PROFILE == null)
			setCurrentProfile(c, String.valueOf(System.currentTimeMillis()));

		DEBUG_MODE = sp.getBoolean("debug",
				c.getString(R.string.debug) == "true" ? true : false);
		TRACE_MODE = DEBUG_MODE
				&& sp.getBoolean("trace",
						c.getString(R.string.trace) == "true" ? true : false) ? "y"
				: "n";
		LOGGING = sp.getBoolean("logs",
				c.getString(R.string.logs) == "true" ? true : false);
		LOG_FILE = sp.getString("logfile", c.getString(R.string.logfile));

		sp = c.getSharedPreferences(CURRENT_PROFILE, Context.MODE_PRIVATE);

		MNT_TARGET = PrefStore.HOME_DIR + "/mnt";
		IMG_TARGET = sp.getString("diskimage", c.getString(R.string.diskimage));
		IMG_SIZE = sp.getString("disksize", c.getString(R.string.disksize));
		FS_TYPE = sp.getString("fstype", c.getString(R.string.fstype));
		DISTRIB = sp.getString("distribution",
				c.getString(R.string.distribution));
		ARCH = sp.getString("architecture", c.getString(R.string.architecture));
		SUITE = sp.getString("suite", c.getString(R.string.suite));
		MIRROR = sp.getString("mirror", c.getString(R.string.mirror));
		USER_NAME = sp.getString("username", c.getString(R.string.username))
				.toLowerCase();
		SERVER_DNS = sp.getString("serverdns", c.getString(R.string.serverdns));
		LOCALE = sp.getString("locale", c.getString(R.string.locale));
		INSTALL_GUI = sp.getBoolean("installgui",
				c.getString(R.string.installgui) == "true" ? true : false) ? "y"
				: "n";
		DESKTOP_ENV = sp.getString("desktopenv",
				c.getString(R.string.desktopenv));

		CUSTOM_STARTUP = sp.getBoolean("customstartup",
				c.getString(R.string.customstartup) == "true" ? true : false) ? sp
				.getString("customscript", c.getString(R.string.customscript))
				: "";
		CUSTOM_MOUNT = sp.getBoolean("mountcustom",
				c.getString(R.string.mountcustom) == "true" ? true : false) ? sp
				.getString("mountpath", c.getString(R.string.mountpath)) : "";
		SSH_START = sp.getBoolean("sshstartup",
				c.getString(R.string.sshstartup) == "true" ? true : false) ? "y"
				: "n";
		SSH_PORT = sp.getString("sshport", c.getString(R.string.sshport));
		VNC_START = sp.getBoolean("vncstartup",
				c.getString(R.string.vncstartup) == "true" ? true : false) ? "y"
				: "n";
		VNC_DISPLAY = sp.getString("vncdisplay",
				c.getString(R.string.vncdisplay));
		VNC_DEPTH = sp.getString("vncdepth", c.getString(R.string.vncdepth));
		VNC_GEOMETRY = sp.getString("vncwidth", c.getString(R.string.vncwidth))
				+ "x"
				+ sp.getString("vncheight", c.getString(R.string.vncheight));

		XSERVER_START = sp.getBoolean("xstartup",
				c.getString(R.string.xstartup) == "true" ? true : false) ? "y"
				: "n";
		XSERVER_DISPLAY = sp.getString("xdisplay",
				c.getString(R.string.xdisplay));
		XSERVER_HOST = sp.getString("xhost", c.getString(R.string.xhost));

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
