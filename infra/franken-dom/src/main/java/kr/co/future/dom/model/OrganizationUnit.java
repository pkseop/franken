/*
 * Copyright 2011 Future Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.dom.model;

import kr.co.future.api.FieldOption;
import kr.co.future.api.MapTypeHint;
import kr.co.future.confdb.CollectionName;
import kr.co.future.dom.model.OrganizationUnit;

import java.text.SimpleDateFormat;
import java.util.*;

@CollectionName("organization-unit")
public class OrganizationUnit {
	@FieldOption(nullable = false)
	private String guid = UUID.randomUUID().toString();

	@FieldOption(length = 60, nullable = false)
	private String name;

	private String parent;

    @FieldOption(length = 100, nullable = true)
    private String name_desc;

	@MapTypeHint({ String.class, Object.class })
	private Map<String, Object> ext = new HashMap<String, Object>();

	@FieldOption(skip = true)
	private List<OrganizationUnit> children = new ArrayList<OrganizationUnit>();

	@FieldOption(nullable = false)
	private Date created = new Date();

	@FieldOption(nullable = false)
	private Date updated = new Date();
	
	private String sourceType;

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

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public Map<String, Object> getExt() {
		return ext;
	}

	public void setExt(Map<String, Object> ext) {
		this.ext = ext;
	}

	public List<OrganizationUnit> getChildren() {
		return children;
	}

	public void setChildren(List<OrganizationUnit> children) {
		this.children = children;
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

    public String getName_desc() {
        return name_desc;
    }

    public void setName_desc(String name_desc) {
        this.name_desc = name_desc;
    }
    
    public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	@Override
	public String toString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return "guid=" + guid + ", name=" + name + ", updated=" + dateFormat.format(updated);
	}

}
