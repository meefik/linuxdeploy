package ru.meefik.linuxdeploy;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

public class EnvUtils {

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

        try (InputStream in = assetManager.open(rootAsset + path)) {
            File fname = new File(target + path);
            fname.delete();
            try (OutputStream out = new FileOutputStream(fname)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
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
        try {
            Process process = Runtime.getRuntime().exec("su");
            try (DataOutputStream stdin = new DataOutputStream(process.getOutputStream());
                 BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                stdin.writeBytes("ls /data\n");
                stdin.writeBytes("exit\n");
                stdin.flush();

                return stdout.readLine() != null;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Update version file
     *
     * @param c context
     * @return true if success
     */
    private static boolean setVersion(Context c) {
        String f = PrefStore.getEnvDir(c) + "/version";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(PrefStore.getVersion());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check latest env version
     *
     * @param c context
     * @return true if success
     */
    public static boolean isLatestVersion(Context c) {
        File f = new File(PrefStore.getEnvDir(c) + "/version");
        if (!f.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            return PrefStore.getVersion().equals(line);
        } catch (IOException e) {
            return false;
        }
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

        if ("su".equals(shell) && !isRooted()) {
            Logger.log(c, "Requires superuser privileges (root).\n");
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(shell);
            pb.directory(new File(PrefStore.getEnvDir(c)));
            // Map<String, String> env = pb.environment();
            // env.put("PATH", PrefStore.getPath(c) + ":" + env.get("PATH"));
            if (PrefStore.isDebugMode(c)) pb.redirectErrorStream(true);
            Process process = pb.start();

            try (DataOutputStream os = new DataOutputStream(process.getOutputStream());
                 InputStream stdout = process.getInputStream()) {
//                final InputStream stderr = process.getErrorStream();

//                params.add(0, "LD_LIBRARY_PATH=" + PrefStore.getLibsDir(c) + ":$LD_LIBRARY_PATH");
                params.add(0, "PATH=" + PrefStore.getBinDir(c) + ":$PATH");
                if (PrefStore.isTraceMode(c))
                    params.add(0, "set -x");
                params.add("exit $?");

                try {
                    for (String cmd : params) {
                        os.writeBytes(cmd + "\n");
                    }
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                (new Thread() {
                    @Override
                    public void run() {
                        Logger.log(c, stdout);
                    }
                }).start();
            }

            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
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
        String arch = PrefStore.getArch();
        switch (arch) {
            case "arm":
                if (!extractDir(c, PrefStore.getBinDir(c), "bin/arm", "")) return false;
                break;
            case "arm_64":
                if (!extractDir(c, PrefStore.getBinDir(c), "bin/arm", "")) return false;
                if (!extractDir(c, PrefStore.getBinDir(c), "bin/arm_64", "")) return false;
                break;
            case "x86":
                if (!extractDir(c, PrefStore.getBinDir(c), "bin/x86", "")) return false;
                break;
            case "x86_64":
                if (!extractDir(c, PrefStore.getBinDir(c), "bin/x86", "")) return false;
                if (!extractDir(c, PrefStore.getBinDir(c), "bin/x86_64", "")) return false;
                break;
        }

        // extract web assets
        if (!extractDir(c, PrefStore.getWebDir(c), "web", "")) return false;

        // make linuxdeploy script
        if (!makeMainScript(c)) return false;

        // set executable app directory
        File appDir = new File(PrefStore.getEnvDir(c) + "/..");
        appDir.setExecutable(true, false);

        // make config directory
        File configDir = new File(PrefStore.getConfigDir(c));
        configDir.mkdirs();

        // make tmp directory
        File tmpDir = new File(PrefStore.getTmpDir(c));
        tmpDir.mkdirs();

        // set executable env directory
        File binDir = new File(PrefStore.getEnvDir(c));
        setPermissions(binDir, true);

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
        String scriptFile = PrefStore.getBinDir(c) + "/linuxdeploy";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(scriptFile))) {
            bw.write("#!" + PrefStore.getShell(c) + "\n");
            bw.write("PATH=" + PrefStore.getPath(c) + ":$PATH\n");
            bw.write("ENV_DIR=\"" + PrefStore.getEnvDir(c) + "\"\n");
            bw.write(". \"${ENV_DIR}/cli.sh\"\n");
            return true;
        } catch (IOException e) {
            return false;
        }
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
        return exec(c, "su", params);
    }

    /**
     * Execute command via service
     *
     * @param c    context
     * @param cmd  command
     * @param args arguments
     */
    public static void execService(Context c, String cmd, String args) {
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
    public static void execServices(Context c, String[] commands, String args) {
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
                makeIssueFile(PrefStore.getEnvDir(c) + "/issue");
                String args = "";
                args += " -l " + PrefStore.getShell(c);
                args += " -p " + PrefStore.getTelnetPort(c);
                args += " -f " + PrefStore.getEnvDir(c) + "/issue";
                if (PrefStore.isTelnetLocalhost(c)) args += " -b 127.0.0.1";
                params.add("pgrep telnetd >/dev/null && exit");
                params.add("export TERM=\"xterm\"");
                params.add("export PS1=\"\\$ \"");
                params.add("export HOME=\"" + PrefStore.getEnvDir(c) + "\"");
                params.add("export TMPDIR=\"" + PrefStore.getTmpDir(c) + "\"");
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
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            for (String part : PrefStore.getHttpConf(c).split(" ")) {
                bw.write(part + "\n");
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Make issue file
     *
     * @return true if success
     */
    private static boolean makeIssueFile(String f) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write("Linux Deploy \\m \\l\n");
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
