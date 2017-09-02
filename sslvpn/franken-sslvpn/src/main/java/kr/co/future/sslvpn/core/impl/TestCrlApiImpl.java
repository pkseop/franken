package kr.co.future.sslvpn.core.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.sslvpn.core.TestCrl;
import kr.co.future.sslvpn.core.TestCrlApi;
import kr.co.future.sslvpn.core.CrlValidator;
import kr.co.future.sslvpn.core.impl.TestCrlApiImpl;

@Component(name = "frodo-test-crl-api")
@Provides
public class TestCrlApiImpl implements TestCrlApi, Runnable {
	final Logger logger = LoggerFactory.getLogger(TestCrlApiImpl.class);

	@Requires
	private ConfigService conf;

	private CopyOnWriteArrayList<TestCrl> crls;
	private Thread t;
	private int milliseconds;
	private volatile boolean doStop;
	private volatile boolean isRunning;
	private volatile boolean useTrace = false;

	@Override
	public void run() {
		doStop = false;
		isRunning = true;
		crls = new CopyOnWriteArrayList<TestCrl>(getTestCrls());

		File logDir = new File("/utm/log/crltest/");

		try {
			if (!logDir.exists())
				logDir.mkdirs();

			while (!doStop) {
				runOnce(logDir);
				Thread.sleep(milliseconds);
			}
		} catch (InterruptedException e) {
			logger.error("frodo core: crl checker interrupted");
		} finally {
			isRunning = false;
		}
		logger.info("frodo core: crl checker thread stopped");
	}

	public void runOnce(File logDir) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		File logFile = new File(logDir, sdf.format(new Date()) + ".log");
		OutputStream os = null;
		OutputStreamWriter writer = null;

		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
		try {
			if (!logFile.exists())
				logFile.createNewFile();

			os = new FileOutputStream(logFile, true);
			writer = new OutputStreamWriter(os, "utf-8");
			String newLine = System.getProperty("line.separator");

			for (TestCrl c : crls) {
				boolean isRevoked = true;
				try {
					isRevoked = CrlValidator.isRevoked(c.getUrl(), new BigInteger(c.getSerial()));
					if (useTrace)
						writer.write("[" + sdf.format(new Date()) + "] " + (isRevoked == true ? "[T] " : "[F] ") + c.toString()
								+ newLine);
				} catch (Exception e) {
					writer.write("[" + sdf.format(new Date()) + "] " + c.toString() + " [" + e.getMessage() + "]" + newLine);
				}
			}
		} catch (IOException e) {
			logger.error("frodo core: cannot write log", e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
			}

			if (os != null)
				try {
					os.close();
				} catch (IOException e1) {
				}
		}

	}

	@Override
	public void addTestCrl(TestCrl crl) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(TestCrl.class, Predicates.field("alias", crl.getAlias()));
		if (c != null)
			throw new IllegalStateException("alias aleady exist");

		db.add(crl);

		if (isRunning() && crls != null)
			crls.add(crl);
	}

	@Override
	public void removeTestCrl(String alias) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(TestCrl.class, Predicates.field("alias", alias));

		if (c == null)
			return;

		db.remove(c);
		TestCrl crl = c.getDocument(TestCrl.class);

		if (isRunning() && crls != null)
			crls.remove(crl);
	}

	@Override
	public void start(int seconds) {
		if (seconds <= 1)
			throw new IllegalStateException("second should be over 1");

		if (isRunning())
			throw new IllegalStateException("crl checker aleady running");

		logger.info("frodo core: starting crl checker thread");
		this.milliseconds = seconds * 1000;

		t = new Thread(this, "CRL Checker");
		t.start();
	}

	@Override
	public void stop() {
		if (isRunning()) {
			doStop = true;
			isRunning = false;
			t.interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public Collection<TestCrl> getTestCrls() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.findAll(TestCrl.class);
		return it.getDocuments(TestCrl.class);
	}

	@Override
	public void setTrace(boolean useTrace) {
		this.useTrace = useTrace;
	}

}
