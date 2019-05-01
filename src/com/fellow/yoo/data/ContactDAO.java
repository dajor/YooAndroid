package com.fellow.yoo.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fellow.yoo.data.criteria.ConjCriteria;
import com.fellow.yoo.data.criteria.ConjCriteria.Conjunction;
import com.fellow.yoo.data.criteria.Criteria;
import com.fellow.yoo.data.criteria.EqualsCriteria;
import com.fellow.yoo.data.criteria.LikeCriteria;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.utils.StringUtils;

import android.annotation.SuppressLint;

public class ContactDAO extends BaseDAO {

	@Override
	public void initTables() {
		Map<String, String> columns = new HashMap<String, String>();
	    columns.put("contactid", "INTEGER PRIMARY KEY");
	    columns.put("firstname", "TEXT");
	    columns.put("lastname", "TEXT");
	    columns.put("hasphone", "TEXT");
	    checkTable("contact", columns);
	    createIndex("contact", false, Arrays.asList("lastname"));
	}
	
	
	private Contact mapRow(Map<String, String> row) {
	    Contact contact = new Contact();
	    contact.setContactId(Integer.parseInt(row.get("contactid")));
	    contact.setFirstName(row.get("firstname"));
	    contact.setLastName(row.get("lastname"));
	    contact.setHasPhone("1".equals(row.get("hasphone")));
	    return contact;
	}
	
	public List<Contact> list() {
		List<Contact> contacts = new ArrayList<Contact>();
		for (Map<String, String> row : select("contact", null, "lastname", 0)) {
			contacts.add(mapRow(row));
		}
	    return contacts;
	}
	
	public int count() {
		return select("contact", null, "lastname", 0).size();
	}
	
	public List<Long> allCcontactIds() { 
		List<Long> allContactIds = new ArrayList<Long>();
		List<Map<String, String>> rows = rawQuery("SELECT contactid FROM contact", null);
		for (Map<String, String> row : rows) {
			allContactIds.add(Long.valueOf(row.get("contactid")));
		}
	    return allContactIds;
	}
	
	@SuppressLint("UseSparseArrays")
	public Map<Long, String> allCcontactNames() { 
		Map<Long, String> contactNames = new HashMap<Long, String>();
		List<Map<String, String>> rows = rawQuery("SELECT * FROM contact", null);
		for (Map<String, String> row : rows) {
			Long id = Long.valueOf(row.get("contactid"));
			String firstName = StringUtils.isEmpty(row.get("firstname"))? "" : row.get("firstname");
			String lastName = StringUtils.isEmpty(row.get("lastname"))? "" : row.get("lastname"); // row.get("lastname");
			String hasPhone = StringUtils.isEmpty(row.get("hasphone"))? "" : row.get("hasphone"); 
			contactNames.put(id, firstName + lastName + hasPhone); 
		}
	    return contactNames;
	}
	
	private Map<String, String> bindItem(Contact contact){
		Map<String, String> item = new HashMap<String, String>();
		String fName = contact.getFirstName();
		String lName = contact.getLastName();
		
		item.put("firstname", StringUtils.isEmpty(fName) ? "" : fName);
		item.put("lastname", StringUtils.isEmpty(lName) ? "" : lName);
		
	    item.put("hasphone", contact.getPhones().size() > 0 ? "1" : "0");
		return item;
	}
	
	public Contact upsert(Contact contact) {
		Map<String, String> item = bindItem(contact);
	    
	    item.put("contactid", String.valueOf(contact.getContactId()));
	    
	    // check if we don't have already this user
	    Criteria idCrit = new EqualsCriteria("contactid", String.valueOf(contact.getContactId()));
	    
	 
	    Map<String, String> existing = selectOne("contact", idCrit);
	    if (existing != null) {
	    	//System.err.println("Update Contact : " + contact.getFirstName() + " " + contact.getLastName());
	    	update("contact", item, idCrit);
	    } else {
	    	//System.err.println("Insert Contact : " + contact.getFirstName() + " " + contact.getLastName());
	    	long conId = insert("contact", item);
	    	contact.setContactId(conId);
	    }
	    return contact;
	}
	
	// TODO will remove
	public Contact insert(Contact contact) {
		
		Map<String, String> item = bindItem(contact);
		if(contact.getContactId() > 0){
			item.put("contactid", String.valueOf(contact.getContactId()));
		}
    	contact.setContactId(insert("contact", item));
    	
	    return contact;
	}
	
	public Contact udpate(Contact contact) {
		Map<String, String> item = bindItem(contact);
		Criteria idCrit = new EqualsCriteria("contactid", String.valueOf(contact.getContactId()));
    	update("contact", item, idCrit);
		return contact;
	}
	
	public Contact find(long contactId) {
		if(contactId == 153){
			System.out.println(">>>>>>>>>> " + contactId); 
		}
	    Criteria idCrit = new EqualsCriteria("contactid", String.valueOf(contactId));
	    List<Map<String, String>> rows = select("contact", idCrit, null, 1);
	    if (rows.size() > 0) {
	    	return mapRow(rows.get(0));
	    } else {
	        return null;
	    }
	}
	
	public List<Contact> filterByName(String name) {
		List<Contact> contacts = new ArrayList<Contact>();
		ConjCriteria conCrit = new ConjCriteria(Conjunction.OR);
	    Criteria frNameCrit = new LikeCriteria("firstname", String.valueOf(name));
	    Criteria lastNameCrit = new LikeCriteria("lastname", String.valueOf(name));
	    conCrit.add(frNameCrit);
	    conCrit.add(lastNameCrit);
	    List<Map<String, String>> rows = select("contact", conCrit, null, 10000);
	    for (Map<String, String> cont : rows) {
	    	contacts.add(mapRow(cont));
		}
	    
	    return contacts;
	}
	
	public Contact findByFName(String fName, String lName) {
		List<Contact> contacts = new ArrayList<Contact>();
		ConjCriteria conCrit = new ConjCriteria(Conjunction.OR);
		
		if(!StringUtils.isEmpty(fName)){
			Criteria frNameCrit = new EqualsCriteria("firstname", fName.trim());
		    conCrit.add(frNameCrit);
		}
	    
	    if(!StringUtils.isEmpty(lName)){
	    	Criteria lastNameCrit = new EqualsCriteria("lastname", lName.trim());
		    conCrit.add(lastNameCrit);
	    }
	    
	    List<Map<String, String>> rows = select("contact", conCrit, null, 1);
	    for (Map<String, String> cont : rows) {
	    	contacts.add(mapRow(cont));
		}
	    
	    if(!contacts.isEmpty()){
	    	return contacts.get(0);
	    }
	    
	    return null;
	}
	
	public void deleteNullContactId(){
		String query = "DELETE FROM contact Where contactid ='' OR contactid IS NULL";
		execSQL(query, new String[]{});
	}
	
	public void deleteByContactId(Long contactId){
		String query = "DELETE FROM contact Where contactid ='' OR contactid ='" + contactId.toString() +"'";
		execSQL(query, new String[]{});
	}

	
	public Boolean isExisting(String table, Criteria idCrit) {
	    Map<String, String> existing = selectOne("contact", idCrit);
	    return (existing != null);
	}
	
	

}
