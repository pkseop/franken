package kr.co.future.sslvpn.core.servlet.csv;

import java.util.List;
import java.util.Map;

public interface ExportObject {
	List<Map<String, Object>> convertToListMap(List<?> list);
	List<String> getKeyList(List<Map<String, Object>> list);
	
	static final char ch = '`';
	
	public static final String DELIM_1ST = ch+"1"+ch;
	
	public static final String DELIM_2ND = ch+"2"+ch;
	
	public static final String DELIM_3RD = ch+"3"+ch;
	
	public static final String DELIM_4TH = ch+"4"+ch;
}
