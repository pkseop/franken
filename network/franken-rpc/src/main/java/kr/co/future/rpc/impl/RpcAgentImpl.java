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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import kr.co.future.rpc.RpcAgent;
import kr.co.future.rpc.RpcBindingProperties;
import kr.co.future.rpc.RpcClient;
import kr.co.future.rpc.RpcConnection;
import kr.co.future.rpc.RpcConnectionEventListener;
import kr.co.future.rpc.RpcConnectionProperties;
import kr.co.future.rpc.RpcPeerRegistry;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.api.KeyStoreManager;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigCollection;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "rpc-agent")
@Provides
public class RpcAgentImpl implements RpcAgent {
	private final Logger logger = LoggerFactory.getLogger(RpcAgentImpl.class.getName());
	private BundleContext bc;
	private RpcPeerRegistry peerRegistry;

	private RpcHandler handler;
	private RpcServiceTracker tracker;

	private ConcurrentMap<RpcBindingProperties, Channel> bindings;

	@Requires
	private KeyStoreManager keyStoreManager;

	@Requires
	private ConfigService conf;

	public RpcAgentImpl(BundleContext bc) {
		this.bc = bc;
		peerRegistry = new RpcPeerRegistryImpl(conf);
		handler = new RpcHandler(getGuid(), peerRegistry);
		tracker = new RpcServiceTracker(bc, handler);
		bindings = new ConcurrentHashMap<RpcBindingProperties, Channel>();
	}

	@Validate
	public void start() throws Exception {
		try {
			handler.start();
			bc.addServiceListener(tracker);

			// open configured bindings
			ConfigDatabase db = conf.ensureDatabase("kraken-rpc");
			ConfigIterator it = db.findAll(RpcBindingProperties.class);
			try {
				while (it.hasNext()) {
					Config c = it.next();
					RpcBindingProperties props = c.getDocument(RpcBindingProperties.class);
					if (props.getKeyAlias() != null && props.getTrustAlias() != null)
						bindSsl(props);
					else
						bind(props);
				}
			} finally {
				it.close();
			}

			// register all auto-wiring RPC services.
			tracker.scan();
		} catch (Exception e) {
			stop();
			throw e;
		}
	}

	@Invalidate
	public void stop() {
		for (RpcBindingProperties props : bindings.keySet())
			unbind(props);

		bc.removeServiceListener(tracker);
		handler.stop();
	}

	@Override
	public String getGuid() {
		ConfigDatabase db = conf.ensureDatabase("kraken-rpc");
		ConfigCollection col = db.ensureCollection("agent");
		Config c = col.findOne(null);
		if (c != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> doc = (Map<String, Object>) c.getDocument();
			return (String) doc.get("guid");
		}

		Map<String, Object> doc = new HashMap<String, Object>();
		String guid = UUID.randomUUID().toString();
		doc.put("guid", guid);
		col.add(doc);
		return guid;
	}

	@Override
	public Collection<RpcBindingProperties> getBindings() {
		return new ArrayList<RpcBindingProperties>(bindings.keySet());
	}

	@Override
	public void open(RpcBindingProperties props) {
		if (bindings.containsKey(props))
			throw new IllegalStateException("already opened: " + props);

		if (props.getKeyAlias() != null && props.getTrustAlias() != null)
			bindSsl(props);
		else
			bind(props);

		ConfigDatabase db = conf.ensureDatabase("kraken-rpc");
		db.add(props, "kraken-rpc", "opened " + props);
	}

	@Override
	public void close(RpcBindingProperties props) {
		ConfigDatabase db = conf.ensureDatabase("kraken-rpc");
		Predicate p = Predicates.and(Predicates.field("addr", props.getHost()),
				Predicates.field("port", props.getPort()));

		Config c = db.findOne(RpcBindingProperties.class, p);
		if (c != null)
			db.remove(c, false, "kraken-rpc", "closed " + props);

		unbind(props);
	}

	@Override
	public RpcConnection connectSsl(RpcConnectionProperties props) {
		RpcClient client = new RpcClient(handler);
		return client.connectSsl(props);
	}

	@Override
	public RpcConnection connect(RpcConnectionProperties props) {
		RpcClient client = new RpcClient(handler);
		return client.connect(props);
	}

	private Channel bindSsl(RpcBindingProperties props) {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();

		SSLContext serverContext = getSSLContext(props);
		if(serverContext == null)
			throw new RuntimeException("rpc-ssl connection failed");

		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new RpcAgentChannelInitializer(serverContext));

        bootstrap.option(ChannelOption.SO_BACKLOG, 256);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, 1048576);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, 1048576);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		
		SocketAddress address = new InetSocketAddress(props.getHost(), props.getPort());
		ChannelFuture channelFuture = bootstrap.bind(address);
		Channel channel = channelFuture.awaitUninterruptibly().channel();
		bindings.put(props, channel);

		logger.info("kraken-rpc: {} ssl port opened", address);
		return channel;
	}
	
	private SSLContext getSSLContext(RpcBindingProperties props) {
		final String keyAlias = props.getKeyAlias();
		final String trustAlias = props.getTrustAlias();
		
		TrustManagerFactory tmf;
		try {
			tmf = keyStoreManager.getTrustManagerFactory(trustAlias, "SunX509");
			KeyManagerFactory kmf = keyStoreManager.getKeyManagerFactory(keyAlias, "SunX509");

	        TrustManager[] trustManagers = null;
	        KeyManager[] keyManagers = null;
	        if (tmf != null)
	            trustManagers = tmf.getTrustManagers();
	        if (kmf != null)
	            keyManagers = kmf.getKeyManagers();

	        SSLContext serverContext = SSLContext.getInstance("TLS");
	        serverContext.init(keyManagers, trustManagers, new SecureRandom());

	        return serverContext;
		} catch (KeyStoreException e) {
			logger.error("get ssl handerl failed: ", e);
		} catch (NoSuchAlgorithmException e) {
			logger.error("get ssl handerl failed: ", e);
		} catch (KeyManagementException e) {
			logger.error("get ssl handerl failed: ", e);
		} catch (UnrecoverableKeyException e) {
			logger.error("get ssl handerl failed: ", e);
		}
        return null;
	}
	
	private class RpcAgentChannelInitializer extends ChannelInitializer<SocketChannel>{
		SSLContext sslContext;
		
		public RpcAgentChannelInitializer(SSLContext sslContext) {
			this.sslContext = sslContext;
		}
		
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			// TODO Auto-generated method stub
			ChannelPipeline pipeline = ch.pipeline();

			if(sslContext != null) {
				SSLEngine engine = sslContext.createSSLEngine();
		        engine.setUseClientMode(false);
		        engine.setNeedClientAuth(true);
		        
				pipeline.addLast("ssl", new SslHandler(engine));
			}
            // decoder, encoder and handler
            pipeline.addLast("decoder", new RpcDecoder());
            pipeline.addLast("encoder", new RpcEncoder());
            pipeline.addLast("handler", handler);
		}
		
	}

	private Channel bind(RpcBindingProperties props) {
		final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();

		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new RpcAgentChannelInitializer(null));

        bootstrap.option(ChannelOption.SO_BACKLOG, 256);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, 1048576);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, 1048576);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

		InetSocketAddress address = new InetSocketAddress(props.getHost(), props.getPort());

		ChannelFuture channelFuture = bootstrap.bind(address);
		Channel channel = channelFuture.awaitUninterruptibly().channel();
		bindings.put(props, channel);

		logger.info("kraken-rpc: {} port opened", address);
		return channel;
	}

	private void unbind(RpcBindingProperties props) {
		Channel channel = bindings.remove(props);
		if (channel == null)
			return;

		logger.info("kraken-rpc: unbinding [{}]", props);
		channel.close().awaitUninterruptibly();
	}

	@Override
	public RpcConnection findConnection(int id) {
		return handler.findConnection(id);
	}

	@Override
	public Collection<RpcConnection> getConnections() {
		return handler.getConnections();
	}

	@Override
	public RpcPeerRegistry getPeerRegistry() {
		return peerRegistry;
	}

	@Override
	public void addConnectionListener(RpcConnectionEventListener listener) {
		handler.addConnectionListener(listener);
	}

	@Override
	public void removeConnectionListener(RpcConnectionEventListener listener) {
		handler.removeConnectionListener(listener);
	}
}
