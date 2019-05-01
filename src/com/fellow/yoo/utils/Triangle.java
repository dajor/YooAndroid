package com.fellow.yoo.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class Triangle extends View {

	private boolean toLeft;
	private Paint paint;
	private Path path;
	
	public Triangle(Context context) {
		super(context);
		init();
	}

	public Triangle(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public Triangle(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	
	public void setToLeft(boolean pToLeft) {
		toLeft = pToLeft;
	}
	
	public void setColor(int color) {
		paint.setColor(color);
	}
	
	private void init() {
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL);
		path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);		 
		if (toLeft) {
			path.moveTo(getWidth(), 0);
			path.lineTo(0, getHeight()/2);
			path.lineTo(getWidth(), getHeight());			
		} else {
			path.moveTo(0, 0);
			path.lineTo(getWidth(), getHeight()/2);
			path.lineTo(0, getHeight());
		}
		path.close();
		canvas.drawPath(path, paint);

	}
	

}
