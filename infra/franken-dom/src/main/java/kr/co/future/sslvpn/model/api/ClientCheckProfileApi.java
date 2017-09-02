package kr.co.future.sslvpn.model.api;

import java.util.List;

import kr.co.future.sslvpn.model.ClientCheckProfile;

public interface ClientCheckProfileApi {

	List<ClientCheckProfile> getClientCheckProfiles();

	ClientCheckProfile getClientCheckProfile(String guid);

	String createClientCheckProfile(ClientCheckProfile p);

	void updateClientCheckProfile(ClientCheckProfile p);

	void removeClientCheckProfile(String guid);

	void removeClientCheckProfiles(List<String> guids);

	void addListener(ClientCheckProfileEventListener listener);

	void removeListener(ClientCheckProfileEventListener listener);
}
