package ru.meefik.linuxdeploy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

public class ExecService extends JobIntentService {

    public static final int JOB_ID = 1;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, ExecService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (intent != null) {
            final String cmd = intent.getStringExtra("cmd");
            final String args = intent.getStringExtra("args");
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    switch (cmd) {
                        case "telnetd":
                            EnvUtils.telnetd(getBaseContext(), args);
                            break;
                        case "httpd":
                            EnvUtils.httpd(getBaseContext(), args);
                            break;
                        default:
                            PrefStore.showNotification(getBaseContext(), null);
                            EnvUtils.cli(getBaseContext(), cmd, args);
                    }
                }
            });
            thread.start();
        }
    }

}
