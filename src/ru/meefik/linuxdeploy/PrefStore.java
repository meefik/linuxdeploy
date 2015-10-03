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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
	public static String MNT_TARGET;
	public static String IMG_TARGET;
	public static String DEPLOY_TYPE;
	public static String IMG_SIZE;
	public static String FS_TYPE;
	public static String DISTRIB;
	public static String SUITE;
	public static String MIRROR;
	public static String ARCH;
	public static String USER_NAME;
	public static String USER_PASSWORD;
	public static String SERVER_DNS;
	public static String LOCALE;
	public static String DESKTOP_ENV;
	public static String USE_COMPONENTS;

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
	public static Boolean XSERVER_XSDL;
	public static String FB_DISPLAY;
	public static String FB_DPI;
	public static String FB_DEV;
	public static String FB_INPUT;
	public static String FB_ARGS;
	public static String FB_FREEZE;

	// miscellaneous
	public static String CURRENT_PROFILE;
	public static String EXTERNAL_STORAGE;
	public static String SHELL;
	public static Boolean CONF_CHANGE = false;
	public static String MARCH = "unknown";
	public static String VERSION = "unknown";
	public static final String ROOT_ASSETS = "root";
	public static final String APP_PREF_FILE_NAME = "app_settings";
	public static final String PROFILES_FILE_NAME = "profiles";

	// get preferences
	public static void get(Context c) {
		EXTERNAL_STORAGE = Environment.getExternalStorageDirectory().getAbsolutePath();
		MARCH = getArch(System.getProperty("os.arch"));
		VERSION = getVersion(c);

		SharedPreferences pref = c.getSharedPreferences(APP_PREF_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		SCREEN_LOCK = pref.getBoolean("screenlock",
				c.getString(R.string.screenlock).equals("true"));
		WIFI_LOCK = pref.getBoolean("wifilock", c.getString(R.string.wifilock)
				.equals("true"));
		
		String fontSize = pref.getString("fontsize", c.getString(R.string.fontsize));
		try {
			FONT_SIZE = Integer.parseInt(fontSize);
		} catch(Exception e) {
			fontSize = c.getString(R.string.fontsize);
			FONT_SIZE = Integer.parseInt(fontSize);
			editor.putString("fontsize", fontSize);
		}
		String maxLine = pref.getString("maxline", c.getString(R.string.maxline));
		try {
			MAX_LINE = Integer.parseInt(maxLine);
		} catch(Exception e) {
			maxLine = c.getString(R.string.maxline);
			MAX_LINE = Integer.parseInt(maxLine);
			editor.putString("maxline", maxLine);
		}
		LANGUAGE = pref.getString("language", c.getString(R.string.language));
		if (LANGUAGE.length() == 0) {
			LANGUAGE = getDefaultLanguage(c);
			editor.putString("language", LANGUAGE);
		}
		THEME = pref.getString("theme", c.getString(R.string.theme));

		ENV_DIR = pref.getString("installdir", c.getString(R.string.installdir));
		if (ENV_DIR.length() == 0) {
			ENV_DIR = c.getApplicationInfo().dataDir + "/linux";
			editor.putString("installdir", ENV_DIR);
		}

		BUILTIN_SHELL = pref.getBoolean("builtinshell",
				c.getString(R.string.builtinshell).equals("true"));
		if (BUILTIN_SHELL) {
			SHELL = ENV_DIR + "/bin/ash";
		} else {
			SHELL = "/system/xbin/ash";
		}
		SYMLINK = pref.getBoolean("symlink", c.getString(R.string.symlink).equals("true"));
		CURRENT_PROFILE = pref.getString("profile", null);
		if (CURRENT_PROFILE == null) {
			setCurrentProfile(c, String.valueOf(System.currentTimeMillis()));
		}

		DEBUG_MODE = pref.getBoolean("debug",
				c.getString(R.string.debug).equals("true")) ? "y"
				: "n";
		TRACE_MODE = pref.getBoolean("debug",
				c.getString(R.string.debug).equals("true"))
				&& pref.getBoolean("trace",
						c.getString(R.string.trace).equals("true")) ? "y" : "n";
		LOGGING = pref.getBoolean("logs",
				c.getString(R.string.logs).equals("true"));
		LOG_FILE = pref.getString("logfile", c.getString(R.string.logfile));
		if (LOG_FILE.length() == 0) {
			LOG_FILE = EXTERNAL_STORAGE + "/linuxdeploy.log";
			editor.putString("logfile", LOG_FILE);
		}

		editor.commit();

		pref = c.getSharedPreferences(CURRENT_PROFILE, Context.MODE_PRIVATE);
		editor = pref.edit();

		MNT_TARGET = pref.getString("mountdir", c.getString(R.string.mountdir));
		IMG_TARGET = pref.getString("diskimage", c.getString(R.string.diskimage));
		if (IMG_TARGET.length() == 0) {
			IMG_TARGET = EXTERNAL_STORAGE + "/linux.img";
			editor.putString("diskimage", IMG_TARGET);
		}
		DEPLOY_TYPE = pref.getString("deploytype", c.getString(R.string.deploytype));
		IMG_SIZE = pref.getString("disksize", c.getString(R.string.disksize));
		FS_TYPE = pref.getString("fstype", c.getString(R.string.fstype));
		DISTRIB = pref.getString("distribution", c.getString(R.string.distribution));
		SUITE = pref.getString("suite", c.getString(R.string.suite));
		MIRROR = pref.getString("mirror", c.getString(R.string.mirror));
		ARCH = pref.getString("architecture", c.getString(R.string.architecture));
		USER_NAME = pref.getString("username", c.getString(R.string.username))
				.toLowerCase(Locale.ENGLISH);
		USER_PASSWORD = pref
				.getString("password", c.getString(R.string.password))
				.toLowerCase(Locale.ENGLISH);
		SERVER_DNS = pref.getString("serverdns", c.getString(R.string.serverdns));
		LOCALE = pref.getString("locale", c.getString(R.string.locale));
		DESKTOP_ENV = pref.getString("desktopenv", c.getString(R.string.desktopenv));
		Set<String> defcomp = new HashSet<String>(Arrays.asList(c
				.getResources().getStringArray(R.array.default_components)));
		if (!pref.contains("xcomponents")) {
			// set default components
			SharedPreferenceCompat.EditorCompat.putStringSet(editor,
					"xcomponents", defcomp);
		}
		Set<String> comp_set = SharedPreferenceCompat.getStringSet(pref,
				"xcomponents", defcomp);
		String components = "";
		for (String str : comp_set) {
			components += str + " ";
		}
		USE_COMPONENTS = components.trim();
		String startup_ssh = pref.getBoolean("sshstartup",
				c.getString(R.string.sshstartup).equals("true")) ? "ssh"
				: "";
		String startup_gui = pref.getBoolean("guistartup",
				c.getString(R.string.guistartup).equals("true")) ? pref
				.getString("guitype", c.getString(R.string.guitype)) : "";
		String startup_custom = pref.getBoolean("customstartup",
				c.getString(R.string.customstartup).equals("true")) ? "custom" : "";
		STARTUP = (startup_ssh + " " + startup_gui + " " + startup_custom)
				.trim();
		CUSTOM_SCRIPTS = pref.getBoolean("customstartup",
				c.getString(R.string.customstartup).equals("true")) ? pref.getString("scripts",
				c.getString(R.string.scripts)).trim() : "";
		CUSTOM_MOUNTS = pref
				.getBoolean("custommounts", c.getString(R.string.custommount)
						.equals("true")) ? pref.getString(
				"mounts", EXTERNAL_STORAGE).trim() : "";
		SSH_PORT = pref.getString("sshport", c.getString(R.string.sshport));
		VNC_DISPLAY = pref.getString("vncdisplay",
				c.getString(R.string.vncdisplay));
		VNC_DEPTH = pref.getString("vncdepth", c.getString(R.string.vncdepth));
		VNC_DPI = pref.getString("vncdpi", c.getString(R.string.vncdpi));
		String vncWidth = pref.getString("vncwidth", c.getString(R.string.vncwidth));
		if (vncWidth.length() == 0) {
			vncWidth = String.valueOf(getWidth(c));
			editor.putString("vncwidth", vncWidth);
		}
		String vncHeight = pref.getString("vncheight", c.getString(R.string.vncheight));
		if (vncHeight.length() == 0) {
			vncHeight = String.valueOf(getHeight(c));
			editor.putString("vncheight", vncHeight);
		}
		VNC_GEOMETRY = vncWidth + "x" + vncHeight;
		VNC_ARGS = pref.getString("vncargs", c.getString(R.string.vncargs));
		XSERVER_DISPLAY = pref.getString("xdisplay",
				c.getString(R.string.xdisplay));
		XSERVER_HOST = pref.getString("xhost", c.getString(R.string.xhost));
		XSERVER_XSDL = pref.getBoolean("xserverxsdl", c.getString(R.string.xserverxsdl).equals("true"));
		FB_DISPLAY = pref.getString("fbdisplay", c.getString(R.string.fbdisplay));
		FB_DPI = pref.getString("fbdpi", c.getString(R.string.fbdpi));
		FB_DEV = pref.getString("fbdev", c.getString(R.string.fbdev));
		FB_INPUT = pref.getString("fbinput", c.getString(R.string.fbinput));
		FB_ARGS = pref.getString("fbargs", c.getString(R.string.fbargs));
		FB_FREEZE = pref.getString("fbfreeze", c.getString(R.string.fbfreeze));

		editor.commit();
	}

	public static String getArch(String arch) {
		String march = "";
		if (arch.length() > 0) {
			char a = arch.toLowerCase().charAt(0);
			switch (a) {
			case 'a':
				if (arch.equals("amd64"))
					march = "intel";
				else
					march = "arm";
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
		CONF_CHANGE = true;
	}

	// Load mounts list
	public static List<String> getMountsList(Context c) {
		SharedPreferences sp = c.getSharedPreferences(CURRENT_PROFILE,
				Context.MODE_PRIVATE);
		String str = sp.getString("mounts", c.getString(R.string.mounts)
				.replace("{storage}", EXTERNAL_STORAGE));
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
		CONF_CHANGE = true;
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
		SharedPreferences pref = c.getSharedPreferences(APP_PREF_FILE_NAME, Context.MODE_PRIVATE);
		LANGUAGE = pref.getString("language", c.getString(R.string.language));
		if (LANGUAGE.length() == 0) {
			LANGUAGE = getDefaultLanguage(c);
		}
		Locale locale = new Locale(LANGUAGE);
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		c.getResources().updateConfiguration(config,
				c.getResources().getDisplayMetrics());
	}

	// themes support
	public static void updateTheme(Context c) {
		SharedPreferences pref = c.getSharedPreferences(APP_PREF_FILE_NAME, Context.MODE_PRIVATE);
		THEME = pref.getString("theme", c.getString(R.string.theme));
		switch (THEME) {
		case "dark":
			c.setTheme(R.style.BlackTheme);
			break;
		case "light":
			c.setTheme(R.style.LightTheme);
			break;
		}
	}

	public static int getResourceId(Context c, String variableName,
			String resourceName) {
		try {
			return c.getResources().getIdentifier(variableName, resourceName,
					c.getPackageName());
		} catch (Exception e) {
			return -1;
		}
	}
	
    // application version
    public static String getVersion(Context c) {
        String version = "";
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            version = pi.versionName + "-" + pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }
    
    public static String getDefaultLanguage(Context c) {
        String countryCode = Locale.getDefault().getLanguage();
        if (Arrays.asList(c.getResources().
        		getStringArray(R.array.language_values)).
            	contains(countryCode)) {
            return countryCode;
        } else {
        	return "en";
        }
    }
}
