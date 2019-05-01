package com.fellow.yoo.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.jivesoftware.smack.util.StringUtils;

import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.criteria.ConjCriteria;
import com.fellow.yoo.data.criteria.ConjCriteria.Conjunction;
import com.fellow.yoo.data.criteria.Criteria;
import com.fellow.yoo.data.criteria.EqualsCriteria;
import com.fellow.yoo.model.YooGroup;

import android.util.Log;

public class GroupDAO extends BaseDAO {
	

	@Override
	public void initTables() {
		
		Map<String, String> columns = new HashMap<String, String>();
	    columns.put("jid", "TEXT");
	    columns.put("alias", "TEXT");
	    columns.put("date", "TEXT");
	    checkTable("yoogroup", columns);
	    createIndex("yoogroup", true, Arrays.asList("jid"));

		Map<String, String> columns2 = new HashMap<String, String>();
	    columns2.put("groupjid", "TEXT");
	    columns2.put("userjid", "TEXT");
	    checkTable("groupmember", columns2);
	    createIndex("groupmember", true, Arrays.asList("groupjid", "userjid"));
		
	}
	
	public List<YooGroup> list() {
	    List<YooGroup> groups = new ArrayList<YooGroup>();
	    List<Map<String, String>> rows = select("yoogroup", null, null, 0);
	    for (Map<String, String> row : rows) {
	    	groups.add(mapRow(row));
	    }
	    return groups;
	}
	
	public List<String> listMembers(String groupJid) {
		Criteria groupCrit = new EqualsCriteria("groupjid", groupJid);
		List<String> members = new ArrayList<String>();
		List<Map<String, String>> rows = select("groupmember", groupCrit, null, 0);
		for (Map<String, String> row : rows) {
			members.add(row.get("userjid"));
	    }
	    return members;
	}
	
	
	private YooGroup findByCriteria(Criteria criteria) {
		List<Map<String, String>> existing = select("yoogroup", criteria, null, 1);
	    if (existing.size() > 0) {
	    	Map<String, String> row = existing.get(0);
	        return mapRow(row);
	    }
	    return null;
	}
	
	public YooGroup find(String name) {
		Criteria jidCrit = new EqualsCriteria("jid", name + "@" + ChatTools.CONFERENCE_DOMAIN);
	    return findByCriteria(jidCrit);
	}
	
	public YooGroup findByJid(String jid) {
		Criteria jidCrit = new EqualsCriteria("jid", jid);
	    return findByCriteria(jidCrit);
	}
	
	public void remove(String groupJid) {
		Criteria jidCrit = new EqualsCriteria("jid", groupJid);
		delete("yoogroup", jidCrit);

		Criteria groupCrit = new EqualsCriteria("groupjid", groupJid);
		delete("groupmember", groupCrit);

		new ChatDAO().markAsRead(groupJid);
	}    
	
	public void upsert(YooGroup group) {
	    Map<String, String> item = new HashMap<String, String>();
	    item.put("jid", group.toJID());
	    item.put("alias", group.getAlias());

	    if (group.getDate() != null) {
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS", Locale.US);
	    	sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	    	item.put("date", sdf.format(group.getDate()));
	    }
	    // check if we don't have already this group
	    Criteria jidCrit = new EqualsCriteria("jid", group.toJID());
	    List<Map<String, String>> existing = select("yoogroup", jidCrit, null, 1);
	    if (existing.size() > 0) {
	    	update("yoogroup", item, jidCrit);
	    } else {
	    	insert("yoogroup", item);
	    }
	}
	
	public void removeMemberFromGroup(String userJid, String groupJid) {
		Log.w("#### removeMemberFromGroup >> userJId", userJid + ", GroupJId : " + groupJid);
		Criteria groupCrit = new EqualsCriteria("groupjid", groupJid);
		Criteria userCrit = new EqualsCriteria("userjid", userJid);
		ConjCriteria andCrit = new ConjCriteria(Conjunction.AND);
		andCrit.add(groupCrit);
		andCrit.add(userCrit);
		delete("groupmember", andCrit);
	}
	
	public void addMemberToGroup(String userJid, String groupJid) {
		Log.w("#### addMemberToGroup >> userJId", userJid + ", GroupJId : " + groupJid);
		Criteria groupCrit = new EqualsCriteria("groupjid", groupJid);
		Criteria userCrit = new EqualsCriteria("userjid", userJid);
		ConjCriteria andCrit = new ConjCriteria(Conjunction.AND);
		andCrit.add(groupCrit);
		andCrit.add(userCrit);
		List<Map<String, String>> existing = select("groupmember", andCrit, null, 1);
	    if (existing.size() == 0) {
	        Map<String, String> item = new HashMap<String, String>();
	        item.put("groupjid", groupJid);
	        item.put("userjid", userJid);
	        insert("groupmember", item);
	    }
	}
	
	private YooGroup mapRow(Map<String, String> row) {
	    String name = StringUtils.parseName(row.get("jid"));
	    String alias = row.get("alias");
	    YooGroup group = new YooGroup(name, alias);
	    if (row.get("date") != null) {
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS", Locale.US);
	    	sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	    	try {
				group.setDate(sdf.parse(row.get("date")));
			} catch (ParseException e) {
				Log.e("GroupAAO : ParseException ", e.toString());
			}
	    }
	    return group;
	}
	
	public void purge() {
		delete("yoogroup", null);
		delete("groupmember", null);
	}


}
