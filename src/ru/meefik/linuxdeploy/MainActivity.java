package ru.meefik.linuxdeploy;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private static TextView logView;
	private static ScrollView logScroll;
	private static Boolean logFlag = false;
	static Handler handler;
	private PowerManager.WakeLock wl;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AppPrefs.updateLocale(getBaseContext());
        
        setContentView(R.layout.activity_main);
        
        logView = (TextView)findViewById(R.id.LogView);
        logScroll = (ScrollView)findViewById(R.id.LogScrollView);
        handler = new Handler();
        
        ImageButton installButton = (ImageButton)findViewById(R.id.InstallBtn);
        installButton.setOnClickListener(this);

        ImageButton startButton = (ImageButton)findViewById(R.id.StartBtn);
        startButton.setOnClickListener(this);

        ImageButton stopButton = (ImageButton)findViewById(R.id.StopBtn);
        stopButton.setOnClickListener(this);
        
        ImageButton uninstallButton = (ImageButton)findViewById(R.id.UninstallBtn);
        uninstallButton.setOnClickListener(this);
        
        // Screen lock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "linuxdeploy");
        
		// ok we back, load the saved text
        if ( savedInstanceState != null ) {
            String savedText = savedInstanceState.getString( "LOG" );
            logView.setText( savedText );
        }

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	AppPrefs.updateLocale(getBaseContext());
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public void onSaveInstanceState( Bundle savedInstanceState ) {
    	// now, save the text if something overlaps this Activity
        savedInstanceState.putString( "LOG", logView.getText().toString() );
    }

	@Override
	public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.InstallBtn:
            {
    			new AlertDialog.Builder(this)
    			.setTitle(R.string.confirm_install_title)
    			.setMessage(R.string.confirm_install_message)
    			.setIcon(android.R.drawable.ic_dialog_alert)
    			.setCancelable(false)
    			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    	            	new ShellEnv(getBaseContext()).DeployCmd("install");
    			    }
    			})
    			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
    			    public void onClick(DialogInterface dialog, int id) {
    			    	dialog.cancel();
    			    }
    			}).show();
                break;
            }

            case R.id.StartBtn:
            {
    			new AlertDialog.Builder(this)
    			.setTitle(R.string.confirm_start_title)
    			.setMessage(R.string.confirm_start_message)
    			.setIcon(android.R.drawable.ic_dialog_alert)
    			.setCancelable(false)
    			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    	            	new ShellEnv(getBaseContext()).DeployCmd("start");
    			    }
    			})
    			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
    			    public void onClick(DialogInterface dialog, int id) {
    			    	dialog.cancel();
    			    }
    			}).show();
            	break;
            }

            case R.id.StopBtn:
            {
    			new AlertDialog.Builder(this)
    			.setTitle(R.string.confirm_stop_title)
    			.setMessage(R.string.confirm_stop_message)
    			.setIcon(android.R.drawable.ic_dialog_alert)
    			.setCancelable(false)
    			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    	            	new ShellEnv(getBaseContext()).DeployCmd("stop");
    			    }
    			})
    			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
    			    public void onClick(DialogInterface dialog, int id) {
    			    	dialog.cancel();
    			    }
    			}).show();
                break;
            }
            
            case R.id.UninstallBtn:
            {
    			new AlertDialog.Builder(this)
    			.setTitle(R.string.confirm_uninstall_title)
    			.setMessage(R.string.confirm_uninstall_message)
    			.setIcon(android.R.drawable.ic_dialog_alert)
    			.setCancelable(false)
    			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    	            	new ShellEnv(getBaseContext()).DeployCmd("uninstall");
    			    }
    			})
    			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
    			    public void onClick(DialogInterface dialog, int id) {
    			    	dialog.cancel();
    			    }
    			}).show();
                break;
            }
            
            default:
                break;
        }
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) 
    	{
    		case R.id.menu_settings:
    	        Intent intent_pref = new Intent(this,PreferencesActivity.class);
    			startActivity(intent_pref);
    	    break;
    		case R.id.menu_about:
    	        Intent intent_about = new Intent(this,AboutActivity.class);
    			startActivity(intent_about);
    	    break;
    		case R.id.menu_status:
    			new Thread(new Runnable(){
    				public void run() {
    					new ShellEnv(getBaseContext()).sysInfo();
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
    protected void onPause() {
        super.onPause();
        // Screen unlock
        if (wl.isHeld()) wl.release();
        // WiFi unlock
        Settings.System.putInt(getContentResolver(),
        		Settings.System.WIFI_SLEEP_POLICY, 
        		Settings.System.WIFI_SLEEP_POLICY_DEFAULT);
    }

	@Override
	public void onResume() {
		super.onResume();
		
		AppPrefs.get(getBaseContext());
		
		String myIP = getLocalIpAddress();
		String titleMsg = " IP:  "+myIP;
		
		int ot = getResources().getConfiguration().orientation;
		if (ot == Configuration.ORIENTATION_LANDSCAPE) { 
			String ssh = "";
			String vnc = "";
			if (AppPrefs.SSH_START == "y") ssh = "     SSH:  " + AppPrefs.SSH_PORT;
			if (AppPrefs.VNC_START == "y") vnc = "     VNC:  " + String.valueOf(5900+Integer.parseInt(AppPrefs.VNC_DISPLAY));
			titleMsg = " IP:  "+myIP +ssh+vnc;
		}
		
		if (myIP != null) this.setTitle(titleMsg);
		else this.setTitle(R.string.app_name);
		
		// Restore text
		logView.setTextSize(TypedValue.COMPLEX_UNIT_SP,AppPrefs.FONT_SIZE);
    	
		// Screen lock
		if (AppPrefs.SCREEN_LOCK) wl.acquire();

		// WiFi lock
		if (AppPrefs.WIFI_LOCK) {
			Settings.System.putInt(getContentResolver(),
					Settings.System.WIFI_SLEEP_POLICY, 
					Settings.System.WIFI_SLEEP_POLICY_NEVER);
		}
	}
	
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()&&!inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("linuxdeploy", e.toString());
        }
        return null;
    }
   
    public static void printLogMsg(String msg) {
    	String printMsg = "";
    	Log.d("linuxdeploy", msg);
    	if (msg.matches("^\\[PRINT_ALL\\].*$")) {
       		msg = msg.replaceFirst("\\[PRINT_ALL\\] ", "") ;
    		logFlag = true;
    	}
    	if (msg.matches("^\\[PRINT_LN\\].*$")) {
    		String currentTimeString = new SimpleDateFormat("HH:mm:ss").format(new Date());
       		printMsg = "[" + currentTimeString + "] " + msg.replaceFirst("\\[PRINT_LN\\] ", "") + "\n";
       		logFlag = false;
    	}
    	if (msg.matches("^\\[PRINT_NOTIME\\].*$")) {
       		printMsg = msg.replaceFirst("\\[PRINT_NOTIME\\] ", "") + "\n";
       		logFlag = false;
    	}
    	if (msg.matches("^\\[PRINT_WAIT\\].*$")) {
    		String currentTimeString = new SimpleDateFormat("HH:mm:ss").format(new Date());
       		printMsg = "[" + currentTimeString + "] " + msg.replaceFirst("\\[PRINT_WAIT\\] ", "");
       		logFlag = false;
    	}
    	if (msg.matches("\\[RESULT_DONE\\]")) {
       		printMsg = "DONE\n";
       		logFlag = false;
    	}
    	if (msg.matches("\\[RESULT_SKIP\\]")) {
       		printMsg = "SKIP\n";
       		logFlag = false;
    	}
    	if (msg.matches("\\[RESULT_FAIL\\]")) {
       		printMsg = "FAIL\n";
       		logFlag = false;
    	}
    	if (msg.matches("\\[RESULT_YES\\]")) {
       		printMsg = "YES\n";
       		logFlag = false;
    	}
    	if (msg.matches("\\[RESULT_NO\\]")) {
       		printMsg = "NO\n";
       		logFlag = false;
    	}
    	if (logFlag) {
    		String currentTimeString = new SimpleDateFormat("HH:mm:ss").format(new Date());
       		printMsg = "[" + currentTimeString + "] " + msg + "\n";
    	}
    	if (printMsg.length() > 0) {
        	logView.append(printMsg);
        	logScroll.fullScroll(ScrollView.FOCUS_DOWN);
    	}
    }

}
