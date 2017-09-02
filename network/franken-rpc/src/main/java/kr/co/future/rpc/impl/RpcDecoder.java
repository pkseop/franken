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
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteBuffer;
import java.util.List;

import kr.co.future.codec.EncodingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDecoder extends ByteToMessageDecoder {
	private final Logger logger = LoggerFactory.getLogger(RpcDecoder.class.getName());

	public static final int ARRAY_TYPE = 1;
	public static final int MAP_TYPE = 2;
	public static final int STRING_TYPE = 3;
	public static final int INT16_TYPE = 4;
	public static final int INT32_TYPE = 5;
	public static final int INT64_TYPE = 6;
	public static final int DATE_TYPE = 7;
	public static final int IPV4_TYPE = 8;
	public static final int IPV6_TYPE = 9;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("kraken-rpc: current readable length {}", in.readableBytes());

		in.markReaderIndex();
		if (in.readableBytes() < 2)
			return;

		// read type byte
		in.readByte();

		// read length bytes
		int lengthBytes = 0;
		byte b = 0;
		boolean eon = false;
		while (true) {
			if (in.readableBytes() == 0)
				break;

			b = in.readByte();
			lengthBytes++;

			if ((b & 0x80) != 0x80) {
				eon = true;
				break;
			}
		}

		if (!eon) {
			in.resetReaderIndex();
			return; // more length bytes needed
		}

		in.resetReaderIndex();

		// read type byte
		in.readByte();

		// byte buffer read does not modify readable index
		long length = EncodingRule.decodeRawNumber(in.nioBuffer());

		if (in.readableBytes() >= lengthBytes + length) {
			in.resetReaderIndex();
			ByteBuffer bb = ByteBuffer.allocate((int) length + lengthBytes + 1);
			in.readBytes(bb);
			bb.flip();

			Object decoded = EncodingRule.decode(bb);
			if (logger.isDebugEnabled())
				logger.debug("kraken-rpc: decoded one message, remaining {}", in.readableBytes());

			out.add(decoded);
		} else 
			in.resetReaderIndex();
	}

}
