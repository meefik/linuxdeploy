package ru.meefik.linuxdeploy;

import android.content.Context;

public class UpdateEnv extends Thread {

	private Context c;

	public UpdateEnv(Context c) {
		this.c = c;
		PrefStore.get(c);
	}

	public void run() {
		// update env and config file
		EnvUtils.updateEnv(c);
		// update config file
		EnvUtils.updateConf();
	}
}
