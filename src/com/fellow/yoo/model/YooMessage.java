package com.fellow.yoo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.util.Log;

public class YooMessage {
	
	
	public enum YooMessageType {
	    // IMPORTANT : add the new types at the end
	    // to keep the same numbers assigned to the types (0,1,2,3..)
	    ymtText, // 0
	    ymtPicture, // 1
	    ymtLocation, // 2
	    ymtContact, // 3
	    ymtAck, // 4
	    ymtInvite, // 5
	    ymtRevoke, // 6
	    ymtSound, // 7
	    ymtCallRequest, // 8
	    ymtCallStatus, // 9
	    ymtMessageRead //10
	};
	
	public enum CallStatus {
		csNone,
	    //csReceivedConferenceNumber,
	    //csRequested,
	    csAccepted,
	    csRejected,
	    csCancelled
	};
	

	private YooMessageType type;
	private String yooId;
	private String ident;
	private YooRecipient from;
	private YooRecipient to;
	private int shared;
	private String message;
	private String thread;
	private List<byte []> pictures;
	private byte [] sound;
	private String conferenceNumber;
	private double [] location;
	boolean read;
	boolean ack;
	boolean sent;
	boolean receipt;
	boolean readByOther;
	private Date date;
	private YooGroup group;
	
	// @property (assign) CallStatus callStatus;
	// @property (nonatomic, retain) NSString *callReqId;
	
	private CallStatus callStatus;
	private String callReqId;
	
	
	public YooMessage() {
		pictures = new ArrayList<byte []>();
		shared = 0;
	}
	
	public void setFrom(YooRecipient pFrom) {
		from = pFrom;
	}
	
	public void setTo(YooRecipient pTo) {
		to = pTo;
	}
	
	public void setMessage(String pMessage) {
		message = pMessage;
	}
	
	public void setThread(String pThread) {
		thread = pThread;
	}
	
	public void setIdent(String pIdent) {
		ident = pIdent;
	}
	
	public String getIdent() {
		return ident;
	}
	
	public void setSound(byte [] pSound) {
		sound = pSound;
	}
	
	public void setGroup(YooGroup pGroup) {
		group = pGroup;
	}
	
	public YooGroup getGroup() {
		return group;
	}
	
	public List<byte []> getPictures() {
		return pictures;
	}
	
	public void setLocation(double [] pLocation) {
		location = pLocation;
	}
	
	public void setConferenceNumber(String pConferenceNumber) {
		conferenceNumber = pConferenceNumber;
	}
	
	public void setType(YooMessageType pType) {
		type = pType;
	}
	
	public YooMessageType getType() {
		return type;
	}
	
	public String getTypeIndex() {
		try {
			int typeIndex = Arrays.asList(YooMessageType.values()).indexOf(getType());
			return String.valueOf(typeIndex);
		} catch (Exception e) {
			Log.w("YooMessage : getTypeIndex >> ", e.getMessage()); 
		}
		return "";
	}
	
	public String getTypeIndex(YooMessageType type) {
		try {
			int typeIndex = Arrays.asList(YooMessageType.values()).indexOf(type);
			return String.valueOf(typeIndex);
		} catch (Exception e) {
			Log.w("YooMessage : getTypeIndex >> ", e.getMessage()); 
		}
		return "";
	}

	
	public YooRecipient getFrom() {
		return from;
	}
	
	
	public boolean isReceipt() {
		return receipt;
	}
	
	public void setYooId(String pYooId) {
		yooId = pYooId;
	}
	
	public void setAck(boolean pAck) {
		ack = pAck;
	}
	
	public void setRead(boolean pRead) {
		read = pRead;
	}
	
	public void setSent(boolean pSent) {
		sent = pSent;
	}
	
	public void setShared(int pShared) {
		shared = pShared;
	}
	
	public void setShared(long pShared) {
		Long value = Long.valueOf(pShared);
		shared = value.intValue();
	}
	
	public void setReceipt(boolean pReceipt) {
		receipt = pReceipt;
	}
	
	public void setDate(Date pDate) {
		date = pDate;
	}
	
	public String getYooId() {
		return yooId;
	}
	
	public String getMessage() {
		
		return message;
	}
	
	
	public double [] getLocation() {
		return location;
	}
	
	public YooRecipient getTo() {
		return to;
	}

	public int getShared() {
		return shared;
	}
	
	public boolean isAck() {
		return ack;
	}
	
	public boolean isRead() {
		return read;
	}
	
	public boolean isSent() {
		return sent;
	}
	
	public byte [] getSound() {
		return sound;
	}
	
	public String getConferenceNumber() {
		return conferenceNumber;
	}
	
	public Date getDate() {
		if(date == null){
			return new Date();
		}
		return date;
	}
	
	public Date getDateCanNull() {
		return date;
	}
	
	public String getThread() {
		return thread;
	}
	
	public CallStatus getCallStatus() {
		if(callStatus == null){
			return CallStatus.csNone;
		}
		return callStatus;
	}
	
	public String getCallStatusIndex(){
		try {
			int typeIndex = Arrays.asList(CallStatus.values()).indexOf(getCallStatus());
			return String.valueOf(typeIndex);
		} catch (Exception e) {
			Log.w("YooMessage : getTypeIndex >> ", e.getMessage()); 
		}
		return "0";
		
	}

	public void setCallStatus(CallStatus callStatus) {
		this.callStatus = callStatus;
	}
	
	public String getCallReqId() {
		return callReqId;
	}

	public void setCallReqId(String callReqId) {
		this.callReqId = callReqId;
	}

	public boolean isReadByOther() {
		return readByOther;
	}

	public void setReadByOther(boolean readByOther) {
		this.readByOther = readByOther;
	}

	
}
