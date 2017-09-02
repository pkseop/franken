package kr.co.future.sslvpn.core.impl;

import javax.naming.CommunicationException;

public class CrlConnectionErrorException extends Exception {
	private static final long serialVersionUID = 1L;

	public CrlConnectionErrorException(CommunicationException e) {
		super(e);
	}

}
