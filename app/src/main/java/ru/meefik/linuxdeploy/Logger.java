package ru.meefik.linuxdeploy;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Logger {

    private static volatile List<String> protocol = new ArrayList<>();
    private static char lastChar = '\n';
    private static String lastLine = "";

    /**
     * Generate timestamp
     *
     * @return timestamp
     */
    private static String getTimeStamp() {
        return "[" + new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(new Date()) + "] ";
    }

    /**
     * Append the message to protocol and show
     *
     * @param c   context
     * @param msg message
     */
    private static synchronized void appendMessage(Context c, final String msg) {
        if (msg.length() == 0) return;
        String out = msg;
        boolean timestamp = PrefStore.isTimestamp(c);
        int maxLines = PrefStore.getMaxLines(c);
        int protocolSize = protocol.size();
        if (protocolSize > 0 && lastChar != '\n') {
            protocol.remove(protocolSize - 1);
            out = lastLine + out;
        }
        lastChar = out.charAt(out.length() - 1);
        String[] lines = out.split("\\n");
        for (int i = 0, l = lines.length; i < l; i++) {
            lastLine = lines[i];
            if (timestamp) protocol.add(getTimeStamp() + lastLine);
            else protocol.add(lastLine);
            if (protocolSize + i >= maxLines) {
                protocol.remove(0);
            }
        }
        // show protocol
        show();
        // write log
        if (PrefStore.isLogger(c)) write(c, msg);
    }

    /**
     * Clear protocol
     *
     * @param c context
     * @return true if success
     */
    public static boolean clear(Context c) {
        protocol.clear();
        File logFile = new File(PrefStore.getLogFile(c));
        return logFile.delete();
    }

    /**
     * Size of protocol
     *
     * @return size
     */
    public static int size() {
        return protocol.size();
    }

    /**
     * Show log on main activity
     */
    public static void show() {
        MainActivity.showLog(get());
    }

    /**
     * Get protocol
     *
     * @return protocol as text
     */
    public static String get() {
        return android.text.TextUtils.join("\n", protocol);
    }

    /**
     * Append message to protocol
     *
     * @param c   context
     * @param msg message
     */
    public static void log(Context c, String msg) {
        appendMessage(c, msg);
    }

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
                // e.printStackTrace();
            }
        }
    }

    /**
     * Write to log file
     *
     * @param c   context
     * @param msg message
     */
    public static void write(Context c, String msg) {
        String logFile = PrefStore.getLogFile(c);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write(msg);
        } catch (IOException e) {
            // e.printStackTrace();
        } finally {
            close(writer);
        }
    }

    /**
     * Append stream messages to protocol
     *
     * @param c      context
     * @param stream stream
     */
    public static void log(Context c, InputStream stream) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            int n;
            char[] buffer = new char[1024];
            while ((n = reader.read(buffer)) != -1) {
                String msg = String.valueOf(buffer, 0, n);
                appendMessage(c, msg);
            }
        } catch (IOException e) {
            // e.printStackTrace();
        } finally {
            close(reader);
        }
    }

}
