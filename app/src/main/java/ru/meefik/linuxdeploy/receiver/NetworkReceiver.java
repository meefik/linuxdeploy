package ru.meefik.linuxdeploy.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import ru.meefik.linuxdeploy.EnvUtils;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = false;
            if (activeNetwork != null) isConnected = activeNetwork.isConnected();
            if (isConnected) {
                EnvUtils.execService(context, "start", "core/net");
            } else {
                EnvUtils.execService(context, "stop", "core/net");
            }
        }
    }
}