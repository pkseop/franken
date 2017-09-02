package kr.co.future.sslvpn.model.api;

import java.util.List;

import kr.co.future.sslvpn.model.UserQuery;

public interface UserQueryApi {

	String createUserQuery(UserQuery userQuery);

	void updateUserQuery(UserQuery userQuery);

	List<String> removeUserQueries(String owner, List<String> guids);

	List<UserQuery> getUserQueries(String owner);
}
