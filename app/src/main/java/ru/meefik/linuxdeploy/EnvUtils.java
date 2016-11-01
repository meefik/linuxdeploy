package ru.meefik.linuxdeploy;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;

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
import java.util.Map;

class EnvUtils {

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
     * @param c         context
     * @param target    target directory
     * @param rootAsset root asset name
     * @param path      path to asset file
     * @return true if success
     */
    private static boolean extractFile(Context c, String target, String rootAsset, String path) {
        AssetManager assetManager = c.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(rootAsset + path);
            File fname = new File(target + path);
            fname.delete();
            out = new FileOutputStream(fname);
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
     * @param c         context
     * @param target    target directory
     * @param rootAsset root asset name
     * @param path      path to asset directory
     * @return true if success
     */
    private static boolean extractDir(Context c, String target, String rootAsset, String path) {
        AssetManager assetManager = c.getAssets();
        try {
            String[] assets = assetManager.list(rootAsset + path);
            if (assets.length == 0) {
                if (!extractFile(c, target, rootAsset, path)) return false;
            } else {
                String fullPath = target + path;
                File dir = new File(fullPath);
                if (!dir.exists()) dir.mkdir();
                for (String asset : assets) {
                    if (!extractDir(c, target, rootAsset, path + "/" + asset))
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
            File[] list = path.listFiles();
            if (list == null) return;
            for (File f : list) {
                if (f.isDirectory()) cleanDirectory(f);
                f.delete();
            }
            path.delete();
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
            path.setReadable(true, false);
            path.setExecutable(true, false);
            File[] list = path.listFiles();
            if (list == null) return;
            for (File f : list) {
                if (f.isDirectory()) setPermissions(f);
                f.setReadable(true, false);
                f.setExecutable(true, false);
            }
        }
    }

    /**
     * Check root permissions
     *
     * @return true if success
     */
    private static boolean isRooted() {
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
     * @return true if success
     */
    private static boolean setVersion(Context c) {
        boolean result = false;
        String f = PrefStore.getDataDir(c) + "/version";
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
     * @return true if success
     */
    static boolean isLatestVersion(Context c) {
        File f = new File(PrefStore.getDataDir(c) + "/version");
        if (!f.exists()) return false;
        boolean result = false;
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

    /**
     * Remove version file
     *
     * @param c context
     * @return true if success
     */
    private static boolean resetVersion(Context c) {
        File f = new File(PrefStore.getDataDir(c) + "/version");
        return f.delete();
    }

    /**
     * Make linuxdeploy script
     *
     * @param c context
     * @return true if success
     */
    private static boolean makeScript(Context c) {
        boolean result = false;
        String scriptFile = PrefStore.getBinDir(c) + "/linuxdeploy";
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(scriptFile));
            bw.write("#!" + PrefStore.getShell(c) + "\n");
            bw.write("PATH=" + PrefStore.getPath(c) + ":$PATH\n");
            bw.write("ENV_DIR=\"" + PrefStore.getEnvDir(c) + "\"\n");
            bw.write("TEMP_DIR=\"" + PrefStore.getTmpDir(c) + "\"\n");
            bw.write(". \"${ENV_DIR}/cli.sh\"\n");
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
     * @param c      context
     * @param params list of commands
     * @return true if success
     */
    public static boolean exec(final Context c, final String shell, final List<String> params) {
        if (params == null || params.size() == 0) {
            Logger.log(c, "No scripts for processing.\n");
            return false;
        }
        if ("su".equals(shell)) {
            if (!isRooted()) {
                Logger.log(c, "Requires superuser privileges (root).\n");
                return false;
            }
        }
        boolean result = false;
        OutputStream stdin = null;
        InputStream stdout;
        try {
            ProcessBuilder pb = new ProcessBuilder(shell);
            pb.directory(new File(PrefStore.getEnvDir(c)));
            // Map<String, String> env = pb.environment();
            // env.put("PATH", PrefStore.getPath(c) + ":" + env.get("PATH"));
            if (PrefStore.isDebugMode(c)) pb.redirectErrorStream(true);
            Process process = pb.start();

            stdin = process.getOutputStream();
            stdout = process.getInputStream();

            params.add(0, "PATH=" + PrefStore.getBinDir(c) + ":$PATH");
            if (PrefStore.isTraceMode(c)) params.add(0, "set -x");
            params.add("exit $?");

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

            if (process.waitFor() == 0) result = true;
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        } finally {
            close(stdin);
        }
        return result;
    }

    /**
     * Update operating environment
     *
     * @param c context
     * @return true if success
     */
    static boolean updateEnv(final Context c) {
        // stop telnetd
        execService(c, "telnetd", "stop");
        //stop httpd
        execService(c, "httpd", "stop");

        // extract bin assets
        if (!extractDir(c, PrefStore.getBinDir(c), "bin/all", "")) return false;
        if (!extractDir(c, PrefStore.getBinDir(c), "bin/" + PrefStore.getArch(), "")) return false;

        // extract env assets
        if (!extractDir(c, PrefStore.getEnvDir(c), "env", "")) return false;

        // extract web assets
        if (!extractDir(c, PrefStore.getWebDir(c), "web", "")) return false;

        // make linuxdeploy script
        if (!makeScript(c)) return false;

        // make tmp directory
        File tmpDir = new File(PrefStore.getTmpDir(c));
        tmpDir.mkdirs();

        // make config directory
        File configDir = new File(PrefStore.getConfigDir(c));
        configDir.mkdirs();

        // create .nomedia
        File noMedia = new File(PrefStore.getEnvDir(c) + "/.nomedia");
        try {
            noMedia.createNewFile();
        } catch (IOException ignored) {
        }

        // set permissions
        File binDir = new File(PrefStore.getBinDir(c));
        setPermissions(binDir);
        File cgiDir = new File(PrefStore.getWebDir(c) + "/cgi-bin");
        setPermissions(cgiDir);

        // install applets
        List<String> params = new ArrayList<>();
        params.add("busybox --install -s " + PrefStore.getBinDir(c));
        exec(c, "sh", params);

        // update cli.conf
        if (!PrefStore.getSettingsConfFile(c).exists()) PrefStore.dumpSettings(c);
        // update profile.conf
        if (!PrefStore.getPropertiesConfFile(c).exists()) PrefStore.dumpProperties(c);

        if (PrefStore.isCliSymlink(c)) {
            if (!EnvUtils.makeSymlink(c)) return false;
        }

        // update version
        if (!setVersion(c)) return false;

        // start telnetd
        execService(c, "telnetd", "start");
        //start httpd
        execService(c, "httpd", "start");

        return true;
    }

    /**
     * Make symlink on linuxdeploy script in /system/bin
     *
     * @param c context
     * @return true if success
     */
    private static boolean makeSymlink(Context c) {
        List<String> params = new ArrayList<>();
        params.add("rm -f /system/bin/linuxdeploy");
        params.add("ln -s "
                + PrefStore.getBinDir(c)
                + "/linuxdeploy /system/bin/linuxdeploy || "
                + "{ mount -o rw,remount /system; rm -f /system/bin/linuxdeploy; ln -s "
                + PrefStore.getBinDir(c)
                + "/linuxdeploy /system/bin/linuxdeploy; mount -o ro,remount /system; }");
        return exec(c, "su", params);
    }

    /**
     * Remove symlink on linuxdeploy script from /system/bin
     *
     * @param c context
     * @return true if success
     */
    private static boolean removeSymlink(Context c) {
        List<String> params = new ArrayList<>();
        params.add("if [ -e /system/bin/linuxdeploy ]; then "
                + "rm -f /system/bin/linuxdeploy || "
                + "{ mount -o rw,remount /system; rm -f /system/bin/linuxdeploy; mount -o ro,remount /system; };"
                + "fi");
        return exec(c, "su", params);
    }

    /**
     * Remove operating environment
     *
     * @param c context
     * @return true if success
     */
    static boolean removeEnv(Context c) {
        // remove version file
        resetVersion(c);

        // stop telnetd
        execService(c, "telnetd", "stop");

        //stop httpd
        execService(c, "httpd", "stop");

        // remove symlink
        File ldSymlink = new File("/system/bin/linuxdeploy");
        if (ldSymlink.exists()) removeSymlink(c);

        // clean web directory
        File webDir = new File(PrefStore.getWebDir(c));
        cleanDirectory(webDir);

        // clean env directory
        File envDir = new File(PrefStore.getEnvDir(c));
        cleanDirectory(envDir);

        // clean tmp directory
        File tmpDir = new File(PrefStore.getTmpDir(c));
        cleanDirectory(tmpDir);

        // clean bin directory
        File binDir = new File(PrefStore.getBinDir(c));
        cleanDirectory(binDir);

        return true;
    }

    /**
     * Execute linuxdeploy script
     *
     * @param c    context
     * @param cmd  command
     * @param args arguments
     * @return true if success
     */
    public static boolean cli(Context c, String cmd, String args) {
        List<String> params = new ArrayList<>();
        String opts = "";
        if (PrefStore.isDebugMode(c)) opts += "-d ";
        if (PrefStore.isTraceMode(c)) opts += "-t ";
        if (args == null) args = "";
        else args = " " + args;
        params.add("printf '>>> " + cmd + "\n'");
        params.add(PrefStore.getBinDir(c) + "/linuxdeploy " + opts + cmd + args);
        params.add("printf '<<< " + cmd + "\n'");
        String shell = PrefStore.isRootRequired(c) ? "su" : "sh";
        return exec(c, shell, params);
    }

    /**
     * Execute command via service
     *
     * @param c context
     * @param args command and arguments
     */
    static void execService(Context c, String cmd, String args) {
        Intent service = new Intent(c, ExecService.class);
        service.putExtra("cmd", cmd);
        service.putExtra("args", args);
        c.startService(service);
    }

    /**
     * Start/stop telnetd daemon
     *
     * @param c context
     * @param cmd command: start, stop or restart
     * @return true if success
     */
    static boolean telnetd(Context c, String cmd) {
        List<String> params = new ArrayList<>();
        if (cmd == null) cmd = PrefStore.isTelnet(c) ? "start" : "stop";
        switch (cmd) {
            case "restart":
            case "stop":
                params.add("pkill -9 telnetd");
                if (cmd.equals("stop")) break;
            case "start":
                if (!PrefStore.isTelnet(c)) break;
                String args = "";
                args += " -l " + PrefStore.getShell(c);
                args += " -p " + PrefStore.getTelnetPort(c);
                args += " -f " + PrefStore.getWebDir(c) + "/issue";
                if (PrefStore.isTelnetLocalhost(c)) args += " -b 127.0.0.1";
                params.add("pgrep telnetd >/dev/null && exit");
                params.add("export TERM=\"xterm\"");
                params.add("export HOME=\"" + PrefStore.getEnvDir(c) + "\"");
                params.add("cd \"$HOME\"");
                params.add("telnetd" + args);
        }
        return params.size() > 0 && exec(c, "sh", params);
    }


    /**
     * Start/stop httpd daemon
     *
     * @param c context
     * @param cmd command: start, stop or restart
     * @return true if success
     */
    static boolean httpd(Context c, String cmd) {
        List<String> params = new ArrayList<>();
        if (cmd == null) cmd = PrefStore.isHttp(c) ? "start" : "stop";
        switch (cmd) {
            case "restart":
            case "stop":
                params.add("pkill -9 httpd");
                if (cmd.equals("stop")) break;
            case "start":
                if (!PrefStore.isHttp(c)) break;
                File conf = PrefStore.getHttpConfFile(c);
                EnvUtils.makeHttpdConf(c, conf);
                params.add("pgrep httpd >/dev/null && exit");
                params.add("export WS_SHELL=\"telnet 127.0.0.1 " + PrefStore.getTelnetPort(c) + "\"");
                params.add("export ENV_DIR=\"" + PrefStore.getEnvDir(c) + "\"");
                params.add("export HOME=\"" + PrefStore.getEnvDir(c) + "\"");
                params.add("cd " + PrefStore.getWebDir(c));
                params.add("httpd " + " -p " + PrefStore.getHttpPort(c) + " -c " + conf);
        }
        return params.size() > 0 && exec(c, "sh", params);
    }

    /**
     * Make httpd.conf file
     *
     * @param c context
     * @return true if success
     */
    private static boolean makeHttpdConf(Context c, File conf) {
        boolean result = false;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(conf));
            for(String part: PrefStore.getHttpConf(c).split(" ")) {
                bw.write(part + "\n");
            }
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(bw);
        }
        return result;
    }

}
