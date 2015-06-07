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
    	String command = intent.getStringExtra("command");
    	ShellEnv env = new ShellEnv(mContext);
    	env.execScript(command);
    	MainActivity.notification(mContext, null);
    	return super.onStartCommand(intent, flags, startId);
    }

}
