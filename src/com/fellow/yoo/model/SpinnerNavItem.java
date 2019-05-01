package com.fellow.yoo.model;

public class SpinnerNavItem {

	private String title;
	private String key;
	private int icon;
	
	public SpinnerNavItem(String key, String title, int icon){
		this.key = key;
		this.title = title;
		this.icon = icon;
	}
	
	public String getTitle(){
		return this.title;		
	}
	
	public int getIcon(){
		return this.icon;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}
	
	
}
