package ru.meefik.linuxdeploy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class ExecService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Context mContext = this;
		MainActivity.notification(mContext, null);
		String command = intent.getStringExtra("command");
		new ExecScript(getApplicationContext(), command).start();
		return super.onStartCommand(intent, flags, startId);
	}

}
