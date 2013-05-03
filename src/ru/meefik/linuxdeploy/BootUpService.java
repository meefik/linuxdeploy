package ru.meefik.linuxdeploy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class BootUpService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	final Context mContext = this;
		(new Thread() {
			@Override
			public void run() {
				new ShellEnv(mContext).deployCmd("start");
			}
		}).start();
    	MainActivity.notification(mContext, null);
    	return super.onStartCommand(intent, flags, startId);
    }

}
