package com.fellow.yoo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fellow.yoo.utils.StringUtils;

public class Contact implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L; 
	
	
	private String firstName;
	private String lastName;
	private String company;
	private String jobTitle;
	private long contactId;
	private List<LabelledValue> emails;
	private List<LabelledValue> phones;
	private List<LabelledValue> messaging;
	private byte [] image;
	private boolean hasPhone;
	
	public Contact() {
		emails = new ArrayList<LabelledValue>();
		phones = new ArrayList<LabelledValue>();
		messaging = new ArrayList<LabelledValue>();
	}
	
	public Contact(long contactId) {
		this.contactId = contactId;
		emails = new ArrayList<LabelledValue>();
		phones = new ArrayList<LabelledValue>();
		messaging = new ArrayList<LabelledValue>();
	}
	
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getCompany() {
		return company;
	}
	public void setCompany(String company) {
		this.company = company;
	}
	public String getJobTitle() {
		return jobTitle;
	}
	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}
	public long getContactId() {
		return contactId;
	}
	public void setContactId(long contactId) {
		this.contactId = contactId;
	}
	public List<LabelledValue> getEmails() {
		return emails;
	}

	public List<LabelledValue> getPhones() {
		return phones;
	}
	
	public List<LabelledValue> getMessaging() {
		return messaging;
	}
	
	public byte[] getImage() {
		return image;
	}
	public void setImage(byte[] image) {
		this.image = image;
	}
	public boolean hasPhone() {
		return hasPhone;
	}
	public void setHasPhone(boolean hasPhone) {
		this.hasPhone = hasPhone;
	}
	

	public String toString() {
		StringBuffer name = new StringBuffer();
		name.append(!StringUtils.isEmpty(firstName)? firstName : "");
		if(!StringUtils.isEmpty(lastName)){
			name.append(" " + lastName);
		}
		return name.toString();
	}
	
}
