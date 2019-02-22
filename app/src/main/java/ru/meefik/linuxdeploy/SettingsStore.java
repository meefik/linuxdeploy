package ru.meefik.linuxdeploy;

import android.content.Context;

class SettingsStore extends ParamUtils {

    public static final String name = "settings_conf";
    private static final String[] params = {"chroot_dir", "profile"};

    SettingsStore() {
        super(name, params);
    }

    @Override
    public String fixOutputParam(Context c, String key, String value) {
        return value;
    }

    @Override
    public String fixInputParam(Context c, String key, String value) {
        return value;
    }
}
