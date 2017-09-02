package kr.co.future.sslvpn.core;

import kr.co.future.sslvpn.core.TrendGraphType;

public enum TrendGraphType {
	Cpu, Memory, Disk, TxBytes, RxBytes, Tunnel, Session;
	
	public static TrendGraphType parse(String s) {
		for (TrendGraphType t : values())
			if (t.toString().equalsIgnoreCase(s))
				return t;
		
		return null;
	}
}
