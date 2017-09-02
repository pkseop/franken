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
package kr.co.future.slpolicy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

@Component(name = "slpolicy-server")
@Provides
public class SilverlightPolicyServerImpl implements SilverlightPolicyServer {
	private Channel listener;

	@Override
	public boolean isOpen() {
		return listener != null;
	}

	@Validate
	@Override
	public synchronized void open() {
		if (listener != null)
			throw new IllegalStateException("tcp/943 already opened");
		
		final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();

		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new SilverlightPolicyPipelineFactory());

		bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
		bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

		InetSocketAddress addr = new InetSocketAddress(943);
		ChannelFuture channelFuture = bootstrap.bind(addr);
		listener = channelFuture.awaitUninterruptibly().channel();
	}

	@Invalidate
	public void stop() {
		if (listener != null) {
			listener.close();
			listener = null;
		}
	}

	@Override
	public synchronized void close() {
		if (listener == null)
			throw new IllegalStateException("policy server is not running");

		listener.close();
		listener = null;
	}
}
