package kr.co.future.logdb.mapreduce;

import kr.co.future.logdb.LogQuery;

import kr.co.future.rpc.RpcConnection;

public class MapQuery {
	/**
	 * mapreduce query guid
	 */
	private String guid;

	/**
	 * upstream rpc connection
	 */
	private RpcConnection connection;

	/**
	 * local node's query
	 */
	private LogQuery query;

	public MapQuery(String guid, RpcConnection connection) {
		this.guid = guid;
		this.connection = connection;
	}

	public String getGuid() {
		return guid;
	}

	public RpcConnection getConnection() {
		return connection;
	}

	public LogQuery getQuery() {
		return query;
	}

	public void setQuery(LogQuery query) {
		this.query = query;
	}

}
