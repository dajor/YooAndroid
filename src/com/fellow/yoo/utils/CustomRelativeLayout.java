package com.fellow.yoo.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class CustomRelativeLayout extends RelativeLayout {

	public CustomRelativeLayout(Context context) {
		super(context);
	}

    public CustomRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public CustomRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public float getXFraction() {
        return getX() / getWidth();
    }

    public void setXFraction(float xFraction) {
        final int width = getWidth();
        setX((width > 0) ? (xFraction * width) : -9999);
    }
	
}
