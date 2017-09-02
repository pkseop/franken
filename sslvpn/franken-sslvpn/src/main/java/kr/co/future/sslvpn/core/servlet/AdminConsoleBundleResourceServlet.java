package kr.co.future.sslvpn.core.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.httpd.ResourceServlet;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminConsoleBundleResourceServlet extends ResourceServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(AdminConsoleBundleResourceServlet.class.getName());
	private Bundle bundle;
	private String basePath;
	private String trustHosts;
	private Boolean doNotShowAdmin;
	
	public AdminConsoleBundleResourceServlet(Bundle bundle, String basePath, Boolean doNotShowAdmin, String trustHosts) {
		this.bundle = bundle;
		this.basePath = basePath;
		this.trustHosts = trustHosts;
		this.doNotShowAdmin = doNotShowAdmin;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.trace("frodo AdminConsoleBundleResourceServlet: httpd session [{}]", req.getSession().getId());
		
		//if this url starts with "http" then redirect to the url which starts with "https".
		if(!req.isSecure()) {
			String newUrl = getHttpsUrl(req);
			logger.trace("frodo AdminConsoleResourceServlet: url redirected to [{}]", newUrl);
			try {
				resp.sendRedirect(newUrl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		super.doGet(req, resp);
	}
	
	private String getHttpsUrl(HttpServletRequest req) {
		StringBuilder newUrl = new StringBuilder("https://");
        newUrl.append(req.getServerName());
        if (req.getRequestURI() != null) {
        	newUrl.append(req.getRequestURI());
        }
        String queryStr = req.getQueryString();
        if (queryStr != null && !queryStr.equals("")) {
        	newUrl.append("?").append(req.getQueryString());
        }
        return newUrl.toString();
	}

	@Override
	protected InputStream getInputStream(HttpServletRequest req) {
		try {
			logger.trace("frodo core: showAdmin : [{}], [{}], [{}]", new Object[] {doNotShowAdmin, trustHosts, req.getRemoteAddr()});
			
			// 관리자 화면 보이지 않게 설정된 경우 dom.trust_hosts 에 설정된 ip 에 대해서만 관리자 화면을 보여준다. 2014-05-20 sjkim77
			if (doNotShowAdmin != null && doNotShowAdmin){
				if (trustHosts != null && !trustHosts.equals("") && trustHosts.indexOf(req.getRemoteAddr()) < 0)
					return null;
			}
				
			logger.trace("frodo core: trying to open bundle [{}] resource, base path [{}], path info [{}]", new Object[] {
					bundle.getBundleId(), req.getRequestURI(), req.getPathInfo() });

			URL url = bundle.getResource(basePath + req.getPathInfo());
			return url.openStream();
		} catch (Exception e) {
			logger.trace("frodo core: cannot open bundle [{}] resource [{}]", bundle.getBundleId(), req.getRequestURI());
			return null;
		}
	}

	@Override
	public String toString() {
		return bundle.getEntry("/").toString();
	}
	
	public void setTrustHosts(String trustHosts) {
		this.trustHosts = trustHosts;
	}
}
