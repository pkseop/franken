
package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class UpgradeLib {
	private final static File baseDir = new File("/");

	private UpgradeLib() {
	}

	public static File backupFile(List<Object> fileType) throws IOException, InterruptedException, RuntimeException {
		File f = new File(XtmConfig.UTM_LOG + "upgrade/backup_db");
		File[] files = f.listFiles();

		List<File> fileList = new ArrayList<File>();
		for (File file : files) {
			String type = (String) fileType.get(1);
			if (file.getName().indexOf(type) != -1)
				fileList.add(file);
		}

		File oldestFile = null;
		long fileDate = Long.MAX_VALUE;

		if (fileList.size() >= 7) {
			for (File file : fileList) {
				long modified = file.lastModified();
				if (fileDate > modified) {
					fileDate = modified;
					oldestFile = file;
				}
			}
		}

		if (oldestFile != null) {
			if (!oldestFile.delete()) {
				throw new RuntimeException("rm error: " + oldestFile.getAbsolutePath());
			}
		}

		String version = (String) fileType.get(2);
		return new File(XtmConfig.UTM_LOG + "upgrade/backup_db/" + (String) fileType.get(1) + "-" + version.substring(6));
	}

	public static boolean applyToSystem(List<Object> fileType, File src, int dpiType) throws IOException, InterruptedException {
		final Logger logger = LoggerFactory.getLogger(UpgradeLib.class.getName());

		boolean retVal = true;
		String saveDir = XtmConfig.UTM_LOG + "upgrade";

		File tar;
		String arch = (String) fileType.get(0);
		String type = (String) fileType.get(1);

		logger.info("frodo xtmconf: upgrade file arch [{}], type [{}]", arch, type);

		if ((arch.equals("MIPS") && type.equals("firmware")) || (arch.equals("MIPS") && type.equals("ramdisk"))) {
			tar = src;
		} else {
			tar = new File(saveDir + "/tmp.tar.gz");
			InputStream fis = new FileInputStream(src);
			OutputStream fos = new FileOutputStream(tar);

			fis.skip(256);

			byte[] b = new byte[256];
			int readLength = 0;
			while (true) {
				readLength = fis.read(b);
				if (readLength < 0)
					break;

				fos.write(b, 0, readLength);
			}

			fos.close();
			fis.close();
		}

		/* execute command for architecture */
		if (type.equals("firmware")) {
			File f = new File("/mnt/kernel/image2");
			File f2 = new File("/mnt/kernel/image1");

			boolean result = executeCommands(tar, f, f2, fileType, arch);
			if (!result) {
				logger.error("frodo xtmconf: firmware upgrade failed");
				return false;
			}
		} else if (type.equals("ramdisk")) {
			File f = new File("/mnt/kernel/disk2.gz");
			File f2 = new File("/mnt/kernel/disk.gz");

			boolean result = executeCommands(tar, f, f2, fileType, arch);
			if (!result) {
				logger.error("frodo xtmconf: ramdisk upgrade failed");
				return false;
			}
		} else if (type.equals("appimg")) {
			executeCommandsForAppImg(tar);
		} else if (type.equals("viruspt")) {
			executeCommandsForVirusPt();
		} else if (type.equals("dpipt")) {
			if (!tar.delete())
				throw new RuntimeException("rm error: " + tar.getAbsolutePath());
		} else if (type.equals("urldb")) {
			File f = new File(saveDir + "/xdb/kiscom_db.txt");
			Process p = new ProcessBuilder("mv", tar.getAbsolutePath(), f.getAbsolutePath()).directory(baseDir).start();
			p.waitFor();
		} else if (type.equals("userdb")) {
			File f = new File(saveDir + "/sav");
			if (!f.delete())
				throw new RuntimeException("rm error: " + f.getAbsolutePath());

			Process p = new ProcessBuilder("tar", "xf", "tmp.tar.gz", "-C", XtmConfig.UTM_LOG + "url").directory(
					new File(saveDir)).start();
			p.waitFor();
			sync();
		} else if (type.equals("malwaredb")) {
			Process p = new ProcessBuilder("dos2unix", tar.getAbsolutePath()).directory(baseDir).start();
			p.waitFor();

			File f = new File(XtmConfig.UTM_LOG + "url/result_malware_urlx.txt");
			Process p2 = new ProcessBuilder("mv", tar.getAbsolutePath(), f.getAbsolutePath()).directory(baseDir).start();
			p2.waitFor();

			refreshUrlDB();
		} else if (type.equals("jre")) {
			File f = new File("/usr/sslplus/jre/");
			f.mkdirs();
			process(f, "rm", "-rf", "/usr/sslplus/jre/");
			f.mkdirs();
			process(f, "tar", "xzpf", tar.getAbsolutePath());
		} else if (type.equals("sslvpn")) {
			File f = new File("/usr/sslplus/");
			f.mkdir();
			process(f, "rm", "-rf", "/usr/sslplus/kraken/bin/");
			process(f, "rm", "-rf", "/usr/sslplus/kraken/cache/");
			process(f, "rm", "-rf", "/usr/sslplus/frodo/");
			process(f, "rm", "-rf", "/usr/sslplus/xenics/");
			process(f, "tar", "xzpf", tar.getAbsolutePath());
		}
		saveCurrentVersion(fileType);
		return retVal;
	}

	private static void process(File dir, String... commands) throws IOException, InterruptedException {
		Process p = new ProcessBuilder(commands).directory(dir).start();
		p.waitFor();
	}

	public static boolean applyToSystem(List<Object> fileType, File uploadFile) throws IOException, InterruptedException {
		String saveDir = "/mnt/kernel";
		mountMsdosPartition();

		String arch = (String) fileType.get(0);
		String type = (String) fileType.get(1);

		File srcFile;
		if ((arch.equals("MIPS") && type.equals("firmware")) || (arch.equals("MIPS") && type.equals("ramdisk"))) {
			srcFile = uploadFile;
		} else {
			srcFile = new File(saveDir + "/tmp.tar.gz");
			InputStream fis = new FileInputStream(uploadFile);
			OutputStream fos = new FileOutputStream(srcFile);

			fis.skip(256);

			byte[] b = new byte[256];
			int readLength = 0;
			while (true) {
				readLength = fis.read(b);
				if (readLength < 0)
					break;

				fos.write(b, 0, readLength);
			}

			fos.close();
			fis.close();
		}

		boolean retVal;
		if (type.equals("firmware")) {
			copyFile(srcFile, new File("/mnt/kernel/image1"));
			srcFile.delete();

			sync();
			File f = new File("/mnt/kernel/image1");
			retVal = checkCrc(fileType, f);
			if (!retVal) {
				copyFile(new File("/mnt/kernel/image2"), f);
				sync();
				umount();
				return retVal;
			}
		} else {
			copyFile(srcFile, new File("/mnt/kernel/disk.gz"));
			srcFile.delete();

			sync();
			File f = new File("/mnt/kernel/disk.gz");
			retVal = checkCrc(fileType, f);
			if (!retVal) {
				copyFile(new File("/mnt/kernel/disk2.gz"), f);
				sync();
				umount();
				return retVal;
			}
		}
		umount();
		saveCurrentVersion(fileType);
		return true;
	}

	public static void copyFile(File src, File dest) throws IOException {
		final Logger logger = LoggerFactory.getLogger(UpgradeLib.class.getName());

		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(src);
			fos = new FileOutputStream(dest);
			FileChannel fc = fis.getChannel();

			logger.info("frodo xtmconf: copying file from [{}] to [{}], size [{}]",
					new Object[] { src.getAbsolutePath(), dest.getAbsolutePath(), fc.size() });
			fc.transferTo(0, fc.size(), fos.getChannel());
		} finally {
			if (fos != null)
				fos.close();
			if (fis != null)
				fis.close();
		}
	}

	private static boolean executeCommands(File uploadFile, File rollbackFile, File targetFile, List<Object> fileType, String arch)
			throws IOException, InterruptedException {
		final Logger logger = LoggerFactory.getLogger(UpgradeLib.class.getName());
		mountMsdosPartition();

		String rollbackPath = rollbackFile.getAbsolutePath();
		String targetPath = targetFile.getAbsolutePath();

		// delete old rollback file, and make new rollback file
		rollbackFile.delete();
		Process p = new ProcessBuilder("mv", targetPath, rollbackPath).directory(baseDir).start();
		int ret = p.waitFor();
		if (ret == 0) {
			logger.info("frodo xtmconf: created rollback file [{}] using [{}]", rollbackPath, targetPath);
		} else {
			logger.info("frodo xtmconf: failed to create rollback file [{}] using [{}]", rollbackPath, targetPath);
		}

		if (arch.equals("X86") || arch.equals("X86_64")) {
			copyFile(uploadFile, new File(targetPath));
			if (!uploadFile.delete()) {
				logger.error("frodo xtmconf: cannot delete file [{}]", uploadFile.getAbsolutePath());
				throw new RuntimeException("rm error: " + uploadFile.getAbsolutePath());
			}
		} else if (arch.equals("MIPS")) {
			copyFile(uploadFile, new File(targetPath));
		}
		sync();

		if (!checkCrc(fileType, new File(targetPath))) {
			logger.error("frodo xtmconf: crc check failed, path [{}]", targetPath);

			// revert file
			File oldFile = new File(rollbackPath);
			if (oldFile.exists())
				copyFile(new File(rollbackPath), new File(targetPath));

			sync();
			umount();
			return false;
		}
		umount();
		return true;
	}

	private static void executeCommandsForAppImg(File srcFile) throws IOException, InterruptedException {
		final Logger logger = LoggerFactory.getLogger(UpgradeLib.class.getName());
		File f = new File("/usr/local/etc/usr_script");
		File usrScript = new File("/tmp/usr_script");

		Process p = new ProcessBuilder("mv", f.getAbsolutePath(), usrScript.getAbsolutePath()).directory(baseDir).start();
		int ret = p.waitFor();
		if (ret == 0) {
			logger.info("frodo xtmconf: moved [{}] to [{}] successfully", f.getAbsolutePath(), usrScript.getAbsolutePath());
		} else {
			logger.info("frodo xtmconf: failed to move [{}] to [{}]", f.getAbsolutePath(), usrScript.getAbsolutePath());
		}

		File f2 = new File("/usr/local/key/ekey");
		if (f2.exists()) {
			File f3 = new File("/usr/local/key");
			File f4 = new File(XtmConfig.UTM_LOG + "tmp/key");

			Process p1_1 = new ProcessBuilder("mv", f3.getAbsolutePath(), f4.getAbsolutePath()).directory(baseDir).start();
			ret = p1_1.waitFor();
			if (ret == 0) {
				logger.info("frodo xtmconf: moved [{}] to [{}] successfully", f3.getAbsolutePath(), f4.getAbsolutePath());
			} else {
				logger.info("frodo xtmconf: failed to move [{}] to [{}]", f3.getAbsolutePath(), f4.getAbsolutePath());
			}
		}

		File f3 = new File("/usr/local/etc/pair");
		File f4 = new File("/tmp/pair");

		Process p1_1 = new ProcessBuilder("mv", f3.getAbsolutePath(), f4.getAbsolutePath()).directory(baseDir).start();
		ret = p1_1.waitFor();
		if (ret == 0) {
			logger.info("frodo xtmconf: moved [{}] to [{}] successfully", f3.getAbsolutePath(), f4.getAbsolutePath());
		} else {
			logger.error("frodo xtmconf: failed to move [{}] to [{}]", f3.getAbsolutePath(), f4.getAbsolutePath());
		}

		/* backup "/usr/local" directory */
		Process p0 = new ProcessBuilder("cp", "-rf", "/usr/local/", XtmConfig.UTM_LOG + "tmp").directory(baseDir).start();
		ret = p0.waitFor();
		if (ret == 0) {
			logger.info("frodo xtmconf: copied [{}] to [{}] successfully", "/usr/local/", XtmConfig.UTM_LOG + "tmp");
		} else {
			logger.error("frodo xtmconf: failed to move [{}] to [{}]", "/usr/local/", XtmConfig.UTM_LOG + "tmp");
		}

		Process p1 = new ProcessBuilder("rm", "-rf", "/usr/local/").directory(baseDir).start();
		ret = p1.waitFor();
		if (ret == 0) {
			logger.info("frodo xtmconf: removed [{}] directory", "/usr/local/");
		} else {
			logger.error("frodo xtmconf: failed to remove [{}] directory", "/usr/local/");
		}

		Process p2 = new ProcessBuilder("rm", "-rf", XtmConfig.UTM_LOG + "tmp/local").directory(baseDir).start();
		ret = p2.waitFor();
		if (ret == 0) {
			logger.info("frodo xtmconf: removed [{}] directory", XtmConfig.UTM_LOG + "tmp/local");
		} else {
			logger.error("frodo xtmconf: failed to remove [{}] directory", XtmConfig.UTM_LOG + "tmp/local");
		}

		Process p3 = new ProcessBuilder("tar", "xzf", srcFile.getName(), "-C", "/usr/local/").directory(
				new File(srcFile.getParent())).start();
		ret = p3.waitFor();
		if (ret == 0) {
			logger.info("frodo xtmconf: untar file [{}] to directory [{}] successfully", srcFile.getName(), "/usr/local/");
		} else {
			logger.error("frodo xtmconf: failed to untar file [{}] to directory [{}]", srcFile.getName(), "/usr/local/");
		}

		if (!srcFile.delete()) {
			logger.error("frodo xtmconf: cannot delete file [{}]", srcFile.getAbsolutePath());
			throw new RuntimeException("rm error: " + srcFile.getAbsolutePath());
		}

		sync();

		File f5 = new File("/tmp/usr_script");
		File file5 = new File("/usr/local/etc/usr_script");

		Process p4 = new ProcessBuilder("mv", f5.getAbsolutePath(), file5.getAbsolutePath()).directory(baseDir).start();
		ret = p4.waitFor();
		if (ret == 0)
			logger.info("frodo xtmconf: moved [{}] to [{}] successfully", f5.getAbsolutePath(), file5.getAbsolutePath());
		else
			logger.error("frodo xtmconf: failed to move [{}] to [{}]", f5.getAbsolutePath(), file5.getAbsolutePath());

		File f6 = new File(XtmConfig.UTM_LOG + "tmp/key");
		if (f6.exists()) {
			for (File file : f6.listFiles()) {
				File newFile = new File("/usr/local/key/" + file.getName());
				Process p4_1 = new ProcessBuilder("mv", file.getAbsolutePath(), newFile.getAbsolutePath()).directory(baseDir)
						.start();
				ret = p4_1.waitFor();
				if (ret == 0)
					logger.info("frodo xtmconf: moved [{}] to [{}] successfully", file.getAbsolutePath(),
							newFile.getAbsolutePath());
				else
					logger.error("frodo xtmconf: failed to move [{}] to [{}]", file.getAbsolutePath(), newFile.getAbsolutePath());
			}

			if (!f6.delete()) {
				logger.error("frodo xtmconf: failed to remove [{}] file", f6.getAbsolutePath());
				throw new RuntimeException("rm error: " + f6.getAbsolutePath());
			}
		}

		File f7 = new File("/tmp/pair");
		File f8 = new File("/usr/local/etc/pair");

		Process p5 = new ProcessBuilder("mv", f7.getAbsolutePath(), f8.getAbsolutePath()).directory(baseDir).start();
		ret = p5.waitFor();
		if (ret == 0)
			logger.info("frodo xtmconf: moved [{}] to [{}] successfully", f7.getAbsolutePath(), f8.getAbsolutePath());
		else
			logger.error("frodo xtmconf: failed to move [{}] to [{}]", f7.getAbsolutePath(), f8.getAbsolutePath());
	}

	private static void executeCommandsForVirusPt() throws IOException, InterruptedException {
		File f = new File(XtmConfig.UTM_LOG + "upgrade/sav");
		if (f.exists())
			if (!f.delete())
				throw new RuntimeException("rm error: " + f.getAbsolutePath());
		if (!f.mkdirs())
			throw new RuntimeException("mkdir error: " + f.getAbsolutePath());

		Process p = new ProcessBuilder("tar", "xvf", "tmp.tar.gz", "-C", "sav").directory(
				new File(XtmConfig.UTM_LOG + "upgrade/")).start();
		p.waitFor();

		File f2 = new File(XtmConfig.UTM_LOG + "upgrade/tmp.tar.gz");
		if (!f2.delete())
			throw new RuntimeException("rm error: " + f2.getAbsolutePath());
	}

	private static void mountMsdosPartition() throws IOException, InterruptedException {
		File fn = new File("/etc/firmdev");
		if (!fn.exists())
			return;

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fn)));
		String partition = br.readLine().trim();
		br.close();

		Process p = new ProcessBuilder("mount", "-t", "msdos", partition, "/mnt/kernel").directory(baseDir).start();
		p.waitFor();
	}

	private static boolean checkCrc(List<Object> fileType, File file) throws IOException {
		int fileSize = (int) file.length();
		InputStream fis = new FileInputStream(file);
		String arch = (String) fileType.get(0);
		byte[] crcData;
		if (arch.equals("MIPS")) {
			fis.skip(256);
			crcData = new byte[fileSize - 256];
		} else
			crcData = new byte[fileSize];

		fis.read(crcData);
		fis.close();

		Checksum crc = new CRC32();
		crc.update(crcData, 0, crcData.length);
		int checksum = (int) crc.getValue();

		return checksum == (Integer) fileType.get(3);
	}

	private static void refreshUrlDB() throws IOException, InterruptedException {
		File f = new File(XtmConfig.UTM_LOG + "upgrade/xdb/url_db.txt");
		if (!f.delete()) {
			throw new RuntimeException("rm error: " + f.getAbsolutePath());
		}
		sync();
	}

	private static void saveCurrentVersion(List<Object> fileType) throws IOException {
		String saveDir = XtmConfig.UTM_LOG + "upgrade/backup_db";
		String verInfo = saveDir + "/versioninfo";
		String firmware = "";
		String ramdisk = "";
		String appimg = "";
		String dpipt = "";
		String viruspt = "";
		String urldb = "";
		String malwaredb = "";
		String jre = "";
		String sslvpn = "";

		File f = new File(verInfo);
		if (!f.exists()) {
			StringBuilder builder = new StringBuilder();
			builder.append("firmware	: " + firmware + "\n");
			builder.append("ramdisk		: " + ramdisk + "\n");
			builder.append("appimg		: " + appimg + "\n");
			builder.append("dpipt		: " + dpipt + "\n");
			builder.append("viruspt		: " + viruspt + "\n");
			builder.append("urldb		: " + urldb + "\n");
			builder.append("malwaredb	: " + malwaredb + "\n");
			builder.append("jre			: " + jre + "\n");
			builder.append("sslvpn		: " + sslvpn + "\n");

			OutputStream fos = new FileOutputStream(f);
			fos.write(builder.toString().getBytes());
			fos.close();
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		String line;
		while ((line = br.readLine()) != null) {
			String[] str = line.split(":");
			String key = str[0].trim();
			String value = "";
			if (str[1] != null) value = str[1].trim();

			String type = (String) fileType.get(1);
			if (key.equals(type)) {
				String version = (String) fileType.get(2);
				value = version.substring(6);
			}

			if (key.equals("firmware"))
				firmware = value;
			else if (key.equals("ramdisk"))
				ramdisk = value;
			else if (key.equals("appimg"))
				appimg = value;
			else if (key.equals("dpipt"))
				dpipt = value;
			else if (key.equals("viruspt"))
				viruspt = value;
			else if (key.equals("urldb"))
				urldb = value;
			else if (key.equals("malwaredb"))
				malwaredb = value;
			else if (key.equals("jre"))
				jre = value;
			else if (key.equals("sslvpn"))
				sslvpn = value;
		}
		if (br != null)
			br.close();

		StringBuilder builder = new StringBuilder();
		builder.append("firmware	: " + firmware + "\n");
		builder.append("ramdisk		: " + ramdisk + "\n");
		builder.append("appimg		: " + appimg + "\n");
		builder.append("dpipt		: " + dpipt + "\n");
		builder.append("viruspt		: " + viruspt + "\n");
		builder.append("urldb		: " + urldb + "\n");
		builder.append("malwaredb	: " + malwaredb + "\n");
		builder.append("jre			: " + jre + "\n");
		builder.append("sslvpn		: " + sslvpn + "\n");

		OutputStream fos = new FileOutputStream(f);
		fos.write(builder.toString().getBytes());
		fos.close();
	}

	public static void sync() throws IOException, InterruptedException {
		final Logger logger = LoggerFactory.getLogger(UpgradeLib.class.getName());
		Process p = new ProcessBuilder("sync").directory(baseDir).start();
		int ret = p.waitFor();
		if (ret == 0)
			logger.info("frodo xtmconf: sync completed");
		else
			logger.info("frodo xtmconf: sync failed");
	}

	private static void umount() throws IOException, InterruptedException {
		final Logger logger = LoggerFactory.getLogger(UpgradeLib.class.getName());
		Process p = new ProcessBuilder("umount", "/mnt/kernel").directory(baseDir).start();
		int ret = p.waitFor();
		if (ret == 0)
			logger.info("frodo xtmconf: unmount completed");
		else
			logger.error("frodo xtmconf: unmount failed");
	}

	public static boolean deleteDirectory(File path) throws IOException, InterruptedException {
		Process p = new ProcessBuilder("rm", "-rf", path.getAbsolutePath()).start();
		p.waitFor();
		return true;
	}
}