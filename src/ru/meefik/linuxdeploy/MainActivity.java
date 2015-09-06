package ru.meefik.linuxdeploy;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockActivity {

	private static TextView logView;
	private static ScrollView logScroll;
	private static WifiLock wifiLock;
	final static int NOTIFY_ID = 1;

	private static void clearLog() {
		Logger.clear();
		logView.setText("");
	}

	public static void showLog() {
		if (logView == null || logScroll == null) return;
		// show log in TextView
		logView.post(new Runnable() {
			@Override
			public void run() {
				logView.setText(Logger.get());
				// scroll TextView to bottom
				logScroll.post(new Runnable() {
					@Override
					public void run() {
						logScroll.fullScroll(View.FOCUS_DOWN);
						logScroll.clearFocus();
					}
				});
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);
		setContentView(R.layout.activity_main);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		logView = (TextView) findViewById(R.id.LogView);
		logScroll = (ScrollView) findViewById(R.id.LogScrollView);
		
		// WiFi lock init
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,
				"linuxdeploy");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		PrefStore.updateLocale(getApplicationContext());
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);

		boolean isLight = PrefStore.THEME.equals("light");

		menu.findItem(R.id.menu_properties).setIcon(
				isLight ? R.drawable.ic_action_properties_light
						: R.drawable.ic_action_properties_dark);

		int ot = getResources().getConfiguration().orientation;
		if (ot == Configuration.ORIENTATION_LANDSCAPE) {
			menu.findItem(R.id.menu_start).setIcon(
					isLight ? R.drawable.ic_action_start_light
							: R.drawable.ic_action_start_dark);
			menu.findItem(R.id.menu_stop).setIcon(
					isLight ? R.drawable.ic_action_stop_light
							: R.drawable.ic_action_stop_dark);
		}

		super.onCreateOptionsMenu(menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			new AlertDialog.Builder(this)
					.setTitle(R.string.confirm_start_title)
					.setMessage(R.string.confirm_start_message)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									new ExecScript(getApplicationContext(),
											"start").start();
									if (PrefStore.STARTUP
											.contains("framebuffer")) {
										Intent intent_fb = new Intent(
												getApplicationContext(),
												FullscreenActivity.class);
										startActivity(intent_fb);
									}
								}
							})
					.setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).show();
			break;
		case R.id.menu_stop:
			new AlertDialog.Builder(this)
					.setTitle(R.string.confirm_stop_title)
					.setMessage(R.string.confirm_stop_message)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									new ExecScript(getApplicationContext(),
											"stop").start();
								}
							})
					.setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							}).show();
			break;
		case R.id.menu_status:
			new ExecScript(getApplicationContext(), "status").start();
			break;
		case R.id.menu_properties:
			Intent intent_properties = new Intent(this,
					PropertiesActivity.class);
			startActivity(intent_properties);
			break;
		case R.id.menu_settings:
			Intent intent_settings = new Intent(this, SettingsActivity.class);
			startActivity(intent_settings);
			break;
		case R.id.menu_about:
			Intent intent_about = new Intent(this, AboutActivity.class);
			startActivity(intent_about);
			break;
		case R.id.menu_clear:
			clearLog();
			break;
		case R.id.menu_exit:
			notification(getApplicationContext());
			if (wifiLock.isHeld())
				wifiLock.release();
			finish();
			break;
		case android.R.id.home:
			Intent intent_profiles = new Intent(this, ProfilesActivity.class);
			startActivity(intent_profiles);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();

		PrefStore.get(getApplicationContext());

		String profileName = PrefStore
				.getCurrentProfile(getApplicationContext());
		String ipaddress = PrefStore.getLocalIpAddress();
		this.setTitle(profileName + "  [ " + ipaddress + " ]");

		// show icon
		notification(getApplicationContext(), this.getIntent());

		// Restore font size
		logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PrefStore.FONT_SIZE);

		// Restore log
		showLog();

		// Screen lock
		if (PrefStore.SCREEN_LOCK)
			this.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			this.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// WiFi lock
		if (PrefStore.WIFI_LOCK)
			wifiLock.acquire();
		else if (wifiLock.isHeld())
			wifiLock.release();

		// update configuration file
		if (PrefStore.CONF_CHANGE) {
			PrefStore.CONF_CHANGE = false;
			// update config file
			EnvUtils.updateConf();
		}
	}

	public static void notification(Context context, Intent intent) {
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (PrefStore.isShowIcon(context)) {
			PrefStore.updateLocale(context);
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
					context)
					.setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle(context.getString(R.string.app_name))
					.setContentText(
							context.getString(R.string.notification_current_profile)
									+ ": "
									+ PrefStore.getCurrentProfile(context));
			Intent resultIntent = intent;
			if (resultIntent == null)
				resultIntent = new Intent(context, MainActivity.class);
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
					0, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
			mBuilder.setOngoing(true);
			mBuilder.setWhen(0);
			mNotificationManager.notify(NOTIFY_ID, mBuilder.build());
		} else {
			mNotificationManager.cancel(NOTIFY_ID);
		}
	}

	public static void notification(Context context) {
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFY_ID);
	}

}
