package ru.meefik.linuxdeploy;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class ExecService extends JobIntentService {

    public static final int JOB_ID = 1;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, ExecService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String cmd = intent.getStringExtra("cmd");
        final String args = intent.getStringExtra("args");
        Thread thread = new Thread(() -> {
            switch (cmd) {
                case "telnetd":
                    EnvUtils.telnetd(getBaseContext(), args);
                    break;
                case "httpd":
                    EnvUtils.httpd(getBaseContext(), args);
                    break;
                default:
                    PrefStore.showNotification(getBaseContext(), null);
                    EnvUtils.cli(getApplicationContext(), cmd, args);
            }
        });
        thread.start();
    }
}
