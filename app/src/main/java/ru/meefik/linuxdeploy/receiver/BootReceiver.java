package ru.meefik.linuxdeploy.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.meefik.linuxdeploy.EnvUtils;
import ru.meefik.linuxdeploy.PrefStore;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                try { // Autostart delay
                    Integer delay_s = PrefStore.getAutostartDelay(context);
                    Thread.sleep(delay_s * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                EnvUtils.execServices(context, new String[]{"telnetd", "httpd"}, "start");
                EnvUtils.execService(context, "start", "-m");
                break;
            case Intent.ACTION_SHUTDOWN:
                EnvUtils.execService(context, "stop", "-u");
                EnvUtils.execServices(context, new String[]{"telnetd", "httpd"}, "stop");
                try { // Shutdown delay
                    Integer delay_s = PrefStore.getAutostartDelay(context);
                    Thread.sleep(delay_s * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}