package com.fellow.yoo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fellow.yoo.bitmaputils.ImageCache;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.BaseDAO;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.data.ContactDAO;
import com.fellow.yoo.data.ContactManager;
import com.fellow.yoo.data.DatabaseHelper;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.fragment.ChatFragment;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.YooLocation;
import com.fellow.yoo.model.YooMessage.CallStatus;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.CatchFileUtils;
import com.fellow.yoo.utils.DateUtils;
import com.fellow.yoo.utils.RecyclingMapView;
import com.fellow.yoo.utils.StringUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.MapView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class YooApplication extends Application {
   

	
	private static Context context;
	private static String gcmId;
	public static List<YooLocation> locations;
	public static Map<String, Bitmap> mapImages;
	public static MapView map; // don't want to create new mapView many time. to avoid memory lakes.
	public static boolean refreshOptionMenu = true;
	public static boolean hasCalling = false;
	private ImageCache imageCache;
	public static Map<String, Integer> background;
	public static List<YooUser> broadcastUsers;
	private static double latitude = -1;
	private static double longitude = -1;
	public static YooUser callMember;
	public static Map<String, Contact> contacts;
	
	// to maintain network connect when user leave the app from everywhere on app
	
	public static byte[] postImageByte = null;
    
    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
        mapImages = new HashMap<String, Bitmap>();
        broadcastUsers = new ArrayList<YooUser>();
        background = new HashMap<String, Integer>();
        contacts = new HashMap<String, Contact>();
        
        background.put("Default", R.drawable.bg1);
        background.put("Space", R.drawable.bg2);
        background.put("Blue", R.drawable.bg3);
        background.put("Grass", R.drawable.bg4);
        background.put("Sand", R.drawable.bg5);
        background.put("Snow", R.drawable.bg6);
        background.put("Lava", R.drawable.bg7);
        background.put("Wood", R.drawable.bg8);
        background.put("Trees", R.drawable.bg9);
        
        
        // Before the secondary dex file can be processed by the DexClassLoader,
        // it has to be first copied from asset resource to a storage location.
        /*
        String SECONDARY_DEX_NAME = "axis2-kernel-1.6.1.jar";
        final int BUF_SIZE = 8 * 1024;
        File dexInternalStoragePath = new File(getDir("dex", Context.MODE_PRIVATE), SECONDARY_DEX_NAME);
        // ...
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;
        try {
        	bis = new BufferedInputStream(getAssets().open(SECONDARY_DEX_NAME));
            dexWriter = new BufferedOutputStream(
                new FileOutputStream(dexInternalStoragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            dexWriter.close();
            bis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
        
        // init the database, and the tables
        DatabaseHelper.init(context);
        for (BaseDAO dao : Arrays.asList(new ContactDAO(), new ChatDAO(), new GroupDAO(), new UserDAO())) {
        	dao.initTables();
        }

        
		//        // login to openfire
		//        new AsyncTask<Void, Void, Void>() {
		//			@Override
		//			protected Void doInBackground(Void... params) {
		//
		//	        	// test with Daniel user for now
		//	            YooUser me = new YooUser("dajor", ChatTools.YOO_DOMAIN);
		//	            me.setAlias("Daniel Jordan");
		//	            me.setCallingCode(49);
		//	            new UserDAO().upsert(me);
		//	            
		//	    		SharedPreferences preferences = YooApplication.getAppContext().getSharedPreferences(
		//	    				"YooPreferences", Context.MODE_PRIVATE);
		//	    		Editor editor = preferences.edit();
		//	    		editor.putString("login", me.getName());
		//	    		editor.commit();
		//	            
		//				ChatTools.sharedInstance().login(me.getName(), "1qklvrks5o");
		//				Log.i("YooApplication", "Login successful");
		//					
		//
		//		        return null;
		//			}
		//		}.execute();
    }
    
   
    private static MediaPlayer mediaPlayer;
	public static String callReqId;
	public static AlertDialog callDialog;
	private static Activity mainActivity;
	@SuppressLint("InflateParams")
	public static void callingPhoneAlert(Activity activity, String name, String phone, Date startDate){
		
		if( (mediaPlayer != null && mediaPlayer.isPlaying()) || 
				callDialog != null && callDialog.isShowing() || hasCalling){
			return;
		}
		
		long waiting = ChatTools.CALL_MAX_DELAY;
		if(startDate != null){
    		long seconds = DateUtils.substructTimeAsSeconds(startDate);
    		if(seconds >= ChatTools.CALL_MAX_DELAY){
    			return;
    		}
    		waiting = waiting - seconds;
    	}
		
		hasCalling = true;
		mainActivity = activity;
		
		LayoutInflater inflator = activity.getLayoutInflater(); 
		ScrollView scrollView = (ScrollView) inflator.inflate(R.layout.popup_layout, null, false);
		
		callDialog = new AlertDialog.Builder(activity).create();
		callDialog.setView(scrollView);
		
		LinearLayout mainLayout = (LinearLayout) scrollView.findViewById(R.id.edit_layout);
		mainLayout.setGravity(Gravity.CENTER);
		mainLayout.removeAllViews();
		int scWidth = ActivityUtils.getScreenWidth(activity);
		int padding = ActivityUtils.dpToPixels(activity, 10);
		int lineS = ActivityUtils.dpToPixels(activity, 1);
		int buttonWidth = scWidth/2 - padding * 2;
		
		TextView txtMessage = new TextView(activity);
		txtMessage.setPadding(padding/2, padding*2, padding/2, padding*2);
		txtMessage.setTextSize(24); 
		txtMessage.setText(activity.getString(R.string.call_from, name, phone)); 
		txtMessage.setGravity(Gravity.CENTER); 
		mainLayout.addView(txtMessage);
		
		LinearLayout buttonLayout = new LinearLayout(activity);
		buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		buttonLayout.setPadding(padding/2, padding/3, padding/2, padding/2); 
		Button btnDecline = newButton(activity, R.string.decline, buttonWidth);
		Button btnAccept = newButton(activity, R.string.accept, buttonWidth);
		
		View line = new View(activity);
		line.setBackgroundColor(Color.LTGRAY);
		line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, lineS));
		
		View line2 = new View(activity);
		line2.setBackgroundColor(Color.LTGRAY);
		line2.setLayoutParams(new LinearLayout.LayoutParams(lineS, LinearLayout.LayoutParams.MATCH_PARENT));
		
		buttonLayout.addView(btnDecline);
		buttonLayout.addView(line2);
		buttonLayout.addView(btnAccept);
		mainLayout.addView(line); 
		mainLayout.addView(buttonLayout);
		
		btnDecline.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!StringUtils.isEmpty(callReqId)){
					new ChatDAO().updateCallStatus(callReqId, CallStatus.csRejected); 
					refreshChatFragment(mainActivity); 
					ChatTools.sharedInstance().answerCall(mainActivity, callReqId, false);
				}
				stopCall();
			}
		});
		
		btnAccept.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!StringUtils.isEmpty(callReqId)){
					new ChatDAO().updateCallStatus(callReqId, CallStatus.csAccepted); 
					refreshChatFragment(mainActivity); 
					ChatTools.sharedInstance().answerCall(mainActivity, callReqId, true);
				}
				stopCall();
			}
		});
		
		mainLayout.setOrientation(LinearLayout.VERTICAL); 
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL); 
		callDialog.show();
		
		playCalTone(activity);
		callWaiting(waiting, activity);
        
	}
	
	
	public static void callWaiting(long waiting, Activity activity){
		Log.i("callWaiting >> ", " start Call waiting... " + waiting);
		mainActivity = activity;
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if(hasCalling){
					Log.i("callWaiting >> ", " end call waiting.. ");
					new ChatDAO().updateCallStatus(callReqId, CallStatus.csCancelled); 
					if(mainActivity != null){
						refreshChatFragment(mainActivity); 
					}
					ChatTools.sharedInstance().cancelCall(YooApplication.callReqId);
					stopCall();
				}
			}
		}, waiting*1000); // 30 seconds
	}
	
	
	private static Button newButton(Activity activity, int label, int buttonWidth){
		Button button = new Button(activity);
		button.setTypeface(null, Typeface.BOLD);
		button.setTextColor(activity.getResources().getColor(R.color.light_blue)); 
		button.setText(label); 
		button.setBackgroundColor(activity.getResources().getColor(R.color.transparent)); 
		button.setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		return button;
	}

    public static void refreshChatFragment(Activity activity){
    	if(activity != null && activity instanceof YooActivity){
			YooActivity yooActivity = (YooActivity) activity;
			Fragment last = yooActivity.fragments.lastElement();
			if(last instanceof ChatFragment){
				final FragmentTransaction ft = last.getFragmentManager().beginTransaction();
    			ft.detach(last);
    			ft.attach(last);
    			ft.commit();
				// (ChatFragment (last)).refreshReceiveMessage(null);
			}
		}
    }
	
    public static void playCalTone(Activity activity){
		try {
			if(context != null){
				AssetFileDescriptor afd = activity.getAssets().openFd("ringtone.mp3");
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				mediaPlayer.prepare();
				mediaPlayer.start();
				mediaPlayer.setLooping(true);
			}
		}catch (Exception e) {
			Log.e("ChatFragment : playCalTone >> ", e.toString());
		}
	}
    
	
	public static void stopCall(){
		if(mediaPlayer != null && mediaPlayer.isPlaying()){
			mediaPlayer.stop();
		}
		
		if(callDialog != null){
			callDialog.dismiss();
		}
		
		hasCalling = false;
	}
    
    public static void importAllContacts(Activity activity){
    	// import the contacts in the table, in background
    	mainActivity = activity;
       // new ContactManager().importAll();
       // ChatTools.sharedInstance().contactsLoaded();
       
       new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				
				new ContactManager().importAll();
				ChatTools.sharedInstance().contactsLoaded();
				
				
				return null;
				
			}
        }.execute();
    }
    

	public static Context getAppContext() {
        return YooApplication.context;
    }
	
	public static Context geMainActivity() {
        return YooApplication.context;
    }
    
    public static String getGcmId() {
    	return YooApplication.gcmId;
    }

    public void setGcmId(String pGcmId) {
    	gcmId = pGcmId;
    }
    
    
    public static Bitmap getBitmap(Activity activity, String bitmapId){
		Bitmap bitmap = mapImages.get(bitmapId);
		if(bitmap == null){
			String fileName = CatchFileUtils.getFileName(bitmapId);
			bitmap = CatchFileUtils.loadFromDirectoryFile(context, fileName);
			if(activity instanceof MainActivity){
				// bitmap = ((MainActivity) activity).getImageFetcher().getBitmap(bitmapId);
	    	}
			// mapImages.put(bitmapId, bitmap);
		}
		return bitmap;
    }
    
    public static Bitmap saveBitmap(Activity activity, String bitmapId, Bitmap bitmap){
    	String fileName = CatchFileUtils.getFileName(bitmapId);
    	CatchFileUtils.saveToDirectoryFile(context, fileName, bitmap);
    	if(activity instanceof MainActivity){
    		// ((MainActivity) activity).getImageFetcher().saveBitMapCatche(bitmapId, bitmap); 
    	}
		// mapImages.put(bitmapId, bitmap);
		return bitmap;
    }
    
    public static String getLastOnline(Context context, Date lastOnline){
    	StringBuffer laOnline = new StringBuffer();
    	if(lastOnline != null){
    		laOnline.append(context.getString(R.string.last_online) + " ");
        	laOnline.append(DateUtils.formatDateLastOnline(context, lastOnline));
    	}else{
    		laOnline.append(context.getString(R.string.offline) + " ");
    	}
    	return laOnline.toString();
    }
    
    
    
	public static MapView getMap(Activity activity) {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		try {
			if (status == ConnectionResult.SUCCESS) {
				if (map == null) {
					map = new RecyclingMapView(activity);
					map.onCreate(Bundle.EMPTY);
					map.onResume();
				} else {
					ViewGroup parentViewGroup = (ViewGroup) map.getParent();
					if (parentViewGroup != null) {
						parentViewGroup.removeView(map);
					}
				}
			}
			if(map != null){
				map.getMap(); // try to test device can access 
			}
		} catch (Exception e) {
			map = null;
			Log.e("YooApplication : GooglePlayServiceUtit >> ", e.toString());
		}
		return map;
	}
	
	public String getCountryZipCode(){
		String CountryID = "";
		String CountryZipCode = "";
		TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		// getNetworkCountryIso
		CountryID = manager.getSimCountryIso().toUpperCase(Locale.US);
		String[] rl = this.getResources().getStringArray(R.array.CountryCodes);
		for (int i = 0; i < rl.length; i++) {
			String[] g = rl[i].split(",");
			if (g[1].trim().equals(CountryID.trim())) {
				CountryZipCode = g[0];
				break;
			}
		}
		return CountryZipCode;
	}
   
   
    public ImageCache getImageCache() {
		return imageCache;
	}


	public void setImageCache(ImageCache imageCache) {
		this.imageCache = imageCache;
	}
	
	@Override
	protected void attachBaseContext(Context base) {
	    super.attachBaseContext(base);
	    // MultiDex.install(this);
	}
    
	public static String getLogin(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		return preferences.getString("login", "") +  "@" + ChatTools.YOO_DOMAIN;
	}
	
	public static YooUser getUserLogin(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		String login = preferences.getString("login", "");
		return new UserDAO().find(login, ChatTools.YOO_DOMAIN);
	}
	
	public boolean isMyPhone(String pPhone) {
		SharedPreferences preferences = YooApplication.getAppContext().getSharedPreferences(
				"YooPreferences", Context.MODE_PRIVATE);
		String phone = preferences.getString("phoneNumber", null);
		return phone != null && pPhone.indexOf(phone) > 0;
	}
	
	public static String geCountryCode(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		return preferences.getString("countryCode", "");
	}
	
	public static String getCallingCode(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		return preferences.getString("callingCode", "");
	}
	
	
	public static String getBckGroundName(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		return preferences.getString("background", "Default");
	}
	
	public static void setBckGround(String background){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("background", background);
		editor.commit();
	}
	
	public static void addRecentChat(String jId){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Recent", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(jId, DateUtils.formatTime(new Date())); 
		editor.commit();
		YooApplication.refreshOptionMenu = true;
	}
	
	public static void removeRecentChat(String jId){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Recent", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(jId);
		editor.commit();
		YooApplication.refreshOptionMenu = true;
	}
	
	public static String getRecentChat(String jId){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Recent", Context.MODE_PRIVATE);  
		return preferences.getString(jId, "");
	}
	
	public static void setCalling(boolean calling){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Recent", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("has_calling", calling); 
		editor.commit();
		hasCalling = calling;
	}
	
	public static void checkCalliing(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Recent", Context.MODE_PRIVATE);  
		hasCalling = preferences.getBoolean("has_calling", false);
	}
	
	
	public static void setLeaveApp(boolean leave){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_LeaveApp", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("leave_app", leave); 
		editor.commit();
	}
	
	public static boolean isLeaveApp(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_LeaveApp", Context.MODE_PRIVATE);  
		return preferences.getBoolean("leave_app", false);
	}
	
	
	public static void setAppOn(boolean on){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_LeaveApp", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("app_on", on); 
		editor.commit();
	}
	
	public static boolean isAppOn(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_LeaveApp", Context.MODE_PRIVATE);  
		return preferences.getBoolean("app_on", false);
	}
	
	public static void setFistRegister(boolean leave){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Register", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("first_register", leave); 
		editor.commit();
	}
	
	public static boolean isFirstRegister(){
		SharedPreferences preferences = context.getSharedPreferences("YooPreferences_Register", Context.MODE_PRIVATE);  
		return preferences.getBoolean("first_register", false);
	}
	
	
	
	public static Contact getContact(Long contactId){
		Contact contact = contacts.get(String.valueOf(contactId));
		if(contact == null){
			contact = new ContactDAO().find(contactId);
			addContact(contact); 
		}
		return contact;
	}
	
	public static void addContact(Contact contact){
		contacts.put(String.valueOf(contact.getContactId()), contact);
	}
	
	
	public static int getBckGroundSource(){
		String bgName = getBckGroundName();
		return background.containsKey(bgName) ? background.get(bgName) : R.drawable.bg1;
	}
	
	public static double getLatitude() {
		return latitude;
	}

	public static void setLatitude(double latitude) {
		YooApplication.latitude = latitude;
	}

	public static double getLongitude() {
		return longitude;
	}

	public static void setLongitude(double longitude) {
		YooApplication.longitude = longitude;
	}

	
	
}
