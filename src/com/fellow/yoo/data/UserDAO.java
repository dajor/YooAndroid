package com.fellow.yoo.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.util.StringUtils;

import com.fellow.yoo.YooApplication;
import com.fellow.yoo.data.criteria.ConjCriteria;
import com.fellow.yoo.data.criteria.ConjCriteria.Conjunction;
import com.fellow.yoo.data.criteria.Criteria;
import com.fellow.yoo.data.criteria.EqualsCriteria;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.DateUtils;

public class UserDAO extends BaseDAO {
	
	@Override
	public void initTables() {
		Map<String, String> columns = new HashMap<String, String>();
	    columns.put("jid", "TEXT");
	    columns.put("alias", "TEXT");
	    columns.put("contactid", "INTEGER");
	    columns.put("picture", "BLOB");
	    columns.put("callingcode", "TEXT");
	    columns.put("lastonline", "TEXT");
	    checkTable("yoouser", columns);
	    createIndex("yoouser", true, Arrays.asList("jid"));
	    createIndex("yoouser", false, Arrays.asList("contactid"));	
	}
	
	public YooUser findByCriteria(Criteria criteria) {
		Map<String, String> existing = selectOne("yoouser", criteria);
	    if (existing != null) {
	    	return mapRow(existing);
	    }
	    return null;
	}
	
	public YooUser findByJid(String jid) {
	    Criteria jidCrit = new EqualsCriteria("jid", jid);
	    return findByCriteria(jidCrit);
	}
	
	public YooUser findByContactId(Long contactId) {
	    Criteria conIdCrit = new EqualsCriteria("contactid", contactId.toString());
	    return findByCriteria(conIdCrit);
	}
	
	public YooUser find(String name, String domain) {
		return findByJid(name + "@" + domain);
	}
	
	public void upsert(YooUser user) {
		Map<String, String> item = new HashMap<String, String>();
		item.put("jid", user.toJID());
		if (user.getAlias() != null) {
			item.put("alias", user.getAlias());
		}
		if (user.getPicture() != null) {
			item.put("picture", StringUtils.encodeBase64(user.getPicture()));
		}
		if (user.getContactId() != -1) {
			item.put("contactid", String.valueOf(user.getContactId()));
		}
		if (user.getCallingCode() != -1) {
			item.put("callingcode", String.valueOf(user.getCallingCode()));
		}

	    // check if we don't have already this user
	    Criteria jidCrit = new EqualsCriteria("jid", user.toJID());
	    Map<String, String> existing = selectOne("yoouser", jidCrit);

	    if (existing != null) {
	    	update("yoouser", item, jidCrit);
	    } else {
	    	insert("yoouser", item);
	    }
	}
	
	public YooUser updateLastOnline(String jId, Date lastOnline) {
	    Criteria jidCrit = new EqualsCriteria("jid", jId);
	    Map<String, String> existing = selectOne("yoouser", jidCrit);
	    if (existing != null) {
	    	 Map<String, String> updateUser = new HashMap<String, String>();
	    	 updateUser.put("lastonline", DateUtils.formatTime(lastOnline)); 
	    	update("yoouser", updateUser, jidCrit);
	    	
	    	return mapRow(existing);
	    } 
	    return null;
	}
	
	public String getUserGroupName(List<String> userIds) {
		StringBuffer names = new StringBuffer();
		ConjCriteria conCrit = new ConjCriteria(Conjunction.OR);
		for (String userId : userIds) {
			conCrit.add(new EqualsCriteria("jid", userId));
		}
		YooUser me = YooApplication.getUserLogin();
	    List<Map<String, String>> rows = select("yoouser", conCrit, null, 10000);
	    for (Map<String, String> cont : rows) {
	    	if(!me.toJID().equals(cont.get("jid"))){
	    		if(names.length() > 0){
					names.append(", ");
				}
		    	names.append(cont.get("alias")); 
	    	}
		}
	    
	    return names.toString();
	}
	
	public List<YooUser> list() {
	    List<YooUser> users = new ArrayList<YooUser>();
	    List<Map<String, String>> rows = select("yoouser", null, null, 0);
	    for (Map<String, String> row : rows) {
	    	users.add(mapRow(row));
	    }
	    return users;
	}

	private YooUser mapRow(Map<String, String> row) {
	    YooUser user = new YooUser(row.get("jid"));
	    user.setAlias(row.get("alias"));
	    if (row.get("contactid") != null) {
	    	user.setContactId(Long.parseLong(row.get("contactid")));
	    }
	    if (row.get("picture") != null) {
	    	user.setPicture(StringUtils.decodeBase64(row.get("picture")));
	    }
	    if (row.get("callingcode") != null) {
	    	user.setCallingCode(Integer.parseInt(row.get("callingcode")));
	    }
	    if (row.get("lastonline") != null) {
	    	user.setLastOnline(DateUtils.parseString(row.get("lastonline"))); 
	    }
	    return user;
	}
	


	
	public void purge() {
		delete("yoouser", null);
	}

}
