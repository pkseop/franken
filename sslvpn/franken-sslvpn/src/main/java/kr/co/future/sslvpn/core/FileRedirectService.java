package kr.co.future.sslvpn.core;

import java.io.File;

import org.osgi.framework.Bundle;

public interface FileRedirectService {

	String getFileByConfig(Bundle bundle, File baseDir, String fileName);

	String getFile(Bundle bundle, String fileName);
}
