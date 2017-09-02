package kr.co.future.sslvpn.core.msgbus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import kr.co.future.sslvpn.core.msgbus.ClientAppPlugin;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.api.ClientAppApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.dom.api.FileUploadApi;
import kr.co.future.dom.model.UploadedFile;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-client-app-plugin")
@MsgbusPlugin
public class ClientAppPlugin {
	private final Logger logger = LoggerFactory.getLogger(ClientAppPlugin.class.getName());

	@Requires
	private ClientAppApi clientAppApi;

	@Requires
	private FileUploadApi fileUploadApi;

	@MsgbusMethod
	public void getClientApps(Request req, Response resp) {
		List<ClientApp> apps = clientAppApi.getClientApps();
		resp.put("apps", Marshaler.marshal(apps));
	}

	@MsgbusMethod
	public void getClientApp(Request req, Response resp) {
		String guid = req.getString("guid");

		ClientApp clientApp = clientAppApi.getClientApp(guid);
		if (clientApp == null) {
			resp.put("app", null);
			return;
		}

		resp.put("app", clientApp.marshal());
	}

	@MsgbusMethod
	public void createClientApp(Request req, Response resp) throws Throwable {
		try {
			ClientApp app = parseClientApp(req);
			if (app.getIcon() != null)
				checkImage(app.getIcon());

			clientAppApi.createClientApp(app);
		} catch (Throwable e) {
			logger.error("frodo core: cannot create app", e);
			throw e;
		}
	}

	@MsgbusMethod
	public void updateClientApp(Request req, Response resp) {
		ClientApp app = parseClientApp(req);
		if (app.getIcon() != null)
			checkImage(app.getIcon());

		clientAppApi.updateClientApp(app);
	}

	private void checkImage(String resourceId) {
		UploadedFile f = fileUploadApi.getFileMetadata("localhost", resourceId);
		int pos = f.getFileName().lastIndexOf('.');
		if (pos < 0)
			throw new MsgbusException("frodo", "file-suffix-not-found");

		String suffix = f.getFileName().substring(pos + 1);
		logger.trace("frodo core: client app image icon [{}], suffix [{}]", f.getFileName(), suffix);

		try {
			File file = new File(f.getPath());
			if (suffix.equalsIgnoreCase("png")) {
				checkSize(file);
			} else if (suffix.equalsIgnoreCase("jpg") || suffix.equalsIgnoreCase("jpeg")) {
				checkSize(file);
			} else {
				throw new MsgbusException("frodo", "invalid-image-format");
			}
		} catch (IOException e) {
			logger.warn("frodo core: cannot read client app image", e);
			throw new MsgbusException("frodo", "image-io-error");
		}
	}

	private void checkSize(File f) throws IOException {
		BufferedImage bimg = ImageIO.read(f);
		int width = bimg.getWidth();
		int height = bimg.getHeight();
		
		logger.trace("image file size check w [{}], h [{}]", new Object[]{width,height});
		
		if (width > 64 || height > 64)
			throw new MsgbusException("frodo", "image-size-over");
	}
	
//	private void checkPng(File f) throws IOException {
//		RandomAccessFile raf = null;
//
//		try {
//			raf = new RandomAccessFile(f, "rw");
//			raf.seek(16);
//			int width = raf.readInt();
//			int height = raf.readInt();
//
//			if (width > 64 || height > 64)
//				throw new MsgbusException("frodo", "image-size-over");
//		} finally {
//			if (raf != null)
//				raf.close();
//		}
//	}
//
//	private void checkJpg(File f) throws IOException {
//		// TODO: exact width/height check
//		if (f.length() > 3000)
//			throw new MsgbusException("frodo", "image-size-over");
//	}

	@SuppressWarnings("unchecked")
	private ClientApp parseClientApp(Request req) {
		ClientApp app = new ClientApp();
		if (req.has("guid"))
			app.setGuid(req.getString("guid"));

		app.setPlatform(req.getString("platform"));
		app.setName(req.getString("name"));
		app.setOperator(req.getString("operator"));
		app.setPhone(req.getString("phone"));
		app.setIcon(req.getString("icon"));
		app.setMetadata((Map<String, Object>) req.get("metadata"));
		return app;
	}

	@MsgbusMethod
	public void removeClientApp(Request req, Response resp) {
		try {
			clientAppApi.removeClientApp(req.getString("guid"));
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeClientApps(Request req, Response resp) {
		List<String> apps = (List<String>) req.get("apps");
		try {
			clientAppApi.removeClientApps(apps);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}
}
