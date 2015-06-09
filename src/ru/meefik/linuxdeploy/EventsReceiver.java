package ru.meefik.linuxdeploy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EventsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) {
		Intent service = new Intent(context, ExecService.class);
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			service.putExtra("command", "start");
			context.startService(service);
			return;
		}
		if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
			service.putExtra("command", "stop");
			context.startService(service);
			return;
		}
	}

}