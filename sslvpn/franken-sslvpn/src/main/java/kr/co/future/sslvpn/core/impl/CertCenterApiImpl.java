package kr.co.future.sslvpn.core.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.ca.CertificateAuthority;
import kr.co.future.ca.CertificateAuthorityService;
import kr.co.future.ca.CertificateMetadata;
import kr.co.future.ca.BaseCertificateAuthorityListener;
import kr.co.future.ca.RevocationReason;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigCollection;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.EntityEventListener;
import kr.co.future.dom.api.EntityState;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.logdb.LogQuery;
import kr.co.future.logdb.LogQueryService;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.sslvpn.core.CertCenterApi;
import kr.co.future.sslvpn.model.Cert;
import kr.co.future.sslvpn.model.CertificateData;
import kr.co.future.sslvpn.model.QueryResult;

@Component(name = "frodo-cert-center-api")
@Provides
public class CertCenterApiImpl implements CertCenterApi {
	private final Logger logger = LoggerFactory.getLogger(CertCenterApiImpl.class.getName());

	@Requires
	private UserApi domUserApi;

	@Requires
	private ConfigService conf;

	@Requires
	private CertificateAuthorityService ca;

	@Requires
	private LogStorage logStorage;

	@Requires
	private LogQueryService qs;

	private CertificateListener certEventListener;

	/**
	 * temporary store for revoke operation
	 */
	private ConcurrentHashMap<String, User> removedUsers = new ConcurrentHashMap<String, User>();

	private EntityEventListener<User> userEventListener = new DefaultEntityEventListener<User>() {

		@Override
		public void entitiesRemoved(String domain, Collection<EntityState> objs) {
			CertificateAuthority authority = ca.getAuthority("local");
			if (authority == null)
				return;

			HashSet<String> loginNames = new HashSet<String>();
			for (EntityState e : objs) {
				User user = (User) e.entity;
				removedUsers.put(user.getLoginName(), user);
				loginNames.add(user.getLoginName());
			}

			ConfigDatabase db = conf.ensureDatabase("frodo");
			ConfigIterator it = db.find(Cert.class, Predicates.in("login_name", loginNames));

			ConfigTransaction xact = db.beginTransaction();
			try {
				while (it.hasNext()) {
					Config c = it.next();
					Cert cert = c.getDocument(Cert.class);

					CertificateMetadata cm = authority.findCertificate("serial", cert.getSerial());
					if (cm != null)
						revokeCert(cm.getSerial(), RevocationReason.Unspecified);
					else
						db.remove(c);
				}

				xact.commit("frodo-core", "cert center revoked certificates caused by user deletion");
			} catch (Throwable t) {
				logger.error("frodo core: cannot revoke certificates caused by user deletion", t);
				xact.rollback();
			}
		}

		@Override
		public void entityRemoved(String domain, User user, Object state) {
			ConfigDatabase db = conf.ensureDatabase("frodo");
			Config c = db.findOne(Cert.class, Predicates.field("login_name", user.getLoginName()));
			if (c == null)
				return;

			removedUsers.put(user.getLoginName(), user);
			Cert cert = c.getDocument(Cert.class);
			CertificateAuthority authority = ca.getAuthority("local");
			if (authority != null) {
				CertificateMetadata cm = authority.findCertificate("serial", cert.getSerial());
				revokeCert(cm.getSerial(), RevocationReason.Unspecified);
			} else
				db.remove(c);
		}
	};

	public CertCenterApiImpl() {
		certEventListener = new CertificateListener();
	}

	@Validate
	public void start() {
		conf.ensureDatabase("frodo").ensureCollection(Cert.class);
		domUserApi.addEntityEventListener(userEventListener);

		ca.addListener(certEventListener);
	}

	@Invalidate
	public void stop() {
		if (domUserApi != null)
			domUserApi.removeEntityEventListener(userEventListener);

		if (ca != null)
			ca.removeListener(certEventListener);
	}

	@Override
	public CertificateData findValidCert(String serial) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(Cert.class, Predicates.field("serial", serial));

		if (c == null)
			return null;

		Cert cert = c.getDocument(Cert.class);
		CertificateAuthority authority = ca.getAuthority("local");
		if (authority == null)
			return null;

		CertificateMetadata cm = authority.findCertificate("serial", cert.getSerial());
		if (cm == null)
			return null;

		User user = domUserApi.findUser("localhost", cert.getLoginName());
		if (user == null)
			return null;

		return toCertificateData(cert.getLoginName(), user.getOrgUnit() == null ? null : user.getOrgUnit().getName(),
				cert.getSerial(), cm.getIssuedDate(), cm.getNotAfter(), cm.getNotBefore(), user.getName(), null,
				user.getOrgUnit() == null ? null : user.getOrgUnit().getGuid());
	}

	@Override
	public QueryResult getValidCerts() {
		return getValidCerts(null, 0, Integer.MAX_VALUE);
	}

	@Override
	public QueryResult getValidCerts(Predicate pred, int offset, int limit) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		QueryResult result = new QueryResult();

		// 밑에 부분은 나중에 ensureCollection시 파일을 무조건 생성하도록 수정 하면 고쳐야 함
		ConfigCollection col = db.ensureCollection(Cert.class);
		if (col.count() == 0) {
			result.setItems(new ArrayList<CertificateData>());
			result.setTotalCount(0);
			return result;
		}
		int total = db.count(Cert.class, pred);
		ConfigIterator it = db.find(Cert.class, pred);

		List<Cert> certs = (List<Cert>) it.getDocuments(Cert.class, null, offset, limit);
		Set<String> loginNames = new HashSet<String>();
		Set<String> serials = new HashSet<String>();
		for (Cert cert : certs) {
			loginNames.add(cert.getLoginName());
			serials.add(cert.getSerial());
		}

		Map<String, User> users = toUserMap(loginNames);
		Map<String, CertificateMetadata> metadatas = getCertificateMetadataMap(serials); // predicate
		List<CertificateData> datas = new ArrayList<CertificateData>();
		for (Cert cert : certs) {
			// fix old version's bug (didn't revoke certificate when delete
			// user)
			CertificateMetadata cm = metadatas.get(cert.getSerial());
			if (cm == null) {
				logger.warn("frodo core: cannot find certificate metadata, remove cert [{}]", cert);
				revokeCert(cert.getSerial(), RevocationReason.Unspecified);
				continue;
			}

			User user = users.get(cert.getLoginName());
			if (user == null) {
				logger.warn("frodo core: cannot find user, remove cert [{}]", cert);
				revokeCert(cert.getSerial(), RevocationReason.Unspecified);
				continue;
			}

			OrganizationUnit orgUnit = user.getOrgUnit();
			CertificateData cd = toCertificateData(cert.getLoginName(), orgUnit == null ? null : orgUnit.getName(),
					cert.getSerial(), cm.getIssuedDate(), cm.getNotAfter(), cm.getNotBefore(), user.getName(), null,
					orgUnit == null ? null : orgUnit.getGuid());
			datas.add(cd);
		}

		result.setItems(datas);
		result.setTotalCount(total);
		return result;
	}

	@Override
	public void revokeCert(String serial, RevocationReason reason) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(Cert.class, Predicates.field("serial", serial));

		if (c == null)
			return;

		CertificateAuthority authority = ca.getAuthority("local");
		CertificateMetadata cm = authority.findCertificate("serial", serial);
		if (cm != null) {
			try {
				authority.revoke(cm, reason);
			} catch (IllegalStateException e) {
				logger.warn("frodo core: already revoked, remove cert [{}]", c.getDocument(Cert.class));
			}
		}
		db.remove(c);
	}

	@Override
	public QueryResult getIssuedCerts(Integer offset, Integer limit, String keyword, String from, String to) {
		return getQueryResult("ssl-ca", offset, limit, keyword, from, to);
	}

	@Override
	public QueryResult getRevokedCerts(Integer offset, Integer limit, String keyword, String from, String to) {
		return getQueryResult("ssl-revoked", offset, limit, keyword, from, to);
	}

	private QueryResult getQueryResult(String table, Integer offset, Integer limit, String keyword, String from, String to) {
		String query = "table";
		if (from != null && !from.isEmpty())
			query += " from=" + from;

		if (to != null && !to.isEmpty())
			query += " to=" + to;

		query += " " + table + " | fields login_name, org_unit_name, user_name, _time, not_before, not_after, serial, reason";

		if (keyword != null && !keyword.trim().isEmpty())
			query += "|search login_name contain " + keyword;

		LogQuery lq = qs.createQuery(query);
		List<CertificateData> datas = new ArrayList<CertificateData>();
		long totalCount = 0;
		try {
			qs.startQuery(lq.getId());

			do {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logger.error("frodo core: log query interrupted", e);
				}
			} while (!lq.isEnd());

			Long resultCount = lq.getResultCount();
			if (resultCount == null)
				logger.warn("frodo core: query result is null, query [{}]", query);
			totalCount = resultCount == null ? 0 : resultCount;

			List<Map<String, Object>> results = null;
			if (offset != null && limit != null)
				results = lq.getResultAsList(offset, limit);
			else
				results = lq.getResultAsList();

			if (results != null)
				for (Map<String, Object> m : results) {
					String loginName = (String) m.get("login_name");
					Date issuedDate = (Date) m.get("_time");
					Date notBefore = (Date) m.get("not_before");
					Date notAfter = (Date) m.get("not_after");
					String serial = (String) m.get("serial");
					String orgUnitName = (String) m.get("org_unit_name");
					String orgUnitGuid = (String) m.get("org_unit_guid");
					String userName = (String) m.get("user_name");
					String reason = (String) m.get("reason");
					datas.add(toCertificateData(loginName, orgUnitName, serial, issuedDate, notAfter, notBefore, userName,
							reason, orgUnitGuid));
				}
		} catch (IOException e) {
			throw new IllegalStateException("cert query failed " + lq, e);
		} finally {
			qs.removeQuery(lq.getId());
		}
		QueryResult result = new QueryResult();
		result.setItems(datas);
		result.setTotalCount((int) totalCount);
		return result;
	}

	private Map<String, User> toUserMap(Set<String> loginNames) {
		Map<String, User> users = new HashMap<String, User>();
		for (User user : domUserApi.getUsers("localhost", loginNames)) {
			users.put(user.getLoginName(), user);
		}
		return users;
	}

	private CertificateData toCertificateData(String loginName, String orgUnitName, String serial, Date issuedDate,
			Date notAfter, Date notBefore, String userName, String reason, String orgUnitGuid) {
		CertificateData cd = new CertificateData();
		cd.setLoginName(loginName);
		cd.setOrgUnitName(orgUnitName);
		cd.setSerial(serial);
		cd.setIssuedDate(issuedDate);
		cd.setNotAfter(notAfter);
		cd.setNotBefore(notBefore);
		cd.setUserName(userName);
		cd.setReason(reason);
		cd.setOrgUnitGuid(orgUnitGuid);
		return cd;
	}

	private Map<String, CertificateMetadata> getCertificateMetadataMap(Set<String> serials) {
		Map<String, CertificateMetadata> m = new HashMap<String, CertificateMetadata>();
		CertificateAuthority authority = ca.getAuthority("local");
		for (CertificateMetadata cm : authority.getCertificates(Predicates.in("serial", serials))) {
			m.put(cm.getSerial(), cm);
		}

		return m;
	}

	private class CertificateListener extends BaseCertificateAuthorityListener {
		@Override
		public void onRevokeCert(CertificateAuthority authority, CertificateMetadata cm, RevocationReason reason) {
			if (!authority.getName().equals("local"))
				return;

			try {
				// DN == CN
				String cn = cm.getSubjectDn();
				String loginName = cn.substring(3);
				String name = null;
				OrganizationUnit ou = null;
				User user = domUserApi.findUser("localhost", loginName);
				if (user == null) {
					User removedUser = removedUsers.remove(loginName);
					if (removedUser == null)
						return;

					name = removedUser.getName();
					ou = removedUser.getOrgUnit();
				} else {
					name = user.getName();
					ou = user.getOrgUnit();
				}
				Map<String, Object> data = buildLogData(cm, cn, loginName, name, ou);
				data.put("reason", reason.toString());

				Log log = new Log("ssl-revoked", new Date(), data);
				logStorage.write(log);

				ConfigDatabase db = conf.ensureDatabase("frodo");
				Config c = db.findOne(Cert.class, Predicates.field("serial", cm.getSerial()));

				if (c != null)
					db.remove(c);
			} catch (IllegalStateException e) {
				logger.error("frodo core: cannot write revoked log", e);
			} catch (Exception e) {
				logger.error("frodo core: cannot add cert");
			}
		}

		@Override
		public void onIssueCert(CertificateAuthority authority, CertificateMetadata cm) {
			if (!authority.getName().equals("local"))
				return;

			Cert cert = new Cert();
			try {
				// DN == CN
				String cn = cm.getSubjectDn();
				String loginName = cn.substring(3);

				User user = domUserApi.findUser("localhost", loginName);
				if (user == null)
					return;

				OrganizationUnit ou = user.getOrgUnit();
				Map<String, Object> data = buildLogData(cm, cn, loginName, user.getName(), ou);

				Log log = new Log("ssl-ca", new Date(), data);
				logStorage.write(log);

				cert.setSerial(cm.getSerial());
				cert.setLoginName(loginName);

				ConfigDatabase db = conf.ensureDatabase("frodo");
				db.add(cert);

			} catch (IllegalStateException e) {
				logger.error("frodo core: cannot write issued log", e);
			} catch (Exception e) {
				logger.error("frodo core: cannot add cert [{}]", cert);
			}

		}

		private Map<String, Object> buildLogData(CertificateMetadata cm, String cn, String loginName, String userName,
				OrganizationUnit orgUnit) {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("user_name", userName);
			data.put("org_unit_name", orgUnit == null ? null : orgUnit.getName());
			data.put("org_unit_guid", orgUnit == null ? null : orgUnit.getGuid());
			data.put("subject", cn);
			data.put("login_name", loginName);
			data.put("not_before", cm.getNotBefore());
			data.put("not_after", cm.getNotAfter());
			data.put("serial", cm.getSerial());
			return data;
		}
	}
}
