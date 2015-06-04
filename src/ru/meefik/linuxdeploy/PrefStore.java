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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.view.Display;
import android.view.WindowManager;

import com.h6ah4i.android.compat.content.SharedPreferenceCompat;

public class PrefStore {

	// to application
	public static Boolean SCREEN_LOCK;
	public static Boolean WIFI_LOCK;
	public static String LANGUAGE;
	public static Integer FONT_SIZE;
	public static Integer MAX_LINE;
	public static String THEME;
	public static String ENV_DIR;
	public static Boolean BUILTIN_SHELL;
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
	public static String DESKTOP_ENV;
	public static String COMPONENTS;

	// to startup
	public static String STARTUP;
	public static String CUSTOM_SCRIPTS;
	public static String CUSTOM_MOUNTS;
	public static String SSH_PORT;
	public static String VNC_DISPLAY;
	public static String VNC_DEPTH;
	public static String VNC_DPI;
	public static String VNC_GEOMETRY;
	public static String VNC_ARGS;
	public static String XSERVER_DISPLAY;
	public static String XSERVER_HOST;
	public static String FB_DISPLAY;
	public static String FB_DPI;
	public static String FB_DEV;
	public static String FB_INPUT;
	public static String FB_ARGS;
	public static String FB_FREEZE;

	// miscellaneous
	public static String CURRENT_PROFILE;
	public static String EXTERNAL_STORAGE;
	public static Boolean PREF_CHANGE = false;
	public static String MARCH = "unknown";
	public static String VERSION = "unknown";
	public static String SHELL = "/system/xbin/ash";
	public static final String ROOT_ASSETS = "root";
	public static final String APP_PREF_FILE_NAME = "app_settings";
	public static final String PROFILES_FILE_NAME = "profiles";

	// get preferences
	public static void get(Context c) {
		EXTERNAL_STORAGE = Environment.getExternalStorageDirectory().getAbsolutePath();

		SharedPreferences sp = c.getSharedPreferences(APP_PREF_FILE_NAME,
				Context.MODE_PRIVATE);
		
		SharedPreferences.Editor prefEditor = sp.edit();

		SCREEN_LOCK = sp.getBoolean("screenlock",
				c.getString(R.string.screenlock).equals("true") ? true : false);
		WIFI_LOCK = sp.getBoolean("wifilock", c.getString(R.string.wifilock)
				.equals("true") ? true : false);
		FONT_SIZE = Integer.parseInt(sp.getString("fontsize",
				c.getString(R.string.fontsize)));
		MAX_LINE = Integer.parseInt(sp.getString("maxline",
				c.getString(R.string.maxline)));
		LANGUAGE = sp.getString("language", c.getString(R.string.language));
		THEME = sp.getString("theme", c.getString(R.string.theme));
		
		ENV_DIR = sp.getString("installdir", c.getString(R.string.envdir));
		if (ENV_DIR.equals("{replace}")) ENV_DIR = c.getApplicationInfo().dataDir+File.separator+"linux";
		// Update ENV_DIR
		prefEditor.putString("installdir", ENV_DIR);
		
		BUILTIN_SHELL = sp.getBoolean("builtinshell", c.getString(R.string.builtinshell).equals("true") ? true : false);
		if (BUILTIN_SHELL) {
			SHELL = ENV_DIR + "/bin/sh";
		}
		SYMLINK = sp.getBoolean("symlink", c.getString(R.string.symlink)
				.equals("true") ? true : false);
		CURRENT_PROFILE = sp.getString("profile", null);
		if (CURRENT_PROFILE == null)
			setCurrentProfile(c, String.valueOf(System.currentTimeMillis()));

		DEBUG_MODE = sp.getBoolean("debug",
				c.getString(R.string.debug).equals("true") ? true : false) ? "y"
				: "n";
		TRACE_MODE = sp.getBoolean("debug",
				c.getString(R.string.debug).equals("true") ? true : false)
				&& sp.getBoolean("trace",
						c.getString(R.string.trace).equals("true") ? true
								: false) ? "y" : "n";
		LOGGING = sp.getBoolean("logs",
				c.getString(R.string.logs).equals("true") ? true : false);
		LOG_FILE = sp.getString("logfile", EXTERNAL_STORAGE + "/linuxdeploy.log");
		
		prefEditor.commit();

		sp = c.getSharedPreferences(CURRENT_PROFILE, Context.MODE_PRIVATE);
		prefEditor = sp.edit();

		IMG_TARGET = sp.getString("diskimage", EXTERNAL_STORAGE + "/linux.img");
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
		DESKTOP_ENV = sp.getString("desktopenv",
				c.getString(R.string.desktopenv));
		Set<String> defcomp = new HashSet<String>(Arrays.asList(c
				.getResources().getStringArray(R.array.default_components)));
		if (!sp.contains("xcomponents")) {
			// set default components
			SharedPreferenceCompat.EditorCompat.putStringSet(prefEditor, "xcomponents", defcomp);
		}
		Set<String> comp_set = SharedPreferenceCompat.getStringSet(sp,
				"xcomponents", defcomp);
		String components = "";
		for (String str : comp_set) {
			components += str + " ";
		}
		COMPONENTS = components.trim();
		String startup_ssh = sp.getBoolean("sshstartup",
				c.getString(R.string.sshstartup).equals("true") ? true : false) ? "ssh"
				: "";
		String startup_gui = sp.getBoolean("guistartup",
				c.getString(R.string.guistartup).equals("true") ? true : false) ? sp
				.getString("guitype", c.getString(R.string.guitype)) : "";
		String startup_custom = sp.getBoolean("customstartup",
				c.getString(R.string.customstartup).equals("true") ? true
						: false) ? "custom" : "";
		STARTUP = (startup_ssh + " " + startup_gui + " " + startup_custom)
				.trim();
		CUSTOM_SCRIPTS = sp.getBoolean("customstartup",
				c.getString(R.string.customstartup).equals("true") ? true
						: false) ? sp.getString("scripts",
				c.getString(R.string.scripts)).trim() : "";
		CUSTOM_MOUNTS = sp
				.getBoolean("custommounts", c.getString(R.string.custommount)
						.equals("true") ? true : false) ? sp.getString(
				"mounts", EXTERNAL_STORAGE).trim() : "";
		SSH_PORT = sp.getString("sshport", c.getString(R.string.sshport));
		VNC_DISPLAY = sp.getString("vncdisplay",
				c.getString(R.string.vncdisplay));
		VNC_DEPTH = sp.getString("vncdepth", c.getString(R.string.vncdepth));
		VNC_DPI = sp.getString("vncdpi", c.getString(R.string.vncdpi));
		VNC_GEOMETRY = sp.getString("vncwidth", String.valueOf(getWidth(c)))
				+ "x" + sp.getString("vncheight", String.valueOf(getHeight(c)));
		VNC_ARGS = sp.getString("vncargs", c.getString(R.string.vncargs));
		XSERVER_DISPLAY = sp.getString("xdisplay",
				c.getString(R.string.xdisplay));
		XSERVER_HOST = sp.getString("xhost", c.getString(R.string.xhost));
		FB_DISPLAY = sp.getString("fbdisplay", c.getString(R.string.fbdisplay));
		FB_DPI = sp.getString("fbdpi", c.getString(R.string.fbdpi));
		FB_DEV = sp.getString("fbdev", c.getString(R.string.fbdev));
		FB_INPUT = sp.getString("fbinput", c.getString(R.string.fbinput));
		FB_ARGS = sp.getString("fbargs", c.getString(R.string.fbargs));
		FB_FREEZE = sp.getString("fbfreeze", c.getString(R.string.fbfreeze));
		
		prefEditor.commit();
		
		MARCH = getArch(System.getProperty("os.arch"));

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
	
	public static String getArch(String arch) {
		String march = "";
		if (arch.length() > 0) {
			char a = arch.toLowerCase().charAt(0);
			switch (a) {
			case 'a': march = "arm"; break;
			case 'm': march = "mips"; break;
			case 'i': 
			case 'x': march = "intel"; break;
			}
		}
	    return march;
	}

	@SuppressLint("NewApi")
	public static int getWidth(Context mContext) {
		int width = 0;
		WindowManager wm = (WindowManager) mContext
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

	@SuppressLint("NewApi")
	public static int getHeight(Context mContext) {
		int height = 0;
		WindowManager wm = (WindowManager) mContext
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
	public static List<Profile<String, String>> getProfiles(Context c) {
		SharedPreferences sp = c.getSharedPreferences(PROFILES_FILE_NAME,
				Context.MODE_PRIVATE);
		List<Profile<String, String>> p = new ArrayList<Profile<String, String>>();
		for (Entry<String, ?> entry : sp.getAll().entrySet()) {
			String key = entry.getKey();
			String value = (String) entry.getValue();
			p.add(new Profile<String, String>(key, value));
		}
		return p;
	}

	// set profiles list
	public static void setProfiles(Context c, List<Profile<String, String>> p) {
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

	// import profile
	public static boolean importProfile(Context c, String key, String src) {
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

	// export profile
	public static boolean exportProfile(Context c, String key, String dst) {
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

	// Load scripts list
	public static List<String> getScriptsList(Context c) {
		SharedPreferences sp = c.getSharedPreferences(CURRENT_PROFILE,
				Context.MODE_PRIVATE);
		String str = sp.getString("scripts", c.getString(R.string.scripts));
		List<String> list = new ArrayList<String>();
		for (String i : str.split(" ")) {
			list.add(i);
		}
		return list;
	}

	// Save scripts list
	public static void setScriptsList(Context c, List<String> list) {
		SharedPreferences sp = c.getSharedPreferences(CURRENT_PROFILE,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = sp.edit();
		String str = "";
		for (String i : list) {
			str += i + " ";
		}
		prefEditor.putString("scripts", str.trim());
		prefEditor.commit();
		PREF_CHANGE = true;
	}

	// Load mounts list
	public static List<String> getMountsList(Context c) {
		SharedPreferences sp = c.getSharedPreferences(CURRENT_PROFILE,
				Context.MODE_PRIVATE);
		String str = sp.getString("mounts", EXTERNAL_STORAGE);
		List<String> list = new ArrayList<String>();
		for (String i : str.split(" ")) {
			list.add(i);
		}
		return list;
	}

	// Save mounts list
	public static void setMountsList(Context c, List<String> list) {
		SharedPreferences sp = c.getSharedPreferences(CURRENT_PROFILE,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = sp.edit();
		String str = "";
		for (String i : list) {
			str += i + " ";
		}
		prefEditor.putString("mounts", str.trim());
		prefEditor.commit();
		PREF_CHANGE = true;
	}

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
	
	public static int getResourceId(Context c, String variableName, String resourceName) {
	    try {
	        return c.getResources().getIdentifier(variableName, resourceName, c.getPackageName());
	    } catch (Exception e) {
	        return -1;
	    } 
	}

}
