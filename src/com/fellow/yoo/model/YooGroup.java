package com.fellow.yoo.model;

import java.util.Date;

import com.fellow.yoo.chat.ChatTools;

public class YooGroup implements YooRecipient {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3543745581287165771L;
	private String name;
	private String alias;
	private Date date;
	private String member;
	
	public YooGroup() {		
	}

	public YooGroup(String pName, String pAlias) {
		name = pName;
		alias = pAlias;
	}
	public String toJID() {
		return name + "@" + ChatTools.CONFERENCE_DOMAIN;
	}


	public boolean isMe() {
	    return false;
	}
	
	public void setMember(String pMember) {
		member = pMember;
	}
	
	public void setName(String pName) {
		name = pName;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String pAlias) {
		alias = pAlias;
	}
	
	public void setDate(Date pDate) {
		date = pDate;
	}
	
	public String getMember() {
		return member;
	}
	
	public Date getDate() {
		if(date == null){
			return new Date();
		}
		return date;
	}
	
	
	public String toString() {
		if(com.fellow.yoo.utils.StringUtils.isEmpty(alias)){
			return name;
		}
		return alias;
	}

	
}
