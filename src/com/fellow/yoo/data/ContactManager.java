package com.fellow.yoo.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fellow.yoo.YooApplication;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.LabelledValue;
import com.fellow.yoo.utils.StringUtils;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.widget.Toast;

public class ContactManager {
	
	public void importAll() {
		List<Contact> contacts = list();
		ContactDAO contactDAO = new ContactDAO();
		contactDAO.deleteNullContactId();
		
		List<Long> contactPhoneIds = new ArrayList<Long>();
		Map<Long, String> contactNames = contactDAO.allCcontactNames();
		Log.i(" #### List All Contacts >> ", contacts.size() + ""); 
		YooApplication.contacts.clear();
		
		int i = 1;
		for (Contact contact : contacts) {
			Contact detailed = getContactPhone(contact);  
			// getDetail(contact.getContactId()); no need to get full contact info. need only first and last name.
			if(contactNames.containsKey(contact.getContactId())){
				String fName = StringUtils.isEmpty(detailed.getFirstName()) ? "" : contact.getFirstName();
				String lName = StringUtils.isEmpty(detailed.getLastName()) ? "" : contact.getLastName();
				String hasPhone = detailed.hasPhone() ? "1" : "0";
				String cName = fName + lName + hasPhone;
				if(!cName.equals(contactNames.get(contact.getContactId()))){ // update only when field values has changed.
					contactDAO.udpate(detailed);
				}
			}else{
				contactDAO.insert(detailed);
			}
			contactPhoneIds.add(contact.getContactId());
			YooApplication.addContact(contact); 
			
			// String hasPhone = detailed.hasPhone() ? "1" : "0";
			// Log.w("Contact manager : Import contacts >> ", i + ". " + contact.toString() + " . hasPhone > " + hasPhone); 
			i++;
		}
		Log.w("Contact manager : Import contacts >> ", i + ""); 
		
		// remove contact that has been deleted from device.
		for (Long contactId : contactNames.keySet()) { 
			if(!contactPhoneIds.contains(contactId)){ // remove contact from Yoo.
				contactDAO.deleteByContactId(contactId);
			}
		}
	}
	

	public List<Contact> list() {
		List<Contact> contacts = new ArrayList<Contact>();
		List<String> checkDuplicate = new ArrayList<String>();
		try {
			Cursor cur = YooApplication.getAppContext().getContentResolver().query(Contacts.CONTENT_URI, null, null, null, null);
			
			if (cur.getCount() > 0) {
				while (cur.moveToNext()) {
					Contact contact = buildContact(cur);
					String name = contact.toString();
					if(!checkDuplicate.contains(name)){
						contacts.add(contact);
					}
					checkDuplicate.add(name);
				}
			}
			cur.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return contacts;
	}

	private Contact buildContact(Cursor cur) {
		long id = cur.getLong(cur.getColumnIndex(Contacts._ID));
		String dispName = cur.getString(cur.getColumnIndex(Contacts.DISPLAY_NAME));
		String [] names = extractName(dispName);
		Contact contact = new Contact();
		contact.setFirstName(names[0]);
		contact.setLastName(names[1]);
		contact.setContactId(id);
		return contact;
	}
	
	public Contact findById(long id) {
		Context context = YooApplication.getAppContext();		
		Cursor cur = context.getContentResolver().query(
				Contacts.CONTENT_URI, null, Contacts._ID + " = ?", new String [] {String.valueOf(id)}, null);
		Contact contact = null;
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				contact = buildContact(cur);
			}
		}
		cur.close();		
		return contact;
	}
	
	
	public Contact findByName(String name) {
		Context context = YooApplication.getAppContext();		
		Cursor cur = context.getContentResolver().query(
				Contacts.CONTENT_URI, null, Contacts.DISPLAY_NAME + " = ? COLLATE NOCASE", new String [] {name.trim()}, null);
		Contact contact = null;
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				contact = buildContact(cur);
			}
		}
		cur.close();		
		return contact;
	}
	
	
	public Contact getContactPhone(Contact contact){
		 // Get phone numbers
		if(contact != null){
			Context context = YooApplication.getAppContext();	
	        Cursor curPhones = context.getContentResolver().query(Phone.CONTENT_URI, null,
	                Phone.CONTACT_ID + " = ?", new String[] {String.valueOf(contact.getContactId())}, null);
	        while (curPhones.moveToNext()) {
	            String number = curPhones.getString(curPhones.getColumnIndex(Phone.NUMBER));
	            int type = curPhones.getInt(curPhones.getColumnIndex(Phone.TYPE));
	            contact.getPhones().add(new LabelledValue(type, number));
	            contact.setHasPhone(true); 
	        }
	        curPhones.close();
		}
        return contact;
	}
	
	public Contact getDetail(long contactId) {
		Context context = YooApplication.getAppContext();		
		Cursor cur = context.getContentResolver().query(Contacts.CONTENT_URI, null, Contacts._ID + " = ?", new String [] {String.valueOf(contactId)}, null);
		Contact contact = null;
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				contact = buildContact(cur);
			}
		}
		cur.close();
		
		if (contact == null){
			return null;
		}
		
		// Get company and title
        Cursor curOrg = context.getContentResolver().query(Data.CONTENT_URI,
                    null, Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?", 
                    new String[] {String.valueOf(contactId), ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE}, null);
        while (curOrg.moveToNext()) {
            String company = curOrg.getString(curOrg.getColumnIndex(CommonDataKinds.Organization.COMPANY));
            String title = curOrg.getString(curOrg.getColumnIndex(CommonDataKinds.Organization.TITLE));
           	contact.setCompany(company);
           	contact.setJobTitle(title);
        }
        curOrg.close();
		
        // Get phone numbers
        Cursor curPhones = context.getContentResolver().query(Phone.CONTENT_URI, null,
                Phone.CONTACT_ID + " = ?", new String[] {String.valueOf(contactId)}, null);
        while (curPhones.moveToNext()) {
            String number = curPhones.getString(curPhones.getColumnIndex(Phone.NUMBER));
            int type = curPhones.getInt(curPhones.getColumnIndex(Phone.TYPE));
            contact.getPhones().add(new LabelledValue(type, number));
        }
        curPhones.close();
        
		// Get email
        Cursor curEmails = context.getContentResolver().query(Email.CONTENT_URI, null,
                Phone.CONTACT_ID + " = ?", new String[] {String.valueOf(contactId)}, null);
        while (curEmails.moveToNext()) {
            String email = curEmails.getString(curEmails.getColumnIndex(Email.ADDRESS));
            int type = curEmails.getInt(curEmails.getColumnIndex(Email.TYPE));
            contact.getEmails().add(new LabelledValue(type, email));
        }
        curEmails.close();
		
        
		// Get messaging stuff
        Cursor curMessaging = context.getContentResolver().query(Data.CONTENT_URI, null,
                Data.CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?", new String[] {String.valueOf(contactId), Im.CONTENT_ITEM_TYPE}, null);
        while (curMessaging.moveToNext()) {
            String im = curMessaging.getString(curMessaging.getColumnIndex(Data.DATA1));
            int type = 0; //curMessaging.getInt(curPhones.getColumnIndex(Data.DATA5));
            contact.getMessaging().add(new LabelledValue(type, im));
        }
        curMessaging.close();
        
        Log.w("ContactManager : GetDetail >> ", contactDetail(contact));
        
        return contact;
	}
	
	public long createFromYoo(Contact contact) {

		Context context = YooApplication.getAppContext();

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		ops.add(ContentProviderOperation.newInsert(
		ContactsContract.RawContacts.CONTENT_URI)
		     .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
		     .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

		ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
		     .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
		     .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
		     .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.toString()).build());
		
		try {
			context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
		
		contact = new ContactManager().findByName(contact.toString());
		return contact.getContactId();
	}
	
	
	public long insetContact(Contact contact) {
		
		Context context = YooApplication.getAppContext();
		
		Contact cont = findByName(contact.toString());
		if(cont != null){
			return cont.getContactId();
		}
		

		long contactId = -1;
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		int rawContactID = ops.size();

		// Adding insert operation to operations list
		// to insert a new raw contact in the table ContactsContract.RawContacts
		ops.add(ContentProviderOperation.newInsert(
				ContactsContract.RawContacts.CONTENT_URI)
				.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
				.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

		// insert name
		ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
				.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.toString()).build());
		
		// insert company
		if(!StringUtils.isEmpty(contact.getCompany())){
			ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
				.withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
				.withValue(CommonDataKinds.Organization.COMPANY, contact.getCompany()).build());
		}
		
				
		// insert title
		if(!StringUtils.isEmpty(contact.getJobTitle())){
			ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
				.withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
				.withValue(CommonDataKinds.Organization.TITLE, contact.getJobTitle()).build());
		}
		

		// insert email 
		for (LabelledValue email : contact.getEmails()) {   
			ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
					.withValue(ContactsContract.Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
					.withValue(CommonDataKinds.Email.DATA, email.getValue())
					.withValue(CommonDataKinds.Email.TYPE, email.getType()).build());
				// Email.TYPE_CUSTOM = 0, Email.TYPE_HOME = 1, Email.TYPE_WORK = 2, Email.TYPE_OTHER = 3, Email.TYPE_MOBILE = 4, 
		}
		
		// insert phone numbers
		for (LabelledValue phone : contact.getPhones()) {
			ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
					.withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
					.withValue(Phone.NUMBER, phone.getValue())
					.withValue(Phone.TYPE, phone.getType()).build());
				// CommonDataKinds.Phone.TYPE_CUSTOM = 0, CommonDataKinds.Phone.TYPE_HOME = 1, CommonDataKinds.Phone.TYPE_MOBILE = 2, 
				// CommonDataKinds.Phone.TYPE_WORK = 3, CommonDataKinds.Phone.TYPE_OTHER = 7
		}
		
		// insert messages
		String[] dataKeys = new String[]{Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA5};
		for (int i = 0; i < dataKeys.length; i++) {
			if(i < contact.getMessaging().size()) {
				LabelledValue message = contact.getMessaging().get(i);
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
						.withValue(ContactsContract.Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
						.withValue(dataKeys[i], message.getValue()).build());
			}
		}
		
		try {
			ContentProviderResult[] results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			contactId = Integer.parseInt(results[0].uri.getLastPathSegment());
		} catch (Exception e) {
			Log.e("ContactManager : InsertContact >> ", e.getMessage());
		}

		return contactId;
	}
	
	public static String updateContact(Activity activity, Contact contact) {

		String contactId = String.valueOf(contact.getContactId());
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		// update name
		ops.add(ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI)
				.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
					new String[] { String.valueOf(contactId), StructuredName.CONTENT_ITEM_TYPE }).withValue(StructuredName.DISPLAY_NAME, contact.toString()).build());
		
		// update company
		ops.add(ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI)
				.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
					new String[] { String.valueOf(contactId), StructuredName.CONTENT_ITEM_TYPE }).withValue(CommonDataKinds.Organization.COMPANY, contact.toString()).build());
		
		// update title
		ops.add(ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI)
				.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
					new String[] { String.valueOf(contactId), StructuredName.CONTENT_ITEM_TYPE }).withValue(CommonDataKinds.Organization.TITLE, contact.toString()).build());

		// update email
		for (LabelledValue email : contact.getEmails()) {   
			ops.add(ContentProviderOperation
					.newUpdate(ContactsContract.Data.CONTENT_URI)
					.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?" + " AND " + Email.TYPE + "=?",
							new String[] { 
									String.valueOf(contactId), Email.CONTENT_ITEM_TYPE, 
									String.valueOf(email.getType()) }).withValue(Email.DATA, email.getValue()).build());
		}
		

		// update phone
		for (LabelledValue phone : contact.getPhones()) {
			ops.add(ContentProviderOperation
					.newUpdate(ContactsContract.Data.CONTENT_URI)
					.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?" + " AND " + Phone.TYPE + "=?",
							new String[] { 
									String.valueOf(contactId), Phone.CONTENT_ITEM_TYPE, 
									String.valueOf(phone.getType()) }).withValue(Phone.NUMBER, phone.getValue()).build());
		}
		

		// update city
		/*ops.add(ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI)
				.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
						new String[] { String.valueOf(contactId), StructuredPostal.CONTENT_ITEM_TYPE }).withValue(StructuredPostal.CITY, city).build());*/

		// update country
		/*ops.add(ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI)
				.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
						new String[] { String.valueOf(contactId), StructuredPostal.CONTENT_ITEM_TYPE }).withValue(StructuredPostal.COUNTRY, country).build());*/

		// update note
		/*ops.add(ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI)
				.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + " = ?",
						new String[] { String.valueOf(contactId), Note.CONTENT_ITEM_TYPE }).withValue(Note.NOTE, note).build());*/

		
		try {

			activity.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(activity, "Update Contact : " + e.getMessage(), Toast.LENGTH_SHORT).show();
			return "-1";
		}

		return contactId;
	}
	
	private String contactDetail(Contact contact){
		StringBuffer sb = new StringBuffer();
		if(contact != null){
			sb.append("FirstName : " + contact.getFirstName() + ", ");
			sb.append("LastName : " + contact.getLastName() + ", ");
			sb.append("Company : " + contact.getCompany() + ", ");
			sb.append("JobTitle : " + contact.getJobTitle() + ", ");
			for (LabelledValue lv : contact.getPhones()) { 
				sb.append("phone : " + lv.getType() + " >>  " + lv.getValue() + ", ");
			}
			for (LabelledValue lv : contact.getEmails()) { 
				sb.append("email : " + lv.getType() + " >>  " + lv.getValue() + ", ");
			}
		}
		return sb.toString();
	}
	
	public static String [] extractName(String dispName) {
		String [] names = new String [] {null, null};
		String [] parts = dispName.split(" ");
		if (parts.length == 1) {
			names[0] = parts[0]; 
		} else if (parts.length >= 2) {
			names[1] = parts[parts.length - 1];
			names[0] = "";
			for (int i = 0; i < parts.length - 1; i++) {
				if (i > 0) names[0] += " ";
				names[0] += parts[i];
			}
		}
		return names;
	}
}
