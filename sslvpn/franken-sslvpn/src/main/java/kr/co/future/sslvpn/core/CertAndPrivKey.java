package kr.co.future.sslvpn.core;

import kr.co.future.ca.CertificateMetadata;

public class CertAndPrivKey {
	private byte[] localCACert;

	private byte[] x509Certificate;

	private byte[] pkcs8PrivKey;

	private CertificateMetadata metadata;

	public byte[] getLocalCACert() {
		return localCACert;
	}

	public void setLocalCACert(byte[] localCACert) {
		this.localCACert = localCACert;
	}

	public byte[] getX509Certificate() {
		return x509Certificate;
	}

	public void setX509Certificate(byte[] x509Certificate) {
		this.x509Certificate = x509Certificate;
	}

	public byte[] getPkcs8PrivKey() {
		return pkcs8PrivKey;
	}

	public void setPkcs8PrivKey(byte[] pkcs8PrivKey) {
		this.pkcs8PrivKey = pkcs8PrivKey;
	}

	public CertificateMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(CertificateMetadata metadata) {
		this.metadata = metadata;
	}

}
