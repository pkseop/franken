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
package kr.co.future.webconsole.impl;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Locale;

import kr.co.future.msgbus.Message;
import kr.co.future.msgbus.Session;
import kr.co.future.webconsole.CometSessionStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CometSession implements Session {
	private final Logger logger = LoggerFactory.getLogger(CometSession.class.getName());

	private CometSessionStore comet;
	private String sessionKey;
	private Channel channel;
	private Date lastAccessTime;

	public CometSession(CometSessionStore comet, String sessionKey, Channel channel) {
		this.comet = comet;
		this.sessionKey = sessionKey;
		this.channel = channel;

	}

	public int getId() {
		return channel.hashCode();
	}

	@Override
	public String getGuid() {
		return sessionKey;
	}

	// TODO:
	@Override
	public boolean has(String key) {
		return false;
	}

	@Override
	public Object get(String key) {
		return comet.get(key, key);
	}

	@Override
	public InetAddress getLocalAddress() {
		return ((InetSocketAddress) channel.localAddress()).getAddress();
	}

	@Override
	public InetAddress getRemoteAddress() {
		return ((InetSocketAddress) channel.remoteAddress()).getAddress();
	}

	@Override
	public String getOrgDomain() {
		return getString("org_domain");
	}

	@Override
	public String getAdminLoginName() {
		return getString("admin_login_name");
	}

	@Override
	public Locale getLocale() {
		if (!comet.containsKey(sessionKey, "locale"))
			return new Locale("en");

		return new Locale(getString("locale"));
	}

	@Override
	public String getString(String key) {
		return (String) get(key);
	}

	@Override
	public Integer getInt(String key) {
		return (Integer) get(key);
	}

	@Override
	public void setProperty(String key, Object value) {
		comet.set(sessionKey, key, value);
	}

	@Override
	public void unsetProperty(String key) {
		comet.unset(sessionKey, key);
	}

	public void send(Message msg) {
		String payload = KrakenMessageEncoder.encode(this, msg);
		if (logger.isDebugEnabled())
			logger.debug("kraken webconsole: sending [{}]", payload);

		if (channel != null) {
			HttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(payload, CharsetUtil.UTF_8));
			ChannelFuture future = channel.writeAndFlush(resp);
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	public void close() {
		channel.close();
		channel = null;
	}

	@Override
	public String toString() {
		return "channel=" + channel.hashCode() + ", sessionkey=" + sessionKey;
	}

	@Override
	public Date getLastAccessTime() {
		return lastAccessTime;
	}

	@Override
	public void setLastAccessTime() {
		this.lastAccessTime = new Date();			
	}
}
