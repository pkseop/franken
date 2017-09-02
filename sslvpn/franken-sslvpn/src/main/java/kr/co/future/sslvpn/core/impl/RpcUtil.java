package kr.co.future.sslvpn.core.impl;

import kr.co.future.rpc.RpcAgent;
import kr.co.future.rpc.RpcConnection;
import kr.co.future.rpc.RpcException;
import kr.co.future.rpc.RpcSession;

public class RpcUtil {

	public static Object call(RpcAgent agent, String serviceName, String method, Object[] params) throws RpcException,
			InterruptedException {
		RpcConnection conn = null;
		RpcSession session = null;
		try {
			conn = findConnection(agent);
			if (conn == null)
				throw new IllegalStateException("marlin connection not found");

			session = conn.createSession(serviceName);
			return session.call(method, params, 10000);
		} finally {
			if (session != null)
				session.close();
		}
	}

	public static void post(RpcAgent agent, String serviceName, String method, Object[] params) throws RpcException,
			InterruptedException {
		RpcConnection conn = null;
		RpcSession session = null;
		try {
			conn = findConnection(agent);
			if (conn == null)
				throw new IllegalStateException("marlin connection not found");

			session = conn.createSession(serviceName);
			session.post(method, params);
		} finally {
			if (session != null)
				session.close();
		}
	}

	private static RpcConnection findConnection(RpcAgent agent) {
		for (RpcConnection conn : agent.getConnections()) {
			String remoteAddr = conn.getRemoteAddress().getAddress().getHostAddress();
			if (conn.isOpen() && conn.getPeerCertificate() != null && remoteAddr.equals("127.0.0.1")) {
				return conn;
			}
		}

		return null;
	}
}
