package kr.co.future.sslvpn.core.msgbus;

import java.util.List;

import kr.co.future.sslvpn.model.UserQuery;
import kr.co.future.sslvpn.model.api.UserQueryApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPermission;
import kr.co.future.msgbus.handler.MsgbusPlugin;

@Component(name = "frodo-user-query-plugin")
@MsgbusPlugin
public class UserQueryPlugin {

	@Requires
	private UserQueryApi queryApi;

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_view")
	public void getUserQueries(Request req, Response resp) {
		String owner = req.getAdminLoginName();
		List<UserQuery> queries = queryApi.getUserQueries(owner);

		resp.put("queries", PrimitiveConverter.serialize(queries));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_view")
	public void createUserQuery(Request req, Response resp) {
		UserQuery query = toUserQuery(req);

		String guid = queryApi.createUserQuery(query);

		resp.put("guid", guid);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_view")
	public void updateUserQuery(Request req, Response resp) {
		UserQuery query = toUserQuery(req);
		queryApi.updateUserQuery(query);

		resp.put("guid", query.getGuid());
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "config_view")
	public void removeUserQueries(Request req, Response resp) {
		String owner = req.getAdminLoginName();

		@SuppressWarnings("unchecked")
		List<String> guids = (List<String>) req.get("guids");

		resp.put("guids", queryApi.removeUserQueries(owner, guids));
	}

	private UserQuery toUserQuery(Request req) {
		UserQuery query = new UserQuery();
		if (req.has("guid") && req.getString("guid") != null)
			query.setGuid(req.getString("guid"));

		query.setOwner(req.getAdminLoginName());
		query.setTitle(req.getString("title"));
		if (req.has("description"))
			query.setDescription(req.getString("description"));

		query.setQuery(req.getString("query"));

		return query;
	}
}
