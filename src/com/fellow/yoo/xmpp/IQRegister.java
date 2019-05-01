package com.fellow.yoo.xmpp;

import org.jivesoftware.smack.packet.IQ;

public class IQRegister extends IQ {
	
	private String username;
	private String password;

	@Override
	public String getChildElementXML() {
		return "";
	}
	
	public void setUsername(String pUsername) {
		username = pUsername;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setPassword(String pPassword) {
		password = pPassword;
	}
	
	public String getPassword() {
		return password;
	}

}
