package ru.meefik.linuxdeploy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED:
                EnvUtils.execService(context, "start", "-m");
                break;
            case Intent.ACTION_SHUTDOWN:
                EnvUtils.execService(context, "stop", "-u");
                break;
        }
    }

}