package kr.co.future.logdb.mapreduce;

import java.util.Collection;
import java.util.List;

import kr.co.future.logdb.query.command.RpcFrom;
import kr.co.future.logdb.query.command.RpcTo;

import kr.co.future.rpc.RpcConnection;
import kr.co.future.rpc.RpcConnectionProperties;

public interface MapReduceService {
	RpcFrom getRpcFrom(String guid);

	RpcTo getRpcTo(String guid);

	List<MapReduceQueryStatus> getQueries();

	MapReduceQueryStatus createQuery(String query);

	void startQuery(String guid);

	void removeQuery(String guid);

	List<RemoteQuery> getRemoteQueries();

	Collection<RpcConnection> getUpstreamConnections();

	Collection<RpcConnection> getDownstreamConnections();

	RpcConnection connect(RpcConnectionProperties props);

	void disconnect(String guid);
}
