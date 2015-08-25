package ru.meefik.linuxdeploy;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class ExecScript extends Thread {

	private Context c;
	private String arg;

	public ExecScript(Context c, String arg) {
		this.c = c;
		this.arg = arg;
		PrefStore.get(c);
	}

	public void run() {
		switch (arg) {
		case "update":
			// update env
			EnvUtils.updateEnv(c);
			// update config file
			EnvUtils.updateConf();
			break;
		case "remove":
			// remove env
			EnvUtils.removeEnv(c);
			break;
		default:
			// update env when version is changed
			if (!PrefStore.isLatestVersion()) {
				EnvUtils.updateEnv(c);
			}
			// update config file
			EnvUtils.updateConf();
			// exec linuxdeploy command
			List<String> params = new ArrayList<String>();
			params.add(PrefStore.ENV_DIR + "/bin/linuxdeploy " + arg);
			EnvUtils.exec(params);
		}
	}
}
