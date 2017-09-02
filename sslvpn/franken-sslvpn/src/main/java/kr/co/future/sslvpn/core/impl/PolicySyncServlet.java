package kr.co.future.sslvpn.core.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.cluster.ClusterConfig;
import kr.co.future.sslvpn.core.cluster.ClusterContext;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.model.DataExporter;
import kr.co.future.sslvpn.model.DataImporter;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.xtmconf.KLogWriter;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.ha.SyncPolicySynchronize;
import kr.co.future.sslvpn.xtmconf.sql.csv.ExportCsvSqlDb;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;
import kr.co.future.sslvpn.xtmconf.system.PolicySync;

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
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.sslvpn.core.cluster.impl.ClusterSyncService;
import kr.co.future.sslvpn.core.impl.PolicySyncServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-policy-sync-servlet")
@Provides(specifications = { Runnable.class })
public class PolicySyncServlet extends HttpServlet implements Runnable {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(PolicySyncServlet.class.getName());
	private final String TMP_DIR = "/utm/log/tmp";
	private final String TMP_XML_DIR = "/utm/log/tmp/object/xml";

	@Requires
	private HttpService httpd;

	@Requires
	private ConfigService conf;

	@Requires
	private AuthorizedDeviceApi deviceApi;

	@Requires
	private FileUploadApi uploadApi;

	@Requires
	private ClusterService cluster;

	@Requires
	private CertificateAuthorityService ca;
	
	@Requires
	private GlobalConfigApi configApi;

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("policysync", this, "/policy/sync");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("policysync");
		}
	}

	@Override
	public void run() {
		try {
			logger.trace("frodo core: start policy sync");
			PolicySync.sync();
			logger.trace("frodo core: policy sync completed");
		} catch (Throwable t) {
			logger.error("frodo core: policy sync error", t);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String type = req.getParameter("type");
		String passwd = req.getParameter("shared_key");

		if (type == null) {
			resp.sendError(500);
			return;
		}

		if (!passwd.equals(cluster.getConfig().getSharedKey())) {
			resp.sendError(500);
			return;
		}

		if (type.equals("cluster")) {
			synchronized (this) {
				Boolean synUserDataOnly = false;
				Boolean syncExcludeUserData = false;
				ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
				Config c = db.findOne(ClusterConfig.class, null);
				if (c != null) {
					ClusterConfig config = c.getDocument(ClusterConfig.class);
					synUserDataOnly = config.getSyncUserDataOnly() == null ? false : config.getSyncUserDataOnly();
					syncExcludeUserData = config.getSyncExcludeUserData() == null ? false : config.getSyncExcludeUserData();
				}
				
				Date begin = new Date();
				logger.info("frodo core: master database replication started");

				Map<String, File> masterDbFiles = new HashMap<String, File>();
				String[] dbNames = { /*"kraken-dom-localhost",*/ "frodo" };
				for (String dbName : dbNames)
					masterDbFiles.put(dbName, new File(TMP_DIR, dbName + ".cdb"));
				
				List<String> csvFilesNames = null;
				
				File authority = new File(TMP_DIR, "authority.cdb");
				if (authority.exists())
					authority.delete();

				File localPem = null;
				ZipOutputStream zos = null;
				OutputStream authorityOs = null;
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				try {
					zos = new ZipOutputStream(os);
					
					if(syncExcludeUserData == false) {
						ExportCsvSqlDb exportCsv = new ExportCsvSqlDb();
						csvFilesNames = exportCsv.exportCsv(TMP_DIR);
						if(csvFilesNames != null) {
							for(String csvFileName : csvFilesNames) {
								File f = new File(csvFileName);
								writeZipEntry(zos, f, "");
								logger.trace("frodo core: add zip entry [{}]", f.getAbsolutePath());
							}
						}
					}
					
					if(synUserDataOnly == false) {		//true일 경우엔 사용자 정보만 동기화되도록
						authorityOs = new FileOutputStream(authority);
						ca.exportAuthority("local", authorityOs);
	
						for (String dbName : dbNames) {
							logger.debug("frodo core: export db [{}]", dbName);
							File cdbFile = masterDbFiles.get(dbName);
							if (cdbFile.exists())
								cdbFile.delete();
							DataExporter.exportData(conf.ensureDatabase(dbName), cdbFile);
						}
	
						logger.trace("frodo core: trying to copy local.pem");
						CommandUtil.run("cp", "-rf", "/utm/log/kraken/data/kraken-ca/CA/local/local.pem", "/utm/log/tmp/");
						localPem = new File(TMP_DIR, "local.pem");
	
						for (String dbName : dbNames) {
							File f = masterDbFiles.get(dbName);
							writeZipEntry(zos, f, "");
							logger.trace("frodo core: add zip entry [{}]", f.getAbsolutePath());
						}
						
						writeZipEntry(zos, authority, "");
						logger.trace("frodo core: authority.cdb file path [{}]", authority.getAbsolutePath());
						writeZipEntry(zos, localPem, "");
						logger.trace("frodo core: local.pem file path [{}]", localPem.getAbsolutePath());
	
						File uploadDirectory = uploadApi.getBaseDirectory("localhost");
						if (uploadDirectory.exists()) {
							for (File f : uploadDirectory.listFiles()) {
								logger.trace("frodo core: file path [{}]", f.getAbsolutePath());
								writeZipEntry(zos, f, "");
							}
						} else {
							logger.debug("frodo core: upload directory does not exist, path=" + uploadDirectory.getAbsolutePath());
						}
						
						includeNACFile(zos);
					}

					sendFile(resp, os.toByteArray());

					logger.info("frodo core: master database replication completed, elapsed [{}]s",
							(new Date().getTime() - begin.getTime()) / 1000);
				} catch (Throwable t) {
					logger.error("frodo core: cluster zip failed, elapsed " + (new Date().getTime() - begin.getTime()) / 1000
							+ "s", t);
				} finally {
					for (File f : masterDbFiles.values())
						f.delete();
					
					if(csvFilesNames != null) {
						for(String fileName : csvFilesNames) {
							File f = new File(fileName);
							if(f.exists())
								f.delete();
						}
					}
					
					if(zos != null)
						zos.close();
					
					if(os != null)
						os.close();

					if (authorityOs != null)
						try {
							authorityOs.close();
						} catch (IOException e) {
						}
					authority.delete();
				}
				return;
			}
		}
	}

	private void sendFile(HttpServletResponse resp, byte[] data) {
		InputStream is = null;
		OutputStream os = null;
		try {
			resp.setContentType("application/octet-stream");
			resp.setHeader("Content-Disposition", "attachment; filename=cluster.zip");
			resp.setStatus(200);
			resp.setContentLength(data.length);

			os = resp.getOutputStream();
			os.write(data, 0, data.length);
		} catch (IOException e) {
			logger.trace("frodo core: download faile", e);
		} finally {
			try {
				if (is != null)
					is.close();
				if (os != null) {
					os.flush();
					os.close();
				}
			} catch (IOException e) {
			}
		}

	}

	private void writeZipEntry(ZipOutputStream zos, File file, String current) throws IOException {
		if (file.isDirectory()) {
			current += file.getName() + "/";
			logger.debug("frodo core: zipping directory [{}], zip entry [{}]", file.getName(), current);

			zos.putNextEntry(new ZipEntry(current));
			for (File f : file.listFiles()) {
				writeZipEntry(zos, f, current);
			}
			zos.closeEntry();

		} else {
			current += file.getName();
			logger.trace("frodo core: zipping file [{}], zip entry [{}]", file.getName(), current);

			ZipEntry e = new ZipEntry(current);

			FileInputStream is = null;
			try {
				is = new FileInputStream(file);
				zos.putNextEntry(e);
				byte[] b = new byte[8096];
				while (true) {
					int read = is.read(b);
					if (read <= 0)
						break;
					zos.write(b, 0, read);
				}
				zos.closeEntry();
				zos.flush();
			} finally {
				if (is != null)
					is.close();
			}
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.trace("frodo core: sync policy request, size [{}]", req.getInputStream().available());

		String password = req.getParameter("password");
		if (password == null) {
			logger.error("frodo core: null password from [{}]", req.getRemoteAddr());
			resp.sendError(500);
			return;
		}

		// check if slave mode
		List<SyncPolicySynchronize> l = XtmConfig.readConfig(SyncPolicySynchronize.class);
		if (l.size() == 0) {
			logger.trace("frodo core: policy sync not used");
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error off");
			return;
		}

		SyncPolicySynchronize policy = l.get(0);

		if (!policy.isUse()) {
			logger.trace("frodo core: policy sync not used");
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error off");
			return;
		}

		if (!password.equals(policy.getPassword())) {
			logger.trace("frodo core: invalid password");
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error off");
			return;
		}

		// remove old files
		CommandUtil.run("rm", "-rf", TMP_DIR);

		// unzip files
		unzipFiles(req);
		try {
			ClusterContext.replicated.set(true);
			File domCdb = new File(TMP_XML_DIR + "/kraken-dom-localhost.cdb");
			if (domCdb.exists()) {
				DataImporter.importData(conf, "kraken-dom-localhost", domCdb);
			}

			File deviceTmp = new File(TMP_XML_DIR + "/devices.tmp");
			if (deviceTmp.exists()) {
				DataImporter.importDevices(conf, deviceApi, deviceTmp);
			} else
				logger.trace("frodo core: does not exist devices.tmp, target path=[{}]", deviceTmp.getAbsolutePath());
		} finally {
			ClusterContext.replicated.set(false);
		}

		// overwrite xml files
		CommandUtil.run("cp", "-rf", TMP_XML_DIR, "/etc/webadmin/");

		// remove tmp files
		CommandUtil.run("rm", "-rf", TMP_DIR + "/*");

		// logging SYSERR_OBJECT_RESTORE
		KLogWriter.write(0x12030014, null, "Policy synchronize from Master");

		// run send policy object
		CommandUtil.run("/usr/bin/php", "/var/www/webadmin/send_policy_object.php");
		logger.trace("frodo core: policy sync completed");

		resp.getWriter().write("ok");
		resp.getWriter().close();
	}

	private void unzipFiles(HttpServletRequest req) throws IOException, FileNotFoundException {
		ZipInputStream is = new ZipInputStream(req.getInputStream());
		try {
			while (true) {
				ZipEntry e = is.getNextEntry();
				if (e == null)
					break;

				if (e.isDirectory()) {
					logger.trace("frodo core: unzipping sync'd directory [{}]", e.getName());
					new File(TMP_DIR, e.getName()).mkdirs();
					continue;
				}

				byte[] b = new byte[8096];
				File f = new File(TMP_DIR, e.getName());
				FileOutputStream os = new FileOutputStream(f);
				logger.trace("frodo core: unzipping sync'd file [{}]", f.getAbsolutePath());

				try {
					while (true) {
						int read = is.read(b);
						if (read <= 0)
							break;
						os.write(b, 0, read);
					}
					is.closeEntry();
				} finally {
					os.close();
				}
			}
		} finally {
			is.close();
		}
	}

	//to clustering the files that were uploaded for NAC
	private void includeNACFile(ZipOutputStream zos) throws IOException {
		GlobalConfig config = configApi.getGlobalConfig();
		String userUiPath = config.getUserUiPath();
		
		File nacDir = new File(userUiPath + "/NAC");
		if(nacDir.exists()) {
			for(File file : nacDir.listFiles()) {
				writeZipEntry(zos, file, ClusterSyncService.NAC_FILE_PREFIX);
			}
		}
	}
}
