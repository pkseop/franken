/*
 * Copyright 2010 NCHOVY
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.rpc.impl;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import kr.co.future.rpc.RpcConnection;
import kr.co.future.rpc.RpcContext;
import kr.co.future.rpc.RpcException;
import kr.co.future.rpc.RpcMethod;
import kr.co.future.rpc.RpcPeer;
import kr.co.future.rpc.RpcPeerRegistry;
import kr.co.future.rpc.RpcService;
import kr.co.future.rpc.RpcServiceBinding;
import kr.co.future.rpc.RpcSession;
import kr.co.future.rpc.RpcSessionEvent;
import kr.co.future.rpc.RpcSessionEventCallback;
import kr.co.future.rpc.RpcTrustLevel;
import kr.co.future.rpc.SimpleRpcService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * all connections shares one control service.
 * 
 * @author xeraph
 * 
 */
public class RpcControlService extends SimpleRpcService {
	private final Logger logger = LoggerFactory.getLogger(RpcControlService.class.getName());
	
	private String guid;
	private RpcPeerRegistry peerRegistry;

	public RpcControlService(String guid, RpcPeerRegistry peerRegistry) {
		this.guid = guid;
		this.peerRegistry = peerRegistry;
	}

	@Override
	public void sessionRequested(RpcSessionEvent e) {
		logger.debug("kraken-rpc: rpc-control session requested");
	}

	@RpcMethod(name = "ping")
	public int ping(int value) {
		return value;
	}

	@RpcMethod(name = "peering-request")
	public Object[] handlePeeringRequest(String peerGuid) {
		RpcConnection conn = RpcContext.getConnection();
		conn.setPeerGuid(peerGuid);

		logger.trace("rpc-control: peering request received from {}", peerGuid);

		if (conn.getPeerCertificate() == null) {
			String nonce = UUID.randomUUID().toString();
			conn.setNonce(nonce);
			return new Object[] { "challenge", guid, nonce };
		}

		RpcPeer peer = peerRegistry.findPeer(peerGuid);
		RpcTrustLevel trustLevel = RpcTrustLevel.Untrusted;
		if (peer != null)
			trustLevel = peer.getTrustLevel();
		else
			trustLevel = RpcTrustLevel.Low;

		conn.setTrustLevel(trustLevel);
		return new Object[] { "success", guid, trustLevel.getCode() };
	}

	@RpcMethod(name = "authenticate")
	public Object[] handleAuthenticate(String guid, String hash) {
		RpcConnection conn = RpcContext.getConnection();
		if (conn.getNonce() == null) {
			logger.warn("kraken-rpc: peer request first");
			throw new RpcException("peer request first");
		}

		RpcPeer peer = null;
		boolean result = false;
		try {
			peer = peerRegistry.authenticate(conn.getPeerGuid(), conn.getNonce(), hash);
			conn.setTrustLevel(peer.getTrustLevel());
			result = true;
		} catch (IllegalStateException e) {
			logger.warn("kraken-rpc: auth failed, peer=" + conn.getPeerGuid(), e);
		}

		int trustLevel = peer != null ? peer.getTrustLevel().getCode() : RpcTrustLevel.Low.getCode();

		return new Object[] { result, trustLevel };
	}

	@RpcMethod(name = "session-request")
	public Integer handleSessionRequest(String serviceName, Map<String, Object> params) {
		RpcConnectionImpl conn = (RpcConnectionImpl) RpcContext.getConnection();
		RpcServiceBinding binding = conn.findServiceBinding(serviceName);
		if (binding == null)
			throw new IllegalStateException("service binding not found: " + serviceName);

		if (conn.getTrustLevel() == null || conn.getTrustLevel() == RpcTrustLevel.Untrusted)
			throw new IllegalStateException("[" + serviceName
					+ "] session request not allowed, establish peering first");

		RpcService service = binding.getService();
		RpcSessionEvent event = new RpcSessionEventImpl(RpcSessionEvent.Requested, null, toProperties(params));

		// may throw exception if session request is not accepted
		service.sessionRequested(event);

		int newSessionId = conn.nextSessionId();
		RpcSession session = conn.openSession(newSessionId, serviceName);
		session.addListener(new RpcServiceSessionClosedCallback(service));

		if (logger.isDebugEnabled())
			logger.debug("kraken-rpc: new [{}] service session [{}] created by session request", serviceName,
					newSessionId);

		return newSessionId;
	}

	private Properties toProperties(Map<String, Object> map) {
		Properties props = new Properties();
		if (map == null)
			return props;

		for (String key : map.keySet()) {
			props.put(key, map.get(key));
		}

		return props;
	}

	/**
	 * It will invoke RpcService.sessionClosed callback when session is closed.
	 * This callback is registered to session when new session request is
	 * accepted at server-side.
	 * 
	 * @author xeraph
	 * 
	 */
	private static class RpcServiceSessionClosedCallback implements RpcSessionEventCallback {
		private RpcService service;

		public RpcServiceSessionClosedCallback(RpcService service) {
			this.service = service;
		}

		@Override
		public void sessionClosed(RpcSession session) {
			RpcSessionEvent event = new RpcSessionEventImpl(RpcSessionEvent.Closed, session);
			service.sessionClosed(event);
		}
	}
}
