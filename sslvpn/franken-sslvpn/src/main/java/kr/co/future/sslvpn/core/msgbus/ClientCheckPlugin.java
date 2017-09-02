package kr.co.future.sslvpn.core.msgbus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.msgbus.ClientCheckPlugin;
import kr.co.future.sslvpn.model.ClientCheckProfile;
import kr.co.future.sslvpn.model.api.ClientCheckProfileApi;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.Session;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

@Component(name = "frodo-client-check-plugin")
@MsgbusPlugin
public class ClientCheckPlugin {
	
	private final Logger logger = LoggerFactory.getLogger(ClientCheckPlugin.class);

	@Requires
	private ClientCheckProfileApi clientCheckProfileApi;
	
	@Requires
	private GlobalConfigApi configApi;

	@MsgbusMethod
	public void getProfiles(Request req, Response resp) {
		try {
			List<ClientCheckProfile> clientCheckProfiles = clientCheckProfileApi.getClientCheckProfiles();
			for (ClientCheckProfile ccp : clientCheckProfiles)
				ccp.getAntivirusCheckList();
			resp.put("profiles", PrimitiveConverter.serialize(clientCheckProfiles));
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void getProfile(Request req, Response resp) {
		String guid = req.getString("guid");
		try {
			ClientCheckProfile clientCheckProfile = clientCheckProfileApi.getClientCheckProfile(guid);
			clientCheckProfile.getAntivirusCheckList();
			resp.put("profile", PrimitiveConverter.serialize(clientCheckProfile));
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void createProfile(Request req, Response resp) {
		ClientCheckProfile p = (ClientCheckProfile) PrimitiveConverter.parse(ClientCheckProfile.class, req.getParams());
		try {
			String guid = clientCheckProfileApi.createClientCheckProfile(p);
			resp.put("guid", guid);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}

	}

	@MsgbusMethod
	public void updateProfile(Request req, Response resp) {
		ClientCheckProfile p = (ClientCheckProfile) PrimitiveConverter.parse(ClientCheckProfile.class, req.getParams());
		try {
			clientCheckProfileApi.updateClientCheckProfile(p);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void removeProfile(Request req, Response resp) {
		String guid = req.getString("guid");
		try {
			clientCheckProfileApi.removeClientCheckProfile(guid);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void removeProfiles(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		List<String> guids = (List<String>) req.get("guids");

		try {
			clientCheckProfileApi.removeClientCheckProfiles(guids);
		} catch (IllegalStateException e) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("rawmsg", e.getMessage());
			throw new MsgbusException("frodo", "used-by-access-profile", params);
		}
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void prepareUpload(Request req, Response resp) throws IOException {
		String fileName = req.getString("filename");
		int fileSize = req.getInteger("filesize");

		Session session = req.getSession();
		Map<String, Object> m = (Map<String, Object>) session.get("upload");
		if (m == null) {
			m = new ConcurrentHashMap<String, Object>();
			session.setProperty("upload", m);
		}

		GlobalConfig config = configApi.getGlobalConfig();
		String userUiPath = config.getUserUiPath();
		if(Strings.isNullOrEmpty(userUiPath)) {
			userUiPath = System.getProperty("kraken.home.dir") + "/resources/";
			new File(userUiPath).mkdirs();
			config.setUserUiPath(userUiPath);
			configApi.setGlobalConfig(config);
		}
			
		String nacDir = userUiPath + "/NAC/";
		File uploadFile = new File(nacDir, fileName);
		uploadFile.getParentFile().mkdirs();

		m.put("data", new UploadMetadata(fileName, fileSize, uploadFile));
	}
	
	private static class UploadMetadata {
		public String fileName;
		public int fileSize;
		public File temp;

		public UploadMetadata(String fileName, int fileSize, File temp) {
			this.fileName = fileName;
			this.fileSize = fileSize;
			this.temp = temp;
		}
	}
	
	private final static String UPLOAD_FILE_KEY = "upload-nac-file";
	
	@MsgbusMethod
	public void uploadPart(Request req, Response resp) throws IOException {
		String data = req.getString("data");
		String flag = req.getString("flag");
		Session session = req.getSession();
		byte[] b = Base64.decodeBase64(data);

		logger.trace("frodo core: flag=[{}]", flag);

		UploadMetadata meta = null;
		if (!flag.equals("close"))
			meta = getUploadMetadata(req);

		FileOutputStream os = (FileOutputStream) session.get(UPLOAD_FILE_KEY);
		if (os == null) {
			os = new FileOutputStream(meta.temp, true);
			session.setProperty(UPLOAD_FILE_KEY, os);
			logger.trace("frodo core: start upload upgrade file");
		}

		if (flag.equals("upload")) {
			os.write(b);
		} else { // flag == close
			if (os != null)
				try {
					os.close();
					session.unsetProperty(UPLOAD_FILE_KEY);
					logger.trace("frodo core: upgrade file stream is closed");
				} catch (IOException e) {
				}
		}
	}
	
	@SuppressWarnings("unchecked")
	private UploadMetadata getUploadMetadata(Request req) {
		Session session = req.getSession();

		Map<String, Object> m = (Map<String, Object>) session.get("upload");
		if (m == null)
			throw new MsgbusException("frodo", "upload-data-not-found");

		UploadMetadata meta = (UploadMetadata) m.get("data");
		if (meta == null)
			throw new MsgbusException("frodo", "upload-data-not-found");
		return meta;
	}
}
