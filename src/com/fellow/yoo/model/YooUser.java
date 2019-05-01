package com.fellow.yoo.model;

import java.util.Date;

import org.jivesoftware.smack.util.StringUtils;

import com.fellow.yoo.YooApplication;

import android.content.Context;
import android.content.SharedPreferences;

public class YooUser implements YooRecipient {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1746427609133079062L;
	private String name;
	private String domain;
	private String alias;
	private long contactId;
	private byte [] picture;
	private Date lastOnline;
	private int callingCode;

	public YooUser(String jid) {
	    name = StringUtils.parseName(jid);
	    domain = StringUtils.parseServer(jid);
	    contactId = -1;
	    callingCode = -1;
	}
	
	public YooUser(String pName, String pDomain) {
	    name = pName;
	    domain = pDomain;
	    contactId = -1;
	    callingCode = -1;
	}
	
	public boolean isSame(YooUser other) {
	    return name.equals(other.name) && domain.equals(other.domain);
	}
	
	public String toJID() {
		return name + "@" + domain;
	}
	
	public String displayName() {
	    if (alias != null) return alias;
	    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
	
	public boolean isMe() {
		SharedPreferences preferences = YooApplication.getAppContext().getSharedPreferences(
				"YooPreferences", Context.MODE_PRIVATE);
		String login = preferences.getString("login", null);
		// String phone = preferences.getString("phoneNumber", null);
		return login.equals(name);
	}
	
	public byte [] getPicture() {
		return picture;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String pAlias) {
		alias = pAlias;
	}
	
	public void setContactId(long pContactId) {
		contactId = pContactId;
	}
	
	public void setPicture(byte [] pPicture) {
		picture = pPicture;
	}
	
	public void setCallingCode(int pCallingCode) {
		callingCode = pCallingCode;
	}
	
	public void setLastOnline(Date pLastOnline) {
		lastOnline = pLastOnline;
	}
	
	public long getContactId() {
		return contactId;
	}
	
	public int getCallingCode() {
		return callingCode;
	}
	
	public String toString() {
		if(com.fellow.yoo.utils.StringUtils.isEmpty(alias)){
			return name;
		}
		return alias;
	}
	
	public Date getLastOnline() {
		return lastOnline;
	}
	
}
