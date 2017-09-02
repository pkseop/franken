package kr.co.future.sslvpn.core;

import java.util.Collection;

import kr.co.future.sslvpn.core.TestCrl;

public interface TestCrlApi {

	void addTestCrl(TestCrl crl);

	void removeTestCrl(String alias);

	void start(int seconds);

	void stop();

	void setTrace(boolean useTrace);

	boolean isRunning();

	Collection<TestCrl> getTestCrls();
}
