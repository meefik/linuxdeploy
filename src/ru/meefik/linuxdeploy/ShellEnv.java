package ru.meefik.linuxdeploy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;

public class ShellEnv {

	private Context c;
	private boolean rooted;

	public ShellEnv(Context c) {
		this.c = c;
		PrefStore.get(c);
		this.rooted = isRooted();
	}

	private boolean exec(List<String> params) {
		boolean result = true;
		try {
			Process process = Runtime.getRuntime().exec("su");

			final OutputStream stdin = process.getOutputStream();
			final InputStream stdout = process.getInputStream();
			final InputStream stderr = process.getErrorStream();

			params.add("exit $?");
			params.add(0, "PATH=" + PrefStore.ENV_DIR
					+ "/bin:$PATH; export PATH");
			if (PrefStore.TRACE_MODE.equals("y"))
				params.add(0, "set -x");

			DataOutputStream os = new DataOutputStream(stdin);
			for (String cmd : params) {
				os.writeBytes(cmd + "\n");
			}
			os.flush();
			os.close();

			(new Thread() {
				@Override
				public void run() {
					sendLogs(stdout);
				}
			}).start();

			if (PrefStore.DEBUG_MODE.equals("y")) {
				(new Thread() {
					@Override
					public void run() {
						sendLogs(stderr);
					}
				}).start();
			}

			process.waitFor();

			if (process.exitValue() != 0)
				result = false;

			stdout.close();
			stderr.close();
			stdin.close();
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
		}
		return result;
	}

	private void sendLogs(InputStream stdstream) {
		if (MainActivity.handler != null) {
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stdstream));
				int n;
				char[] buffer = new char[1024];
				while ((n = reader.read(buffer)) != -1) {
					final String logLine = String.valueOf(buffer, 0, n);
					MainActivity.handler.post(new Runnable() {
						@Override
						public void run() {
							MainActivity.printLogMsg(logLine);
						}
					});
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendLogs(final String msg) {
		if (MainActivity.handler != null) {
			MainActivity.handler.post(new Runnable() {
				@Override
				public void run() {
					MainActivity.printLogMsg(msg);
				}
			});
		}
	}

	private boolean isRooted() {
		// exec linuxdeploy command
		List<String> params = new ArrayList<String>();
		params.add("ls /data/local 1>/dev/null");
		if (!exec(params)) {
			sendLogs("Require superuser privileges (root)!\n");
			return false;
		} else {
			return true;
		}
	}

	private boolean extractFile(String rootAsset, String path) {
		boolean result = true;
		AssetManager assetManager = c.getAssets();
		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open(rootAsset + path);
			String fullPath = PrefStore.ENV_DIR + path;
			out = new FileOutputStream(fullPath);
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
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
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	private boolean extractDir(String rootAsset, String path) {
		AssetManager assetManager = c.getAssets();
		String assets[] = null;
		try {
			assets = assetManager.list(rootAsset + path);
			if (assets.length == 0) {
				if (!extractFile(rootAsset, path))
					return false;
			} else {
				String fullPath = PrefStore.ENV_DIR + path;
				File dir = new File(fullPath);
				if (!dir.exists()) {
					dir.mkdir();
				}

				for (int i = 0; i < assets.length; ++i) {
					if (!extractDir(rootAsset, path + "/" + assets[i]))
						return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void updateEnv() {
		if (!rooted)
			return;

		sendLogs("Updating environment ... ");

		if (PrefStore.ENV_DIR.length() == 0) {
			sendLogs("fail\n");
			return;
		}

		List<String> params = new ArrayList<String>();
		if (!PrefStore.DEBUG_MODE.equals("y")) {
			params.add("exec 1>/dev/null");
			params.add("exec 2>/dev/null");
		}
		params.add("mkdir " + PrefStore.ENV_DIR);
		params.add("rm -R " + PrefStore.ENV_DIR + "/bin");
		params.add("rm -R " + PrefStore.ENV_DIR + "/etc");
		params.add("rm -R " + PrefStore.ENV_DIR + "/deploy");
		params.add("chmod 777 " + PrefStore.ENV_DIR);
		if (!exec(params)) {
			sendLogs("fail\n");
			return;
		}

		if (!extractDir(PrefStore.ROOT_ASSETS, "")) {
			sendLogs("fail\n");
			return;
		}
		if (!extractDir(PrefStore.MARCH + "/all", "")) {
			sendLogs("fail\n");
			return;
		}
		// PIE for Android L
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			if (!extractDir(PrefStore.MARCH + "/pie", "")) {
				sendLogs("fail\n");
				return;
			}
		} else {
			if (!extractDir(PrefStore.MARCH + "/nopie", "")) {
				sendLogs("fail\n");
				return;
			}
		}

		params.clear();
		if (!PrefStore.DEBUG_MODE.equals("y")) {
			params.add("exec 1>/dev/null");
			params.add("exec 2>/dev/null");
		}
		if (PrefStore.BUILTIN_SHELL) {
			params.add("chmod 755 " + PrefStore.ENV_DIR + "/bin/busybox");
			params.add(PrefStore.ENV_DIR + "/bin/busybox --install -s "
					+ PrefStore.ENV_DIR + "/bin");
		}
		if (PrefStore.SYMLINK) {
			params.add("rm -f /system/bin/linuxdeploy");
			params.add("ln -s "
					+ PrefStore.ENV_DIR
					+ "/bin/linuxdeploy /system/bin/linuxdeploy || "
					+ "{ mount -o rw,remount /system; rm -f /system/bin/linuxdeploy; ln -s "
					+ PrefStore.ENV_DIR
					+ "/bin/linuxdeploy /system/bin/linuxdeploy; mount -o ro,remount /system; }");
		}
		params.add("echo '" + PrefStore.VERSION + "' > " + PrefStore.ENV_DIR
				+ "/etc/version");
		params.add("chmod 755 " + PrefStore.ENV_DIR);
		String[] dirs = { PrefStore.ENV_DIR + "/bin",
				PrefStore.ENV_DIR + "/etc", PrefStore.ENV_DIR + "/deploy" };
		for (int i = 0; i < dirs.length; i++) {
			params.add("chmod -R 755 " + dirs[i]);
			params.add("find " + dirs[i]
					+ " | while read f; do chmod 755 $f; done");
		}
		if (!exec(params)) {
			sendLogs("fail\n");
			return;
		}
		sendLogs("done\n");
	}

	public void updateConfig() {
		if (!rooted)
			return;

		File f = new File(PrefStore.ENV_DIR + "/etc/deploy.conf");
		if (!f.exists())
			return;

		sendLogs("Updating configuration file ... ");

		List<String> params = new ArrayList<String>();
		if (!PrefStore.DEBUG_MODE.equals("y")) {
			params.add("exec 1>/dev/null");
			params.add("exec 2>/dev/null");
		}
		params.add("cd " + PrefStore.ENV_DIR);
		params.add("sed -i 's|^ENV_DIR=.*|ENV_DIR=\"" + PrefStore.ENV_DIR
				+ "\"|g' " + PrefStore.ENV_DIR + "/bin/linuxdeploy");
		params.add("sed -i 's|^#!.*|#!" + PrefStore.SHELL + "|g' "
				+ PrefStore.ENV_DIR + "/bin/linuxdeploy");
		params.add("sed -i 's|^#!.*|#!" + PrefStore.SHELL + " -e|g' "
				+ PrefStore.ENV_DIR + "/bin/debootstrap");
		params.add("sed -i 's|^DEBUG_MODE=.*|DEBUG_MODE=\""
				+ PrefStore.DEBUG_MODE + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^TRACE_MODE=.*|TRACE_MODE=\""
				+ PrefStore.TRACE_MODE + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^IMG_TARGET=.*|IMG_TARGET=\""
				+ PrefStore.IMG_TARGET + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^IMG_SIZE=.*|IMG_SIZE=\"" + PrefStore.IMG_SIZE
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^FS_TYPE=.*|FS_TYPE=\"" + PrefStore.FS_TYPE
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^DEPLOY_TYPE=.*|DEPLOY_TYPE=\""
				+ PrefStore.DEPLOY_TYPE + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^DISTRIB=.*|DISTRIB=\"" + PrefStore.DISTRIB
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^ARCH=.*|ARCH=\"" + PrefStore.ARCH + "\"|g' "
				+ PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^SUITE=.*|SUITE=\"" + PrefStore.SUITE + "\"|g' "
				+ PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^MIRROR=.*|MIRROR=\"" + PrefStore.MIRROR
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^USER_NAME=.*|USER_NAME=\"" + PrefStore.USER_NAME
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^SERVER_DNS=.*|SERVER_DNS=\""
				+ PrefStore.SERVER_DNS + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^LOCALE=.*|LOCALE=\"" + PrefStore.LOCALE
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^DESKTOP_ENV=.*|DESKTOP_ENV=\""
				+ PrefStore.DESKTOP_ENV + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^COMPONENTS=.*|COMPONENTS=\""
				+ PrefStore.COMPONENTS + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^STARTUP=.*|STARTUP=\"" + PrefStore.STARTUP
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^CUSTOM_SCRIPTS=.*|CUSTOM_SCRIPTS=\""
				+ PrefStore.CUSTOM_SCRIPTS + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^CUSTOM_MOUNTS=.*|CUSTOM_MOUNTS=\""
				+ PrefStore.CUSTOM_MOUNTS + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^SSH_PORT=.*|SSH_PORT=\"" + PrefStore.SSH_PORT
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^VNC_DISPLAY=.*|VNC_DISPLAY=\""
				+ PrefStore.VNC_DISPLAY + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^VNC_DEPTH=.*|VNC_DEPTH=\"" + PrefStore.VNC_DEPTH
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^VNC_DPI=.*|VNC_DPI=\"" + PrefStore.VNC_DPI
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^VNC_GEOMETRY=.*|VNC_GEOMETRY=\""
				+ PrefStore.VNC_GEOMETRY + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^VNC_ARGS=.*|VNC_ARGS=\"" + PrefStore.VNC_ARGS
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^XSERVER_DISPLAY=.*|XSERVER_DISPLAY=\""
				+ PrefStore.XSERVER_DISPLAY + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^XSERVER_HOST=.*|XSERVER_HOST=\""
				+ PrefStore.XSERVER_HOST + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^FB_DISPLAY=.*|FB_DISPLAY=\""
				+ PrefStore.FB_DISPLAY + "\"|g' " + PrefStore.ENV_DIR
				+ "/etc/deploy.conf");
		params.add("sed -i 's|^FB_DPI=.*|FB_DPI=\"" + PrefStore.FB_DPI
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^FB_DEV=.*|FB_DEV=\"" + PrefStore.FB_DEV
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^FB_INPUT=.*|FB_INPUT=\"" + PrefStore.FB_INPUT
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^FB_ARGS=.*|FB_ARGS=\"" + PrefStore.FB_ARGS
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");
		params.add("sed -i 's|^FB_FREEZE=.*|FB_FREEZE=\"" + PrefStore.FB_FREEZE
				+ "\"|g' " + PrefStore.ENV_DIR + "/etc/deploy.conf");

		if (!exec(params)) {
			sendLogs("fail\n");
			return;
		}
		sendLogs("done\n");
	}

	public void execScript(String arg) {
		if (!rooted)
			return;

		// check for update env
		boolean update = true;
		File f = new File(PrefStore.ENV_DIR + "/etc/version");
		if (f.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				try {
					String line = br.readLine();
					if (PrefStore.VERSION.equals(line))
						update = false;
				} finally {
					br.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (update) {
			updateEnv();
			updateConfig();
			// sendLogs("Need to update the operating environment!\nTry Menu -> Settings -> Update ENV\n");
			// return;
		}
		// exec linuxdeploy command
		List<String> params = new ArrayList<String>();
		params.add(PrefStore.ENV_DIR + "/bin/linuxdeploy " + arg);
		exec(params);
	}

}
