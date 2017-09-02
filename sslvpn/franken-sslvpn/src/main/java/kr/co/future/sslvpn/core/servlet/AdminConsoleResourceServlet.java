package kr.co.future.sslvpn.core.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.servlet.AdminConsoleResourceServlet;
import kr.co.future.httpd.ResourceServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminConsoleResourceServlet extends ResourceServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(AdminConsoleResourceServlet.class.getName());
	private String path;
	private String trustHosts;
	private Boolean doNotShowAdmin;

	public AdminConsoleResourceServlet(String path, Boolean doNotShowAdmin, String trustHosts) {
		this.path = path;
		this.trustHosts = trustHosts;
		this.doNotShowAdmin = doNotShowAdmin;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.trace("frodo AdminConsoleResourceServlet: httpd session [{}]", req.getSession().getId());
		
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
		
		logger.trace("frodo core: showAdmin : [{}], [{}], [{}]", new Object[] {doNotShowAdmin, trustHosts, req.getRemoteAddr()});
		
		// 관리자 화면 보이지 않게 설정된 경우 dom.trust_hosts 에 설정된 ip 에 대해서만 관리자 화면을 보여준다. 2014-05-20 sjkim77
		if (doNotShowAdmin != null && doNotShowAdmin){
			if (trustHosts != null && !trustHosts.equals("") && trustHosts.indexOf(req.getRemoteAddr()) < 0)
				return null;
		}
		
		File webDir = new File(path);

		if (!webDir.exists() || !webDir.isDirectory())
			return null;

		File index = new File(webDir, req.getPathInfo());
		InputStream is = null;

		try {
			is = new FileInputStream(index);
		} catch (Exception e) {
			return null;
		}

		return is;
	}

	public void setTrustHosts(String trustHosts) {
		this.trustHosts = trustHosts;
	}
}
