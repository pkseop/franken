package kr.co.future.sslvpn.auth.ngp;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("config")
public class NgpConfig {
	@FieldOption(nullable = false)
	private String url;

	@FieldOption(nullable = false)
	private int port;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "Url=" + url + ", port=" + port;
	}

}
