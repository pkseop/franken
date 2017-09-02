package kr.co.future.sslvpn.core.login;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.cluster.ClusterConfig;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.IpLease;
import kr.co.future.sslvpn.model.IpLeaseRange;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.core.impl.InternalIp;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpLeaseStage extends Stage {

	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());
	
	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		String loginName = loginContext.getLoginName();
		User user = loginContext.getUser();
		UserExtension ext = loginContext.getUserExt();
		AccessProfile profile = loginContext.getProfile();
		int tunnelId = loginContext.getTunnelId();
		
		if(profile.getIpLeaseRanges().size() == 0) {
			logger.error("lease ip range doesn't exist.");
			loginContext.setResult(loginUtil.fail(AuthCode.IpLeaseFail, profile));
			return;
		}
		
		try{
			InetAddress leaseIp = null;
			ClusterConfig clusterConfig = loginUtil.cluster.getConfig();

			// tun0를 포함한 bridge 상태 일경우 internalIp 및 netmask 를 반환 하도록 한다.
			InternalIp internalValue = new InternalIp(loginUtil.internalLogger, loginUtil.bridgeCache, profile);
			InetAddress tunIp = internalValue.getIp();
			InetAddress tunNetmask = internalValue.getNetmask();

			if (clusterConfig.isEnabled() && clusterConfig.isClusterLease()) {
				leaseIp = leaseFromCluster(loginUtil, user.getLoginName(), profile, tunIp, tunNetmask);
				if (leaseIp == null) {
					loginContext.setResult(loginUtil.fail(AuthCode.IpLeaseFail, profile));
					return;
				}
			} else {
				int retryCnt = 1;

			while (retryCnt <= 100) {		//무한루프의 위험이 있기 때문에 시도를 최대 100번까지 하도록 수정.
                logger.info("try request lease ip...count: " + retryCnt);

                if (retryCnt == 1) {
                    leaseIp = loginUtil.ipLeaseApi.request(tunnelId, user, tunIp, tunNetmask, false);
                } else {
                    leaseIp = loginUtil.ipLeaseApi.request(tunnelId, user, tunIp, tunNetmask, true);
                }

                logger.info("get request lease ip: " + leaseIp);

                if(leaseIp != null)
                 	 break;
                
                retryCnt++;
            }
			}
			
			if (leaseIp != null) {
				ext.setLoginFailures(0);
				ext.setLastPasswordFailTime(null);
				ext.setLastLoginTime(new Date());
				ext.setLastIp(leaseIp.getHostAddress());
				loginUtil.submitUpdate(ext.getUser());
			} else {
				loginContext.setResult(loginUtil.fail(AuthCode.IpLeaseFail, profile));
				return;
			}

			if (loginUtil.externalAuth.isEnabled() && loginUtil.externalAuth.useSso()) {
				if (!loginUtil.externalAuth.verifySso(loginName, leaseIp.getHostAddress())) {
					logger.error("frodo core: cannot connect sso server");
					loginContext.setResult(loginUtil.fail(AuthCode.RpcError, profile));
					return;
				}
			}
			
			loginContext.setUserExt(ext);
			loginContext.setLeaseIp(leaseIp);
			
		} catch (IllegalStateException e) {
			logger.warn("frodo core: cannot lease ip", e);
			loginContext.setResult(loginUtil.fail(AuthCode.IpLeaseFail, profile));
			return;
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
	   
	}
	
	private InetAddress leaseFromCluster(LoginUtil loginUtil, String loginName, AccessProfile profile, InetAddress tunIp, InetAddress tunNetmask) {
		// tun의 범위
		long longTunIp = IpLease.toLong(tunIp.getHostAddress());
		long longTunNetmask = IpLease.toLong(tunNetmask.getHostAddress());

		long tunFrom = (longTunIp & longTunNetmask) + 1;
		long tunTo = longTunIp | (~longTunNetmask & 0xFFFFFFFL);

		// 프로필에 설정되어 있는 대역의 총 범위를 poolSize로
		List<IpLeaseRange> filteredIpLeaseRanges = loginUtil.ipLeaseApi.getFilteredRanges(profile.getIpLeaseRanges(), tunFrom, tunTo);
		List<Integer> offsets = loginUtil.ipLeaseApi.getPoolSizeOffsets(filteredIpLeaseRanges);
		int poolSize = offsets.get(offsets.size() - 1);

		// 아무것도 임대대역에 걸치치 못하면?
		if (offsets.isEmpty()) {
			logger.error("frodo core: offsets are empty. do not Iplease");
			return null;
		}

		if (logger.isDebugEnabled())
			logger.debug("frodo core: cluster lease request login [{}], profile [{}], pool size [{}]", new Object[] { loginName,
					profile.getName(), poolSize });

		int offset = loginUtil.cluster.leaseIp(loginName, profile.getName(), poolSize);
		if (offset == -1)
			return null;

		long candidate = loginUtil.ipLeaseApi.calculateIpAddress(filteredIpLeaseRanges, offsets, offset);

		InetAddress ip = IpLease.toInetAddress(candidate);
		if (logger.isDebugEnabled()) {
			logger.debug("frodo core: cluster lease result => login [{}], profile [{}], offset [{}], ip [{}]", new Object[] {
					loginName, profile.getName(), offset, ip });
		}

		return ip;
	}
	
}
