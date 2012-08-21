package ru.meefik.linuxdeploy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

	final static String ROOT_ASSETS = "home";
	static String HOME_DIR = "/data/local/linux";
	
	private static TextView logView;
	private static ScrollView logScroll;
	private static Boolean logFlag = false;
	static Handler handler;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        logView = (TextView)findViewById(R.id.LogView);
        logScroll = (ScrollView)findViewById(R.id.LogScrollView);
        handler = new Handler();
        
        //prepareEnv();
        
/*
        // Screen keep on
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK
            | PowerManager.ON_AFTER_RELEASE,
            "Linux Deploy");
		wl.acquire();
		// ...
		//wl.release();
		
		// WiFi keep on
		Settings.System.putInt(getContentResolver(),
				  Settings.System.WIFI_SLEEP_POLICY, 
				  Settings.System.WIFI_SLEEP_POLICY_NEVER);
*/

        final ImageButton button1 = (ImageButton)findViewById(R.id.InstallBtn);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	deployCMD("install");
            }
        });
        
        final ImageButton button2 = (ImageButton)findViewById(R.id.StartBtn);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	deployCMD("start");
            }
        });
        
        final ImageButton button3 = (ImageButton)findViewById(R.id.StopBtn);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	deployCMD("stop");
            }
        });
        
        final ImageButton button4 = (ImageButton)findViewById(R.id.ResetBtn);
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	//deployCMD("uninstall");
            	SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            	 
            	String strUserName = SP.getString("default_edittext", "NA");
            	boolean bAppUpdates = SP.getBoolean("default_checkbox",false);
            	//String downloadType = SP.getString("downloadType","1");
            	
            	logView.append(strUserName);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
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
    	    default:

    	    break;
    	}
    	return false;
    	}
    
    public static void addLogMsg(String msg) {
    	if (logFlag) {
    		logFlag = false;
    	} else {
    		String currentTimeString = new SimpleDateFormat("HH:mm:ss").format(new Date());
    		msg = "[" + currentTimeString + "] " + msg;
    	}
    	if (msg.matches("^.* \\.\\.\\. $")) {
    		logFlag = true;
    	} else {
       		msg = msg + "\n";
    	}
      	logView.append(msg);
      	logScroll.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private void copyFileOrDir(String path) {
        AssetManager assetManager = this.getAssets();
        String assets[] = null;
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(this, path);
            	//System.out.println("copy file: "+path);
            } else {
                String fullPath = HOME_DIR + "/" + path.replaceFirst(ROOT_ASSETS, ""); //path for storing internally to data/data
                //String fullPath = getFilesDir().getAbsolutePath()+File.separator+path;
            	File dir = new File(fullPath);
                if (!dir.exists()){
                    dir.mkdir();
                    //System.out.println("Created directory: "+fullPath);
                }

                for (int i = 0; i < assets.length; ++i) {
                    copyFileOrDir(path + "/" + assets[i]);
                }
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

    public static void copyFile(Activity c, String filename) 
    {
        AssetManager assetManager = c.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try 
        {
            in = assetManager.open(filename);
            String newFileName = HOME_DIR + "/" + filename.replaceFirst(ROOT_ASSETS, "");
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) 
            {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (IOException e) {
        	e.printStackTrace();
        }finally{
            if(in!=null){
                try {
                    in.close();
                } catch (IOException e) {
                	e.printStackTrace();
                }
            }
            if(out!=null){
                try {
                    out.close();
                } catch (IOException e) {
                	e.printStackTrace();
                }
            }
        }
    }
    
    public Thread execCMD(List<String> params) {
    	ExecCmd ex = new ExecCmd(params);
    	Thread thread = new Thread(ex, "ExecCmd");
    	thread.start();
    	return thread;
    }
    
    public void prepareEnv() {
    	logView.setText("");
    	//File dir = new File(HOME_DIR);
        //if (dir.exists()) return;
        (new Thread() {
        	public void run() {
            	handler.post(new Runnable() {
            		@Override
            		public void run() {
            			addLogMsg("Preparing environment ... ");
            		}
            	});
            	
        		List<String> params = new ArrayList<String>();
        		params.add("su");
       			params.add("rm -rf "+HOME_DIR+"/bin "+HOME_DIR+"/etc "+HOME_DIR+"/share 2>/dev/null");
        		params.add("[ ! -d '"+HOME_DIR+"' ] && mkdir "+HOME_DIR);
        		params.add("chmod 777 "+HOME_DIR);
        		params.add("exit");
				new ExecCmd(params).run();
          
               	copyFileOrDir(ROOT_ASSETS);

               	params.clear();
                params.add("su");
                params.add("chmod 755 "+HOME_DIR);
                params.add("chmod -R 755 "+HOME_DIR+"/bin");
                params.add(HOME_DIR+"/bin/busybox --install "+HOME_DIR+"/bin");
                params.add("PATH="+HOME_DIR+"/bin:$PATH; export PATH");
                params.add("chmod 755 "+HOME_DIR+"/share/debootstrap/pkgdetails");
                params.add("chmod -R a+rX "+HOME_DIR+"/etc "+HOME_DIR+"/share");
                params.add("chown -R root:root "+HOME_DIR+"/bin "+HOME_DIR+"/etc "+HOME_DIR+"/share");
                params.add("exit");
                new ExecCmd(params).run();

            	handler.post(new Runnable() {
            		@Override
            		public void run() {
            			addLogMsg("DONE");
            		}
            	});
        	}
        }).start();
    }
    
    public void deployCMD(String cmd) {
    	List<String> params = new ArrayList<String>();
    	params.add("su");
    	params.add("echo '>>> begin: "+cmd+"'");
    	params.add("PATH="+HOME_DIR+"/bin:$PATH; export PATH");
    	params.add("cd "+HOME_DIR);
    	params.add("linuxdeploy "+cmd);
    	params.add("echo '<<< end: "+cmd+"'");
    	params.add("exit");
    	execCMD(params);    	
    }

}
