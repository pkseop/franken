package kr.co.future.sslvpn.core.util;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.PushApi;
import kr.co.future.msgbus.Session;

public class DownloadUtil {

	public static void pushMsgOfExceededMaxBufSize(PushApi pushApi, Session session) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", "exceeded-max-buffer");
		m.put("name", "");
		m.put("address", "");
		pushApi.push(session, "frodo-external-server-check", m);
	}
	
}
