package com.fellow.yoo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fellow.yoo.chat.ChatListener;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.fragment.RegisterFragment;
import com.fellow.yoo.fragment.RegisterFragment.RegisterCallbacks;
import com.fellow.yoo.gcm.GcmIntentService;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.BadgeUtils;
import com.fellow.yoo.utils.StringUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

public class RegisterActivity extends Activity implements RegisterCallbacks, ChatListener {

	private int step = 1;
	private Map<String, String> data = new HashMap<String, String>();
	private String smsSent;
	private RegisterFragment fragment;
	
	// Google messaging
	private final static String SENDER_ID = "1058456216304";    
	private GoogleCloudMessaging gcm;
	private String regid;
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Check device for Play Services APK.
		registerGCM();
		
		if (testStartApp()) return;
		
		ChatTools.sharedInstance().addListener(this);

		TelephonyManager tm = (TelephonyManager)this.getSystemService(TELEPHONY_SERVICE);
		data.put("countryCode", tm.getNetworkCountryIso().toUpperCase(Locale.US));

		setContentView(R.layout.activity_register);

		fragment = RegisterFragment.newInstance(data, step);
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.register_container, fragment);
		transaction.commit();

		updateTitle();
		
		// login to openfire with registration user
		ChatTools.sharedInstance().asyncLogin(ChatTools.REGISTRATION_USER, ChatTools.REGISTRATION_USER);
		
		YooApplication.setFistRegister(true); 

	}
	
	private void registerGCM(){
		if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(this);

            if (regid.isEmpty()) {
                registerInBackground();
            } else {
            	((YooApplication)getApplication()).setGcmId(regid);
            }
        } else {
            Log.i("MainActivity", "No valid Google Play Services APK found.");
        }
	}
	
	@Override
	protected void onDestroy() {
		GcmIntentService.setPushMessage(this, 0);
		BadgeUtils.setBadge(this, 0); 
		ChatTools.sharedInstance().removeListener(this);
		super.onDestroy();
	}

	@Override
	public void onNext(Map<String, String> pData) {
		data = pData;
		if (step == 2) {
			if(ActivityUtils.checkNetworkConnected(this, R.string.no_internet_connect)){
				if (smsSent == null || !smsSent.equals(data.get("formattedPhone"))) { 
					ChatTools.sharedInstance().registerUser(
							data.get("nickname"), data.get("formattedPhone"), Integer.parseInt(data.get("callingCode")));
					smsSent = data.get("formattedPhone");
				}
			}else{
				return;
			}
		}
		if (step == 3) {
			ChatTools.sharedInstance().registerUser(smsSent, data.get("activationCode"));
		}

		if (step < 3) {
			step++;
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
			fragment = RegisterFragment.newInstance(data, step);
			ft.replace(R.id.register_container, fragment);
			ft.commit();
	
			updateTitle();
		} else {
			// dialog = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.please_wait));
		}

	}

	@Override
	public void onPrev(Map<String, String> pData) {
		data = pData;
		step--;
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right);
		fragment = RegisterFragment.newInstance(data, step);
		ft.replace(R.id.register_container, fragment);
		ft.commit();

		updateTitle();
	}
	
	private void updateTitle() {
		getActionBar().setTitle(getString(R.string.register_step_title, step));
	}

	@Override
	public void friendListChanged(List<YooUser> newFriends) {
	}

	@Override
	public void didReceiveMessage(YooMessage message) {
	}

	
	private boolean testStartApp() {
		SharedPreferences preferences = getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		if (!StringUtils.isEmpty(preferences.getString("login", null)) && 
				!StringUtils.isEmpty(preferences.getString("password", null))) {
			ChatTools.sharedInstance().asyncLogin(preferences.getString("login", null), preferences.getString("password", null));
			
			if(YooApplication.isFirstRegister()){
				YooApplication.setFistRegister(false); 
				registerGCM();
				
			}
		    Intent intent = new Intent(this, MainActivity.class);
		    // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    startActivity(intent);
		    finish();
		    return true;
		}
		return false;
	}
	
	@Override
	public void didLogin(String login, String error) {
		try {
			if (login != null && !login.equals(ChatTools.REGISTRATION_USER)) {
				if (StringUtils.isEmpty(error)) {
					testStartApp();
				}
			}
		} catch (Exception e) {
			Log.e("RigisterActivity : didLogin", e.getMessage());
		}
		
	}

	@Override
	public void didReceiveRegistrationInfo(String login, String password) {
		if (password == null) {		
			runOnUiThread(new Runnable() {
		        @Override
		        public void run() {
					fragment.wrongCode();
		        }
			});
		}else{
			runOnUiThread(new Runnable() {
		        @Override
		        public void run() {
		        	ProgressDialog.show(RegisterActivity.this, getString(R.string.loading), getString(R.string.please_wait));
		        }
			});
			
			
			ChatTools.sharedInstance().asyncDisconnect(false);
			
			SharedPreferences preferences = getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
			SharedPreferences.Editor editor = preferences.edit();

			editor.putString("login", login);
			editor.putString("password", password);
			editor.putString("nickname", data.get("nickname"));
			editor.putString("countryCode", data.get("countryCode"));
			editor.putString("callingCode", data.get("callingCode"));
			editor.putString("phoneNumber", data.get("phone"));
		    editor.commit();
		    
		    YooUser yooUser = new YooUser(login, ChatTools.YOO_DOMAIN);
		    yooUser.setAlias(data.get("nickname"));
		    yooUser.setCallingCode(Integer.parseInt(data.get("callingCode")));
		    new UserDAO().upsert(yooUser);
    
		    ChatTools.sharedInstance().asyncLogin(login, password);
		}
		
	}

	@Override
	public void didReceiveUserFromPhone(Map<String, String> info) {
	}

	@Override
	public void addressBookChanged() {
	}
	
	/* Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    if (resultCode != ConnectionResult.SUCCESS) {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, this,
	                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        } else {
	            Log.i("MainActivity", "This device is not supported.");
	            finish();
	        }
	        return false;
	    }
	    return true;
	}
	

    
	/**
	 * Gets the current registration ID for application on GCM service.
	 * <p>
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.isEmpty()) {
	        Log.i("MainActivity", "Registration not found.");
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion) {
	        Log.i("MainActivity", "App version changed.");
	        return "";
	    }
	    return registrationId;
	}
	
	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
	    // This sample app persists the registration ID in shared preferences, but
	    // how you store the regID in your app is up to you.
	    return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
	    new AsyncTask<Void, Void, String>() {
	        @Override
	        protected String doInBackground(Void... params) {
	            String msg = "";
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(RegisterActivity.this);
	                }
	                regid = gcm.register(SENDER_ID);
	                msg = "Device registered, registration ID=" + regid;

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                sendRegistrationIdToBackend();

	                // For this demo: we don't need to send it because the device
	                // will send upstream messages to a server that echo back the
	                // message using the 'from' address in the message.

	                // Persist the regID - no need to register again.
	                storeRegistrationId(RegisterActivity.this, regid);
	            } catch (IOException ex) {
	                msg = "Error :" + ex.getMessage();
	                // If there is an error, don't just keep trying to register.
	                // Require the user to click a button again, or perform
	                // exponential back-off.
	            }
	            return msg;
	        }

	        @Override
	        protected void onPostExecute(String msg) {
	            
	        }
	    }.execute(null, null, null);
	}

	
	/**
	 * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
	 * or CCS to send messages to your app. Not needed for this demo since the
	 * device sends upstream messages to a server that echoes back the message
	 * using the 'from' address in the message.
	 */
	private void sendRegistrationIdToBackend() {
	    // Your implementation here.
		Log.i("MainActivity", "Sending regId : " + regid);
	}
	
	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i("MainActivity", "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
	}


}
