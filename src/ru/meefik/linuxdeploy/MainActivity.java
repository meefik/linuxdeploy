package ru.meefik.linuxdeploy;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
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
	private static boolean newLine = false;
	static Handler handler;

	private static String getTimeStamp() {
		return "["
				+ new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
						.format(new Date()) + "] ";
	}

	public static void printLogMsg(String msg) {
		if (msg.length() > 0) {
			if (logView.length() == 0) {
				msg = getTimeStamp() + msg;
				newLine = false;
			}
			// add '\n' character
			if (newLine) {
				msg = "\n" + msg;
				newLine = false;
			}
			// remove all last '\n' characters
			while (msg.length() > 0 && msg.charAt(msg.length() - 1) == '\n') {
				msg = msg.substring(0, msg.length() - 1);
				newLine = true;
			}
			msg = msg.replaceAll("\\n", "\n" + getTimeStamp());
			logView.append(msg);
			// logView.scrollTo(logView.getLeft(), logView.getBottom());
			logScroll.post(new Runnable() {
				@Override
				public void run() {
					logScroll.fullScroll(View.FOCUS_DOWN);
					logScroll.clearFocus();
				}
			});
			if (PrefStore.LOGGING) {
				saveLogs(msg);
			}
		}
	}

	public static void saveLogs(String msg) {
		byte[] data = msg.getBytes();
		try {
			FileOutputStream fos = new FileOutputStream(PrefStore.LOG_FILE,
					true);
			fos.write(data);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		handler = new Handler();

		// ok we back, load the saved text
		if (savedInstanceState != null) {
			String savedText = savedInstanceState.getString("textlog");
			logView.setText(savedText);
			logScroll.post(new Runnable() {
				@Override
				public void run() {
					logScroll.fullScroll(View.FOCUS_DOWN);
				}
			});
		}
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
									(new Thread() {
										@Override
										public void run() {
											new ShellEnv(
													getApplicationContext())
													.deployCmd("start");
										}
									}).start();
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
									(new Thread() {
										@Override
										public void run() {
											new ShellEnv(
													getApplicationContext())
													.deployCmd("stop");
										}
									}).start();
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
			(new Thread() {
				@Override
				public void run() {
					new ShellEnv(getApplicationContext()).deployCmd("status");
				}
			}).start();
			break;
		case R.id.menu_properties:
			Intent intent_properties = new Intent(this,
					DeployPrefsActivity.class);
			startActivity(intent_properties);
			break;
		case R.id.menu_settings:
			Intent intent_settings = new Intent(this, AppPrefsActivity.class);
			startActivity(intent_settings);
			break;
		case R.id.menu_about:
			Intent intent_about = new Intent(this, AboutActivity.class);
			startActivity(intent_about);
			break;
		case R.id.menu_clear:
			logView.setText("");
			break;
		case R.id.menu_exit:
			finish();
			break;
		case android.R.id.home:
			Intent intent_profiles = new Intent(this, ProfilesActivity.class);
			startActivity(intent_profiles);
			break;
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();

		PrefStore.get(getApplicationContext());

		String profileName = PrefStore.getCurrentProfile(getApplicationContext());
		String ipaddress = PrefStore.getLocalIpAddress();
		/*
		String ports = "";
		if (PrefStore.SSH_START != null && PrefStore.SSH_START.equals("y")) {
			ports = ", SSH: " + PrefStore.SSH_PORT;
		}
		if (PrefStore.VNC_START != null && PrefStore.VNC_START.equals("y")) {
			try {
				ports += ", VNC: " + String.valueOf(Double.valueOf(PrefStore.VNC_DISPLAY).intValue()+5900);
			} catch (NumberFormatException ex) {
				// ignore
			}
		}
		*/
		this.setTitle(profileName+"  [ "+ipaddress+" ]");
		
		// show icon
		notification(getApplicationContext(), this.getIntent());
		
		// Restore text
		logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PrefStore.FONT_SIZE);

		// Screen lock
		if (PrefStore.SCREEN_LOCK)
			this.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			this.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// update configuration file
		if (PrefStore.PREF_CHANGE) {
			(new Thread() {
				@Override
				public void run() {
					new ShellEnv(getApplicationContext()).updateConfig();
				}
			}).start();
			PrefStore.PREF_CHANGE = false;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// now, save the text if something overlaps this Activity
		savedInstanceState.putString("textlog", logView.getText().toString());
	}
	
	public static void notification(Context context, Intent intent) {
		final int NOTIFY_ID = 1;
		NotificationManager mNotificationManager =
			    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (PrefStore.isShowIcon(context)) {
			PrefStore.updateLocale(context);
			NotificationCompat.Builder mBuilder =
			        new NotificationCompat.Builder(context)
			        .setSmallIcon(R.drawable.ic_launcher)
			        .setContentTitle(context.getString(R.string.app_name))
			        .setContentText(context.getString(R.string.notification_current_profile)+": "+PrefStore.getCurrentProfile(context));
			Intent resultIntent = intent;
			if (resultIntent == null)
				resultIntent = new Intent(context, MainActivity.class);
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent =
			        stackBuilder.getPendingIntent(
			            0,
			            PendingIntent.FLAG_UPDATE_CURRENT
			        );
			mBuilder.setContentIntent(resultPendingIntent);
			mBuilder.setOngoing(true);
			mBuilder.setWhen(0);
			mNotificationManager.notify(NOTIFY_ID, mBuilder.build());
		} else {
			mNotificationManager.cancel(NOTIFY_ID);
		}
    }

}
