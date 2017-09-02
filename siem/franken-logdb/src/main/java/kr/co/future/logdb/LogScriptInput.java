package kr.co.future.logdb;

import java.util.Map;

import org.osgi.framework.BundleContext;

public interface LogScriptInput {
	BundleContext getBundleContext();

	Map<String, Object> getData();
}
