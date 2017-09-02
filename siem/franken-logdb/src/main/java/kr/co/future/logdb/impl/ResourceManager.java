package kr.co.future.logdb.impl;

import java.util.List;
import java.util.concurrent.ExecutorService;

import kr.co.future.logdb.query.ResourceManagerImpl.CommandThread;

public interface ResourceManager {
	ExecutorService getExecutorService();

	List<CommandThread> getThreads();

	List<Runnable> setThreadPoolSize(int nThreads);

	<T> T get(Class<T> cls);
}
