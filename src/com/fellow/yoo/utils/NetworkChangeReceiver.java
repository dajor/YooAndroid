package com.fellow.yoo.utils;

import com.fellow.yoo.YooApplication;
import com.fellow.yoo.chat.ChatTools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {
	
	public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;
	 
    @Override
    public void onReceive(final Context context, final Intent intent) {
 
        String status = getConnectivityStatusString(context);
        if(!StringUtils.isEmpty(status)){
        	Toast.makeText(context, status, Toast.LENGTH_LONG).show();
        }
    }
    
    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI){
            	return TYPE_WIFI;
            }else if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE){
            	return TYPE_MOBILE;
            }
        } 
        return TYPE_NOT_CONNECTED;
    }
     
    public static String getConnectivityStatusString(final Context context) {
        int conn = getConnectivityStatus(context);
        String status = null;
        if(YooApplication.isAppOn()){
        	if (conn == TYPE_WIFI) {
                //status = context.getString(R.string.wifi_enable);
                ChatTools.sharedInstance().reLogin(context); 
            } else if (conn == TYPE_MOBILE) {
                //status = context.getString(R.string.mobile_enable);
                ChatTools.sharedInstance().reLogin(context);
            } else if (conn == TYPE_NOT_CONNECTED) {
                //status = context.getString(R.string.no_internet_connect);
                ChatTools.sharedInstance().asyncDisconnect(true);
            }
        }
        return status;
    }
}
