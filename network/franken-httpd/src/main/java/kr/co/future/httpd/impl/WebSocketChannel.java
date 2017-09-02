/*
 * Copyright 2012 Future Systems
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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.net.InetSocketAddress;

import kr.co.future.httpd.WebSocket;

public class WebSocketChannel implements WebSocket {

	private Channel channel;
	
	private InetSocketAddress localAddress;
	
	private InetSocketAddress remoteAddress;

	public WebSocketChannel(Channel channel) {
		this.channel = channel;
		if(channel != null) {
			InetSocketAddress temp = (InetSocketAddress)channel.localAddress();
			this.localAddress = new InetSocketAddress(temp.getAddress(), temp.getPort());
			temp = (InetSocketAddress)channel.remoteAddress();
			this.remoteAddress = new InetSocketAddress(temp.getAddress(), temp.getPort());
		}
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	@Override
	public void send(String text) {
		if (channel != null && channel.isWritable())
			channel.writeAndFlush(new TextWebSocketFrame(text));
	}

	@Override
	public void close() {
		if (channel != null) {
			channel.close();
			channel = null;
		}
	}

}
