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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Response implements HttpServletResponse {
	private final Logger logger = LoggerFactory.getLogger(Response.class.getName());
	private final int INITIAL_BUF_CAPACITY = 1024 * 1024;
	private final int MAX_BUF_SIZE = 100 * 1024 * 1024;

	private BundleContext bc;
	private ChannelHandlerContext ctx;
	private HttpServletRequest req;
	private ServletOutputStream os;
	private PrintWriter writer;
	private HttpResponseStatus status = HttpResponseStatus.OK;
	private Map<String, List<String>> headers = new HashMap<String, List<String>>();
	private Set<Cookie> cookies = new HashSet<Cookie>();

	public Response(BundleContext bc, ChannelHandlerContext ctx, HttpServletRequest req) {
		this.bc = bc;
		this.ctx = ctx;
		this.req = req;
		this.os = new ResponseOutputStream();
		this.writer = new PrintWriter(new OutputStreamWriter(os, Charset.forName("utf-8")));
	}

	private class ResponseOutputStream extends ServletOutputStream {
		private boolean closed = false;
		private ByteBufAllocator alloc = ctx.channel().alloc();
		private int contentSize = 0;
		private ByteBuf buf = alloc.buffer(INITIAL_BUF_CAPACITY, MAX_BUF_SIZE);
		private boolean isExccededMaxBufSize = false;

		@Override
		public void write(int b) throws IOException {
			contentSize += 1;
			if(contentSize >= MAX_BUF_SIZE) {
				isExccededMaxBufSize = true;
				throw new IndexOutOfBoundsException("excceeded-max-buffer-size");
			}
			buf.writeByte(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			contentSize += len;
			if(contentSize >= MAX_BUF_SIZE) {
				isExccededMaxBufSize = true;
				throw new IndexOutOfBoundsException("excceeded-max-buffer-size");
			}
			buf.writeBytes(b, off, len);
		}

		@Override
		public void close() throws IOException {
			if (closed) {
				if (logger.isDebugEnabled())
					logger.debug("kraken httpd: response output closed");
				return;
			}

			closed = true;
			if(isExccededMaxBufSize)        //if response size bigger than max buffer size than close the channel. 
				ctx.close();
			else
				flush(true);

			String transferEncoding = getHeader(HttpHeaders.Names.TRANSFER_ENCODING);
			if (logger.isDebugEnabled())
				logger.debug("kraken httpd: transfer encoding header [{}]", transferEncoding);

			if (transferEncoding != null && transferEncoding.equals("chunked")) {
				ctx.write(Unpooled.EMPTY_BUFFER);
				if (logger.isDebugEnabled())
					logger.debug("kraken httpd: channel [{}], last empty chunk", ctx.channel());
			}

			if (logger.isDebugEnabled())
				logger.debug("kraken httpd: closing channel [{}]", ctx.channel());

			if (!isKeepAlive()) {
				if (logger.isDebugEnabled())
					logger.debug("kraken httpd: channel [{}] will be closed", ctx.channel());
				// If keep-alive is off, close the connection once the content is fully written.
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
			}
		}

		@Override
		public void flush() throws IOException {
//			flush(false);
		}

		private void flush(boolean force) {
			String transferEncoding = getHeader(HttpHeaders.Names.TRANSFER_ENCODING);
			boolean isChunked = transferEncoding != null && transferEncoding.equals("chunked");

//			if ((force || isChunked) && !sentHeader)
			{
				// send response if not sent
				FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);

				HttpSessionImpl session = (HttpSessionImpl) req.getSession(false);
				if (session != null) {
					if (session.isNew()) {
						resp.headers().add(HttpHeaders.Names.SET_COOKIE, "JSESSIONID=" + session.getId() + "; path=/");
						session.setNew(false);
					}

					session.setLastAccess(new Date());
				}

//				if (!isChunked)
					resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, contentSize);

				for (Cookie c : cookies) {
					resp.headers().add(HttpHeaders.Names.SET_COOKIE, c.getName() + "=" + c.getValue());
				}

				for (String name : headers.keySet())
					resp.headers().set(name, headers.get(name));

				if (logger.isDebugEnabled())
					logger.debug("kraken httpd: channel [{}], sent header", ctx.channel());

				// write http header
				ctx.write(resp);

//				sentHeader = true;
			}

//			if (isChunked) {
//				if (logger.isDebugEnabled())
//					logger.debug("kraken httpd: channel [{}], flush chunk [{}]", ctx.getChannel(), buf.readableBytes());
//
//				ctx.getChannel().write(new DefaultHttpChunk(buf));
//				buf = ChannelBuffers.dynamicBuffer();
//			} else if (sentHeader) {
//				if (logger.isDebugEnabled())
//					logger.debug("kraken httpd: channel [{}], flush response [{}]", ctx.getChannel(), buf.readableBytes());
//
//				ctx.getChannel().write(buf);
//				buf = ChannelBuffers.dynamicBuffer();
//			}
		}

		private boolean isKeepAlive() {
			String connection = req.getHeader(HttpHeaders.Names.CONNECTION);
			if (connection != null && HttpHeaders.Values.CLOSE.equalsIgnoreCase(connection))
				return false;

			if (req.getProtocol().equals("HTTP/1.1")) {
				return !HttpHeaders.Values.CLOSE.equalsIgnoreCase(connection);
			} else {
				return HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(connection);
			}
		}

	}

	@Override
	public String getCharacterEncoding() {
		List<String> contentTypes = (List<String>) headers.get(HttpHeaders.Names.CONTENT_TYPE);
		if (contentTypes == null)
			return null;

		String contentType = contentTypes.get(0);
		if (!contentType.contains("charset"))
			return null;

		for (String t : contentType.split(";")) {
			if (t.trim().startsWith("charset"))
				return t.split("=")[1].trim();
		}
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return os;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return writer;
	}

	@Override
	public void setContentLength(int len) {
		headers.put(HttpHeaders.Names.CONTENT_LENGTH, Arrays.asList(Integer.toString(len)));
	}

	@Override
	public void setContentType(String type) {
		headers.put(HttpHeaders.Names.CONTENT_TYPE, Arrays.asList(type));
	}

	@Override
	public void addCookie(Cookie cookie) {
		cookies.add(cookie);
		logger.trace("cookie added [{}]", cookie);
	}

	@Override
	public boolean containsHeader(String name) {
		return headers.containsKey(name);
	}

	@Override
	public String encodeRedirectURL(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Deprecated
	@Override
	public String encodeRedirectUrl(String url) {
		return encodeRedirectURL(url);
	}

	@Override
	public String encodeURL(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Deprecated
	@Override
	public String encodeUrl(String url) {
		return encodeURL(url);
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
		this.status = HttpResponseStatus.valueOf(sc);
		String html = null;
		if(sc >= 400 && sc < 600){
			html = getHtmlPage("not_found.html");
		}
		else {
			if (msg == null)
				msg = "";
	
			html = "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" //
					+ "<html><head><title>" + sc + " " + status.reasonPhrase()
					+ "</title></head>\n" //
					+ "<body><h1>" + sc + " " + status.reasonPhrase() + "</h1><pre>" + msg
					+ "</pre><hr/><address>Kraken HTTPd/"
					+ bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION) + "</address></body></html>";
		}
		writer.append(html);
		writer.close();
	}

	@Override
	public void sendError(int sc) throws IOException {
		sendError(sc, null);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		this.status = HttpResponseStatus.MOVED_PERMANENTLY;
		setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
		setHeader(HttpHeaders.Names.LOCATION, location);
	}

	public void close() {
		writer.close();
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCharacterEncoding(String charset) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBufferSize(int size) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLocale(Locale loc) {
		// TODO Auto-generated method stub

	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHeader(String name) {
		List<String> l = headers.get(name);
		if (l == null || l.size() == 0)
			return null;

		return l.get(0);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return headers.get(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headers.keySet();
	}

	@Override
	public void addDateHeader(String name, long date) {
		addHeader(name, new Date(date).toString());
	}

	@Override
	public void addIntHeader(String name, int value) {
		addHeader(name, Integer.toString(value));
	}

	@Override
	public void addHeader(String name, String value) {
		List<String> l = headers.get(name);
		if (l == null) {
			l = new ArrayList<String>();
			headers.put(name, l);
		}

		l.add(value);
	}

	@Override
	public int getStatus() {
		return status.code();
	}

	@Override
	public void setDateHeader(String name, long date) {
		setHeader(name, new Date(date).toString());
	}

	@Override
	public void setIntHeader(String name, int value) {
		setHeader(name, Integer.toString(value));
	}

	@Override
	public void setHeader(String name, String value) {
		if (logger.isDebugEnabled())
			logger.debug("kraken httpd: set response header [name: {}, value: {}]", name, value);
		headers.put(name, Arrays.asList(value));
	}

	@Deprecated
	@Override
	public void setStatus(int sc, String sm) {
		setStatus(sc);
	}

	@Override
	public void setStatus(int sc) {
		this.status = HttpResponseStatus.valueOf(sc);
	}
	
	private String getHtmlPage(String page) {
		Bundle bundle = bc.getBundle();
		URL url = bundle.getEntry("/WEB-INF/" + page);
		byte[] b = new byte[4096];
		InputStream is = null;
		StringBuilder sb = new StringBuilder();

		try {
			is = url.openStream();

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("kraken httpd: cannot load not_found page", e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return sb.toString();
	}

}
