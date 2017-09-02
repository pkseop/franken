package kr.co.future.sslvpn.model;

import java.util.Date;
import java.util.UUID;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("user_queries")
public class UserQuery {

	@FieldOption(name = "guid", nullable = false)
	private String guid = UUID.randomUUID().toString();

	@FieldOption(name = "title", nullable = false)
	private String title;

	@FieldOption(name = "description", nullable = true)
	private String description;

	@FieldOption(name = "owner", nullable = false)
	private String owner;

	@FieldOption(name = "query", nullable = false)
	private String query;

	@FieldOption(name = "created", nullable = true)
	private Date created;

	@FieldOption(name = "updated", nullable = true)
	private Date updated;

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	@Override
	public String toString() {
		return "UserQuery [guid=" + guid + ", title=" + title + ", description=" + description + ", owner=" + owner
				+ ", query=" + query + ", created=" + created + ", updated=" + updated + "]";
	}
}
