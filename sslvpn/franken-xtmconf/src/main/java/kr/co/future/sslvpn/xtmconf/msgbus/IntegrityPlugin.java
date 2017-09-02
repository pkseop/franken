package kr.co.future.sslvpn.xtmconf.msgbus;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.system.Integrity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-xtmconf-integrity-plugin")
@MsgbusPlugin
public class IntegrityPlugin {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@MsgbusMethod
	public void checkIntegrity(Request req, Response resp) {
		try {
			Map<String, Object> hashm = Integrity.checkIntegrity();
			resp.put("result", hashm);
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}
}