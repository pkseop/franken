package kr.co.future.sslvpn.core;

import kr.co.future.ca.RevocationReason;
import kr.co.future.confdb.Predicate;

import kr.co.future.sslvpn.model.CertificateData;
import kr.co.future.sslvpn.model.QueryResult;

public interface CertCenterApi {
	CertificateData findValidCert(String serial);

	QueryResult getValidCerts();

	QueryResult getValidCerts(Predicate pred, int offset, int limit);

	QueryResult getIssuedCerts(Integer offset, Integer limit, String keyword, String from, String to);

	QueryResult getRevokedCerts(Integer offset, Integer limit, String keyword, String from, String to);

	void revokeCert(String serial, RevocationReason reson);
}
