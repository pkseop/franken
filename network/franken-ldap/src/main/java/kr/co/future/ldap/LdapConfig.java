package kr.co.future.ldap;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("config")
public class LdapConfig {
	
	@FieldOption(name = "page_size", nullable = false)
	private int pageSize;
	
	public int getPageSize() {
		return pageSize;
	}
	
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
}
