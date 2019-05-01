package com.fellow.yoo.gcm;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.fellow.yoo.R;
import com.fellow.yoo.RegisterActivity;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.utils.BadgeUtils;
import com.fellow.yoo.utils.StringUtils;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmIntentService extends IntentService {
	public static final int NOTIFICATION_ID = 1;
	//	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				sendNotification("Send error: " + extras.toString(), "");
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				sendNotification("Deleted messages on server: " + extras.toString(), "");
				// If it's a regular GCM message, do some work.
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				
				// Post notification of received message.
				String messgeId = extras.getString("messageId");
				String message = extras.getString("message");
				String sender = extras.getString("sender");
				String messageBody = extras.getString("messageArg1");
				
				// sender=Cbst2
				if(!StringUtils.isEmpty(message)){
					try {
						new ChatDAO().findIdent(message);
						message = URLDecoder.decode(message, "UTF8");
					} catch (UnsupportedEncodingException e) {
						Log.w("GcmIntentService", e.getMessage(), e);
					}
				}
				
				if(YooApplication.getAppContext() != null){
					Context context = YooApplication.getAppContext();
					if(!checkPushMessageId(context, messgeId)){ // check if count duplicate messageId
						if(message.toLowerCase(Locale.US).indexOf("new message") == -1){ // when sms from read message, not count
							BadgeUtils.setBadge(context, GcmIntentService.countPushMessage(context)); 
						}
					}
					sendNotification(message + " " + messageBody, sender);
					
					Log.i("GcmIntentService", "Received: " + extras.toString());
				}
			}
		}

		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	// private void sendNotification(String msg) {
	// mNotificationManager = (NotificationManager)
	// this.getSystemService(Context.NOTIFICATION_SERVICE);
	//
	// PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
	// new Intent(this, MainActivity.class), 0);
	//
	// NotificationCompat.Builder mBuilder =
	// new NotificationCompat.Builder(this)
	// .setSmallIcon(R.drawable.ic_bubble)
	// .setContentTitle("Yoo Notification")
	// .setStyle(new NotificationCompat.BigTextStyle()
	// .bigText(msg))
	// .setContentText(msg);
	//
	// mBuilder.setContentIntent(contentIntent);
	// mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	// }
	
	/*
	 String  android_id = Secure.getString(getApplicationContext().getContentResolver(),Secure.ANDROID_ID);         
    Log.e("LOG","android id >>" + android_id);

    PushService.setDefaultPushCallback(this, MainActivity.class);

    ParseInstallation installation = ParseInstallation.getCurrentInstallation();
    installation.put("installationId",android_id);

    installation.saveInBackground();
    
    NotificationCompat.Builder mBuilder =
       new NotificationCompat.Builder(context)
           .setDefaults(defaults)
           .setSmallIcon(resourceId)
           .setWhen(System.currentTimeMillis())
           .setContentTitle(extras.getString("title"))
           .setTicker(extras.getString("title"))
           .setContentIntent(contentIntent)
           .setAutoCancel(true);
	 */

	private void sendNotification(String message, String sender) {
		
		if(YooApplication.getAppContext() != null){}
		
		int count = GcmIntentService.countPushMessageNew(YooApplication.getAppContext());
		StringBuilder titleMessage = new StringBuilder(getString(R.string.app_name)); 
		titleMessage.append(" " + count + " " + getString(count > 1 ? R.string.yoo_messages : R.string.yoo_message));
		
		
		Intent intent = new Intent(this, RegisterActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code  */, intent, PendingIntent.FLAG_ONE_SHOT);
		Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		System.err.println("############  : sendNotification message : " + message); 
		
		if(message != null && message.indexOf("NEW_CALL") != -1){ // message sound calling.
			defaultSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" +  R.raw.ringtone);
			
			Context context = getBaseContext();
			Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
		    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    startActivity(launchIntent);
		    
			// return;
		}
		
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)				
				.setSmallIcon(R.drawable.app_icon)
				.setContentTitle(titleMessage.toString())
				.setContentText(getTranslateMessage(message, sender))
				.setAutoCancel(true)
				.setSound(defaultSoundUri)
				.setContentIntent(pendingIntent);
		
		//GCMRegistrar.register(RegisterActivity.this,GCMIntentService.SENDER_ID);
		//String  msgREGID = GCMRegistrar.getRegistrationId(RegisterActivity.this);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
	
	}
	
	
	private String getTranslateMessage(String message, String sender){
		if(message != null){
			String trMessage = "";
			if(message.indexOf("NEW_PHOTO_MESSAGE") != -1){
				trMessage = getString(R.string.new_photo_shared_by);
			}else if(message.indexOf("NEW_CONTACT_MESSAGE") != -1){
				trMessage = getString(R.string.new_contact_shared_by);
			}else if(message.indexOf("NEW_LOCATION_MESSAGE") != -1){
				trMessage = getString(R.string.new_location_shared_by);
			}else if(message.indexOf("NEW_VOICE_MESSAGE") != -1){
				trMessage = getString(R.string.new_voice_message_shared_by);
			}else if(message.indexOf("NEW_CALL") != -1){
				trMessage = getString(R.string.call_from_);
			}else if(message.indexOf("NEW_TEXT_MESSAGE") != -1){
				trMessage = getString(R.string.from);
				return message.replace("NEW_TEXT_MESSAGE", trMessage + " " + sender + " : ");
			}
			return trMessage + " " + sender;
		}
		return "";
	}
	
	/*private int getNotificationIcon() {
	    boolean whiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
	    // return whiteIcon ? R.drawable.icon_silhouette : R.drawable.app_icon;
	    if(whiteIcon){
	    	Resources r = getResources();
	        int resourceId = r.getIdentifier("app_icon_144", "raw", YooApplication.getAppContext().getPackageName());
	        return resourceId;
	    	// return R.drawable.app_icon_144;
	    }
	    
	    return R.drawable.app_icon;
	}*/
	
	/*private Bitmap getLargeNotificationIcon() {
	    Context context = YooApplication.getAppContext();
	    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
	        return BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon_144); // R.drawable.your_notifiation_icon_lollipop);
	    }else{
	        return BitmapFactory.decodeResource(context.getResources(), R.drawable.app_icon); // R.drawable.your_notifiation_icon);
	    }
	}*/
	
	public static void setPushMessage(Context context, int count){
		setPushMessage(context, count, true); 
	}
	
	
	public static void setPushMessage(Context context, int count, boolean clear){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_PushCount", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("push_count", count); 
		if(clear){
			editor.putInt("push_count_new", 0); 
		}
		editor.commit();
	}
	
	public static void setPushMessageNew(Context context, int countNew){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_PushCount", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("push_count_new", countNew); 
		editor.commit();
	}
	
	public static int countPushMessage(Context context){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_PushCount", Context.MODE_PRIVATE);  
		int count = preferences.getInt("push_count", 0) + 1;
		setPushMessage(context, count, false);
		return count;
	}
	
	public static int countPushMessageNew(Context context){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_PushCount", Context.MODE_PRIVATE);  
		int count = preferences.getInt("push_count_new", 0) + 1;
		setPushMessageNew(context, count);
		return count;
	}
	
	public static void setPushMessageId(Context context, Set<String> msgIds){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_PushMessageId", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putStringSet("push_messageId", msgIds);
		editor.commit();
	}
	
	public static boolean checkPushMessageId(Context context, String msgId){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_PushMessageId", Context.MODE_PRIVATE);  
		Set<String> msgIds = preferences.getStringSet("push_messageId", new HashSet<String>());
		if(msgIds.contains(msgId)){
			return true;
		}
		
		msgIds.add(msgId);
		setPushMessageId(context, msgIds);
		
		return false;
	}
	
	public static void removePushMessageId(String msgId){
		Context context = YooApplication.getAppContext();
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_PushMessageId", Context.MODE_PRIVATE);  
		Set<String> msgIds = preferences.getStringSet("push_messageId", new HashSet<String>());
		if(msgIds.contains(msgId)){
			msgIds.remove(msgId);
			setPushMessageId(context, msgIds);
		}
	}
	
	/*public static void clearPushMessageId(Context context){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_PushMessageId", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.clear();
		editor.commit();
	}*/
	
	
	
}
