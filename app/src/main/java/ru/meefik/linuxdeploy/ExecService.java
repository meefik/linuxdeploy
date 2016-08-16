package ru.meefik.linuxdeploy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class ExecService extends Service {

    Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getBaseContext();
        PrefStore.showNotification(mContext, null);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String cmd = intent.getStringExtra("cmd");
            final String args = intent.getStringExtra("args");
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    switch (cmd) {
                        case "telnetd":
                            EnvUtils.telnetd(mContext, args);
                            break;
                        case "httpd":
                            EnvUtils.httpd(mContext, args);
                            break;
                        default:
                            EnvUtils.cli(mContext, cmd, args);
                    }
                }
            });
            thread.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
