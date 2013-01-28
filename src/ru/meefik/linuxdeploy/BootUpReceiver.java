package ru.meefik.linuxdeploy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootUpReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) {
		SharedPreferences sp = context.getSharedPreferences(
				PrefStore.APP_PREF_FILE_NAME, Context.MODE_PRIVATE);
		boolean autoStart = sp.getBoolean("autostart",
				context.getString(R.string.autostart) == "true" ? true : false);
		if (autoStart) {
			(new Thread() {
				@Override
				public void run() {
					new ShellEnv(context).deployCmd("start");
				}
			}).start();
		}
	}

}
