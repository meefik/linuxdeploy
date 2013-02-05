package ru.meefik.linuxdeploy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

public class ExecCmd implements Runnable {

	private List<String> params;
	public boolean status;

	public ExecCmd(List<String> params) {
		this.params = params;
		status = true;
	}

	@Override
	public void run() {
		try {
			Process process = Runtime.getRuntime().exec(params.get(0));
			params.remove(0);

			OutputStream stdin = process.getOutputStream();
			DataOutputStream os = new DataOutputStream(stdin);
			for (String tmpCmd : params) {
				os.writeBytes(tmpCmd + "\n");
			}
			os.flush();
			os.close();

			final InputStream stdout = process.getInputStream();
			final InputStream stderr = process.getErrorStream();

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
				status = false;

			stdout.close();
			stderr.close();
			stdin.close();
		} catch (Exception e) {
			status = false;
			e.printStackTrace();
		}
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
				status = false;
				e.printStackTrace();
			}
		}
	}

}
