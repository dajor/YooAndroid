

package com.fellow.yoo.utils;

import com.google.android.gms.maps.MapView;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class RecyclingMapView extends MapView {

    public RecyclingMapView(Context context) {
        super(context);
    }

    public RecyclingMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @see android.widget.ImageView#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        // This has been detached from Window, so clear the drawable
        // setImageDrawable(null);

        super.onDetachedFromWindow();
    }


	@Override
	public void setEnabled(boolean enabled) {
		
		super.setEnabled(enabled);
	}

	@Override
	protected boolean verifyDrawable(Drawable who) {
		return super.verifyDrawable(who);
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
			case (MotionEvent.ACTION_DOWN): {
				
				return true;
			}
		}
		// 'super' go to the mapView procedures and scroll map in own algorithm
		return super.dispatchTouchEvent(ev);
	}

}
