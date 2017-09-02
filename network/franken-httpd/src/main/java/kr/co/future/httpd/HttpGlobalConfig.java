package kr.co.future.httpd;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("http_global_config")
public class HttpGlobalConfig {
	
	@FieldOption(name = "id", nullable = false)
	private int id;
	
	@FieldOption(nullable = true)
	Long readLimit;
	
	@FieldOption(nullable = true)
	Long writeLimit;	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public Long getReadLimit() {
		return readLimit;
	}

	public void setReadLimit(Long readLimit) {
		this.readLimit = readLimit;
	}
	
	public Long getWriteLimit() {
		return writeLimit;
	}

	public void setWriteLimit(Long writeLimit) {
		this.writeLimit = writeLimit;
	}
}
