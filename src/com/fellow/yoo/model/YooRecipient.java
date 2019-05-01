package com.fellow.yoo.model;

import java.io.Serializable;

public interface YooRecipient extends Serializable {
	public String toJID();
	public boolean isMe();
	public String toString();
	public String getName();
}
