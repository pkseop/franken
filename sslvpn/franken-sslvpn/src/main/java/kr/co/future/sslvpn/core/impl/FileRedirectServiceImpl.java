package kr.co.future.sslvpn.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import kr.co.future.sslvpn.core.FileRedirectService;
import kr.co.future.sslvpn.core.impl.FileRedirectServiceImpl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-file-redirect-service")
@Provides
public class FileRedirectServiceImpl implements FileRedirectService {
	private final Logger logger = LoggerFactory.getLogger(FileRedirectServiceImpl.class.getName());

	@Override
	public String getFile(Bundle bundle, String fileName) {
		URL url = bundle.getEntry("/WEB-INF/" + fileName);
		byte[] b = new byte[4096];
		InputStream is = null;
		StringBuilder sb = new StringBuilder();

		try {
			is = url.openStream();

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("frodo core: cannot load " + fileName, e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return sb.toString();
	}
	
	
	
	@Override
	public String getFileByConfig(Bundle bundle, File baseDir, String fileName) {
		File file = new File(baseDir, fileName);
		if (!file.exists())
			return null;

		InputStream is = null;
		byte[] b = new byte[4096];
		StringBuilder sb = new StringBuilder();

		try {
			is = new FileInputStream(file);

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("frodo core: cannot load " + fileName, e);
			return null;
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return sb.toString();
	}

}
