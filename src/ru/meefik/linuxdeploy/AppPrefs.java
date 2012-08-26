package ru.meefik.linuxdeploy;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

public class AppPrefs {

	// to deploy
	public static String HOME_DIR;
	public static String MNT_TARGET;
	public static String IMG_TARGET;
	public static String IMG_SIZE;
	public static String ARCH;
	public static String SUITE;
	public static String MIRROR;
	public static String USER_NAME;
	public static String INSTALL_GUI;
	public static String CUSTOM_STARTUP;
	public static String SSH_START;
	public static String SSH_PORT;
	public static String VNC_START;
	public static String VNC_DISPLAY;
	public static String VNC_DEPTH;
	public static String VNC_GEOMETRY;
	
	// to application
	public static Boolean SCREEN_LOCK;
	public static Boolean WIFI_LOCK;
	public static String LANGUAGE;
	public static Integer FONT_SIZE;
	
	public static final String ROOT_ASSETS = "home";
	
	
	public static void get(Context c) {
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		
		HOME_DIR = sp.getString("installdir", "/data/local/linux");
		MNT_TARGET = HOME_DIR + "/mnt";
		IMG_TARGET = sp.getString("diskimage", "/mnt/sdcard/linux.img");
		IMG_SIZE = sp.getString("disksize", "1024");
		ARCH = sp.getString("architecture", "armhf");
		SUITE = sp.getString("suite", "wheezy");
		MIRROR = sp.getString("mirror", "http://mirror.yandex.ru/debian");
		USER_NAME = sp.getString("username", "admin");
		INSTALL_GUI = sp.getBoolean("installgui", false) ? "y" : "n";
		CUSTOM_STARTUP = sp.getString("customstartup", "");
		SSH_START = sp.getBoolean("sshstartup", false) ? "y" : "n";
		SSH_PORT = sp.getString("sshport", "22");
		VNC_START = sp.getBoolean("vncstartup", false) ? "y" : "n";
		VNC_DISPLAY = sp.getString("vncdisplay", "0");
		VNC_DEPTH = sp.getString("vncdepth", "16");
		VNC_GEOMETRY = sp.getString("vncwidth", "800")+"x"+sp.getString("vncheight", "400");
		
		SCREEN_LOCK = sp.getBoolean("screenlock", false);
		WIFI_LOCK = sp.getBoolean("wifilock", false);
		LANGUAGE = sp.getString("language", "en");
		FONT_SIZE = Integer.parseInt(sp.getString("fontsize", "10"));

	}
	
    // multilanguage support
    public static void updateLocale(Context c) {
    	
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
    	LANGUAGE = sp.getString("language", "en");
    	
        Locale locale = new Locale(LANGUAGE); 
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        c.getResources().updateConfiguration(config,c.getResources().getDisplayMetrics());
        
    }
    
}
