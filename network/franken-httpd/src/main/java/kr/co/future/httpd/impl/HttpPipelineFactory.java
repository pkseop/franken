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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import kr.co.future.httpd.HttpConfiguration;
import kr.co.future.httpd.HttpConfigurationListener;
import kr.co.future.httpd.HttpContextRegistry;
import kr.co.future.httpd.HttpGlobalConfig;
import kr.co.future.httpd.HttpGlobalConfigApi;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpPipelineFactory extends ChannelInitializer<SocketChannel> implements HttpConfigurationListener {
	private final Logger logger = LoggerFactory.getLogger(HttpPipelineFactory.class.getName());
	private BundleContext bc;
	private HttpConfiguration config;
	private HttpContextRegistry contextRegistry;
	private SSLContext sslContext;
	
	//pks> 2014-11-17. use as static to share with every port (80, 443, 4502) 
	private static GlobalTrafficShapingHandler globalTrafficShapingHandler = null;

	public HttpPipelineFactory(BundleContext bc, HttpConfiguration config, HttpContextRegistry contextRegistry,
			HttpGlobalConfigApi globalConfigApi, SSLContext sslContext) {
		this.bc = bc;
		this.config = config;
		this.contextRegistry = contextRegistry;
		this.sslContext = sslContext;
		createGlobalTrafficShapingHandler(globalConfigApi);		
		config.getListeners().add(this);
	}
	
	private void createGlobalTrafficShapingHandler(HttpGlobalConfigApi globalConfigApi) {
		if(globalTrafficShapingHandler != null)
			return;
		
		Long readLimit = 0L, writeLimit = 0L, defaultLimitSize = 0L;
		
		int cpuCount = Runtime.getRuntime().availableProcessors();
		defaultLimitSize = (cpuCount * 10L ) * 1024 * 1024;
		logger.info("cpu count : [{}], default traffic bandwidth [{}]", new Object[]{cpuCount, defaultLimitSize});
		
		if(globalConfigApi == null) {
			readLimit = writeLimit = defaultLimitSize;
		} else {
			HttpGlobalConfig config = globalConfigApi.getHttpGlobalConfig();
			if(config == null) {
				readLimit = writeLimit = defaultLimitSize;
			} else {
				readLimit = config.getReadLimit();
				writeLimit = config.getWriteLimit();
				
				logger.info("configured traffic bandwidth => read limit: [{}] bytes/second, write limit [{}] bytes/second", new Object[]{readLimit, writeLimit});
			}
		}
		ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(cpuCount);
		globalTrafficShapingHandler = new GlobalTrafficShapingHandler(scheduledThreadPoolExecutor, writeLimit, readLimit);
	}

	@Override
	public void onSet(String fieldName, Object value) {
		if (fieldName.equals("idleTimeout")) {
			logger.debug("kraken httpd: http config field [{}] changed to [{}]", fieldName, value);
		}
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		//pks. 2014-11-17. limit traffic bandwidth for stable http service.
		p.addLast("GLOBAL_TRAFFIC_SHAPING", globalTrafficShapingHandler);
		
		if(sslContext != null) {
			SSLEngine engine = sslContext.createSSLEngine();
			engine.setUseClientMode(false);
			
			String[] enabledCipherSutes = {
					"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
					"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
					"TLS_RSA_WITH_AES_256_CBC_SHA256",
					"TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
					"TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
					"TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
					"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
					"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
					"TLS_RSA_WITH_AES_256_CBC_SHA",
					"TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
					"TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
					"TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
					"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
					"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
					"TLS_RSA_WITH_AES_128_CBC_SHA256",
					"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
					"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
					"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
					"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
					"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
					"TLS_RSA_WITH_AES_128_CBC_SHA",
					"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
					"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
					"TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
					"TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
					"TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
					"TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
					"TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
					"TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
					"TLS_ECDHE_RSA_WITH_RC4_128_SHA",
					"SSL_RSA_WITH_RC4_128_SHA",
					"TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
					"TLS_ECDH_RSA_WITH_RC4_128_SHA",
					"SSL_RSA_WITH_RC4_128_MD5",
					"TLS_EMPTY_RENEGOTIATION_INFO_SCSV" };
			
			engine.setEnabledCipherSuites(enabledCipherSutes);
			
			SslHandler sslHandler = new SslHandler(engine);
			
			p.addLast("ssl", sslHandler);
		}

		p.addLast("decoder", new HttpRequestDecoder());
		p.addLast("aggregator", new HttpObjectAggregator(config.getMaxContentLength()));
		p.addLast("encoder", new HttpResponseEncoder());
		p.addLast("chunkedWriter",new ChunkedWriteHandler());
		p.addLast("idleStateHandler", new IdleStateHandler(0, 0, config.getIdleTimeout()));
		p.addLast("handler", new HttpServerHandler(bc, config, contextRegistry));
	}
	
	public static void setReadLimit(long readLimit) {
		globalTrafficShapingHandler.setReadLimit(readLimit);
	}

	public static void setWriteLimit(long writeLimit) {
		globalTrafficShapingHandler.setWriteLimit(writeLimit);
	}
	
	public static long getReadLimit() {
		return globalTrafficShapingHandler.getReadLimit();
	}
	
	public static long getWriteLimit() {
		return globalTrafficShapingHandler.getWriteLimit();
	}
}