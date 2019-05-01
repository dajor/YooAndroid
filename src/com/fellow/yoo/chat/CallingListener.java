package com.fellow.yoo.chat;

import com.fellow.yoo.model.YooMessage;

public interface CallingListener {
	
	void requestCall(YooMessage yooMsg);
	void requestCallFromRecipient(YooMessage yooMsg);
	void cancelCall(YooMessage yooMsg);
	void acceptCall(YooMessage yooMsg, boolean accept); 
	void acceptCallFromRecipient(YooMessage yooMsg, boolean accept); 
	

}
