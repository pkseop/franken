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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteBuffer;

import kr.co.future.rpc.RpcMessage;

import kr.co.future.codec.EncodingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {
	private final Logger logger = LoggerFactory.getLogger(RpcEncoder.class.getName());

	@Override
	protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
		Object m = msg.marshal();

		int length = EncodingRule.lengthOf(m);
		ByteBuffer bb = ByteBuffer.allocate(length);
		EncodingRule.encode(bb, m);
		bb.flip();

		if (logger.isDebugEnabled())
			logger.debug("kraken-rpc: sending id: {}, method: {}, size: {}", new Object[] { msg.getHeader("id"),
					msg.getString("method"), bb.remaining() });
			
		
		byte[] dst = new byte[bb.remaining()];
		bb.get(dst, 0, dst.length);
		out.writeBytes(dst);
	}
}
