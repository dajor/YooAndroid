package com.fellow.yoo.fragment;

import android.app.Fragment;
import android.content.res.Resources;

public abstract class YooFragment extends Fragment {
	
	

	public abstract String getTitle(Resources resources);


	public int getActionTitle() {
		return -1;
	}

	public int getActionIcon() {
		return -1;
	}

	public void actionClicked(int actionId) {
		
	}
	
	

}
