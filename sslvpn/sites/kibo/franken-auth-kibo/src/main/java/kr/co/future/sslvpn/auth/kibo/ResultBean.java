package kr.co.future.sslvpn.auth.kibo;

import java.net.HttpURLConnection;

public class ResultBean {
	private String msg;
	private HttpURLConnection httpURLConnection;

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public HttpURLConnection getHttpURLConnection() {
		return httpURLConnection;
	}

	public void setHttpURLConnection(HttpURLConnection httpURLConnection) {
		this.httpURLConnection = httpURLConnection;
	}
}
