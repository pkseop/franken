package kr.co.future.sslvpn.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;

@CollectionName("servers")
public class Server implements Marshalable {
	private String guid = UUID.randomUUID().toString();

	@FieldOption(nullable = false, length = 60)
	private String name;

	@FieldOption(length = 250)
	private String description;

	@FieldOption(name = "operator", length = 20)
	private String operator;

	@FieldOption(name = "phone", length = 60)
	private String phone;

	@FieldOption(name = "created_at", nullable = false)
	private Date createDateTime;

	@FieldOption(name = "updated_at", nullable = false)
	private Date updateDateTime;

	@CollectionTypeHint(IpEndpoint.class)
	private List<IpEndpoint> endpoints = new ArrayList<IpEndpoint>();

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public Date getCreateDateTime() {
		return createDateTime;
	}

	public void setCreateDateTime(Date createDateTime) {
		this.createDateTime = createDateTime;
	}

	public Date getUpdateDateTime() {
		return updateDateTime;
	}

	public void setUpdateDateTime(Date updateDateTime) {
		this.updateDateTime = updateDateTime;
	}

	public List<IpEndpoint> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(List<IpEndpoint> endpoints) {
		this.endpoints = endpoints;
	}

	@Override
	public Map<String, Object> marshal() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("guid", guid);
		m.put("name", name);
		m.put("description", description);
		m.put("operator", operator);
		m.put("phone", phone);
		m.put("created_at", dateFormat.format(createDateTime));
		m.put("updated_at", dateFormat.format(updateDateTime));
		m.put("endpoints", Marshaler.marshal(endpoints));
		return m;
	}

}
