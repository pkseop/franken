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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.List;

import kr.co.future.httpd.HttpConfiguration;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpContextRegistry;
import kr.co.future.httpd.VirtualHost;
import kr.co.future.httpd.WebSocket;
import kr.co.future.httpd.WebSocketFrame;
import kr.co.future.httpd.netty.websocket.KrakenWebSocketServerHandshaker00;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerHandler extends SimpleChannelInboundHandler<Object> {
	private final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class.getName());
	private final Logger httpAccessLogger = LoggerFactory.getLogger("HttpAccessLogger");	
	
	private static final int MAX_WEBSOCKET_FRAME_SIZE = 8 * 1024 * 1024;
	private static final String WEBSOCKET_PATH = "/websocket";
	
	private BundleContext bc;
	private HttpConfiguration config;
	private HttpContextRegistry contextRegistry;
	
	private WebSocketServerHandshaker handshaker;

	public HttpServerHandler(BundleContext bc, HttpConfiguration config, HttpContextRegistry contextRegistry) {
		this.bc = bc;
		this.config = config;
		this.contextRegistry = contextRegistry;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.ALL_IDLE) {
				Channel channel = ctx.channel();
				logger.trace("kraken httpd: closing idle connection [local={}, remote={}, state={}]",
							new Object[] { channel.localAddress(), channel.remoteAddress(), e.state() });

				ctx.close();
			}
		} 
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, msg);
		} 	else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(ctx, (WebSocketFrame)msg);;
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, Object msg) throws IOException {
		FullHttpRequest req = (FullHttpRequest)msg;
		
		//only allow GET & POST method
		HttpMethod method = req.getMethod();
		if(!method.equals(HttpMethod.GET) && !method.equals(HttpMethod.POST)) {
			Request request = new Request(ctx, req, null);
			Response response = new Response(bc, ctx, request);
			request.setResponse(response);

			response.sendError(405);
			return;
		}
		
		HttpContext httpContext = findHttpContext(req.headers().get(HttpHeaders.Names.HOST));
		HttpContent httpContent = null;
		if(msg instanceof HttpContent) {
			httpContent = (HttpContent)msg;
		}
		Request request = new Request(ctx, req, httpContent);
		httpAccessLogger.info("request scheme [{}], uri: [{}], from [{}]", new Object[]{request.getScheme(),req.getUri(), ctx.channel().remoteAddress()});
		
		if (httpContext == null) {
			Response response = new Response(bc, ctx, request);
			request.setResponse(response);

			response.sendError(404);
			return;
		}

		Response response = new Response(bc, ctx, request);
		request.setResponse(response);
		
		// Websocket Handshake
		if(req.getUri().equals(WEBSOCKET_PATH)) {
			handshaker = new KrakenWebSocketServerHandshaker00(getWebSocketLocation(req), null, MAX_WEBSOCKET_FRAME_SIZE, 
					req.headers().get(HttpHeaders.Names.HOST), ctx.channel());
			if (handshaker == null) {
				WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
			} else {
				handshaker.handshake(ctx.channel(), req);
				
				// open session
				WebSocket socket = new WebSocketChannel(ctx.channel());
				httpContext.getWebSocketManager().register(socket);
			}
			
		} else 
			httpContext.handle(request, response);
	}
	
	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		httpAccessLogger.info("websocket request from: [{}], data [{}]", new Object[]{frame.getRemote(), frame.getTextData()});
		
		HttpContext httpContext = findHttpContext(frame.getHost());
		httpContext.getWebSocketManager().dispatch(frame);;
	}

	private HttpContext findHttpContext(String host) {
		if (host != null) {
			for (VirtualHost v : config.getVirtualHosts())
				if (v.matches(host))
					return contextRegistry.findContext(v.getHttpContextName());
		}

		String contextName = config.getDefaultHttpContext();
		if (contextName == null)
			return null;
		return contextRegistry.findContext(contextName);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		List<String> trace = Arrays.asList("Connection reset by peer",
				"An existing connection was forcibly closed by the remote host");

		if (cause instanceof IOException && trace.contains(cause.getMessage())) {
			logger.trace("kraken httpd: connection reset", cause.getCause().getMessage());
		} else if (cause instanceof ClosedChannelException) {
			logger.trace("kraken httpd: connection closed", cause.getCause().getMessage());
		} else {
			logger.error("kraken httpd: transport error", cause.getCause().getMessage());
		}
		ctx.close();
	}
	
	public void channelInactive(ChannelHandlerContext ctx) {
		// At this moment, channel is already closed
		// Do NOT call ctx.getChannel().close() again
		for (String name : contextRegistry.getContextNames()) {
			HttpContext context = contextRegistry.findContext(name);
			InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
			context.getWebSocketManager().unregister(remote);
		}
	}

	private String getWebSocketLocation(FullHttpRequest req) {
		return "ws://" + req.headers().get(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
	}
}
