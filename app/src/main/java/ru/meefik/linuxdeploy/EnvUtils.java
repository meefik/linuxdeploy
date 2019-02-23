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
    private static void setPermissions(File path, Boolean executable) {
        if (path == null) return;
        if (path.exists()) {
            path.setReadable(true, false);
            if (path.isDirectory()) {
                path.setExecutable(true, false);
                File[] list = path.listFiles();
                if (list == null) return;
                for (File f : list) {
                    setPermissions(f, executable);
                }
            } else {
                path.setExecutable(executable, false);
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
        String f = PrefStore.getEnvDir(c) + "/version";
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
        File f = new File(PrefStore.getEnvDir(c) + "/version");
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
        try {
            ProcessBuilder pb = new ProcessBuilder(shell);
            pb.directory(new File(PrefStore.getEnvDir(c)));
            // Map<String, String> env = pb.environment();
            // env.put("PATH", PrefStore.getPath(c) + ":" + env.get("PATH"));
            if (PrefStore.isDebugMode(c)) pb.redirectErrorStream(true);
            Process process = pb.start();

            stdin = process.getOutputStream();
            final InputStream stdout = process.getInputStream();
//            final InputStream stderr = process.getErrorStream();

//            params.add(0, "LD_LIBRARY_PATH=" + PrefStore.getLibsDir(c) + ":$LD_LIBRARY_PATH");
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

            (new Thread() {
                @Override
                public void run() {
                    Logger.log(c, stdout);
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
        // stop services
        execServices(c, new String[]{"telnetd", "httpd"}, "stop");

        // extract env assets
        if (!extractDir(c, PrefStore.getEnvDir(c), "env", "")) return false;

        // extract bin assets
        if (!extractDir(c, PrefStore.getBinDir(c), "bin/all", "")) return false;
        if (!extractDir(c, PrefStore.getBinDir(c), "bin/" + PrefStore.getArch(), "")) return false;

        // extract web assets
        if (!extractDir(c, PrefStore.getWebDir(c), "web", "")) return false;

        // make linuxdeploy script
        if (!makeMainScript(c)) return false;

        // set executable bin directory
        File binDir = new File(PrefStore.getBinDir(c));
        setPermissions(binDir, true);

        // set executable cgi-bin directory
        File cgiDir = new File(PrefStore.getWebDir(c) + "/cgi-bin");
        setPermissions(cgiDir, true);

        // make config directory
        File configDir = new File(PrefStore.getConfigDir(c));
        configDir.mkdirs();

        // make tmp directory
        File tmpDir = new File(PrefStore.getTmpDir(c));
        tmpDir.mkdirs();

        // create .nomedia
        File noMedia = new File(PrefStore.getEnvDir(c) + "/.nomedia");
        try {
            noMedia.createNewFile();
        } catch (IOException ignored) {
        }

        List<String> params = new ArrayList<>();
        // install busybox applets
        params.add("busybox --install -s " + PrefStore.getBinDir(c));
        // replace shell interpreter in some scripts
        String[] scripts = {
                PrefStore.getBinDir(c) + "/websocket.sh",
                PrefStore.getWebDir(c) + "/cgi-bin/resize",
                PrefStore.getWebDir(c) + "/cgi-bin/sync",
                PrefStore.getWebDir(c) + "/cgi-bin/terminal"
        };
        for (String f : scripts) {
            params.add("sed -i 's|^#!/.*|#!" + PrefStore.getShell(c) + "|' " + f);
        }
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

        // start services
        execServices(c, new String[]{"telnetd", "httpd"}, "start");

        return true;
    }

    /**
     * Make linuxdeploy script
     *
     * @param c context
     * @return true if success
     */
    private static boolean makeMainScript(Context c) {
        boolean result = false;
        String scriptFile = PrefStore.getBinDir(c) + "/linuxdeploy";
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(scriptFile));
            bw.write("#!" + PrefStore.getShell(c) + "\n");
            bw.write("PATH=" + PrefStore.getPath(c) + ":$PATH\n");
            bw.write("ENV_DIR=\"" + PrefStore.getEnvDir(c) + "\"\n");
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
     * Make symlink on linuxdeploy script in /system/bin
     *
     * @param c context
     * @return true if success
     */
    private static boolean makeSymlink(Context c) {
        List<String> params = new ArrayList<>();
        params.add("{ rm -f /system/bin/linuxdeploy; ln -s "
                + PrefStore.getBinDir(c)
                + "/linuxdeploy /system/bin/linuxdeploy; } 2>/dev/null || "
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
                + "rm -f /system/bin/linuxdeploy 2>/dev/null || "
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
        // stop services
        execServices(c, new String[]{"telnetd", "httpd"}, "stop");

        // remove symlink
        File ldSymlink = new File("/system/bin/linuxdeploy");
        if (ldSymlink.exists()) removeSymlink(c);

        // clean env directory
        File envDir = new File(PrefStore.getEnvDir(c));
        cleanDirectory(envDir);

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
     * @param c    context
     * @param cmd  command
     * @param args arguments
     */
    static void execService(Context c, String cmd, String args) {
        Intent service = new Intent(c, ExecService.class);
        service.putExtra("cmd", cmd);
        service.putExtra("args", args);
        ExecService.enqueueWork(c, service);
    }

    /**
     * Execute commands via service
     *
     * @param c        context
     * @param commands commands
     * @param args     command and arguments
     */
    static void execServices(Context c, String[] commands, String args) {
        for (String cmd : commands) {
            execService(c, cmd, args);
        }
    }

    /**
     * Start/stop telnetd daemon
     *
     * @param c   context
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
                makeIssueFile(c, PrefStore.getEnvDir(c) + "/issue");
                String args = "";
                args += " -l " + PrefStore.getShell(c);
                args += " -p " + PrefStore.getTelnetPort(c);
                args += " -f " + PrefStore.getEnvDir(c) + "/issue";
                if (PrefStore.isTelnetLocalhost(c)) args += " -b 127.0.0.1";
                params.add("pgrep telnetd >/dev/null && exit");
                params.add("export TERM=\"xterm\"");
                params.add("export PS1=\"\\$ \"");
                params.add("export HOME=\"" + PrefStore.getEnvDir(c) + "\"");
                params.add("cd \"$HOME\"");
                params.add("telnetd" + args);
        }
        return params.size() > 0 && exec(c, "sh", params);
    }

    /**
     * Start/stop httpd daemon
     *
     * @param c   context
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
                makeHttpdConf(c, PrefStore.getEnvDir(c) + "/httpd.conf");
                params.add("pgrep httpd >/dev/null && exit");
                params.add("export WS_SHELL=\"telnet 127.0.0.1 " + PrefStore.getTelnetPort(c) + "\"");
                params.add("export ENV_DIR=\"" + PrefStore.getEnvDir(c) + "\"");
                params.add("export HOME=\"" + PrefStore.getEnvDir(c) + "\"");
                params.add("cd " + PrefStore.getWebDir(c));
                params.add("httpd " + " -p " + PrefStore.getHttpPort(c) + " -c " + PrefStore.getEnvDir(c) + "/httpd.conf");
        }
        return params.size() > 0 && exec(c, "sh", params);
    }

    /**
     * Make httpd.conf file
     *
     * @param c context
     * @return true if success
     */
    private static boolean makeHttpdConf(Context c, String f) {
        boolean result = false;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(f));
            for (String part : PrefStore.getHttpConf(c).split(" ")) {
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

    /**
     * Make issue file
     *
     * @param c context
     * @return true if success
     */
    private static boolean makeIssueFile(Context c, String f) {
        boolean result = false;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(f));
            bw.write("Linux Deploy \\m \\l\n");
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(bw);
        }
        return result;
    }
}
