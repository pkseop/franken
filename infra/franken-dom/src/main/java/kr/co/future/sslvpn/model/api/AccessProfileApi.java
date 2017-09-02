package kr.co.future.sslvpn.model.api;

import java.util.List;

import kr.co.future.dom.api.EntityEventProvider;
import kr.co.future.dom.model.User;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.OrgUnitExtension;

public interface AccessProfileApi extends EntityEventProvider<AccessProfile> {
	List<AccessProfile> getAccessProfiles();

	AccessProfile getProfile(int id);

	AccessProfile determineOrgUnitProfile(String orgUnitName);

	AccessProfile determineProfile(String loginName);

	AccessProfile determineProfile(User user);

	AccessProfile getDefaultProfile();

	AccessProfile getAccessProfile(String guid);

	String createAccessProfile(AccessProfile p);

	void updateAccessProfile(AccessProfile p);

	void removeAccessProfile(String guid);

	void removeAccessProfiles(List<String> guids);

	OrgUnitExtension findOrgUnitExtension(String orgUnitId);

	void setOrgUnitExtension(String orgUnitId, String profileId, boolean setChildren);

	List<Object> serialize();

	boolean isAllAuthMethodSame();
	
	boolean isAllEncryptionsSame();
}
