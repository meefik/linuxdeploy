package ru.meefik.linuxdeploy.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import ru.meefik.linuxdeploy.EnvUtils;
import ru.meefik.linuxdeploy.R;
import ru.meefik.linuxdeploy.activity.MainActivity;

import static ru.meefik.linuxdeploy.App.SERVICE_CHANNEL_ID;

public class ActionReceiver extends BroadcastReceiver {

    final static int NOTIFY_ID = 2;
    static long attemptTime = 0;
    static long attemptNumber = 1;

    private void showNotification(Context c, int icon, String text) {
        NotificationManager mNotificationManager = (NotificationManager) c
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c, SERVICE_CHANNEL_ID)
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
        // am broadcast -a ru.meefik.linuxdeploy.BROADCAST_ACTION --user 0 --esn "start"
        if (intent.hasExtra("start")) {
            System.out.println("START");
            EnvUtils.execService(context, "start", "-m");
            return;
        }
        // am broadcast -a ru.meefik.linuxdeploy.BROADCAST_ACTION --user 0 --esn "stop"
        if (intent.hasExtra("stop")) {
            System.out.println("STOP");
            EnvUtils.execService(context, "stop", "-u");
            return;
        }
        // am broadcast -a ru.meefik.linuxdeploy.BROADCAST_ACTION --user 0 --esn "show"
        if (intent.hasExtra("show")) {
            if (attemptTime > System.currentTimeMillis() - 5000) {
                attemptNumber++;
            } else {
                attemptNumber = 1;
            }
            attemptTime = System.currentTimeMillis();
            if (attemptNumber >= 5) {
                attemptNumber = 1;
                Intent mainIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(mainIntent);
            }
        }
    }
}