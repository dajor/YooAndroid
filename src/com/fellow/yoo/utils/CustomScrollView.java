package com.fellow.yoo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

@SuppressLint("DrawAllocation")
public class CustomScrollView extends ScrollView {

	

	private boolean scrollToBottom = true;
	public CustomScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CustomScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomScrollView(Context context) {
		super(context);
	}
	
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// scroll to the end
		post(new Runnable() {            
		    @Override
		    public void run() {
		    	if(scrollToBottom){
		    		fullScroll(View.FOCUS_DOWN); 
		    	}else{
		    		pageScroll(View.FOCUS_RIGHT);
		    	}
		    }
		});
	}
	
	
	public boolean isScrollToBottom() {
		return scrollToBottom;
	}

	public void setScrollToBottom(boolean scrollToBottom) {
		this.scrollToBottom = scrollToBottom;
	}



}
