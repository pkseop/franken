package kr.co.future.sslvpn.core.msgbus;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import kr.co.future.sslvpn.core.CertCenterApi;
import kr.co.future.sslvpn.core.msgbus.CertCenterPlugin;
import kr.co.future.sslvpn.model.CertificateData;
import kr.co.future.sslvpn.model.QueryResult;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONConverter;
import org.json.JSONException;

import kr.co.future.ca.RevocationReason;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.User;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPermission;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-cert-center-plugin")
@MsgbusPlugin
public class CertCenterPlugin {
	private final Logger logger = LoggerFactory.getLogger(CertCenterPlugin.class.getName());

	@Requires
	private CertCenterApi certApi;

	@Requires
	private UserApi domUserApi;

	@Requires
	private AdminApi adminApi;

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_edit")
	public void revokeCert(Request req, Response resp) {
		String serial = req.getString("serial");
		if (serial == null)
			throw new MsgbusException("frodo", "serial-not-found");

		String reason = req.getString("reason");
		if (reason == null)
			throw new MsgbusException("frodo", "reason-not-found");

		Admin admin = adminApi.findAdmin(req.getOrgDomain(), req.getAdminLoginName());
		if (admin == null)
			throw new MsgbusException("frodo", "admin-not-found");

		CertificateData cm = certApi.findValidCert(serial);
		if (cm == null)
			throw new MsgbusException("frodo", "certficate-not-found");

		User user = domUserApi.findUser(req.getOrgDomain(), cm.getLoginName());
		if (user == null)
			throw new MsgbusException("frodo", "user-not-found");

		if (!adminApi.canManage(req.getOrgDomain(), admin, user))
			throw new MsgbusException("frodo", "no-permission");

		certApi.revokeCert(serial, RevocationReason.valueOf(reason));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_view")
	public void getValidCert(Request req, Response resp) {
		String serial = req.getString("serial");
		if (serial == null)
			throw new MsgbusException("frodo", "serial-not-found");

		CertificateData data = certApi.findValidCert(serial);
		try {
			resp.put("valid_certs", JSONConverter.jsonize(data.marshal()));
		} catch (JSONException e) {
			logger.error("frodo model: cannot convert to json");
		}
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_view")
	public void getValidCerts(Request req, Response resp) {

		long begin = new Date().getTime();
		Predicate pred = null;
		if (req.has("keyword") || (req.has("ou_guid") && req.getString("ou_guid") != null)) {
			if (req.has("keyword"))
				pred = new KeyMatcher(req.getString("keyword"));

			Collection<String> loginNames = domUserApi.getLoginNames("localhost", req.getString("ou_guid"), true, pred, 0,
					Integer.MAX_VALUE);
			pred = Predicates.in("login_name", loginNames);
		}

		int offset = 0;
		int limit = Integer.MAX_VALUE;

		if (req.has("offset"))
			offset = req.getInteger("offset");
		if (req.has("limit"))
			limit = req.getInteger("limit");

		QueryResult certs = certApi.getValidCerts(pred, offset, limit);

		long end = new Date().getTime();
		logger.trace("frodo core: completed get valid certs, [{}]ms elapsed", end - begin);

		resp.put("valid_certs", Marshaler.marshal(certs.getItems()));
		resp.put("total_count", certs.getTotalCount());
	}

	private static class KeyMatcher implements Predicate {
		private String name;

		public KeyMatcher(String name) {
			this.name = name;
		}

		@Override
		public boolean eval(Config c) {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>) c.getDocument();
			String owner = (String) m.get("name");
			if (owner == null)
				return false;
			String loginName = (String) m.get("login_name");
			if (loginName == null)
				return false;

			return owner.contains(name) || loginName.contains(name);
		}
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_view")
	public void getRevokedCerts(Request req, Response resp) {
		Integer offset = req.getInteger("offset");
		Integer limit = req.getInteger("limit");
		String keyword = req.getString("keyword");
		String from = req.getString("from");
		String to = req.getString("to");

		QueryResult result = certApi.getRevokedCerts(offset, limit, keyword, from, to);

		resp.put("revoked_certs", Marshaler.marshal(result.getItems()));
		resp.put("total_count", result.getTotalCount());
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_view")
	public void getIssuedCerts(Request req, Response resp) {
		Integer offset = req.getInteger("offset");
		Integer limit = req.getInteger("limit");
		String keyword = req.getString("keyword");
		String from = req.getString("from");
		String to = req.getString("to");

		QueryResult result = certApi.getIssuedCerts(offset, limit, keyword, from, to);

		resp.put("issued_certs", Marshaler.marshal(result.getItems()));
		resp.put("total_count", result.getTotalCount());
	}
}
