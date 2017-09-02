package kr.co.future.sslvpn.core.login;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientIpRange;
import kr.co.future.sslvpn.model.IpLease;
import kr.co.future.sslvpn.model.UserExtension;

public class CheckValidClientIpStage extends Stage {

	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		UserExtension ext = loginContext.getUserExt();
		AccessProfile profile = loginContext.getProfile();
		InetSocketAddress remote = loginContext.getRemote();
		
		try{
			// check client ip range
			if (profile.isVerifyClientIp()) {
				long ip = IpLease.toLong(remote.getAddress().getHostAddress());
				List<ClientIpRange> ipRanges = new ArrayList<ClientIpRange>();
	
				if (!ext.getAllowIpRanges().isEmpty())
					ipRanges = ext.getAllowIpRanges();
				else
					ipRanges = profile.getClientIpRanges();
	
				if (!checkIpRanges(ip, ipRanges)) {
					loginContext.setResult(loginUtil.fail(AuthCode.ClientIPRange, profile));
					return;
				}
			}
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
	}
	
	private boolean checkIpRanges(long hostAddress, List<ClientIpRange> ipRanges) {
		if (ipRanges.isEmpty())
			return true;

		for (ClientIpRange ipRange : ipRanges) {
			long ipFrom = IpLease.toLong(ipRange.getIpFrom());
			long ipTo = IpLease.toLong(ipRange.getIpTo());

			if (hostAddress >= ipFrom && hostAddress <= ipTo)
				return true;
		}
		return false;
	}

}
