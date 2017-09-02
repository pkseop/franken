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
package kr.co.future.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import kr.co.future.rpc.impl.RpcDecoder;
import kr.co.future.rpc.impl.RpcEncoder;
import kr.co.future.rpc.impl.RpcHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClient {
	private final Logger logger = LoggerFactory.getLogger(RpcClient.class.getName());
	private final boolean ownHandler;

	private EventLoopGroup group;
	private RpcHandler handler;
	private RpcConnection conn;

	public RpcClient(String guid) {
		this.handler = new RpcHandler(guid, new TrustPeerRegistry());
		this.ownHandler = true;
	}

	public RpcClient(RpcHandler handler) {
		this.handler = handler;
		this.ownHandler = false;
	}

	public RpcConnection connect(RpcConnectionProperties props) {
		if (conn != null)
			return conn;

		if (ownHandler)
			handler.start();

		group = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group)
			.channel(NioSocketChannel.class)
			.handler(new RpcClientInitializer(null));
		
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

		Channel channel = null;
		try {
			ChannelFuture channelFuture = bootstrap.connect(props.getRemoteAddress());
			channel = channelFuture.awaitUninterruptibly().channel();
			return doPostConnectSteps(channel, props);
		} catch (Exception e) {
			if (channel != null)
				channel.close();

			// shutdown executors
			group.shutdownGracefully();

			throw new RuntimeException("rpc connection failed", e);
		}
	}
	
	private class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
		private SSLContext sslContext;
		
		public RpcClientInitializer(SSLContext sslContext) {
			this.sslContext = sslContext;
		}
		
		@Override
		public void initChannel(SocketChannel ch) {
			ChannelPipeline p = ch.pipeline();
			
			if(sslContext != null) {
				SSLEngine engine = sslContext.createSSLEngine();
				engine.setUseClientMode(true);
				p.addLast("ssl", new SslHandler(engine));
			}
			p.addLast("decoder", new RpcDecoder());
			p.addLast("encoder", new RpcEncoder());
			p.addLast("handler", handler);
		}
	}

	public RpcConnection connectSsl(RpcConnectionProperties props) {
		if (conn != null)
			return conn;

		if (ownHandler)
			handler.start();
		
		SSLContext sslContext = getSSLContext(props);
		if(sslContext == null)
			throw new RuntimeException("rpc-ssl connection failed");
		
		group = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group)
			.channel(NioSocketChannel.class)
			.handler(new RpcClientInitializer(sslContext));

		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

		ChannelFuture channelFuture = bootstrap.connect(props.getRemoteAddress());
		Channel channel = channelFuture.awaitUninterruptibly().channel();
		try {
			SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
			sslHandler.handshakeFuture().await(5000);
			X509Certificate peerCert = (X509Certificate) sslHandler.engine().getSession().getPeerCertificates()[0];
			props.setPeerCert(peerCert);

			return doPostConnectSteps(channel, props);
		} catch (Exception e) {
			if (channel != null)
				channel.close();

			// shutdown executors
			group.shutdownGracefully();

			throw new RuntimeException("rpc-ssl connection failed", e);
		}
	}

	/**
	 * Receive and set peer certificate for connection.
	 */
	private RpcConnection doPostConnectSteps(Channel channel, RpcConnectionProperties props) {
		if (channel != null && channel.isActive()) {
			if (props.getPeerCert() != null)
				logger.trace("kraken-rpc: connected with peer {}", props.getPeerCert().getSubjectDN().getName());

			return handler.newClientConnection(channel, props);
		}

		return null;
	}

	public void close() {
		if (ownHandler)
			handler.stop();

		if (conn != null && conn.isOpen()) {
			conn.close();
		}
		conn = null;

		if (group != null) {
			group.shutdownGracefully();
		}
		group = null;
	}

	private SSLContext getSSLContext(RpcConnectionProperties props) {
		KeyManagerFactory kmf = props.getKeyManagerFactory();
		TrustManagerFactory tmf = props.getTrustManagerFactory();
		
		TrustManager[] trustManagers = null;
		KeyManager[] keyManagers = null;
		if (tmf != null)
			trustManagers = tmf.getTrustManagers();
		if (kmf != null)
			keyManagers = kmf.getKeyManagers();

		SSLContext clientContext;
		try {
			clientContext = SSLContext.getInstance("TLS");
			clientContext.init(keyManagers, trustManagers, new SecureRandom());
			
			return clientContext;
		} catch (NoSuchAlgorithmException e) {
			logger.error("get ssl handerl failed: ", e);
		} catch (KeyManagementException e) {
			logger.error("getSslHanderl failed: ", e);
		}
		return null;		
	}
}
