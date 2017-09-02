package kr.co.future.sslvpn.core;

import java.util.Date;

import kr.co.future.sslvpn.core.AlertCategory;
import kr.co.future.sslvpn.core.AlertStatus;
import kr.co.future.sslvpn.core.LicenseStatus;
import kr.co.future.sslvpn.core.LiveStatus;
import kr.co.future.sslvpn.core.NetworkService;

public interface DashboardService {
	LiveStatus getServiceStatus(NetworkService name);

	LicenseStatus getLicenseStatus();

	AlertStatus getAlert(AlertCategory category);

	void setAlert(AlertCategory category, Date date, boolean alert);
}
