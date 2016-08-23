package ru.meefik.linuxdeploy;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ParamUtils {

    private String name;
    private List<String> params;

    public ParamUtils(String name, String[] params) {
        this.name = name;
        this.params = Arrays.asList(params);
    }

    /**
     * Closeable helper
     *
     * @param c closable object
     */
    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<String, String> readConf(File confFile) {
        TreeMap<String, String> map = new TreeMap<>();
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(confFile));
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#") && !line.isEmpty()) {
                    String[] pair = line.split("=");
                    String key = pair[0];
                    String value = pair[1];
                    map.put(key, value.replaceAll("\"",""));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(br);
        }
        return map;
    }

    public static boolean writeConf(Map<String, String> map, File confFile) {
        Boolean result = false;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(confFile));
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                bw.write(key + "=\"" + value+"\"");
                bw.newLine();
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(bw);
        }
        return result;
    }

    public String fixOutputParam(Context c, String key, String value) {
        return value;
    }

    public String get(Context c, String key) {
        SharedPreferences pref = c.getSharedPreferences(this.name, Context.MODE_PRIVATE);
        int resourceId = PrefStore.getResourceId(c, key, "string");
        Map<String, ?> source = pref.getAll();
        String defaultValue = "";
        if (resourceId > 0) defaultValue = c.getString(resourceId);
        Object value = source.get(key);
        if (value == null) value = defaultValue;
        return fixOutputParam(c, key, value.toString());
    }

    public Map<String, String> get(Context c) {
        SharedPreferences pref = c.getSharedPreferences(this.name, Context.MODE_PRIVATE);
        Map<String, ?> source = pref.getAll();
        Map<String, String> target = new TreeMap<>();
        for (String key : this.params) {
            int resourceId = PrefStore.getResourceId(c, key, "string");
            String defaultValue = "";
            if (resourceId > 0) defaultValue = c.getString(resourceId);
            Object value = source.get(key);
            if (value == null) value = defaultValue;
            target.put(key.toUpperCase(), fixOutputParam(c, key, value.toString()));
        }
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            if (!key.matches("^[A-Z0-9_]+$")) continue;
            if (!target.containsKey(key)) target.put(key, entry.getValue().toString());
        }
        return target;
    }

    public String fixInputParam(Context c, String key, String value) {
        return value;
    }

    public void set(Context c, String key, String value) {
        SharedPreferences pref = c.getSharedPreferences(this.name, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();
        if (value.equals("true") || value.equals("false")) {
            prefEditor.putBoolean(key, fixInputParam(c, key, value).equals("true"));
        } else {
            prefEditor.putString(key, fixInputParam(c, key, value));
        }
        prefEditor.apply();
    }

    public void set(Context c, Map<String, String> source) {
        SharedPreferences pref = c.getSharedPreferences(this.name, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();
            if (!key.matches("^[A-Z0-9_]+$")) continue;
            String value = entry.getValue();
            String lowerKey = key.toLowerCase();
            if (params.contains(lowerKey)) {
                if (value.equals("true") || value.equals("false")) {
                    prefEditor.putBoolean(lowerKey, fixInputParam(c, lowerKey, value).equals("true"));
                } else {
                    prefEditor.putString(lowerKey, fixInputParam(c, lowerKey, value));
                }
            } else {
                prefEditor.putString(key, value);
            }
        }
        prefEditor.apply();
    }

    public boolean dump(Context c, File f) {
        return writeConf(get(c), f);
    }

    public boolean restore(Context c, File f) {
        clear(c, false);
        if (f.exists()) {
            set(c, readConf(f));
            return true;
        }
        return false;
    }

    public void clear(Context c, boolean all) {
        SharedPreferences pref = c.getSharedPreferences(this.name, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();
        if (all) prefEditor.clear();
        else {
            for (Map.Entry<String, ?> entry : pref.getAll().entrySet()) {
                String key = entry.getKey();
                if (!key.matches("^[A-Z0-9_]+$")) continue;
                prefEditor.remove(key);
            }
        }
        prefEditor.apply();
    }

}
