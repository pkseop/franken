/*
 * Copyright 2011 Future Systems
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
package kr.co.future.httpd.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import kr.co.future.httpd.HttpConfiguration;
import kr.co.future.httpd.HttpConfigurationListener;
import kr.co.future.httpd.HttpContextRegistry;
import kr.co.future.httpd.HttpGlobalConfigApi;
import kr.co.future.httpd.HttpServer;
import kr.co.future.httpd.VirtualHost;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import kr.co.future.api.KeyStoreManager;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "http-server")
@Provides
public class HttpServerImpl implements HttpServer, HttpConfigurationListener {
	private final Logger logger = LoggerFactory.getLogger(HttpServerImpl.class.getName());

	private BundleContext bc;
	private HttpConfiguration config;
	private HttpContextRegistry contextRegistry;
	private KeyStoreManager keyStoreManager;
	private Channel listener;
	private ConfigService conf;
	
	public HttpServerImpl(BundleContext bc, HttpConfiguration config, HttpContextRegistry contextRegistry,
			KeyStoreManager keyStoreManager, ConfigService conf) {
		this.bc = bc;
		this.config = config;
		this.contextRegistry = contextRegistry;
		this.keyStoreManager = keyStoreManager;
		this.conf = conf;

		// set configuration set listener
		config.getListeners().add(this);
	}

	@Override
	public void open() {
		open(null);
	}
	
	@Override
	public void open(HttpGlobalConfigApi globalConfigApi) {
		final SSLContext sslContext;
		if(config.isSsl()) {
			sslContext = newSslContext(config.getKeyAlias(), config.getTrustAlias());
			sslContext.getServerSessionContext().setSessionCacheSize(10 * 1024 * 1024);
			sslContext.getServerSessionContext().setSessionTimeout(30);
		} else 
			sslContext = null;
		
		final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();
		try{
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new HttpPipelineFactory(bc, config, contextRegistry, globalConfigApi, sslContext));
	
	        bootstrap.option(ChannelOption.SO_BACKLOG, 512);
	        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
	        bootstrap.childOption(ChannelOption.SO_SNDBUF, 1048576);
	        bootstrap.childOption(ChannelOption.SO_RCVBUF, 1048576);
	        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
	        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
	        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
	
	
			// Bind and start to accept incoming connections.
			InetSocketAddress addr = config.getListenAddress();
			listener = bootstrap.bind(addr.getPort()).sync().channel();
	
			logger.info("kraken httpd: {} ({}) opened", addr, config.isSsl() ? "https" : "http");
			
			listener.closeFuture().addListener(new ChannelFutureListener(){

				@Override
				public void operationComplete(ChannelFuture future)
						throws Exception {
					logger.info("server [{}] channel closed", listener.localAddress());
					bossGroup.shutdownGracefully();
					workerGroup.shutdownGracefully();
				}
				
			});
		} catch (Exception e) {
			logger.error("server [{}] open error", config.getListenAddress());
		}
	}

	@Override
	public HttpConfiguration getConfiguration() {
		return config;
	}

	@Override
	public void addVirtualHost(VirtualHost vhost) {
		if (vhost.getHttpContextName() == null)
			throw new IllegalArgumentException("kraken httpd: http context name shoud not null");

		VirtualHost target = findVirtualHost(vhost.getHttpContextName());
		if (target != null)
			throw new IllegalStateException("duplicated http context exists: " + vhost.getHttpContextName());

		config.getVirtualHosts().add(vhost);
		saveConfig();
	}

	@Override
	public void removeVirtualHost(String httpContextName) {
		VirtualHost target = findVirtualHost(httpContextName);
		if (target != null) {
			config.getVirtualHosts().remove(target);
			saveConfig();
		}
	}

	private void saveConfig() {
		Map<String, Object> filter = getFilter();

		ConfigDatabase db = conf.ensureDatabase("kraken-httpd");
		Config c = db.findOne(HttpConfiguration.class, Predicates.field(filter));
		if (c != null) {
			db.update(c, this.config);
		} else {
			logger.error("kraken httpd: cannot find configuration for " + config.getListenAddress());
		}
	}

	private Map<String, Object> getFilter() {
		Map<String, Object> filter = new HashMap<String, Object>();
		InetSocketAddress listen = config.getListenAddress();
		filter.put("listen_address", listen.getAddress().getHostAddress());
		filter.put("listen_port", listen.getPort());
		return filter;
	}

	private VirtualHost findVirtualHost(String httpContextName) {
		VirtualHost target = null;

		for (VirtualHost h : config.getVirtualHosts())
			if (h.getHttpContextName().equals(httpContextName))
				target = h;
		return target;
	}

	@Override
	public void onSet(String field, Object value) {
		saveConfig();
	}

	@Override
	public void close() {
		// remove reference
		config.getListeners().remove(this);

		try {
			if (listener != null) {
				logger.info("kraken httpd: {} closed", listener.localAddress());
				listener.close();
			}
		} catch (Throwable t) {
			logger.error("kraken httpd: cannot close " + listener.localAddress(), t);
		}
	}

	@Override
	public boolean isOpened() {
		// TODO Auto-generated method stub
		return  listener == null ? false : listener.isOpen();
	}
	
	private SSLContext newSslContext(String keyAlias, String trustAlias) {
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			
			TrustManager[] trustManagers = null;
			KeyManager[] keyManagers = null;

			TrustManagerFactory tmf = keyStoreManager.getTrustManagerFactory(trustAlias, "SunX509");
			KeyManagerFactory kmf = keyStoreManager.getKeyManagerFactory(keyAlias, "SunX509");

			if (tmf != null)
				trustManagers = tmf.getTrustManagers();
			if (kmf != null)
				keyManagers = kmf.getKeyManagers();

			sslContext.init(keyManagers, trustManagers, new SecureRandom());
			return sslContext;
		} catch (Exception e) {
			throw new RuntimeException("cannot create ssl context", e);
		}
	}
}
