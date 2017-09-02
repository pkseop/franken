package kr.co.future.sslvpn.xtmconf.msgbus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;

@Component(name = "frodo-xtmconf-utils-plugin")
@MsgbusPlugin
public class UtilsPlugin {
	@MsgbusMethod
	public void getInterfaceCount(Request req, Response resp) {
		resp.put("count", Utils.getInterfaceCount());
	}

	@MsgbusMethod
	public void getInterfaceNames(Request req, Response resp) {
		resp.put("names", Utils.getInterfaceNames());
	}

	@MsgbusMethod
	public void getSerial(Request req, Response resp) {
		File f = new File("/proc/utm/serial");
		String serial = null;

		if (f.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				serial = br.readLine();
			} catch (IOException e) {
			} finally {
				try {
					if (br != null)
						br.close();
				} catch (IOException e) {
				}
			}
		}

		resp.put("serial", serial);
	}
}
