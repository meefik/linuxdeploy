package ru.meefik.linuxdeploy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PowerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        EnvUtils.execService(context, "start", "core/power");
    }
}
