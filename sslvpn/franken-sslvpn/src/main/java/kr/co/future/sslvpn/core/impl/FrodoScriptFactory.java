package kr.co.future.sslvpn.core.impl;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.DashboardService;
import kr.co.future.sslvpn.core.ExternalAuthService;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.InstallerApi;
import kr.co.future.sslvpn.core.TestCrlApi;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.ClientAppApi;
import kr.co.future.sslvpn.model.api.IpLeaseApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.BundleManager;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.UserApi;
import kr.co.future.httpd.HttpService;
import kr.co.future.ldap.LdapService;
import kr.co.future.sslvpn.core.ImportCsv;
import kr.co.future.sslvpn.core.impl.FrodoScript;

import org.osgi.framework.BundleContext;

@Component(name = "frodo-script-factory")
@Provides
public class FrodoScriptFactory implements ScriptFactory {
	@ServiceProperty(name = "alias", value = "frodo")
	private String alias;

	@Requires
	private ConfigService conf;

	@Requires
	private AuthService auth;

	@Requires
	private IpLeaseApi leaseApi;

	@Requires
	private DashboardService dash;

	@Requires
	private HttpService httpd;

	@Requires
	private ExternalAuthService externalAuth;

	@Requires
	private LdapService ldap;

	@Requires
	private UserApi domUserApi;

	@Requires
	private AccessGatewayApi gatewayApi;

	@Requires
	private TestCrlApi testCrlApi;

	@Requires
	private ClientAppApi clientAppApi;

	@Requires
	private GlobalConfigApi configApi;

	@Requires
	private BundleManager bundleManager;

	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;

	@Requires
	private InstallerApi installer;

	private BundleContext bc;
	
	@Requires
	private ImportCsv importCsv;
	
	@Requires
   private AuthorizedDeviceApi authDeviceApi;

	public FrodoScriptFactory(BundleContext bc) {
		this.bc = bc;
	}

	@Override
	public Script createScript() {
		return new FrodoScript(bc, conf, auth, leaseApi, dash, httpd, externalAuth, ldap, domUserApi, gatewayApi, testCrlApi,
				clientAppApi, configApi, userApi, bundleManager, installer, importCsv, authDeviceApi);
	}
}
