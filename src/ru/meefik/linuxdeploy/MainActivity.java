package ru.meefik.linuxdeploy;

//import android.support.v4.*;

import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private static TextView logView;
	private static ScrollView logScroll;
	private static Boolean fullLogFlag = false;
	static Handler handler;

	public static void printLogMsg(String msg) {
		if (msg.length() <= 0)
			return;
		String printMsg = "";
		String currentTimeString = "["
				+ new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] ";
		if (PrefStore.DEBUG_MODE.equals("y")) {
			printMsg = currentTimeString + msg + "\n";
		} else {
			if (fullLogFlag) {
				printMsg = currentTimeString + msg + "\n";
			}
			if (msg.matches("^\\[RESULT\\].*$")) {
				printMsg = msg.replaceFirst("\\[RESULT\\] ", "");
			}
			if (msg.matches("^\\[RESULT_LN\\].*$")) {
				printMsg = msg.replaceFirst("\\[RESULT_LN\\] ", "") + "\n";
			}
			if (msg.matches("^\\[RESULT_ALL\\].*$")) {
				//printMsg = msg.replaceFirst("\\[RESULT_ALL\\] ", "") + "\n";
				fullLogFlag = false;
			}
			if (msg.matches("^\\[PRINT\\].*$")) {
				printMsg = currentTimeString
						+ msg.replaceFirst("\\[PRINT\\] ", "");
			}
			if (msg.matches("^\\[PRINT_LN\\].*$")) {
				printMsg = currentTimeString
						+ msg.replaceFirst("\\[PRINT_LN\\] ", "") + "\n";
			}
			if (msg.matches("^\\[PRINT_ALL\\].*$")) {
				printMsg = currentTimeString
						+ msg.replaceFirst("\\[PRINT_ALL\\] ", "") + "\n";
				fullLogFlag = true;
			}
		}
		if (printMsg.length() > 0) {
			logView.append(printMsg);
			logView.scrollTo(0, logView.getBottom());
			logScroll.post(new Runnable() {
				@Override
				public void run() {
					logScroll.fullScroll(View.FOCUS_DOWN);
				}
			});
			if (PrefStore.LOGGING) {
				saveLogs(printMsg);
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

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isLinkLocalAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.installBtn: {
			new AlertDialog.Builder(this)
					.setTitle(R.string.confirm_install_title)
					.setMessage(R.string.confirm_install_message)
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
													.deployCmd("install");
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
		}

		case R.id.startBtn: {
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
		}

		case R.id.stopBtn: {
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
		}

		case R.id.propertiesBtn: {
			Intent intent_properties = new Intent(this,
					DeployPrefsActivity.class);
			startActivity(intent_properties);
			break;
		}

		default:
			break;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);
		setContentView(R.layout.activity_main);

		logView = (TextView) findViewById(R.id.LogView);
		logScroll = (ScrollView) findViewById(R.id.LogScrollView);
		handler = new Handler();

		ImageButton installButton = (ImageButton) findViewById(R.id.installBtn);
		installButton.setOnClickListener(this);
		if (PrefStore.THEME.equals("light"))
			installButton.setImageResource(R.raw.installbtn_dark);
		if (PrefStore.THEME.equals("dark"))
			installButton.setImageResource(R.raw.installbtn_light);

		ImageButton startButton = (ImageButton) findViewById(R.id.startBtn);
		startButton.setOnClickListener(this);
		if (PrefStore.THEME.equals("light"))
			startButton.setImageResource(R.raw.startbtn_dark);
		if (PrefStore.THEME.equals("dark"))
			startButton.setImageResource(R.raw.startbtn_light);

		ImageButton stopButton = (ImageButton) findViewById(R.id.stopBtn);
		stopButton.setOnClickListener(this);
		if (PrefStore.THEME.equals("light"))
			stopButton.setImageResource(R.raw.stopbtn_dark);
		if (PrefStore.THEME.equals("dark"))
			stopButton.setImageResource(R.raw.stopbtn_light);

		ImageButton uninstallButton = (ImageButton) findViewById(R.id.propertiesBtn);
		uninstallButton.setOnClickListener(this);
		if (PrefStore.THEME.equals("light"))
			uninstallButton.setImageResource(R.raw.propertiesbtn_dark);
		if (PrefStore.THEME.equals("dark"))
			uninstallButton.setImageResource(R.raw.propertiesbtn_light);

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
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_profiles:
			Intent intent_profiles = new Intent(this, ProfilesActivity.class);
			startActivity(intent_profiles);
			break;
		case R.id.menu_settings:
			Intent intent_settings = new Intent(this, AppPrefsActivity.class);
			startActivity(intent_settings);
			break;
		case R.id.menu_about:
			Intent intent_about = new Intent(this, AboutActivity.class);
			startActivity(intent_about);
			break;
		case R.id.menu_status:
			new Thread(new Runnable() {
				@Override
				public void run() {
					new ShellEnv(getApplicationContext()).sysInfo();
				}
			}).start();
			break;
		case R.id.menu_clear:
			logView.setText("");
			break;
		case R.id.menu_exit:
			finish();
			break;
		default:

			break;
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();

		PrefStore.get(getApplicationContext());

		String titleMsg = PrefStore.getCurrentProfile(getApplicationContext());
		String myIP = getLocalIpAddress();
		String ssh = "";
		String vnc = "";

		int ot = getResources().getConfiguration().orientation;
		if (ot == Configuration.ORIENTATION_LANDSCAPE) {
			if (myIP == null)
				myIP = "127.0.0.1";
			myIP = "IP: " + myIP;
			if (PrefStore.SSH_START == "y")
				ssh = "  SSH: " + PrefStore.SSH_PORT;
			if (PrefStore.VNC_START == "y")
				vnc = "  VNC: "
						+ String.valueOf(5900 + (int) Double
								.parseDouble(PrefStore.VNC_DISPLAY));
			titleMsg += "  [ " + myIP + ssh + vnc + " ]";
		}

		this.setTitle(titleMsg);

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
			new Thread(new Runnable() {
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

}
