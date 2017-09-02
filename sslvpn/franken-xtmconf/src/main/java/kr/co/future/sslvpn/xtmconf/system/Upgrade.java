package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import kr.co.future.sslvpn.xtmconf.KLogWriter;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Upgrade {
	private final static String baseDirPath = "";
	private final static File baseDir = new File("/");

	private Upgrade() {
	}

	public static String upgrade(File file, String fileName) throws Exception {
		boolean zipLogData = false;
		boolean certData = false;

		if (fileName.endsWith("zip"))
			zipLogData = true;
		else if (fileName.endsWith("der") || fileName.endsWith("crt") || fileName.endsWith("pem") || fileName.endsWith("csr"))
			certData = true;
		else {
			List<Object> fileType = getFileType(file);
			String arch = (String) fileType.get(0);
			String type = (String) fileType.get(1);
			String version = (String) fileType.get(2);

			if (arch.equals("") || type.equals("") || version.equals("")) {
				return putAddLog(fileType, false);
			}
			//if (!arch.equals("ALL") && !arch.equals(getSystemArch())) {
			if (!arch.equals("ALL") && arch.indexOf("X86") == -1) {
				return putAddLog(fileType, false);
			}
		}

		if (zipLogData) {
			File f = new File(baseDirPath + XtmConfig.UTM_LOG + "backup/" + fileName);
			Process p = new ProcessBuilder("mv", file.getAbsolutePath(), f.getAbsolutePath()).directory(baseDir).start();
			p.waitFor();
		} else if (certData) {
			try {
				handleCertData(file);
			} catch (Exception e) {
				KLogWriter.write(0x14030021, null, "Certification Upgrade Failed");
				throw e;
			}
		} else {
			return handleNormalData(file);
		}
		return null;
	}

	public static List<Object> getFileType(File file) throws IOException, RuntimeException {

		byte[] read = new byte[256];
		Checksum crc = new CRC32();
		byte[] crcData = new byte[8192];

		InputStream fis = null;
		try {
			fis = new FileInputStream(file);
			
			// skip header
			fis.read(read);

			int readCount = 0;
			while ((readCount = fis.read(crcData)) != -1) {
				crc.update(crcData, 0, readCount);
			}
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {
			}
		}

		int checksum = (int) crc.getValue();
		ByteBuffer bb = ByteBuffer.allocate(256);
		bb.put(read);
		bb.rewind();

		int magicNumber = bb.getInt();
		bb.position(bb.position() + 20);
		int dcrc = bb.getInt();
		bb.get();
		byte arch = bb.get();
		byte type = bb.get();
		bb.position(bb.position() + 69);
		byte[] verBytes = new byte[32];
		bb.get(verBytes);
		String version = new String(verBytes).trim();

		if (magicNumber != 0x27051956)
			throw new RuntimeException("invalid magic number");

		if (checksum != dcrc)
			throw new RuntimeException("checksum error");

		if (version.indexOf("%$@%") == -1)
			throw new RuntimeException("invalid version");

		List<Object> result = new ArrayList<Object>(4);
		switch (arch) {
		case 0x03:
			result.add("X86");
			break;
		case 0x05:
			result.add("MIPS");
			break;
		case 0x0f:
			result.add("X86_64");
			break;
		case 0x09:
			result.add("ALL");
			break;
		default:
			result.add("");
			break;
		}

		switch (type) {
		case 0x02:
			result.add("firmware");
			break;
		case 0x03:
			result.add("ramdisk");
			break;
		case 0x07:
			result.add("appimg");
			break;
		case 0x09:
			result.add("viruspt");
			break;
		case 0x0a:
			result.add("dpipt");
			break;
		case 0x0b:
			result.add("urldb");
			break;
		case 0x0c:
			result.add("userdb");
			break;
		case 0x0d:
			result.add("malwaredb");
			break;
		case 0x10:
			result.add("jre");
			break;
		case 0x11:
			result.add("sslvpn");
			break;
		default:
			result.add("");
			break;
		}

		String archStr = (String) result.get(0);
		String typeStr = (String) result.get(1);
		if (!archStr.equals("") && !typeStr.equals("")) {
			result.add(version);
			result.add(dcrc);
		} else {
			result.add("");
			result.add(-1);
		}
		return result;
	}

	private static String getSystemArch() throws IOException, InterruptedException {
		Process p = new ProcessBuilder("uname", "-a").directory(baseDir).start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String result = br.readLine();
		if (br != null){
			try {
				br.close();
			} catch (IOException e) {
			}
		}

		int rc = p.waitFor();
		if (rc != 0)
			throw new RuntimeException("failed to get system architecture");

		if (result.indexOf("mips") != -1)
			return "MIPS";
		else if (result.indexOf("x86_64") != -1)
			return "X86_64";
		else
			return "X86";
	}

	private static void handleCertData(File file) throws IOException, InterruptedException, CertificateException {
		File f = new File(baseDirPath + XtmConfig.UTM_LOG + "cert");
		String fileName = file.getName();
		String[] formatArray = fileName.split(".");

		if (!f.exists()) {
			File f2 = new File(baseDir + XtmConfig.UTM_LOG + "cert");
			if (!f2.mkdirs()) {
				throw new RuntimeException("mkdir error: " + f2.getAbsolutePath());
			}
		}

		String uploadFilePath = baseDirPath + XtmConfig.UTM_LOG + "cert/" + fileName;
		File uploadFile = new File(uploadFilePath);

		Process p = new ProcessBuilder("mv", file.getAbsolutePath(), uploadFile.getAbsolutePath()).directory(baseDir).start();
		p.waitFor();

		if (fileName.indexOf("pem") == -1) {
			String format = formatArray[1].toUpperCase();
			Process p2 = new ProcessBuilder("openssl", "x509", "-in", uploadFilePath, "-inform", format, "-out", formatArray[0]
					+ ".pem", "-outform", "PEM", ">", "/dev/null").directory(baseDir).start();
			int rc = p2.waitFor();
			if (rc != 0)
				throw new RuntimeException("failed to execute openssl");

			if (!uploadFile.delete()) {
				throw new RuntimeException("rm error: " + uploadFilePath);
			}
		}

		String fileName2 = formatArray[0] + ".pem";
		File f2 = new File(baseDirPath + XtmConfig.UTM_LOG + "cert/" + fileName2);
		if (f2.exists()) {
			InputStream fis = new FileInputStream(f2);
			byte[] certBytes = new byte[8192];
			fis.read(certBytes);
			fis.close();

			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
			int ca = cert.getBasicConstraints();

			if (ca > 0 && ca < Integer.MAX_VALUE) {
				File f3 = new File(baseDirPath + XtmConfig.UTM_LOG + "cert/ca_cert");
				if (!f3.exists()) {
					if (!f3.mkdirs()) {
						throw new RuntimeException("mkdir error: " + f3.getAbsolutePath());
					}
				}

				File file2 = new File(baseDir + XtmConfig.UTM_LOG + "cert/ca_cert" + fileName2);
				Process p3 = new ProcessBuilder("mv", f2.getAbsolutePath(), file2.getAbsolutePath()).directory(baseDir).start();
				p3.waitFor();
			} else {
				File f3 = new File(baseDirPath + XtmConfig.UTM_LOG + "cert/cert");
				if (!f3.exists()) {
					if (!f3.mkdirs()) {
						throw new RuntimeException("mkdir error: " + f3.getAbsolutePath());
					}
				}
				File file2 = new File(baseDir + XtmConfig.UTM_LOG + "cert/cert" + fileName2);
				Process p3 = new ProcessBuilder("mv", f2.getAbsolutePath(), file2.getAbsolutePath()).directory(baseDir).start();
				p3.waitFor();
			}
		}
		KLogWriter.write(0x14020020, null, "Certification Upgrade Success");
	}

	private static String handleNormalData(File file) throws IOException, InterruptedException, RuntimeException {
		File f = new File(baseDirPath + XtmConfig.UTM_LOG + "upgrade");
		List<Object> fileType = getFileType(file);
		if (f.exists()) {
			File src = UpgradeLib.backupFile(fileType);

			/* kiscom db version check */
			String type = (String) fileType.get(1);
			if (type.equals("urldb")) {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				br.skip(256);
				String oneLine = br.readLine();
				br.close();

				if (oneLine.length() != 42) {
					if (!file.delete()) {
						throw new RuntimeException("rm error: " + file.getAbsolutePath());
					}
					UpgradeLib.sync();
					return null;
				}
			}

			Process p = new ProcessBuilder("mv", file.getAbsolutePath(), src.getAbsolutePath()).directory(baseDir).start();
			p.waitFor();

			if (UpgradeLib.applyToSystem(fileType, src, 1)) {
				return putAddLog(fileType, true);
			} else {
				return putAddLog(fileType, false);
			}

		} else {
			String type = (String) fileType.get(1);
			if (type.equals("firmware") || type.equals("ramdisk")) {
				File destination = null;
				if (type.equals("firmware"))
					destination = new File(baseDirPath + "/mnt/kernel/image1");
				else if (type.equals("ramdisk"))
					destination = new File(baseDirPath + "/mnt/kernel/disk.gz");

				UpgradeLib.copyFile(file, destination);
				file.delete();

				if (UpgradeLib.applyToSystem(fileType, destination)) {
					return putAddLog(fileType, true);
				} else {
					return putAddLog(fileType, false);
				}
			} else {
			}
			return null;
		}
	}

	// TODO: You'll modified stub method.
	private static String putAddLog(List<Object> fileType, boolean flag) {
		String type = fileType.get(1).toString();

		if (flag) {
			if (type.equals("firmware"))
				KLogWriter.write(0x14020010, null, "Firmware Upgrade Success");
			else if (type.equals("ramdisk"))
				KLogWriter.write(0x14020012, null, "Ramdisk Upgrade Success");
			else if (type.equals("appimg"))
				KLogWriter.write(0x14020014, null, "Appimg Upgrade Success");
			else if (type.equals("jre"))
				KLogWriter.write(0x14020022, null, "JRE Upgrade Success");
			else if (type.equals("sslvpn"))
				KLogWriter.write(0x14020024, null, "SSL VPN Upgrade Success");

			return "Upgrade Succeed";
		}

		if (type.equals("firmware"))
			KLogWriter.write(0x14030011, null, "Firmware Upgrade Failed");
		else if (type.equals("ramdisk"))
			KLogWriter.write(0x14030013, null, "Ramdisk Upgrade Failed");
		else if (type.equals("appimg"))
			KLogWriter.write(0x14030015, null, "Appimg Upgrade Failed");
		else if (type.equals("jre"))
			KLogWriter.write(0x14040023, null, "JRE Upgrade Failed");
		else if (type.equals("sslvpn"))
			KLogWriter.write(0x14040025, null, "SSL VPN Upgrade Failed");

		return "Upgrade Failed";
	}
}