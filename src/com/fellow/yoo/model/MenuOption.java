package com.fellow.yoo.model;

import com.fellow.yoo.R;

public class MenuOption {
	
	
	
	private int label;
	private int image;
	private RecentItem recentItem;

	public MenuOption(int pImage, int pLabel) {
		label = pLabel;
		image = pImage;
	}
	
	public MenuOption(RecentItem recentItem) {
		label = R.string.recent_item;
		this.recentItem = recentItem;
	}

	public int getLabel() {
		
		return label;
	}
	
	public String getLabelString() {
		if(recentItem != null){
			return recentItem.getAlias();
		}
		return "";
	}

	public int getImage() {
		return image;
	}
	
	
	public RecentItem getRecentItem() {
		return recentItem;
	}
	
	public String getRecentJid() {
		if(recentItem != null){
			return recentItem.getjId();
		}
		return "";
	}

	

}
