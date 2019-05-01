package com.fellow.yoo.model;

public class ContactInfo {
	
	
	public Contact getContact() {
		return contact;
	}

	private Contact contact; // never null 
	private YooUser user; // can be null
	
	
	public ContactInfo(Contact pContact, YooUser pUser) {
		contact = pContact;
		user = pUser;
	}
	
	public String toString() {
		return contact.toString();
	}
	
	public long getContactId() {
		return contact.getContactId();
	}
	
	public YooUser getUser() {
		return user;
	}
	
	
	
}
