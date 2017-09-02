package kr.co.future.sslvpn.xtmconf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import kr.co.future.msgbus.Session;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KLogWriter {
	private static Logger logger = LoggerFactory.getLogger(KLogWriter.class);

	public static void write(int event, Session session, String desc) {
		// check level
		LogSetting logSetting = null;
		for (LogSetting ls : XtmConfig.readConfig(LogSetting.class)) {
			if (ls.getType() == LogSetting.Type.Setting)
				logSetting = ls;
		}
		int logType = (event & 0xF0000000) >> 28;
		int logLevel = (event & 0x000F0000) >> 16;
		int systemLogLevel = 0;
		if (logSetting == null) {
			logger.trace("frodo xtmconf: cannot find log_setting.xml");
			return;
		}
		logger.trace(String.format("frodo xtmconf: 0x%x %s, klog type %d, level %d", event, desc, logType, logLevel));

		switch (logType) {
		case 1: // System
			systemLogLevel = logSetting.getSystem().ordinal();
			break;
		case 3: // IPSec VPN
			systemLogLevel = logSetting.getIpsec().ordinal();
			break;
		case 5: // Network
			systemLogLevel = logSetting.getNetwork().ordinal();
			break;
		default:
			return;
		}

		if (systemLogLevel > logLevel)
			return;

		// write
		ByteBuffer bb = ByteBuffer.allocate(372);
		bb.put(L(0));
		bb.put(L(0));
		bb.put(L(event));
		if (session != null)
			bb.put(session.getRemoteAddress().getAddress());
		else
			bb.put(new byte[4]);
		for (int i = 0; i < 12; i++)
			bb.put(c((char) 0));
		if (session != null)
			bb.put(session.getLocalAddress().getAddress());
		else
			bb.put(new byte[4]);
		for (int i = 0; i < 12; i++)
			bb.put(c((char) 0));
		for (int i = 0; i < 32; i++)
			bb.put(c((char) 0));
		bb.put(S((short) 0));
		bb.put(c((char) 1));
		bb.put(c((char) 0));
		bb.put(S((short) 0));
		bb.put(S((short) 0));
		bb.put(S((short) 0));
		bb.put(S((short) 0));
		bb.put(L(0));
		bb.put(L(0));
		bb.put(L(0));
		long millis = System.currentTimeMillis();
		bb.put(L((int) (millis / 1000L)));
		bb.put(L((int) (millis % 1000L)));
		bb.put(L(0));
		bb.put(L(0));
		bb.put(L(0));
		bb.put(L(0));
		bb.put(L(0));
		bb.put(L(0));
		for (int i = 0; i < 80; i++)
			bb.put(c((char) 0));
		byte[] description = null;
		try {
			description = desc.getBytes("utf-8");
		} catch (UnsupportedEncodingException e1) {
		}
		int len = (description.length < 100) ? description.length : 100;
		for (int i = 0; i < len; i++)
			bb.put(description[i]);
		for (int i = len; i < 100; i++)
			bb.put(c((char) 0));
		for (int i = 0; i < 60; i++)
			bb.put(c((char) 0));

		FileOutputStream fos = null;
		try {
			File f = new File("/proc/utm/conf/klog");
			fos = new FileOutputStream(f);
			fos.write(bb.array());
		} catch (IOException e) {
			logger.error("frodo xtmconf: failed to write log, event {}, desc {}", event, desc);
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
				}
		}
	}

	private static byte[] L(int l) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) (l & 0xff);
			l >>= 8;
		}
		return b;
	}

	private static byte[] c(char c) {
		return new byte[] { (byte) c };
	}

	private static byte[] S(short l) {
		byte[] b = new byte[2];
		for (int i = 0; i < 2; i++) {
			b[i] = (byte) (l & 0xff);
			l >>= 8;
		}
		return b;
	}
}
