package kr.co.future.logdb.query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import kr.co.future.log.api.LogParserFactoryRegistry;
import kr.co.future.logdb.DataSourceRegistry;
import kr.co.future.logdb.LogQuery;
import kr.co.future.logdb.LogQueryCommand;
import kr.co.future.logdb.LogScriptRegistry;
import kr.co.future.logdb.LookupHandlerRegistry;
import kr.co.future.logdb.impl.ResourceManager;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogTableRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@Component(name = "resource-manager")
@Provides
public class ResourceManagerImpl implements ResourceManager {
	private static ExecutorService exesvc;
	private List<CommandThread> threads;

	private BundleContext bc;

	@Requires
	private DataSourceRegistry dataSourceRegistry;

	@Requires
	private LogStorage logStorage;

	@Requires
	private LogTableRegistry logTableRegistry;

	@Requires
	private LookupHandlerRegistry lookupHandlerRegistry;

	@Requires
	private LogScriptRegistry logScriptRegistry;

	@Requires
	private LogParserFactoryRegistry logParserFactoryRegistry;

	public ResourceManagerImpl(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		if (exesvc == null)
			exesvc = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactoryImpl());
	}

	@Invalidate
	public void stop() {
		exesvc.shutdownNow();
		exesvc = null;
	}

	@Override
	public ExecutorService getExecutorService() {
		return exesvc;
	}

	@Override
	public List<CommandThread> getThreads() {
		return threads;
	}

	@Override
	public List<Runnable> setThreadPoolSize(int nThreads) {
		List<Runnable> result = null;
		if (exesvc != null)
			result = exesvc.shutdownNow();
		exesvc = Executors.newFixedThreadPool(nThreads, new ThreadFactoryImpl());
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> cls) {
		if (cls == DataSourceRegistry.class)
			return (T) dataSourceRegistry;
		else if (cls == LogStorage.class)
			return (T) logStorage;
		else if (cls == LogTableRegistry.class)
			return (T) logTableRegistry;
		else if (cls == LookupHandlerRegistry.class)
			return (T) lookupHandlerRegistry;
		else if (cls == LogScriptRegistry.class)
			return (T) logScriptRegistry;
		else if (cls == LogParserFactoryRegistry.class)
			return (T) logParserFactoryRegistry;

		ServiceReference ref = bc.getServiceReference(cls.getName());
		if (ref == null)
			throw new NullPointerException();
		return (T) bc.getService(ref);
	}

	private class ThreadFactoryImpl implements ThreadFactory {
		private AtomicInteger id = new AtomicInteger(1);
		private List<CommandThread> threads = new ArrayList<CommandThread>();

		public ThreadFactoryImpl() {
			ResourceManagerImpl.this.threads = threads;
		}

		@Override
		public Thread newThread(Runnable r) {
			CommandThread thread = new CommandThread(r, id.getAndIncrement());
			threads.add(thread);
			return thread;
		}
	}

	public static class CommandThread extends Thread {
		private LogQuery query;
		private LogQueryCommand cmd;

		public CommandThread(Runnable r, int id) {
			super(r, "LogDB Command Thread " + id);
		}

		public LogQuery getQuery() {
			return query;
		}

		public LogQueryCommand getCommand() {
			return cmd;
		}

		public void set(LogQuery query, LogQueryCommand cmd) {
			this.query = query;
			this.cmd = cmd;
		}

		public void unset() {
			this.query = null;
			this.cmd = null;
		}
	}
}
