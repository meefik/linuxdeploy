package ru.meefik.linuxdeploy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;


public class ShellEnv {
	
	public class DeployCmd implements Runnable {
		
		private Context c;
		private String cmd;
		
	    public DeployCmd(Context c, String cmd) {
	    	this.c = c;
	    	this.cmd = cmd;
	    }
		@Override
		public void run() {
			File f = new File(AppPrefs.HOME_DIR+"/bin/sh");
			if (!f.exists()) new ShellEnv(c).updateEnv();
			new ShellEnv(c).updateConfig();
		
			List<String> params = new ArrayList<String>();
			params.add("su");
			if (AppPrefs.TRACE_MODE.equals("y")) params.add("set -x");
			params.add("PATH="+AppPrefs.HOME_DIR+"/bin:$PATH; export PATH");
			params.add("echo '[PRINT_LN] >>> begin: "+cmd+"'");
			params.add("cd "+AppPrefs.HOME_DIR);
			params.add("linuxdeploy "+cmd);
			params.add("echo '[PRINT_LN] <<< end: "+cmd+"'");
			params.add("exit");

			new ExecCmd(params).run();
		}
	}
	
	private Context c;
	
	public ShellEnv(Context c) {
		this.c = c;
		AppPrefs.get(c);
	}
	
	private void sendLogs(final String msg) {
		MainActivity.handler.post(new Runnable() {
       		@Override
       		public void run() {
       			MainActivity.printLogMsg(msg);
       		}
       	});
	}
	
	public Thread DeployCmd(String cmd) {
		DeployCmd ex = new DeployCmd(c,cmd);
    	Thread thread = new Thread(ex, "DeployCmd_"+cmd);
    	thread.start();
    	return thread;
	}

    private boolean copyFileOrDir(String homeDir, String path) {
        AssetManager assetManager = c.getAssets();
        String assets[] = null;
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                if (!copyFile(homeDir, path)) return false;
            } else {
                String fullPath = homeDir + path.replaceFirst(AppPrefs.ROOT_ASSETS, "");
                //String fullPath = getFilesDir().getAbsolutePath()+File.separator+path;
            	File dir = new File(fullPath);
                if (!dir.exists()){
                    dir.mkdir();
                    //Log.d("linuxdeploy", "mkdir: "+fullPath);
                }

                for (int i = 0; i < assets.length; ++i) {
                    if (!copyFileOrDir(homeDir, path + "/" + assets[i])) return false;
                }
            }
        } catch (IOException e) {
        	e.printStackTrace();
        	return false;
        }
        return true;
    }

    private boolean copyFile(String homeDir, String filename) 
    {
    	boolean result = true;
        AssetManager assetManager = c.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try 
        {
            in = assetManager.open(filename);
            String newFileName = homeDir + filename.replaceFirst(AppPrefs.ROOT_ASSETS, "");
            //Log.d("linuxdeploy", "extract: "+filename+" to "+newFileName);
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
        	result = false;
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
        return result;
    }
    
    public void updateEnv() {
   		sendLogs("[PRINT] Updating environment ... ");
   	    
   		if (AppPrefs.HOME_DIR.length() == 0) {
       		sendLogs("[RESULT_LN] fail");
       		return;
   		}

   		List<String> params = new ArrayList<String>();
   		params.add("su");
   		if (AppPrefs.TRACE_MODE.equals("y")) params.add("set -x");
   		params.add("mkdir "+AppPrefs.HOME_DIR);
   		params.add("rm -R "+AppPrefs.HOME_DIR+"/bin");
   		params.add("rm -R "+AppPrefs.HOME_DIR+"/etc");
   		params.add("rm -R "+AppPrefs.HOME_DIR+"/deploy");
   		params.add("chmod 777 "+AppPrefs.HOME_DIR);
   		params.add("exit");
   		ExecCmd ex = new ExecCmd(params);
   		ex.run();
   		if (!ex.status) {
   			sendLogs("[RESULT_LN] fail");
   			return;
   		}

       	boolean copyResult = copyFileOrDir(AppPrefs.HOME_DIR,AppPrefs.ROOT_ASSETS);
   		if (!copyResult) {
   			sendLogs("[RESULT_LN] fail");
   			return;
   		}

       	params.clear();
       	params.add("su");
       	if (AppPrefs.TRACE_MODE.equals("y")) params.add("set -x");
       	params.add("chmod 755 "+AppPrefs.HOME_DIR);
       	params.add("chmod 755 "+AppPrefs.HOME_DIR+"/bin");
        params.add("chmod 755 "+AppPrefs.HOME_DIR+"/bin/busybox");
        params.add(AppPrefs.HOME_DIR+"/bin/busybox --install "+AppPrefs.HOME_DIR+"/bin");
        params.add("PATH="+AppPrefs.HOME_DIR+"/bin:$PATH; export PATH");
        params.add("chmod -R 755 "+AppPrefs.HOME_DIR+"/bin");
        params.add("chmod -R a+rX "+AppPrefs.HOME_DIR+"/etc "+AppPrefs.HOME_DIR+"/deploy");
        params.add("chmod 755 "+AppPrefs.HOME_DIR+"/deploy/debootstrap/pkgdetails");
        params.add("chmod -R 755 "+AppPrefs.HOME_DIR+"/deploy/openssh");
        params.add("chown -R root:root "+AppPrefs.HOME_DIR+"/bin "+AppPrefs.HOME_DIR+"/etc "+AppPrefs.HOME_DIR+"/deploy");
        params.add("exit");
   		ex = new ExecCmd(params);
   		ex.run();
   		if (!ex.status) {
   			sendLogs("[RESULT_LN] fail");
   			return;
   		}
   		sendLogs("[RESULT_LN] done");
    }

    public void updateConfig() {

    	File f = new File(AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	if (!f.exists()) return;
    	
    	List<String> params = new ArrayList<String>();
    	params.add("su");
    	if (AppPrefs.TRACE_MODE.equals("y")) params.add("set -x");
    	params.add("PATH="+AppPrefs.HOME_DIR+"/bin:$PATH; export PATH");
    	params.add("echo '[PRINT] Updating configuration file ... '");
    	params.add("cd "+AppPrefs.HOME_DIR);
    	params.add("sed -i 's|^HOME_DIR=.*|HOME_DIR=\""+AppPrefs.HOME_DIR+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^MNT_TARGET=.*|MNT_TARGET=\""+AppPrefs.MNT_TARGET+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^IMG_TARGET=.*|IMG_TARGET=\""+AppPrefs.IMG_TARGET+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^IMG_SIZE=.*|IMG_SIZE=\""+AppPrefs.IMG_SIZE+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^ARCH=.*|ARCH=\""+AppPrefs.ARCH+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^SUITE=.*|SUITE=\""+AppPrefs.SUITE+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^MIRROR=.*|MIRROR=\""+AppPrefs.MIRROR+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^USER_NAME=.*|USER_NAME=\""+AppPrefs.USER_NAME+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^SERVER_DNS=.*|SERVER_DNS=\""+AppPrefs.SERVER_DNS+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^INSTALL_GUI=.*|INSTALL_GUI=\""+AppPrefs.INSTALL_GUI+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^DESKTOP_ENV=.*|DESKTOP_ENV=\""+AppPrefs.DESKTOP_ENV+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^LANGUAGE=.*|LANGUAGE=\""+AppPrefs.LANGUAGE+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^TRACE_MODE=.*|TRACE_MODE=\""+AppPrefs.TRACE_MODE+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^CUSTOM_STARTUP=.*|CUSTOM_STARTUP=\""+AppPrefs.CUSTOM_STARTUP+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^CUSTOM_MOUNT=.*|CUSTOM_MOUNT=\""+AppPrefs.CUSTOM_MOUNT+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^SSH_START=.*|SSH_START=\""+AppPrefs.SSH_START+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^SSH_PORT=.*|SSH_PORT=\""+AppPrefs.SSH_PORT+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^VNC_START=.*|VNC_START=\""+AppPrefs.VNC_START+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^VNC_DISPLAY=.*|VNC_DISPLAY=\""+AppPrefs.VNC_DISPLAY+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^VNC_DEPTH=.*|VNC_DEPTH=\""+AppPrefs.VNC_DEPTH+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^VNC_GEOMETRY=.*|VNC_GEOMETRY=\""+AppPrefs.VNC_GEOMETRY+"\"|g' "+AppPrefs.HOME_DIR+"/etc/deploy.conf");
    	params.add("sed -i 's|^HOME_DIR=.*|HOME_DIR=\""+AppPrefs.HOME_DIR+"\"|g' "+AppPrefs.HOME_DIR+"/bin/linuxchroot");
    	params.add("[ $? -eq 0 ] && echo '[RESULT_LN] done' || echo '[RESULT_LN] fail'");
   		if (AppPrefs.DEBUG_MODE) params.add("cat "+AppPrefs.HOME_DIR+"/etc/deploy.conf | grep -v ^#");
    	params.add("exit");
    	
    	new ExecCmd(params).run();
    }
    
    public void sysInfo() {
   		List<String> params = new ArrayList<String>();
   		params.add("sh");
   		if (AppPrefs.TRACE_MODE.equals("y")) params.add("set -x");
   		params.add("PATH="+AppPrefs.HOME_DIR+"/bin:$PATH; export PATH");
   		params.add("MNT_TARGET="+AppPrefs.HOME_DIR+"/mnt");
   		params.add("echo '[PRINT_LN] =================='");
   		params.add("echo '[PRINT_LN] SYSTEM INFORMATION'");
   		params.add("echo '[PRINT_LN] =================='");
   		params.add("echo '[PRINT_LN] Device: '$(getprop ro.product.brand) $(getprop ro.product.device)");
   		params.add("echo '[PRINT_LN] CPU: '$(cat /proc/cpuinfo | grep ^Processor | awk -F': ' '{print $2}')");
   		params.add("echo '[PRINT_LN] Android version: '$(getprop ro.build.version.release)");
   		params.add("echo '[PRINT_LN] '$(cat /proc/meminfo | grep ^MemTotal)");
   		params.add("echo '[PRINT_LN] '$(cat /proc/meminfo | grep ^SwapTotal)");
   		params.add("echo '[PRINT_LN] Storages available:'");
   		params.add("for disk in $EXTERNAL_STORAGE $SECONDARY_STORAGE; do echo \"[PRINT] $disk: \"; " +
   				"stat -f $disk | grep ^Block | tr -d '\n' | awk '{avail=sprintf(\"%.1f\",$10*$3/1024/1024/1024);" +
   				"total=sprintf(\"%.1f\",$6*$3/1024/1024/1024);print \"[RESULT_LN] \"avail\"/\"total\" GB\"}'; done");
   		params.add("echo '[PRINT_LN] SU: '");
   	   	params.add("echo '[PRINT_LN] Version: '$(su -v)");
   	   	params.add("echo '[PRINT_LN] Location: '$(which su)");
   	    params.add("echo '[PRINT_LN] BusyBox: '");
   		params.add("echo '[PRINT_LN] Version: '$(busybox | grep '^BusyBox v' | awk '{print $2}')");
   		params.add("echo '[PRINT_LN] Location: '$(which busybox)");
   		params.add("echo '[PRINT] Support loop device: '");
   		params.add("is_loop=`ls /dev/block/loop0`; [ -n \"$is_loop\" ] && echo '[RESULT_LN] yes' || echo '[RESULT_LN] no'");
   		params.add("echo '[PRINT] Supported file systems: '");
   		params.add("for fs in ext2 ext4; do " +
   				"fs_support=`cat /proc/filesystems | grep $fs`; [ -n \"$fs_support\" ] && echo \"[RESULT] $fs \"; done");
   		params.add("echo '[RESULT_LN] '");
   		params.add("echo '[PRINT_LN] Mounted parts: '");
   		params.add("for i in `cat /proc/mounts | grep $MNT_TARGET | awk '{print $2}' | sed \"s|$MNT_TARGET/*|/|g\"`; "+
   				"do echo \"[PRINT_LN] * $i\"; is_mounted=1; done");
   		params.add("[ -z \"$is_mounted\" ] && echo '[PRINT_LN] ...not mounted anything'");
   		params.add("echo '[PRINT_LN] Running services: '");
   		params.add("echo '[PRINT] SSH server: '");
   		params.add("is_ssh=`ps | grep '/usr/sbin/sshd' | grep -v grep`");
   		params.add("[ -n \"$is_ssh\" ] && echo '[RESULT_LN] yes' || echo '[RESULT_LN] no'");
   		params.add("echo '[PRINT] VNC server: '");
   		params.add("is_ssh=`ps | grep 'Xtightvnc' | grep -v grep`");
   		params.add("[ -n \"$is_ssh\" ] && echo '[RESULT_LN] yes' || echo '[RESULT_LN] no'");
   		params.add("exit");
   		new ExecCmd(params).run();

    }
    
}
