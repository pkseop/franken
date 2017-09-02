package kr.co.future.sslvpn.core.msgbus;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.NacClientPolicy;
import kr.co.future.sslvpn.model.NacProfile;
import kr.co.future.sslvpn.model.api.NacProfileApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.AllowGuestAccess;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

@Component(name = "frodo-nac-profile-plugin")
@MsgbusPlugin
public class NacProfilePlugin extends DefaultEntityEventListener<NacProfile> {

	@Requires
	private NacProfileApi nacProfileApi;

	@SuppressWarnings("unchecked")
	@AllowGuestAccess
	@MsgbusMethod
	public void getConfig(Request req, Response resp) {
		NacClientPolicy policy = nacProfileApi.getClientPolicy();

		resp.putAll((Map<? extends String, ? extends Object>) PrimitiveConverter.serialize(policy));
	}

	@MsgbusMethod
	public void getProfiles(Request req, Response resp) {

		Collection<NacProfile> profiles = nacProfileApi.getNacProfiles();

		resp.put("profiles", PrimitiveConverter.serialize(profiles));
	}

	@MsgbusMethod
	public void getProfile(Request req, Response resp) {
		String guid = req.getString("guid");
		NacProfile profile = nacProfileApi.getNacProfile(guid);

		resp.put("profile", PrimitiveConverter.serialize(profile));
	}

	@MsgbusMethod
	public void createProfile(Request req, Response resp) {
		NacProfile p = (NacProfile) PrimitiveConverter.overwrite(new NacProfile(), req.getParams());
		String guid = nacProfileApi.createNacProfile(p);

		resp.put("guid", guid);
	}

	@MsgbusMethod
	public void updateProfile(Request req, Response resp) {
		NacProfile p = (NacProfile) PrimitiveConverter.overwrite(new NacProfile(), req.getParams());
		nacProfileApi.updateNacProfile(p);
	}

	@MsgbusMethod
	public void removeProfile(Request req, Response resp) {
		String guid = req.getString("guid");
		nacProfileApi.removeNacProfile(guid);
	}

	@MsgbusMethod
	public void removeProfiles(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		List<String> profiles = (List<String>) req.get("guids");
		nacProfileApi.removeNacProfiles(profiles);
	}
}
