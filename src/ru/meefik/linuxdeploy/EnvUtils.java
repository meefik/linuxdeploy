package ru.meefik.linuxdeploy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;

public class EnvUtils {

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
		boolean result = false;
		try {
			Process process = Runtime.getRuntime().exec("su");
			final OutputStream stdin = process.getOutputStream();
			final InputStream stdout = process.getInputStream();

			DataOutputStream os = new DataOutputStream(stdin);
			os.writeBytes("ls /data\n");
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stdout));
			int n = 0;
			while (reader.readLine() != null) {
				n++;
			}
			reader.close();

			if (n > 0) {
				result = true;
			}
			stdout.close();
			stdin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	// closeable helper
	private static void close(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// update version file
	public static Boolean setVersion() {
		Boolean result = false;
		String f = PrefStore.ENV_DIR + "/etc/version";
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(f));
			bw.write(PrefStore.VERSION);
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close(bw);
		}
		return result;
	}

	// check latest version
	public static Boolean isLatestVersion() {
		Boolean result = false;
		String f = PrefStore.ENV_DIR + "/etc/version";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
			String line = br.readLine();
			if (PrefStore.VERSION.equals(line))
				result = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close(br);
		}
		return result;
	}
	
	// update deploy.conf
	public static Boolean storeConfig() {
		Boolean result = false;
		String confFile = PrefStore.ENV_DIR + "/etc/deploy.conf";
		List<String> lines = new ArrayList<>();
		lines.add("DEBUG_MODE=\"" + PrefStore.DEBUG_MODE + "\"");
		lines.add("TRACE_MODE=\"" + PrefStore.TRACE_MODE + "\"");
		lines.add("MNT_TARGET=\"" + PrefStore.MNT_TARGET + "\"");
		lines.add("IMG_TARGET=\"" + PrefStore.IMG_TARGET + "\"");
		lines.add("IMG_SIZE=\"" + PrefStore.IMG_SIZE + "\"");
		lines.add("FS_TYPE=\"" + PrefStore.FS_TYPE + "\"");
		lines.add("DEPLOY_TYPE=\"" + PrefStore.DEPLOY_TYPE + "\"");
		lines.add("DISTRIB=\"" + PrefStore.DISTRIB + "\"");
		lines.add("ARCH=\"" + PrefStore.ARCH + "\"");
		lines.add("SUITE=\"" + PrefStore.SUITE + "\"");
		lines.add("MIRROR=\"" + PrefStore.MIRROR + "\"");
		lines.add("USER_NAME=\"" + PrefStore.USER_NAME + "\"");
		lines.add("USER_PASSWORD=\"" + PrefStore.USER_PASSWORD + "\"");
		lines.add("SERVER_DNS=\"" + PrefStore.SERVER_DNS + "\"");
		lines.add("LOCALE=\"" + PrefStore.LOCALE + "\"");
		lines.add("DESKTOP_ENV=\"" + PrefStore.DESKTOP_ENV + "\"");
		lines.add("USE_COMPONENTS=\"" + PrefStore.USE_COMPONENTS + "\"");
		lines.add("STARTUP=\"" + PrefStore.STARTUP + "\"");
		lines.add("CUSTOM_SCRIPTS=\"" + PrefStore.CUSTOM_SCRIPTS + "\"");
		lines.add("CUSTOM_MOUNTS=\"" + PrefStore.CUSTOM_MOUNTS + "\"");
		lines.add("SSH_PORT=\"" + PrefStore.SSH_PORT + "\"");
		lines.add("VNC_DISPLAY=\"" + PrefStore.VNC_DISPLAY + "\"");
		lines.add("VNC_DEPTH=\"" + PrefStore.VNC_DEPTH + "\"");
		lines.add("VNC_DPI=\"" + PrefStore.VNC_DPI + "\"");
		lines.add("VNC_GEOMETRY=\"" + PrefStore.VNC_GEOMETRY + "\"");
		lines.add("VNC_ARGS=\"" + PrefStore.VNC_ARGS + "\"");
		lines.add("XSERVER_DISPLAY=\"" + PrefStore.XSERVER_DISPLAY + "\"");
		lines.add("XSERVER_HOST=\"" + PrefStore.XSERVER_HOST + "\"");
		lines.add("FB_DISPLAY=\"" + PrefStore.FB_DISPLAY + "\"");
		lines.add("FB_DPI=\"" + PrefStore.FB_DPI + "\"");
		lines.add("FB_DEV=\"" + PrefStore.FB_DEV + "\"");
		lines.add("FB_INPUT=\"" + PrefStore.FB_INPUT + "\"");
		lines.add("FB_ARGS=\"" + PrefStore.FB_ARGS + "\"");
		lines.add("FB_FREEZE=\"" + PrefStore.FB_FREEZE + "\"");
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(confFile));
			for (String s : lines) {
				bw.write(s + "\n");
			}
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(bw);
		}
		return result;
	}

	public static boolean exec(List<String> params) {
		boolean result = true;
		try {
			Process process = Runtime.getRuntime().exec("su");

			final OutputStream stdin = process.getOutputStream();
			final InputStream stdout = process.getInputStream();
			final InputStream stderr = process.getErrorStream();

			params.add("exit $?");
			params.add(0, "export PATH=" + PrefStore.ENV_DIR + "/bin:$PATH");
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
					Logger.log(stdout);
				}
			}).start();

			if (PrefStore.DEBUG_MODE.equals("y")) {
				(new Thread() {
					@Override
					public void run() {
						Logger.log(stderr);
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

	public static boolean updateEnv(Context c) {
		if (!isRooted()) {
			Logger.log("Require superuser privileges (root).\n");
			return false;
		}

		Logger.log("Updating environment ... ");

		if (PrefStore.ENV_DIR.length() == 0) {
			Logger.log("fail\n");
			return false;
		}
		
		// env directories
		String[] dirs = { PrefStore.ENV_DIR + "/bin",
				PrefStore.ENV_DIR + "/etc", PrefStore.ENV_DIR + "/share" };

		// clean env directory
		File envDir = new File(PrefStore.ENV_DIR);
		envDir.mkdirs();
		for (String dir : dirs) {
			cleanDirectory(new File(dir));
		}

		// extract assets
		if (!extractDir(c, PrefStore.ROOT_ASSETS, "")) {
			Logger.log("fail\n");
			return false;
		}
		if (!extractDir(c, PrefStore.MARCH + "/all", "")) {
			Logger.log("fail\n");
			return false;
		}
		// PIE for Android L
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			if (!extractDir(c, PrefStore.MARCH + "/pie", "")) {
				Logger.log("fail\n");
				return false;
			}
		} else {
			if (!extractDir(c, PrefStore.MARCH + "/nopie", "")) {
				Logger.log("fail\n");
				return false;
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
		if (!PrefStore.BUILTIN_SHELL) {
			params.add("rm " + PrefStore.ENV_DIR + "/bin/ash");
			params.add("rm " + PrefStore.ENV_DIR + "/bin/chroot");
		}
		// set shell
		params.add("sed -i 's|^#!.*|#!" + PrefStore.SHELL + "|g' "
				+ PrefStore.ENV_DIR + "/bin/linuxdeploy");
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
			Logger.log("fail\n");
			return false;
		}

		// update version
		if (!setVersion()) {
			Logger.log("fail\n");
			return false;
		}

		Logger.log("done\n");
		return true;
	}

	public static boolean removeEnv(Context c) {
		Logger.log("Removing environment ... ");

		// exec shell commands
		List<String> params = new ArrayList<String>();
		params.add(PrefStore.ENV_DIR + "/bin/linuxdeploy umount 1>/dev/null");
		params.add("[ $? -ne 0 ] && exit 1");
		params.add("if [ -e /system/bin/linuxdeploy ]; then "
				+ "rm -f /system/bin/linuxdeploy || "
				+ "{ mount -o rw,remount /system; rm -f /system/bin/linuxdeploy; mount -o ro,remount /system; };"
				+ "fi");
		params.add("rm -rf " + PrefStore.ENV_DIR);

		if (exec(params)) {
			Logger.log("done\n");
			return true;
		} else {
			Logger.log("fail\n");
			return false;
		}
	}

	public static boolean updateConf() {
		if (!EnvUtils.isLatestVersion()) return false;
		Logger.log("Updating configuration file ... ");
		// update config file
		if (storeConfig()) {
			Logger.log("done\n");
			return true;
		} else {
			Logger.log("fail\n");
			return false;
		}
	}

}
