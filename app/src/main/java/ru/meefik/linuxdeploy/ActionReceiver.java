package ru.meefik.linuxdeploy;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class ActionReceiver extends BroadcastReceiver {

    final static int NOTIFY_ID = 2;

    private void showNotification(Context c, int icon, String text) {
        NotificationManager mNotificationManager = (NotificationManager) c
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c)
                .setSmallIcon(icon)
                .setContentTitle(c.getString(R.string.app_name))
                .setContentText(text);
        mBuilder.setWhen(0);
        mNotificationManager.notify(NOTIFY_ID, mBuilder.build());
    }

    private void hideNotification(Context c) {
        NotificationManager mNotificationManager = (NotificationManager) c
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFY_ID);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        // am broadcast -a ru.meefik.linuxdeploy.BROADCAST_ACTION --user 0 --esn "hide"
        if (intent.hasExtra("hide")) {
            hideNotification(context);
            return;
        }
        // am broadcast -a ru.meefik.linuxdeploy.BROADCAST_ACTION --user 0 --es "info" "Hello World!"
        if (intent.hasExtra("info")) {
            showNotification(context, android.R.drawable.ic_dialog_info, intent.getStringExtra("info"));
            return;
        }
        // am broadcast -a ru.meefik.linuxdeploy.BROADCAST_ACTION --user 0 --es "alert" "Hello World!"
        if (intent.hasExtra("alert")) {
            showNotification(context, android.R.drawable.ic_dialog_alert, intent.getStringExtra("alert"));
            return;
        }
    }

}