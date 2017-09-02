package kr.co.future.sslvpn.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kr.co.future.api.FieldOption;
import kr.co.future.api.ReferenceKey;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("org_units")
public class OrgUnitExtension implements Marshalable {
	private String guid = UUID.randomUUID().toString();

	@FieldOption(name = "org_unit_id")
	private String orgUnitId;

	@ReferenceKey("guid")
	private AccessProfile profile;

	@FieldOption(name = "created_at", nullable = false)
	private Date createDateTime;

	@FieldOption(name = "updated_at", nullable = false)
	private Date updateDateTime;

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getOrgUnitId() {
		return orgUnitId;
	}

	public void setOrgUnitId(String orgUnitId) {
		this.orgUnitId = orgUnitId;
	}

	public AccessProfile getProfile() {
		return profile;
	}

	public void setProfile(AccessProfile profile) {
		this.profile = profile;
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

	@Override
	public Map<String, Object> marshal() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("org_unit_id", orgUnitId);
		m.put("profile_id", profile == null ? null : profile.getGuid());
		m.put("created_at", dateFormat.format(createDateTime));
		m.put("updated_at", dateFormat.format(updateDateTime));
		return m;
	}
}
