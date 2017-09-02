package kr.co.future.sslvpn.core.impl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;

import kr.co.future.api.BundleManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class WindowsClientVersion {
	private final BundleManager bundleManager;
	private final BundleContext bundleContext;
	private final GlobalConfigApi configApi;

	public WindowsClientVersion(BundleContext bundleContext, BundleManager bundleManager, GlobalConfigApi configApi) {
		super();
		this.bundleContext = bundleContext;
		this.bundleManager = bundleManager;
		this.configApi = configApi;
	}

	private String webRevision_;
	private String buildNumber_;
	private String revision_;

	public String revision() {
		return this.revision_;
	}

	public void SetRevision(String rev) {
		this.revision_ = rev;
	}

	public String buildNumber() {
		return this.buildNumber_;
	}

	public void SetBuildNumber(String bn) {
		this.buildNumber_ = bn;
	}

	public String webRevision() {
		return this.webRevision_;
	}

	public void SetWebRevision(String webRev) {
		this.webRevision_ = webRev;
	}

	public void load() {
		try {
			List<String> w32Lines = readVersionTxt("version.win32.txt");

			if (w32Lines.size() >= 3) {
				this.SetRevision(w32Lines.get(0));
				this.SetBuildNumber(w32Lines.get(2) + " " + w32Lines.get(1));
			}
		} catch (RuntimeException re) {
			this.SetRevision(re.getMessage());
			this.SetBuildNumber(re.getMessage());
		}
		try {
			List<String> webLines = readVersionTxt("version.js.txt");
			if (webLines.size() >= 1) {
				this.SetWebRevision(webLines.get(0));
			}
		} catch (RuntimeException re) {
			this.SetWebRevision(re.getMessage());
		}
	}

	private List<String> readVersionTxt(String filename) {
		// find proper version.win32.txt
		GlobalConfig config = configApi.getGlobalConfig();

		String userUiPath = null;

		if (config != null)
			userUiPath = config.getUserUiPath();

		Reader verReader = null;
		BufferedReader br = null;
		try {
			if (userUiPath != null && !userUiPath.trim().isEmpty()) {
				verReader = getReaderFromFsPath(userUiPath, filename);
			}

			if (verReader == null) {
				verReader = getReaderFromBundle(filename);
			}

			if (verReader == null)
				throw new RuntimeException("cannot open " + filename + " from both bundle and userui path");

			br = new BufferedReader(verReader);

			List<String> result = new ArrayList<String>();
			try {
				while (true) {
					String line = br.readLine();
					if (line == null)
						break;
					result.add(line);
				}
				return result;
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("exception while reading " + filename, e);
			}
		} finally {
			if (br != null){
            try {
                br.close();
            } catch (IOException e) {
            }
			}
			safeClose(verReader);
		}

	}

	private void safeClose(Closeable reader) {
		if (reader == null)
			return;
		try {
			reader.close();
		} catch (IOException e) {
		}
	}

	private Reader getReaderFromBundle(String filename) {
		Bundle uiBundle = null;
		for (Bundle b : bundleContext.getBundles())
			if (b.getSymbolicName().equals("kr.co.future.frodo.userui")) {
				uiBundle = b;
				break;
			}
		if (uiBundle == null)
			throw new RuntimeException("no user-ui bundle exists");

		try {
			String result = bundleManager.getEntry(uiBundle.getBundleId(), "WEB-INF/" + filename);
			return new StringReader(result);

		} catch (IOException e) {
			throw new RuntimeException("exception while reading " + filename, e);
		}

	}

	private Reader getReaderFromFsPath(String userUiPath, String filename) {
		try {
			return new InputStreamReader(new FileInputStream(new File(userUiPath, filename)));
		} catch (FileNotFoundException e) {
			return null;
		}
	}
}
