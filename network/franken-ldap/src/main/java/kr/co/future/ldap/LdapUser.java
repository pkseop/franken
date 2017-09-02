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

import com.unboundid.ldap.sdk.SearchResultEntry;

public class LdapUser {
    private String accountName;
    private boolean domainAdmin;
    private boolean allowDialIn;
    private int logonCount;
    private int userAccountControl;
    private String[] memberOf;
    private String distinguishedName;
    private String userPrincipalName;
    private String organizationUnitName;
    private String displayName;
    private String surname;
    private String givenName;
    private String title;
    private String department;
    private String departmentNumber;
    private String mail;
    private String mobile;
    private Date lastLogon;
    private Date whenCreated;
    private Date pwdLastSet;
    private Date accountExpires;

    @SuppressWarnings("unused")
    private LdapUser() {
        // for primitive parse
    }

    public LdapUser(SearchResultEntry entry, String idAttr) {
        this.accountName = getString(entry, "sAMAccountName");
        if (accountName == null)
            accountName = getString(entry, idAttr);
        this.domainAdmin = getInt(entry, "adminCount") > 0;
        this.userAccountControl = getInt(entry, "userAccountControl");
        this.allowDialIn = "TRUE".equals(getString(entry, "msNPAllowDialin"));
        this.logonCount = getInt(entry, "logonCount");
        this.memberOf = getStringArray(entry, "memberOf");
        this.distinguishedName = getString(entry, "distinguishedName");
        this.userPrincipalName = getString(entry, "userPrincipalName");
        this.displayName = getString(entry, "displayName");
        if (displayName == null)
            displayName = getString(entry, "cn");
        this.surname = getString(entry, "sn");
        this.givenName = getString(entry, "givenName");
        this.title = getString(entry, "title");
        this.department = getString(entry, "department");
        this.departmentNumber = getString(entry, "departmentNumber");
        this.mail = getString(entry, "mail");
        this.mobile = getString(entry, "mobile");
        this.lastLogon = getTimestamp(entry, "lastLogon");
        this.whenCreated = getDate(entry, "whenCreated");
        this.pwdLastSet = getTimestamp(entry, "pwdLastSet");
        long expire = getLong(entry, "accountExpires");
        if (expire != 0L && expire != 0x7FFFFFFFFFFFFFFFL)
            this.accountExpires = getTimestamp(entry, "accountExpires");

        if (distinguishedName != null) {
            for (String token : distinguishedName.split("(?<!\\\\),")) {
                String attr = token.split("=")[0];
                String value = token.split("=")[1];
                if (attr.equals("OU")) {
                    this.organizationUnitName = value;
                    break;
                }
            }
        }
    }

    private int getInt(SearchResultEntry entry, String attrName) {
    	Integer val = entry.getAttributeValueAsInteger(attrName);
        return val == null ? 0 : val;
    }

    private long getLong(SearchResultEntry entry, String attrName) {
    	Long val = entry.getAttributeValueAsLong(attrName);
        return val == null ? 0L : val;
    }

    private Date getDate(SearchResultEntry entry, String attrName) {
        return entry.getAttributeValueAsDate(attrName);
    }

    private Date getTimestamp(SearchResultEntry entry, String attrName) {
        Long attr = getLong(entry, attrName);
        return (attr == null) ? null : new Date(attr / 10000L - 11644473600000L);
    }

    private String getString(SearchResultEntry entry, String attrName) {
        return entry.getAttributeValue(attrName);
    }

    private String[] getStringArray(SearchResultEntry entry, String attrName) {
        return entry.getAttributeValues(attrName);
    }

    public String getAccountName() {
        return accountName;
    }

    public boolean isDomainAdmin() {
        return domainAdmin;
    }

    public boolean isAllowDialIn() {
        return allowDialIn;
    }

    public int getLogonCount() {
        return logonCount;
    }

    public int getUserAccountControl() {
        return userAccountControl;
    }

    public String[] getMemberOf() {
        return memberOf;
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public String getUserPrincipalName() {
        return userPrincipalName;
    }

    public String getOrganizationUnitName() {
        return organizationUnitName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSurname() {
        return surname;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getTitle() {
        return title;
    }

    public String getDepartment() {
        return department;
    }

    public String getDepartmentNumber() {
        return departmentNumber;
    }

    public String getMail() {
        return mail;
    }

    public String getMobile() {
        return mobile;
    }

    public Date getLastLogon() {
        return lastLogon;
    }

    public Date getWhenCreated() {
        return whenCreated;
    }

    public Date getPwdLastSet() {
        return pwdLastSet;
    }

    public Date getAccountExpires() {
        return accountExpires;
    }

    @Override
    public String toString() {
        return String.format("account=%s, name=%s, title=%s, dept=%s, mail=%s", accountName, displayName, nullToEmpty(title),
                nullToEmpty(department), nullToEmpty(mail));
    }

    private String nullToEmpty(String str) {
        return (str == null) ? "" : str;
    }
}
