/*
 * Copyright 2011 Future Systems
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
package kr.co.future.ldap;

import java.util.Date;

import kr.co.future.api.DateFormat;

import com.unboundid.ldap.sdk.SearchResultEntry;

public class LdapOrgUnit {
    private String distinguishedName;
    private String name;
    private Date whenCreated;
    private Date whenChanged;

    @SuppressWarnings("unused")
    private LdapOrgUnit() {
        // for primitive parse
    }

    public LdapOrgUnit(SearchResultEntry entry) {
        this.distinguishedName = getString(entry, "distinguishedName");
        this.name = getString(entry, "name");
        if (this.name == null)
            this.name = getString(entry, "ou");

        this.whenCreated = getDate(entry, "whenCreated");
        this.whenChanged = getDate(entry, "whenChanged");
    }

    private String getString(SearchResultEntry entry, String attrName) {
        return entry.getAttributeValue(attrName);
    }

    private Date getDate(SearchResultEntry entry, String attrName) {
        return entry.getAttributeValueAsDate(attrName);
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public String getName() {
        return name;
    }

    public Date getWhenCreated() {
        return whenCreated;
    }

    public Date getWhenChanged() {
        return whenChanged;
    }

    @Override
    public String toString() {
        return String.format("name=%s, whenCreated=%s, whenChanged=%s", name,
                DateFormat.format("yyyy-MM-dd HH:mm:ss", whenCreated), DateFormat.format("yyyy-MM-dd HH:mm:ss", whenChanged));
    }
}
