package kr.co.future.logdb;

import java.util.Map;

public interface LogScriptFactory {
	LogScript create(Map<String, Object> params);
	
	String getDescription();
}
