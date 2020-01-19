package ru.meefik.linuxdeploy.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

import ru.meefik.linuxdeploy.EnvUtils;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            boolean isConnected;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(activeNetwork);

                isConnected = networkCapabilities != null
                        && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }

            if (isConnected) {
                EnvUtils.execService(context, "start", "core/net");
            } else {
                EnvUtils.execService(context, "stop", "core/net");
            }
        }
    }
}