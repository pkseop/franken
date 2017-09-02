package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {
	private ZipUtil() {
	}

	public static void unzip(File zipFile, String unzipPath) throws IOException {
		final int BUFFER_SIZE = 2048;
		InputStream fis = new FileInputStream(zipFile);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

		BufferedOutputStream dest = null;
		ZipEntry entry = null;
		while ((entry = zis.getNextEntry()) != null) {
			int count;
			if (entry.isDirectory()) {
				new File(unzipPath, entry.getName()).mkdirs();
			} else {
				byte data[] = new byte[BUFFER_SIZE];
				OutputStream fos = new FileOutputStream(unzipPath + entry.getName());
				dest = new BufferedOutputStream(fos, BUFFER_SIZE);
				while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
		}
		zis.close();
	}
}