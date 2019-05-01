package com.fellow.yoo.model;

public class RecentItem {
	
	private String jId;
	private String alias;
	private String chatDate;
	private boolean group;

	public RecentItem(String jId, String alias, String chatDate, boolean group) {
		this.jId = jId;
		this.alias = alias;
		this.chatDate = chatDate;
		this.group = group;
	}
	
	public String getjId() {
		return jId;
	}

	public String getChatDate() {
		return chatDate;
	}

	public String getAlias() {
		return alias;
	}

	public boolean isGroup() {
		return group;
	}


}
