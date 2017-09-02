package kr.co.future.sslvpn.core.login;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.UserLimitException;
import kr.co.future.rpc.RpcException;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorStage extends Stage{

	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());
	
	@Override
   public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		BaseError error = (BaseError)context.getErrors().get(0);
		Exception e = error.getRelatedException();
		
		logger.error("frodo core: cannot login", e);
	   
		if(e instanceof UserLimitException) {
			loginContext.setResult(loginUtil.fail(AuthCode.UserLimit, null));
			return;
		}
		
		throw new RpcException(e.getMessage());
   }
	
}
