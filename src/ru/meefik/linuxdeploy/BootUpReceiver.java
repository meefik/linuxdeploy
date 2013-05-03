package ru.meefik.linuxdeploy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) {
	    Intent service = new Intent(context, BootUpService.class);
	    context.startService(service);
	}

}
