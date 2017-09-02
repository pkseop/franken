package kr.co.future.sslvpn.auth.ncc;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("config")
public class NccConfig {
	@FieldOption(nullable = false)	
	private String url;
	
	@FieldOption(nullable = false)
	private int timeout;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public String toString() {
		return "Url=" + url + ", timeout=" + timeout;
	}
	
}
