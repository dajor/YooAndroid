package com.fellow.yoo.xmpp;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;

public class IQFindUser extends IQ {
	
	
	
	public static class UserInfo {
		private long contactId;
		private String name;
		private int callingCode;
		
		public UserInfo() {			
		}

		public long getContactId() {
			return contactId;
		}

		public void setContactId(long contactId) {
			this.contactId = contactId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getCallingCode() {
			return callingCode;
		}

		public void setCallingCode(int callingCode) {
			this.callingCode = callingCode;
		}
		
		
	}
	
	private List<UserInfo> users = new ArrayList<UserInfo>();
	
	public List<UserInfo> getUsers() {
		return users;
	}
	
	@Override
	public String getChildElementXML() {
		return "";
	}
}
