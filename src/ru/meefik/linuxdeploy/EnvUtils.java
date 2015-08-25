package ru.meefik.linuxdeploy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;

public class EnvUtils {

	private static void sendLogs(InputStream stdstream) {
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

	private static void sendLogs(final String msg) {
		if (MainActivity.handler != null) {
			MainActivity.handler.post(new Runnable() {
				@Override
				public void run() {
					MainActivity.printLogMsg(msg);
				}
			});
		}
	}

	private static boolean extractFile(Context c, String rootAsset, String path) {
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

	private static boolean extractDir(Context c, String rootAsset, String path) {
		AssetManager assetManager = c.getAssets();
		String assets[] = null;
		try {
			assets = assetManager.list(rootAsset + path);
			if (assets.length == 0) {
				if (!extractFile(c, rootAsset, path))
					return false;
			} else {
				String fullPath = PrefStore.ENV_DIR + path;
				File dir = new File(fullPath);
				if (!dir.exists()) {
					dir.mkdir();
				}

				for (int i = 0; i < assets.length; ++i) {
					if (!extractDir(c, rootAsset, path + "/" + assets[i]))
						return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static void cleanDirectory(File path) {
		if (path == null)
			return;
		if (path.exists()) {
			for (File f : path.listFiles()) {
				if (f.isDirectory())
					cleanDirectory(f);
				f.delete();
			}
		}
	}

	private static void setPermissions(File path) {
		if (path == null)
			return;
		if (path.exists()) {
			for (File f : path.listFiles()) {
				if (f.isDirectory())
					setPermissions(f);
				f.setReadable(true);
				f.setExecutable(true, false);
			}
		}
	}

	private static boolean isRooted() {
		// exec shell command
		List<String> params = new ArrayList<String>();
		params.add("ls /data/local 1>/dev/null");
		if (!exec(params)) {
			sendLogs("Require superuser privileges (root)!\n");
			return false;
		} else {
			return true;
		}
	}

	public static boolean exec(List<String> params) {
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

	public static void updateEnv(Context c) {
		if (!isRooted()) {
			return;
		}

		sendLogs("Updating environment ... ");

		if (PrefStore.ENV_DIR.length() == 0) {
			sendLogs("fail\n");
			return;
		}

		// env directories
		String[] dirs = { PrefStore.ENV_DIR + "/bin",
				PrefStore.ENV_DIR + "/etc", PrefStore.ENV_DIR + "/deploy" };

		// clean env directory
		File f = new File(PrefStore.ENV_DIR);
		f.mkdirs();
		for (String dir : dirs) {
			cleanDirectory(new File(dir));
		}

		// extract assets
		if (!extractDir(c, PrefStore.ROOT_ASSETS, "")) {
			sendLogs("fail\n");
			return;
		}
		if (!extractDir(c, PrefStore.MARCH + "/all", "")) {
			sendLogs("fail\n");
			return;
		}
		// PIE for Android L
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			if (!extractDir(c, PrefStore.MARCH + "/pie", "")) {
				sendLogs("fail\n");
				return;
			}
		} else {
			if (!extractDir(c, PrefStore.MARCH + "/nopie", "")) {
				sendLogs("fail\n");
				return;
			}
		}

		// set permissions
		for (String dir : dirs) {
			setPermissions(new File(dir));
		}

		// exec shell commands
		List<String> params = new ArrayList<String>();
		// switch debug mode
		if (!PrefStore.DEBUG_MODE.equals("y")) {
			params.add("exec 1>/dev/null");
			params.add("exec 2>/dev/null");
		}
		// fallback permissions
		for (String dir : dirs) {
			params.add("chmod -R 755 " + dir);
			params.add("find " + dir + " | while read f; do chmod 755 $f; done");
		}
		// install BusyBox
		params.add(PrefStore.ENV_DIR + "/bin/busybox --install -s "
				+ PrefStore.ENV_DIR + "/bin");
		// set Shell
		if (PrefStore.BUILTIN_SHELL) {
			params.add("sed -i 's|^#!.*|#!" + PrefStore.ENV_DIR
					+ "/bin/ash|g' " + PrefStore.ENV_DIR + "/bin/linuxdeploy");
		} else {
			params.add("rm " + PrefStore.ENV_DIR + "/bin/ash");
			params.add("sed -i 's|^#!.*|#!/system/xbin/ash|g' "
					+ PrefStore.ENV_DIR + "/bin/linuxdeploy");
		}
		// set ENV_DIR
		params.add("sed -i 's|^ENV_DIR=.*|ENV_DIR=\"" + PrefStore.ENV_DIR
				+ "\"|g' " + PrefStore.ENV_DIR + "/bin/linuxdeploy");
		// update symlink
		if (PrefStore.SYMLINK) {
			params.add("rm -f /system/bin/linuxdeploy");
			params.add("ln -s "
					+ PrefStore.ENV_DIR
					+ "/bin/linuxdeploy /system/bin/linuxdeploy || "
					+ "{ mount -o rw,remount /system; rm -f /system/bin/linuxdeploy; ln -s "
					+ PrefStore.ENV_DIR
					+ "/bin/linuxdeploy /system/bin/linuxdeploy; mount -o ro,remount /system; }");
		}
		if (!exec(params)) {
			sendLogs("fail\n");
			return;
		}

		// update version
		if (!PrefStore.setVersion()) {
			sendLogs("fail\n");
			return;
		}

		sendLogs("done\n");
	}

	public static void removeEnv(Context c) {
		sendLogs("Removing environment ... ");

		// exec shell commands
		List<String> params = new ArrayList<String>();
		params.add(PrefStore.ENV_DIR + "/bin/linuxdeploy umount 1>/dev/null");
		params.add("[ $? -ne 0 ] && exit 1");
		params.add("if [ -e /system/bin/linuxdeploy ]; then "
				+ "rm -f /system/bin/linuxdeploy || "
				+ "{ mount -o rw,remount /system; rm -f /system/bin/linuxdeploy; mount -o ro,remount /system; };"
				+ "fi");
		params.add("rm -rf " + PrefStore.ENV_DIR + "/mnt " + PrefStore.ENV_DIR
				+ "/deploy " + PrefStore.ENV_DIR + "/etc " + PrefStore.ENV_DIR
				+ "/bin");
		params.add("rmdir " + PrefStore.ENV_DIR);

		if (exec(params)) {
			sendLogs("done\n");
		} else {
			sendLogs("fail\n");
		}
	}

	public static void updateConf() {
		sendLogs("Updating configuration file ... ");
		// update config file
		if (PrefStore.storeConfig()) {
			sendLogs("done\n");
		} else {
			sendLogs("fail\n");
		}
	}

}
