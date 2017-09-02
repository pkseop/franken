package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("client_apps")
public class ClientApp implements Marshalable {
	private int id;

	private String guid = UUID.randomUUID().toString();

	private String appGuid;

	@FieldOption(nullable = false)
	private String platform;

	@FieldOption(nullable = false, length = 20)
	private String name;

	@FieldOption(length = 20)
	private String operator;

	@FieldOption(length = 40)
	private String phone;

	@FieldOption(name = "icon")
	private String icon;

	@FieldOption(length = 250)
	private String url;

	@FieldOption(length = 250)
	private String description;

	@FieldOption(length = 40)
	private String mobile;

	@FieldOption(length = 100)
	private String email;

	private Map<String, Object> metadata;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getAppGuid() {
		return appGuid;
	}

	public void setAppGuid(String appGuid) {
		this.appGuid = appGuid;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getMobile() {
		return mobile;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", id);
		m.put("guid", guid);
		m.put("app_guid", appGuid);
		m.put("platform", platform);
		m.put("name", name);
		m.put("operator", operator);
		m.put("phone", phone);
		m.put("mobile", mobile);
		m.put("desc", description);
		m.put("email", email);
		m.put("icon", icon);
		m.put("metadata", metadata);
		return m;
	}

}
