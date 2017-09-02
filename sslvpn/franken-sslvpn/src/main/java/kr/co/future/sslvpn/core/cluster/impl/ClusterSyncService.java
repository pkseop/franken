package kr.co.future.sslvpn.core.cluster.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.cluster.ClusterConfig;
import kr.co.future.sslvpn.core.cluster.ClusterContext;
import kr.co.future.sslvpn.core.cluster.ClusterEventListener;
import kr.co.future.sslvpn.core.cluster.ClusterNode;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.model.DataImporter;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.xtmconf.SSLTrustHelper;
import kr.co.future.sslvpn.xtmconf.sql.csv.ImportCsvSqlDb;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;
import kr.co.future.sslvpn.core.cluster.ClusterSync;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.ca.CertificateAuthorityService;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.FileUploadApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

@Component(name = "frodo-cluster-sync-service")
@Provides
public class ClusterSyncService implements ClusterSync, ClusterEventListener {
	private final Logger logger = LoggerFactory.getLogger(ClusterSyncService.class.getName());
	
	public final static String NAC_FILE_PREFIX = "NAC_FILE_";

	private final String TMP_DIR = "/utm/log/tmp";

	@Requires
	private ConfigService conf;

	@Requires
	private ClusterService cluster;

	@Requires
	private AuthorizedDeviceApi deviceApi;

	@Requires
	private FileUploadApi uploadApi;

	@Requires
	private CertificateAuthorityService ca;
	
	@Requires
	private GlobalConfigApi configApi;

	@Validate
	public void start() {
		cluster.addEventListener(this);
		logger.info("frodo core: cluster sync service started");

		ClusterNode master = cluster.getClusterMaster();
		if (cluster.getConfig().isEnabled() && master != null)
			requestMasterData(master);
	}

	@Invalidate
	public void stop() {
		if (cluster != null)
			cluster.removeEventListener(this);

		logger.info("frodo core: cluster sync service stopped");
	}

	@Override
	public void onMasterChange(ClusterNode master) {
		// skip if current node is master
		if (master == null)
			return;

		requestMasterData(master);
	}
	
	private void requestMasterData(ClusterNode master) {
		requestNodeData(master.getAddress().getAddress().getHostAddress());
	}

	@Override
	public void requestNodeData(String hostAddress) {
		Boolean synUserDataOnly = false;
		Boolean syncExcludeUserData = false;
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
		Config c = db.findOne(ClusterConfig.class, null);
		if (c != null) {
			ClusterConfig config = c.getDocument(ClusterConfig.class);
			synUserDataOnly = config.getSyncUserDataOnly() == null ? false : config.getSyncUserDataOnly();
			syncExcludeUserData = config.getSyncExcludeUserData() == null ? false : config.getSyncExcludeUserData();
		}
		
		HttpURLConnection con = null;
		final String nodeAddress = hostAddress;
		String password = cluster.getConfig().getSharedKey();
		String nodeUrl = "http://" + nodeAddress + "/policy/sync?type=cluster&shared_key=" + password;

		long start = new Date().getTime();
		logger.info("frodo core: sync'ing master database, url [{}]", nodeUrl);

		File unzipDir = new File(TMP_DIR, "localhost");
		String[] dbNames = { /*"kraken-dom-localhost",*/ "frodo" };
		Map<String, File> masterDbFiles = new HashMap<String, File>();
		for (String dbName : dbNames)
			masterDbFiles.put(dbName, new File(unzipDir, dbName + ".cdb"));
		File nodePem = new File(unzipDir, "local.pem");
		File authority = new File(unzipDir, "authority.cdb");
		File deviceFile = new File(unzipDir, "devices.tmp");// 하위 호환성을 위해 남겨둠
		InputStream is = null;
		try {
			ClusterContext.replicated.set(true);
			con = connect(nodeUrl);
			if (con.getResponseCode() != 200) {
				logger.error("frodo core: invalid shared key, status code=" + con.getResponseCode());
				return;
			}

			CommandUtil.run("rm", "-rf", unzipDir.getAbsolutePath());
			unzipDir.mkdir();

			unzipFiles(unzipDir, con.getInputStream());
			
			if(syncExcludeUserData == false) {
				ImportCsvSqlDb importCsv = new ImportCsvSqlDb();
				List<String> importedFiles = importCsv.importCsv(unzipDir.getAbsolutePath());
				for(String file : importedFiles) {
					File f = new File(file);
					if(f.exists())
						f.delete();
				}
			}
			
			if(synUserDataOnly == false) {	//true일 경우엔 사용자 정보만 동기화되도록
				for (String dbName : masterDbFiles.keySet()) {
					File cdbFile = masterDbFiles.get(dbName);
					if (cdbFile.exists() && cdbFile.length() != 0) {
						logger.info("frodo core: import db [{}]", dbName);
						DataImporter.importData(conf, dbName, cdbFile);
						cdbFile.delete();
					} else {
						// 펌웨어 버전이 맞지 않아서 CDB 파일이 존재하지 않을 가능성이 존재함.
						logger.warn("frodo core: cdb file [{}] does not exist, firmware version mismatch!", cdbFile.getName());
					}
				}
				
				//clustering files for NAC
				locateNACFiles(unzipDir.getAbsolutePath());
				
				// 하위 버전과의 동기화를 위해 남겨둠. 새 버전에는 해당 파일이 존재하지 않음
				if (deviceFile.exists()) {
					logger.info("frodo core: import device, file size [{}]", deviceFile.length());
					DataImporter.importDevices(conf, deviceApi, deviceFile);
					deviceFile.delete();
				}
	
				if (authority.exists() && authority.length() != 0) {
					is = new FileInputStream(authority);
					if (ca.getAuthority("local") != null) {
						logger.info("frodo core: dropping certificate authority");
						ca.removeAuthority("local");
					}
					logger.info("frodo core: import local authority, file size [{}]", authority.length());
					ca.importAuthority("local", is);
					authority.delete();
				}
	
				if (nodePem.exists() && nodePem.length() != 0) {
					File localPem = new File("/utm/log/kraken/data/kraken-ca/CA/local/local.pem");
					if (!canIgnorePemFileUpdate(nodePem, localPem)) {
						logger.info("frodo core: replace master pem file to local pem file, file size [{}]", nodePem.length());
						CommandUtil.run(unzipDir, "cp", "-rf", nodePem.getAbsolutePath(),
								"/utm/log/kraken/data/kraken-ca/CA/local/");
						CommandUtil.run("killall", "marlin");
					}
					nodePem.delete();
				}
			}

			// copy upload files
			File baseDirectory = uploadApi.getBaseDirectory("localhost");
			CommandUtil.run("cp", "-rf", unzipDir.getAbsolutePath(), baseDirectory.getParentFile().getAbsolutePath());
			CommandUtil.run("rm", "-rf", unzipDir.getAbsolutePath());

			logger.info("frodo core: completed master database sync");
			if (logger.isDebugEnabled())
				logger.debug("frodo core: cluster sync comlete, [{}] ms elapsed", new Date().getTime() - start);
		} catch (IOException e) {
			logger.error("frodo core: master database sync failed", e);
		} finally {
			ClusterContext.replicated.set(false);
			if (con != null)
				con.disconnect();

			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

	}

	private void unzipFiles(File unzipDir, InputStream is) throws IOException, FileNotFoundException {
		ZipInputStream zis = new ZipInputStream(is);
		try {
			while (true) {
				ZipEntry e = zis.getNextEntry();
				if (e == null)
					break;

				logger.trace("frodo core: unzipping cluster sync'd zip entry [{}]", e.getName());

				if (e.isDirectory()) {
					File newDir = new File(unzipDir, e.getName());
					newDir.mkdirs();
					logger.trace("frodo core: create new directory [{}]", newDir.getAbsolutePath());
					continue;
				}

				byte[] b = new byte[8096];
				File f = new File(unzipDir, e.getName());
				logger.trace("frodo core: extracting file [{}]", f.getAbsolutePath());
				FileOutputStream os = new FileOutputStream(f);

				try {
					while (true) {
						int read = zis.read(b);
						if (read <= 0)
							break;
						os.write(b, 0, read);
					}
				} finally {
					zis.closeEntry();
					os.close();
				}
			}
		} finally {
			zis.close();
		}
	}

	private HttpURLConnection connect(String maserUrl) throws IOException {
		SSLTrustHelper.trustAll();
		URL url = new URL(maserUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setConnectTimeout(10000);
		connection.setReadTimeout(120000);
		connection.setAllowUserInteraction(false);
		return connection;
	}

	private boolean canIgnorePemFileUpdate(File masterPem, File localPem) {
		byte[] masterBlob = readBlob(masterPem);
		if (masterBlob == null)
			return true;
		byte[] localBlob = readBlob(localPem);
		if (localBlob == null)
			return false;

		return Arrays.equals(masterBlob, localBlob);
	}

	private byte[] readBlob(File f) {
		ByteArrayOutputStream baos = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			baos = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];
			int readLength = 0;
			while ((readLength = fis.read(buf)) != -1)
				baos.write(buf, 0, readLength);

			baos.flush();

			return baos.toByteArray();
		} catch (IOException e) {
			logger.error("frodo core: cannot read file [{}]", f.getAbsolutePath(), e);
			return null;
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e1) {
				}
			if (baos != null)
				try {
					baos.close();
				} catch (IOException e) {
				}
		}
	}
	
	private void locateNACFiles(String unzipPath) {
		if(Strings.isNullOrEmpty(unzipPath))
			return;
		
		GlobalConfig config = configApi.getGlobalConfig();
		String userUiPath = config.getUserUiPath();
		if(Strings.isNullOrEmpty(userUiPath)) {
			userUiPath = System.getProperty("kraken.home.dir") + "/resources/";
			new File(userUiPath).mkdirs();
			config.setUserUiPath(userUiPath);
			configApi.setGlobalConfig(config);
		}

		String nacDir = userUiPath + "/NAC/";
		new File(nacDir).mkdirs();
				
		File path = new File(unzipPath);
		File[] files = path.listFiles();
		if(files != null) {
			for(File file : files) {
				String fileName = file.getName();
				if(fileName.startsWith(NAC_FILE_PREFIX)) {
					fileName = fileName.replace(NAC_FILE_PREFIX, "");
					file.renameTo(new File(nacDir, fileName));
				}
			}
		}
	}
}
