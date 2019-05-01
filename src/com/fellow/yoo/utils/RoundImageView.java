package com.fellow.yoo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RoundImageView extends ImageView {

    private RectF rect;
    private Paint paint;

    public RoundImageView(Context context) {
        super(context);
    }

    public RoundImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    
    private void initShader() {
    	if (getDrawable() == null){
    		return;
    	}
    	
		Bitmap bitmap = ((BitmapDrawable)getDrawable()).getBitmap();
		
		BitmapShader shader;
		shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
		Matrix mShaderMatrix = new Matrix();
		mShaderMatrix.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()),  
				new RectF(0, 0, getWidth(), getHeight()), Matrix.ScaleToFit.FILL);
		shader.setLocalMatrix(mShaderMatrix);
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setShader(shader);

		rect = new RectF(0.0f, 0.0f, getWidth(), getHeight());
    }
    
	@Override
    protected void onDraw(Canvas canvas) {
    	if (getDrawable() == null) return;
		if (paint == null) {
			initShader();
		}
		canvas.drawRoundRect(rect, getWidth()/2, getHeight()/2, paint);
    }
}
