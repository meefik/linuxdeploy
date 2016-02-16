package ru.meefik.linuxdeploy;

import android.content.Context;

public class ExecScript extends Thread {

    private Context c;
    private String arg;

    public ExecScript(Context c, String arg) {
        this.c = c;
        this.arg = arg;
        Logger.setLogFile(c);
    }

    @Override
    public void run() {
        switch (arg) {
        case "update":
            if (!EnvUtils.updateEnv(c)) break;
            EnvUtils.updateConf(c);
            if (PrefStore.isUseCli(c)) {
                EnvUtils.makeSymlink(c);
            }
            break;
        case "remove":
            EnvUtils.removeEnv(c);
            break;
        default:
            if (!EnvUtils.isLatestVersion(c)) {
                if (!EnvUtils.updateEnv(c)) break;
            }
            EnvUtils.updateConf(c);
            EnvUtils.linuxdeploy(c, arg);
        }
    }
}
