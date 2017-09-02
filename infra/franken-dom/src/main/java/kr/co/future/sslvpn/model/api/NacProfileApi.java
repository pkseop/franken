package kr.co.future.sslvpn.model.api;

import java.util.List;

import kr.co.future.dom.api.EntityEventProvider;
import kr.co.future.sslvpn.model.NacClientPolicy;
import kr.co.future.sslvpn.model.NacProfile;

public interface NacProfileApi extends EntityEventProvider<NacProfile> {
	NacClientPolicy getClientPolicy();

	List<NacProfile> getNacProfiles();

	NacProfile getNacProfile(String guid);

	String createNacProfile(NacProfile p);

	void updateNacProfile(NacProfile p);

	void removeNacProfile(String guid);

	void removeNacProfiles(List<String> guids);
}
