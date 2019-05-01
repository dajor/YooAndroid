package com.fellow.yoo.model;

public class Chat {
	
	private YooRecipient recipient;
	private YooMessage lastMsg;
	private int unread;
	
	public Chat(YooRecipient pRecipient, YooMessage pLastMsg, int pUnread) {
		recipient = pRecipient;
		lastMsg = pLastMsg;
		unread = pUnread;
	}
	
	public YooRecipient getRecipient() {
		return recipient;
	}
	public YooMessage getLastMsg() {
		return lastMsg;
	}
	public int getUnread() {
		return unread;
	}
	
	
}
