package com.fellow.yoo.xmpp;

import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.json.JSONObject;

import com.fellow.yoo.chat.ChatTools;

import android.util.Log;

public class IQCalling extends IQ {
	

	private String callingId;
	private List<String> conferenceNumber;
	
	@Override
	public String getChildElementXML() {
		return "";
	}
	
	public String getCallingId() {
		return callingId;
	}
	
	public void setCallingId(String callingId) {
		this.callingId = callingId;
	}


	public List<String> getConferenceNumber() {
		return conferenceNumber;
	}
	

	public String getCallTo(String me) {
		for (String userJid : conferenceNumber) { 
			if(userJid.contains(ChatTools.YOO_DOMAIN)){
				if(!userJid.equals(me)){
					return userJid;
				}
			}
		}
		return ""; 
	}
	
	public String getConferenceNumberAsString() {
		if(conferenceNumber != null){
			try {
				if(conferenceNumber.size() > 3){
					JSONObject obj = new JSONObject();
					for (int i = 0; i < conferenceNumber.size(); i++) {
						obj.put(conferenceNumber.get(i), conferenceNumber.get(i + 1));
						i++;
					}
					
					String conf = obj.toString().replace("[", "").replace("]", "");
					Log.i(" IQCalling : getConferenceNumberAsString >> ", conf);
					return conf;
				}
			} catch (Exception e) {
				Log.e("getConferenceNumber", e.getMessage());
			}
		}
		return "";
	}

	public void setConferenceNumber(List<String> conferenceNumber) {
		this.conferenceNumber = conferenceNumber;
	}

	
}
