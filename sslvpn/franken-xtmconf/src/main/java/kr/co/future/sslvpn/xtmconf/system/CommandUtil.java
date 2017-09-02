package kr.co.future.sslvpn.xtmconf.system;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandUtil {
	public static int run(List<String> command) throws IOException {
		return run(command.toArray(new String[0]));
	}

	public static int run(String... command) throws IOException {
		return run(new File("/"), command);
	}

	public static int run(File workingDir, List<String> command) throws IOException {
		return run(workingDir, command.toArray(new String[0]));
	}

	public static int run(File workingDir, String... command) throws IOException {
		try {
			Logger logger = LoggerFactory.getLogger(CommandUtil.class.getName());
			ProcessBuilder builder = new ProcessBuilder(command).directory(workingDir);
			Process p = builder.start();
			clear(p.getInputStream());
			clear(p.getErrorStream());
			int waitFor = p.waitFor();
			logger.trace("frodo xtmconf: running command {}, exit {}", command, waitFor);
			return waitFor;
		} catch (InterruptedException e) {
			return -1;
		}
	}

	private static void clear(InputStream is) {
		StringBuilder sb = new StringBuilder();
		try {
			byte[] b = new byte[4096];
			while (true) {
				int read = is.read(b);
				if (read <= 0)
					break;

				sb.append(new String(b, 0, read));
			}
		} catch (IOException e) {
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}

			Logger logger = LoggerFactory.getLogger(CommandUtil.class.getName());
			logger.trace("frodo xtmconf: command output [{}]", sb.toString());
		}
	}
}
