package kr.co.future.logdb;

import java.util.Map;

public interface LogScriptOutput {
	void write(Map<String, Object> data);
}
