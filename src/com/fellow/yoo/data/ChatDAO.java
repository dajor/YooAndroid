package com.fellow.yoo.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jivesoftware.smack.util.StringUtils;

import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.criteria.ConjCriteria;
import com.fellow.yoo.data.criteria.ConjCriteria.Conjunction;
import com.fellow.yoo.gcm.GcmIntentService;
import com.fellow.yoo.data.criteria.Criteria;
import com.fellow.yoo.data.criteria.EqualsCriteria;
import com.fellow.yoo.data.criteria.NotEqualsCriteria;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooMessage.CallStatus;
import com.fellow.yoo.model.YooMessage.YooMessageType;
import com.fellow.yoo.model.YooRecipient;
import com.fellow.yoo.model.YooUser;

public class ChatDAO extends BaseDAO {
	
	public void initTables() {
		
		
		Map<String, String> columns = new HashMap<String, String>();
	    columns.put("yooid", "INTEGER PRIMARY KEY");
	    columns.put("sender", "TEXT");
	    columns.put("recipient", "TEXT");
	    columns.put("shared", "INTEGER");
	    columns.put("message", "TEXT");
	    columns.put("id", "TEXT");
	    columns.put("date", "TEXT");
	    columns.put("read", "TEXT");
	    columns.put("sent", "TEXT");
	    columns.put("ack", "TEXT");
	    columns.put("readbyother", "TEXT");
	    columns.put("receipt", "TEXT");
	    columns.put("type", "INTEGER");
	    columns.put("location", "TEXT");
	    columns.put("groupmember", "TEXT");
	    columns.put("sound", "BLOB");
	    columns.put("conferenceNumber", "TEXT");
	    columns.put("callstatus", "TEXT");
	    checkTable("chat", columns);
	    createIndex("chat", false, Arrays.asList("id"));
	    createIndex("chat", false, Arrays.asList("date"));
	    createIndex("chat", false, Arrays.asList("sender"));
	    createIndex("chat", false, Arrays.asList("recipient"));


		Map<String, String> columns2 = new HashMap<String, String>();
	    columns2.put("yooid", "TEXT");
	    columns2.put("picid", "TEXT");
	    columns2.put("data", "BLOB");
	    checkTable("picture", columns2);
	    createIndex("picture", true, Arrays.asList("yooid", "picid"));
	    
	}
	
	private YooMessage mapRow(Map<String, String> row) {
	    YooMessage yooMsg = new YooMessage();
	    yooMsg.setType(YooMessageType.values()[Integer.parseInt(row.get("type"))]);
	    yooMsg.setYooId(row.get("yooid"));
	    yooMsg.setIdent(row.get("id"));
	    yooMsg.setCallReqId(row.get("id"));  
	    yooMsg.setAck("1".equals(row.get("ack")));
	    yooMsg.setSent("1".equals(row.get("sent")));
	    yooMsg.setReadByOther("1".equals(row.get("readbyother")));;
	    yooMsg.setMessage(row.get("message"));
	    if (StringUtils.parseServer(row.get("sender")).equals(ChatTools.CONFERENCE_DOMAIN)) {
	        String groupCode = StringUtils.parseName(row.get("sender"));
	        YooGroup group = new YooGroup(groupCode, groupCode);
	        group.setMember(row.get("groupmember"));
	        yooMsg.setFrom(group);
	    } else {
	        yooMsg.setFrom(new YooUser(row.get("sender")));
	    }
	    if (StringUtils.parseServer(row.get("recipient")).equals(ChatTools.CONFERENCE_DOMAIN)) {
	        String groupCode = StringUtils.parseName(row.get("recipient"));
	        YooGroup group = new YooGroup(groupCode, groupCode);
	        yooMsg.setTo(group);
	    } else {
	        yooMsg.setTo(new YooUser(row.get("recipient")));
	    }

	    if (row.get("sound") != null) {
	        yooMsg.setSound(StringUtils.decodeBase64(row.get("sound")));
	    }
	    if (row.get("shared") != null) {
	    	yooMsg.setShared(Integer.parseInt(row.get("shared")));
	    }
	    yooMsg.setRead("1".equals(row.get("read")));
	    yooMsg.setReceipt("1".equals(row.get("receipt")));
	    yooMsg.setConferenceNumber(row.get("conferenceNumber"));
	    try {
	    	yooMsg.setCallStatus(CallStatus.values()[Integer.parseInt(row.get("callstatus"))]); 
		} catch (Exception e) {
			yooMsg.setCallStatus(CallStatus.values()[0]); 
		}
	    
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS", Locale.US);	    
	    try {
			yooMsg.setDate(sdf.parse(row.get("date")));
		} catch (ParseException e) {
			// do nothing
		}
	    if (row.get("location") != null) {
	    	String [] parts = row.get("location").split("/");
	        yooMsg.setLocation(new double [] {Double.parseDouble(parts[0]), Double.parseDouble(parts[1])});
	    }
	    
	    return yooMsg;
	}
	
	
	public YooMessage findById(String msgId, boolean withPictures) {
		EqualsCriteria orCrit = new EqualsCriteria("yooid", msgId); 
		List<Map<String, String>> rows = select("chat", orCrit, "date DESC", 1);
	    for (Map<String, String> row : rows) {
	        YooMessage yooMsg = mapRow(row);
	        if (withPictures && yooMsg.getType() == YooMessageType.ymtPicture) {
	            Criteria picCrit = new EqualsCriteria("yooid", yooMsg.getYooId());
	            List<Map<String, String>> rows2 = select("picture", picCrit, "picid ASC", 1);
	            if (rows2.size() > 0) {
	            	for (Map<String, String> row2 : rows2) {
	            		yooMsg.getPictures().add(StringUtils.decodeBase64(row2.get("data")));
	            	}
	            }
	        }
	        return yooMsg;
	    }
		return null;
	}
	
	public YooMessage findIdent(String ident) {
		EqualsCriteria orCrit = new EqualsCriteria("id", ident); 
		List<Map<String, String>> rows = select("chat", orCrit, "date DESC", 1);
	    for (Map<String, String> row : rows) {
	        YooMessage yooMsg = mapRow(row);
	        return yooMsg;
	    }
		return null;
	}
	
	
	public List<YooMessage> list(YooRecipient recipient, boolean withPictures) {
		return list(recipient, withPictures, 0);
	}
	
	public List<YooMessage> list(YooRecipient recipient, boolean withPictures, int limit) {
		// System.out.println(" >>>>>>>>>>>>>>>> list : " + recipient.toJID()); 
		String jId = recipient != null? recipient.toJID() : "broadcast";
		List<YooMessage> msgs = new ArrayList<YooMessage>();
	    ConjCriteria orCrit = new ConjCriteria(Conjunction.OR);
	    orCrit.add(new EqualsCriteria("sender", jId));
	    orCrit.add(new EqualsCriteria("recipient", jId));
	    List<Map<String, String>> rows = select("chat", orCrit, "date DESC", limit);
	    for (Map<String, String> row : rows) {
	        YooMessage yooMsg = mapRow(row);
	        if (withPictures && yooMsg.getType() == YooMessageType.ymtPicture) {
	            Criteria picCrit = new EqualsCriteria("yooid", yooMsg.getYooId());
	            List<Map<String, String>> rows2 = select("picture", picCrit, "picid ASC", limit);
	            if (rows2.size() > 0) {
	            	for (Map<String, String> row2 : rows2) {
	            		yooMsg.getPictures().add(StringUtils.decodeBase64(row2.get("data")));
	            	}
	            }
	        }
	        msgs.add(yooMsg);
	    }
	    return msgs;
	}
	
	
	// to avoid slow loading on chat view
	public boolean isManyImage(YooRecipient recipient, int limit) {
		int count = 0;
	    ConjCriteria orCrit = new ConjCriteria(Conjunction.OR);
	    orCrit.add(new EqualsCriteria("sender", recipient.toJID()));
	    orCrit.add(new EqualsCriteria("recipient", recipient.toJID()));
	    List<Map<String, String>> rows = select("chat", orCrit, "date DESC", limit);
	    for (Map<String, String> row : rows) {
	    	YooMessage.YooMessageType type = YooMessageType.values()[Integer.parseInt(row.get("type"))];
	    	if(type.equals(YooMessage.YooMessageType.ymtLocation) || type.equals(YooMessage.YooMessageType.ymtPicture)){
	    		count++;
	    	}
	    	if(count > 5){
	    		return true;
	    	}
	    }
	    return false;
	}
	
	public void insert(YooMessage yooMsg) {
	    // check if we don't have already this message
	    if (yooMsg.getIdent() != null) {
	        Map<String, String> existing = selectOne("chat", new EqualsCriteria("id", yooMsg.getIdent()));
	        if (existing != null) {
	            return;
	        }
	    }
	 	
	    Map<String, String> item = new HashMap<String, String>();
	    if (yooMsg.getIdent() != null) {
	    	item.put("id", yooMsg.getIdent());
	    }
	    if (yooMsg.getMessage() != null) {
	    	item.put("message", yooMsg.getMessage());
	    }
	    int typeIdx = Arrays.asList(YooMessageType.values()).indexOf(yooMsg.getType());
	    item.put("type", typeIdx == -1 ? "0" : String.valueOf(typeIdx));
	    
	    if (yooMsg.getType() == YooMessageType.ymtLocation) {
	    	item.put("location", yooMsg.getLocation()[0] + "/" + yooMsg.getLocation()[1]);
	    }
	    item.put("sender", yooMsg.getFrom() != null? yooMsg.getFrom().toJID() : "broadcast");
	    
	    if (yooMsg.getFrom() instanceof YooGroup) {
	    	item.put("groupmember", ((YooGroup) yooMsg.getFrom()).getMember());
	    }
	    
	    item.put("recipient", yooMsg.getTo() != null ? yooMsg.getTo().toJID() : "broadcast");
	    
	    if (yooMsg.getShared() != 0) {
	    	item.put("shared", String.valueOf(yooMsg.getShared()));
	    }
	    
	    item.put("callstatus", yooMsg.getCallStatusIndex());
	    item.put("read", yooMsg.isRead() ? "1" : "0");
	    item.put("sent", yooMsg.isSent() ? "1" : "0");
	    item.put("receipt", yooMsg.isReceipt() ? "1" : "0");
	    item.put("readbyother", yooMsg.isReadByOther() ? "1" : "0");
	    if (yooMsg.getSound() != null) {
	    	item.put("sound", StringUtils.encodeBase64(yooMsg.getSound()));
	    }
	    if (yooMsg.getConferenceNumber() != null) {
	    	item.put("conferenceNumber", yooMsg.getConferenceNumber());
	    }
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS", Locale.US);
	    item.put("date", sdf.format(yooMsg.getDate())); //sdf.format(new Date()));
	    
	    insert("chat", item);
	    

	    yooMsg.setYooId(getLastYooId());
	    
	    if (yooMsg.getPictures().size() > 0) {
	        int i = 1;
	        for (byte [] picData : yooMsg.getPictures()) {
	        	Map<String, String> item2 = new HashMap<String, String>();
	        	item2.put("yooid", yooMsg.getYooId());
	        	item2.put("picid", String.valueOf(i));
	        	item2.put("data", StringUtils.encodeBase64(picData));
	        	insert("picture", item2);
	            i++;
	        }
	    }
	}
	
	public List<YooMessage> unreadList(YooRecipient recipient) {
		List<YooMessage> unread = new ArrayList<YooMessage>();
		// NSObject <Criteria> *unreadByotherCrit = [[NotEqualsCriteria alloc] initWithField:@"readbyother" value:@"1"];
		// Criteria unreadByOtherCrit = new NotEqualsCriteria("readbyother", "1");
	    Criteria unreadCrit = new NotEqualsCriteria("read", "1");
	    Criteria senderCrit = new EqualsCriteria("sender", recipient.toJID());
	    ConjCriteria andCrit = new ConjCriteria(Conjunction.AND);
	    // andCrit.add(unreadByOtherCrit);
	    andCrit.add(unreadCrit);
	    andCrit.add(senderCrit);
	    List<Map<String, String>> rows = select("chat", andCrit, null, 0);
	    for (Map<String, String> row : rows) {
	    	YooMessage message = mapRow(row);
	    	unread.add(message);
	    }
	    return unread;
	}
	
	public boolean removeChatBroadcast(){
		removeChatSender("broadcast");
		removeChatRecipient("broadcast");
		return true;
	}
	
	public boolean removeChat(String jId){
		removeChatSender(jId);
		removeChatRecipient(jId);
		return true;
	}
	
	public boolean removeChatSender(String senderJid) {
	    List<Map<String, String>> rows = select("chat", new EqualsCriteria("sender", senderJid), null, 0);
	    for (Map<String, String> row : rows) {
			Criteria delPic = new EqualsCriteria("yooid", row.get("yooid"));
	    	delete("picture", delPic);
	    }
    	delete("chat", new EqualsCriteria("sender", senderJid));
	    return true;
	}
	
	public boolean removeChatRecipient(String recipientJid) {
	    List<Map<String, String>> rows = select("chat", new EqualsCriteria("recipient", recipientJid), null, 0);
	    for (Map<String, String> row : rows) {
			Criteria delPic = new EqualsCriteria("yooid", row.get("yooid"));
	    	delete("picture", delPic);
	    }
    	delete("chat", new EqualsCriteria("recipient", recipientJid));
	    return true;
	}
	
	
	
	public YooMessage acknowledge(String msgId) {
		Criteria idCrit = new EqualsCriteria("id", msgId);
		List<Map<String, String>> rows = select("chat", idCrit, null, 0);
	    YooMessage yooMsg = null;
	    if (rows.size() > 0) {
	    	Map<String, String> row = rows.get(0);
	        yooMsg = mapRow(row);
	        Map<String, String> item = Collections.singletonMap("ack", "1");
	        update("chat", item, idCrit);
	    }
	    return yooMsg;		
	}
	
	public void markAsRead(String userJid) {
		Map<String, String> item = new HashMap<String, String>();
		item.put("read", "1");
		item.put("receipt", "0");
		item.put("readbyother", "1");
		update("chat", item, new EqualsCriteria("sender", userJid));
		update("chat", item, new EqualsCriteria("recipient", userJid));
	}
	
	public void markAsReadByOther(String ident) {
		Map<String, String> item = new HashMap<String, String>();
		item.put("readbyother", "1");
		update("chat", item, new EqualsCriteria("id", ident));
		
		GcmIntentService.removePushMessageId(ident);
	}
	
	public void markAsSent(String ident) {
    	Map<String, String> item = new HashMap<String, String>();
    	item.put("sent", "1");
    	update("chat", item, new EqualsCriteria("id", ident));
	}
	
	public void updateCallStatus(String callReqId, CallStatus callstatus) {
		int callStatusIdx = Arrays.asList(CallStatus.values()).indexOf(callstatus);
    	Map<String, String> item = new HashMap<String, String>();
    	item.put("callstatus", String.valueOf(callStatusIdx));
    	update("chat", item, new EqualsCriteria("id", callReqId));
	}
	
	
	public String getLastYooId() {
		List<Map<String, String>> rows = rawQuery("SELECT MAX(yooid) maxid FROM chat", null);
	    if (rows.size() > 0) {
	    	return rows.get(0).get("maxid");
	    }
	    return null;

	}
	
	public int unreadCountForSender(YooRecipient recipient) {
		List<Map<String, String>> rows = rawQuery("SELECT COUNT(*) total FROM chat WHERE read != '1' AND sender = ?", new String [] {recipient.toJID()});	    
	    if (rows.size() > 0) {
	    	return Integer.parseInt(rows.get(0).get("total"));
	    }
	    return 0;
	}
	
	public void purge() {
		delete("chat", null);
		delete("picture", null);
	}
	
	public int countRecieved(YooUser user){
		String sql = "SELECT yooid FROM chat WHERE recipient = '" + user.toJID() + "'";
		return selectCount(sql);
	}
	
	public int countSent(YooUser user){
		String sql = "SELECT yooid FROM chat WHERE sender = '" + user.toJID() + "'";
		return selectCount(sql);
	}
	
	public int count(YooRecipient user){
		String sql = "SELECT yooid FROM chat WHERE sender = '" + user.toJID() + "' OR recipient = '" + user.toJID() + "'";
		return selectCount(sql);
	}
	
	public int countUnreads(){
		String sql = "SELECT yooid FROM chat WHERE read != '1' OR read IS NULL";
		return selectCount(sql);
	}

}
