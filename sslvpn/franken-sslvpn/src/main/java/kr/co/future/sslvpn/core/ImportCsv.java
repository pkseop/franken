package kr.co.future.sslvpn.core;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ImportCsv {
	
	void importAccessProfiles(List<Map<String, String>> datas) throws Exception;
	
	public List<Map<String, String>> parseCsvNoDecode(byte[] csvBytes, String charset) throws IOException;
	
}
