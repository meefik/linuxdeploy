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

    /**
     * Closeable helper
     * 
     * @param c closable object
     */
    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Extract file to env directory
     * 
     * @param c context
     * @param rootAsset root asset name
     * @param path path to asset file
     * @return false if error
     */
    private static boolean extractFile(Context c, String rootAsset, String path) {
        AssetManager assetManager = c.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(rootAsset + path);
            String fullPath = PrefStore.getEnvDir(c) + path;
            out = new FileOutputStream(fullPath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            close(in);
            close(out);
        }
        return true;
    }

    /**
     * Extract path to env directory
     * 
     * @param c context
     * @param rootAsset root asset name
     * @param path path to asset directory
     * @return false if error
     */
    private static boolean extractDir(Context c, String rootAsset, String path) {
        AssetManager assetManager = c.getAssets();
        try {
            String[] assets = assetManager.list(rootAsset + path);
            if (assets.length == 0) {
                if (!extractFile(c, rootAsset, path)) return false;
            } else {
                String fullPath = PrefStore.getEnvDir(c) + path;
                File dir = new File(fullPath);
                if (!dir.exists()) dir.mkdir();
                for (String asset : assets) {
                    if (!extractDir(c, rootAsset, path + "/" + asset))
                        return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Recursive remove all from directory
     * 
     * @param path path to directory
     */
    private static void cleanDirectory(File path) {
        if (path == null) return;
        if (path.exists()) {
            for (File f : path.listFiles()) {
                if (f.isDirectory()) cleanDirectory(f);
                f.delete();
            }
        }
    }

    /**
     * Recursive set permissions to directory
     * 
     * @param path path to directory
     */
    private static void setPermissions(File path) {
        if (path == null) return;
        if (path.exists()) {
            for (File f : path.listFiles()) {
                if (f.isDirectory()) setPermissions(f);
                f.setReadable(true, false);
                f.setExecutable(true, false);
            }
        }
    }

    /**
     * Check root permissions
     * 
     * @param c context
     * @return false if error
     */
    public static boolean isRooted(Context c) {
        boolean result = false;
        OutputStream stdin = null;
        InputStream stdout = null;
        try {
            Process process = Runtime.getRuntime().exec("su");
            stdin = process.getOutputStream();
            stdout = process.getInputStream();

            DataOutputStream os = null;
            try {
                os = new DataOutputStream(stdin);
                os.writeBytes("ls /data\n");
                os.writeBytes("exit\n");
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(os);
            }

            int n = 0;
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(stdout));
                while (reader.readLine() != null) {
                    n++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(reader);
            }

            if (n > 0) {
                result = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(stdout);
            close(stdin);
        }
        return result;
    }

    /**
     * Update version file
     * 
     * @param c context
     * @return false if error
     */
    private static Boolean setVersion(Context c) {
        Boolean result = false;
        String f = PrefStore.getEnvDir(c) + "/etc/version";
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(f));
            bw.write(PrefStore.getVersion(c));
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(bw);
        }
        return result;
    }

    /**
     * Check latest env version
     * 
     * @param c context
     * @return false if error
     */
    public static Boolean isLatestVersion(Context c) {
        Boolean result = false;
        String f = PrefStore.getEnvDir(c) + "/etc/version";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            if (PrefStore.getVersion(c).equals(line)) result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(br);
        }
        return result;
    }

    public static boolean isValidShell(Context c) {
        File ash = new File(PrefStore.getShell(c));
        return ash.exists();
    }

    public static Boolean makeScript(Context c) {
        boolean result = false;
        String scriptFile = PrefStore.getEnvDir(c) + "/bin/linuxdeploy";
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(scriptFile));
            bw.write("#!" + PrefStore.getShell(c) + "\n");
            bw.write("PATH=" + PrefStore.getEnvDir(c) + "/bin:" +
                    "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:" +
                    PrefStore.getBusyboxDir(c) + ":$PATH\n");
            bw.write("ENV_DIR=\"" + PrefStore.getEnvDir(c) + "\"\n");
            bw.write(". ${ENV_DIR}/share/main.sh\n");
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(bw);
        }
        return result;
    }

    /**
     * Execute commands from system shell
     * 
     * @param c context
     * @param params list of commands
     * @return false, if error
     */
    public static boolean exec(final Context c, final String shell,
            final List<String> params) {
        if (params == null || params.size() == 0) {
            Logger.log(c, "No scripts for processing.\n");
            return false;
        }
        boolean result = false;
        OutputStream stdin = null;
        InputStream stdout = null;
        InputStream stderr = null;
        try {
            Process process = Runtime.getRuntime().exec(shell);

            stdin = process.getOutputStream();
            stdout = process.getInputStream();
            stderr = process.getErrorStream();

            params.add(0, "PATH=" + PrefStore.getEnvDir(c) + "/bin:"
                    + PrefStore.getBusyboxDir(c) + ":$PATH");
            params.add("exit $?");
            if (PrefStore.isTraceMode(c)) params.add(0, "set -x");

            DataOutputStream os = null;
            try {
                os = new DataOutputStream(stdin);
                for (String cmd : params) {
                    os.writeBytes(cmd + "\n");
                }
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(os);
            }

            // show stdout log
            final InputStream out = stdout;
            (new Thread() {
                @Override
                public void run() {
                    Logger.log(c, out);
                }
            }).start();

            // show stderr log
            final InputStream err = stderr;
            if (PrefStore.isDebugMode(c)) {
                (new Thread() {
                    @Override
                    public void run() {
                        Logger.log(c, err);
                    }
                }).start();
            }

            if (process.waitFor() == 0) result = true;
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        } finally {
            close(stdout);
            close(stderr);
            close(stdin);
        }
        return result;
    }

    /**
     * Update operating environment
     * 
     * @param c context
     * @return true, if no errors
     */
    public static boolean updateEnv(Context c) {
        if (!isRooted(c)) {
            Logger.log(c, "Requires superuser privileges (root).\n");
            return false;
        }

        if (!isValidShell(c)) {
            Logger.log(c, "Requires an installed BusyBox.\n");
            return false;
        }

        Logger.log(c, "Updating environment ... ");

        // clean env directory
        File envDir = new File(PrefStore.getEnvDir(c));
        envDir.mkdirs();
        cleanDirectory(envDir);

        // extract assets
        if (!extractDir(c, "all", "")) {
            Logger.log(c, "fail\n");
            return false;
        }
        if (!extractDir(c, PrefStore.getArch(), "")) {
            Logger.log(c, "fail\n");
            return false;
        }

        // make linuxdeploy script
        if (!makeScript(c)) {
            Logger.log(c, "fail\n");
            return false;
        }

        // set permissions
        setPermissions(envDir);

        // update version
        if (!setVersion(c)) {
            Logger.log(c, "fail\n");
            return false;
        }

        Logger.log(c, "done\n");
        return true;
    }

    /**
     * Make symlink on linuxdeploy script in /system/bin
     * 
     * @param c context
     * @return true, if no errors
     */
    public static boolean makeSymlink(Context c) {
        Logger.log(c, "Setting command line interface ... ");
        List<String> params = new ArrayList<String>();
        params.add("rm -f /system/bin/linuxdeploy");
        params.add("ln -s "
                + PrefStore.getEnvDir(c)
                + "/bin/linuxdeploy /system/bin/linuxdeploy || "
                + "{ mount -o rw,remount /system; rm -f /system/bin/linuxdeploy; ln -s "
                + PrefStore.getEnvDir(c)
                + "/bin/linuxdeploy /system/bin/linuxdeploy; mount -o ro,remount /system; }");
        if (EnvUtils.exec(c, "su", params)) {
            Logger.log(c, "done\n");
            return true;
        } else {
            Logger.log(c, "fail\n");
            return false;
        }
    }

    /**
     * Remove operating environment
     * 
     * @param c context
     * @return true, if no errors
     */
    public static boolean removeEnv(Context c) {
        Logger.log(c, "Removing environment ... ");

        // remove symlink
        File ldSymlink = new File("/system/bin/linuxdeploy");
        if (ldSymlink.exists()) {
            // exec shell commands
            List<String> params = new ArrayList<String>();
            params.add("if [ -e /system/bin/linuxdeploy ]; then "
                    + "rm -f /system/bin/linuxdeploy || "
                    + "{ mount -o rw,remount /system; rm -f /system/bin/linuxdeploy; mount -o ro,remount /system; };"
                    + "fi");
            exec(c, "su", params);
        }
        
        // clean env directory
        File envDir = new File(PrefStore.getEnvDir(c));
        cleanDirectory(envDir);

        Logger.log(c, "done\n");
        return true;
    }

    /**
     * Update configuration file
     * 
     * @param c context
     * @return true, if no errors
     */
    public static boolean updateConf(Context c) {
        if (!EnvUtils.isLatestVersion(c)) return false;
        Logger.log(c, "Updating configuration file ... ");

        Boolean result = false;
        String confFile = PrefStore.getEnvDir(c) + "/etc/deploy.conf";
        List<String> lines = new ArrayList<>();
        lines.add("MNT_TARGET=\"" + PrefStore.getChrootDir(c) + "\"");
        lines.add("IMG_TARGET=\"" + PrefStore.getTargetPath(c) + "\"");
        lines.add("IMG_SIZE=\"" + PrefStore.getImageSize(c) + "\"");
        lines.add("FS_TYPE=\"" + PrefStore.getFilesystem(c) + "\"");
        lines.add("DEPLOY_TYPE=\"" + PrefStore.getDeployType(c) + "\"");
        lines.add("DISTRIB=\"" + PrefStore.getDistribution(c) + "\"");
        lines.add("ARCH=\"" + PrefStore.getArchitecture(c) + "\"");
        lines.add("SUITE=\"" + PrefStore.getSuite(c) + "\"");
        lines.add("MIRROR=\"" + PrefStore.getSourcePath(c) + "\"");
        lines.add("USER_NAME=\"" + PrefStore.getUserName(c) + "\"");
        lines.add("USER_PASSWORD=\"" + PrefStore.getUserPassword(c) + "\"");
        lines.add("SERVER_DNS=\"" + PrefStore.getServerDns(c) + "\"");
        lines.add("LOCALE=\"" + PrefStore.getLocale(c) + "\"");
        lines.add("DESKTOP_ENV=\"" + PrefStore.getDesktopEnv(c) + "\"");
        lines.add("USE_COMPONENTS=\"" + PrefStore.getUseComponents(c) + "\"");
        lines.add("STARTUP=\"" + PrefStore.getStartup(c) + "\"");
        lines.add("CUSTOM_SCRIPTS=\"" + PrefStore.getCustomScripts(c) + "\"");
        lines.add("CUSTOM_MOUNTS=\"" + PrefStore.getCustomMounts(c) + "\"");
        lines.add("SSH_PORT=\"" + PrefStore.getSshPort(c) + "\"");
        lines.add("VNC_DISPLAY=\"" + PrefStore.getVncDisplay(c) + "\"");
        lines.add("VNC_DEPTH=\"" + PrefStore.getVncDepth(c) + "\"");
        lines.add("VNC_DPI=\"" + PrefStore.getVncDpi(c) + "\"");
        lines.add("VNC_GEOMETRY=\"" + PrefStore.getVncGeometry(c) + "\"");
        lines.add("VNC_ARGS=\"" + PrefStore.getVncArgs(c) + "\"");
        lines.add("XSERVER_DISPLAY=\"" + PrefStore.getXserverDisplay(c) + "\"");
        lines.add("XSERVER_HOST=\"" + PrefStore.getXserverHost(c) + "\"");
        lines.add("FB_DISPLAY=\"" + PrefStore.getFbDisplay(c) + "\"");
        lines.add("FB_DPI=\"" + PrefStore.getFbDpi(c) + "\"");
        lines.add("FB_DEV=\"" + PrefStore.getFbDev(c) + "\"");
        lines.add("FB_INPUT=\"" + PrefStore.getFbInput(c) + "\"");
        lines.add("FB_ARGS=\"" + PrefStore.getFbArgs(c) + "\"");
        lines.add("FB_FREEZE=\"" + PrefStore.getFbFreezeMode(c) + "\"");
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

        if (result) {
            Logger.log(c, "done\n");
        } else {
            Logger.log(c, "fail\n");
        }
        return result;
    }

    /**
     * Execute linuxdeploy script
     * 
     * @param c context
     * @param arg arguments
     * @return true, if no errors
     */
    public static boolean linuxdeploy(Context c, String arg) {
        List<String> params = new ArrayList<String>();
        String opts = "";
        if (PrefStore.isDebugMode(c)) opts = "-d ";
        if (PrefStore.isTraceMode(c)) opts = "-t ";
        params.add("printf '>>> " + arg + "\n'");
        params.add("linuxdeploy " + opts + arg);
        params.add("printf '<<< " + arg + "\n'");
        boolean result = exec(c, "su", params);
        return result;
    }

}
