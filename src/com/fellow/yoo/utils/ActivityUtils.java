package com.fellow.yoo.utils;

import com.fellow.yoo.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityUtils {

	public static int dpToPixels(Context context, int dp) {
		float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dp * scale + 0.5f);
	}

	public static int pixelsToDp(Context context, float px) {
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float dp = px / (metrics.densityDpi / 160f);
		return (int) dp;
	}

	public static int getScreenWidth(Activity activity) {
		Display display = activity.getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size.x;
	}

	public static int getScreenHeight(Activity activity) {
		Display display = activity.getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size.y;
	}

	public static void displayToast(final String msg, final Activity act) {
		if (act != null) {
			if (!StringUtils.isEmpty(msg)) {
				act.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(act, msg, Toast.LENGTH_LONG).show();
					}
				});
			}
		}
	}

	public static void displayDiaMsg(final String title, final String msg, Activity act) {

		AlertDialog.Builder dia = new AlertDialog.Builder(act);
		dia.setTitle(title);

		LinearLayout view = new LinearLayout(act);
		TextView txtMessage = new TextView(act);
		txtMessage.setText(Html.fromHtml(msg));
		int padding = ActivityUtils.dpToPixels(act, 15);
		txtMessage.setPadding(padding, padding, padding, padding);
		view.addView(txtMessage);

		dia.setView(view);
		dia.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {}
		});
		dia.show();
	}
	
	public static boolean checkNetworkConnected(final Activity activity, final int msg) {
		ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null) {
			if(activity != null){
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(msg != -1){
							ActivityUtils.displayToast(activity.getString(msg), activity);
						}
					}
				});
			}
			return false;
		} 
		
		return true;
	}
	
	/*public static boolean isNetworkAvailable(final Context context) {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}*/
	
	public static boolean isNoInternet(final Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean isOnline =  cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
		
		return !isOnline;
	}
	
	public static Drawable getIcon(Activity act, int iconId, int size){
		Bitmap icon = BitmapFactory.decodeResource(act.getResources(), iconId); 
   	 	Bitmap new_icon = ActivityUtils.resizeBitmapImageFn(icon, ActivityUtils.dpToPixels(act, size)); 
   	 	Drawable drawable = new BitmapDrawable(act.getResources(),new_icon); 
   	 	
   	 	return drawable;
	}
	
	public static Bitmap resizeBitmapImageFn(Bitmap bmpSource, int maxResolution){
	    int iWidth = bmpSource.getWidth();
	    int iHeight = bmpSource.getHeight();
	    int newWidth = iWidth ;
	    int newHeight = iHeight ;
	    float rate = 0.0f;

	    if(iWidth > iHeight ){
	        if(maxResolution < iWidth ){
	            rate = maxResolution / (float) iWidth ;
	            newHeight = (int) (iHeight * rate);
	            newWidth = maxResolution;
	        }
	    }else{
	        if(maxResolution < iHeight ){
	            rate = maxResolution / (float) iHeight ;
	            newWidth = (int) (iWidth * rate);
	            newHeight = maxResolution;
	        }
	    }
	    return Bitmap.createScaledBitmap(bmpSource, newWidth, newHeight, true);
	}
	
	
	public static void hideSoftKeyboad(View v) {
		InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}
	
	public static void showSoftKeyboad(View v) {
		InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 1);
	}
	
	public static void setTextHtml(Context context, TextView tv, String value, String colorCode){
		String styledText = "<font color='" + colorCode + "'>" + value + "</font>";
		if(StringUtils.isEmpty(colorCode)){   // colorCode = "#FFFFFF";
			styledText = value;
		}
		
		tv.setText(Html.fromHtml(styledText),  TextView.BufferType.SPANNABLE); 
	}
	
	public static void setTextHtmlLink(Context context, TextView tv, String value, String colorCode){
		String styledText = "<font color='COLOR_CODE'><a href=\"url\">VALUE_</a></font>";
		styledText = styledText.replace("VALUE_", value);
		styledText = styledText.replace("COLOR_CODE", colorCode);
		tv.setText(Html.fromHtml(styledText),  TextView.BufferType.SPANNABLE); 
	}

}
