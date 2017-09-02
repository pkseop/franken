package kr.co.future.sslvpn.core.msgbus;

import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.core.CertificatesChangeApi;
import kr.co.future.sslvpn.core.msgbus.CertificatePlugin;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-certificates-plugin")
@MsgbusPlugin
public class CertificatePlugin {
	private Logger logger = LoggerFactory.getLogger(CertificatePlugin.class.getName());
	@Requires
	private CertificatesChangeApi certificatesApi;

	@MsgbusMethod
	public void changeCertificates(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> certFiles = (List<Map<String, Object>>) req.get("cert_files");
		if (certFiles == null)
			throw new MsgbusException("frodo", "certificate-file-not-found");
		
		String password = req.getString("cert_password");
		if (password == null)
			throw new MsgbusException("frodo", "password-not-found");
		
		try {
			certificatesApi.changeCertificates(certFiles, password);
		} catch (IllegalStateException e) {
			certificatesApi.rollBack();
			throw new MsgbusException("frodo", e.getMessage());
		}

	}
	
}
