package ru.meefik.linuxdeploy;

import android.content.Context;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

class PropertiesStore extends ParamUtils {

    public static final String name = "properties_conf";
    private static final String[] params = {"method", "distrib", "arch", "suite", "source_path",
            "target_type", "target_path", "disk_size", "fs_type", "user_name", "user_password",
            "privileged_users", "dns", "locale", "init", "init_path", "init_level", "init_user",
            "init_async", "ssh_port", "ssh_args", "pulse_host", "pulse_port", "graphics",
            "vnc_display", "vnc_depth", "vnc_dpi", "vnc_width", "vnc_height", "vnc_args",
            "x11_display", "x11_host", "x11_sdl", "x11_sdl_delay", "fb_display", "fb_dev",
            "fb_input", "fb_args", "fb_refresh", "fb_freeze", "desktop", "mounts", "include"};

    PropertiesStore() {
        super(name, params);
    }

    @Override
    public String fixOutputParam(Context c, String key, String value) {
        switch (key) {
            case "user_password":
                if (value.isEmpty()) value = PrefStore.generatePassword();
                break;
            case "vnc_width":
                if (value.isEmpty())
                    value = String.valueOf(Math.max(PrefStore.getScreenWidth(c), PrefStore.getScreenHeight(c)));
                break;
            case "vnc_height":
                if (value.isEmpty())
                    value = String.valueOf(Math.min(PrefStore.getScreenWidth(c), PrefStore.getScreenHeight(c)));
                break;
            case "mounts":
                if (!get(c, "is_mounts").equals("true")) value = "";
                break;
            case "include":
                Set<String> includes = new TreeSet<>();
                includes.add("bootstrap");
                if (get(c, "is_init").equals("true")) {
                    includes.add("init");
                } else {
                    includes.remove("init");
                }
                if (get(c, "is_ssh").equals("true")) {
                    includes.add("extra/ssh");
                } else {
                    includes.remove("extra/ssh");
                }
                if (get(c, "is_pulse").equals("true")) {
                    includes.add("extra/pulse");
                } else {
                    includes.remove("extra/pulse");
                }
                if (get(c, "is_gui").equals("true")) {
                    includes.add("graphics");
                    includes.add("desktop");
                } else {
                    includes.remove("graphics");
                    includes.remove("desktop");
                }
                value = TextUtils.join(" ", includes);
                break;
        }
        return value;
    }

    @Override
    public String fixInputParam(Context c, String key, String value) {
        if (value != null) {
            switch (key) {
                case "mounts":
                    if (!value.isEmpty()) set(c, "is_mounts", "true");
                    break;
                case "include":
                    List includes = Arrays.asList(value.split(" "));
                    if (includes.contains("init")) set(c, "is_init", "true");
                    if (includes.contains("extra/ssh")) set(c, "is_ssh", "true");
                    if (includes.contains("extra/pulse")) set(c, "is_pulse", "true");
                    if (includes.contains("graphics")) set(c, "is_gui", "true");
                    break;
            }
        }
        return value;
    }
}
