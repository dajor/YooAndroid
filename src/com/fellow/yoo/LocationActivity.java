package com.fellow.yoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import com.fellow.yoo.adapter.TitleNavigationAdapter;
import com.fellow.yoo.fragment.LocationFragment;
import com.fellow.yoo.model.SpinnerNavItem;
import com.fellow.yoo.model.YooLocation;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.GPSTracker;
import com.fellow.yoo.utils.PlaceJSONParser;
import com.fellow.yoo.utils.StringUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

@SuppressWarnings("deprecation")
public class LocationActivity extends Activity implements ActionBar.OnNavigationListener {
	
	
	
	private ArrayList<SpinnerNavItem> navSpinner;
	private TitleNavigationAdapter adapter;

	// private String provider;
	// private LocationManager locationManager;
	private YooLocation currentLocation;
	private AsyncTask<?, ?, ?> tasks;
	private String placeType = "";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("");
		
		getActionBar().setDisplayShowCustomEnabled(true);
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		
		if(YooApplication.locations == null){
			YooApplication.locations = new ArrayList<YooLocation>();
		}
		
		// Get current user location
		GPSTracker gps = new GPSTracker(this);
		if (gps.isGPSEnabled()) {
			YooApplication.setLatitude(gps.getLatitude());
			YooApplication.setLongitude(gps.getLongitude());
		} else {
			gps.showSettingsAlert();
		}
		
		navSpinner = new ArrayList<SpinnerNavItem>();
		navSpinner.add(new SpinnerNavItem("", "", R.drawable.current_64));
		navSpinner.add(new SpinnerNavItem("restaurant", "Restaurant", R.drawable.restaurant_64));
		navSpinner.add(new SpinnerNavItem("lodging", "Hotel", R.drawable.hotel_64));
		navSpinner.add(new SpinnerNavItem("cafe", "Coffee", R.drawable.coffee_64));
		navSpinner.add(new SpinnerNavItem("bar", "Bar", R.drawable.bar_64));

		// title drop down adapter
		adapter = new TitleNavigationAdapter(getApplicationContext(), navSpinner);
		getActionBar().setListNavigationCallbacks(adapter, this);
		
		loadCurrentLocation();
		
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
	}
	
	@Override
	protected void onResume() {
		loadCurrentLocation();
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	
	private void loadCurrentLocation(){
		
	    // Use the location manager through GPS
	    currentLocation = new YooLocation(getString(R.string.current_location), "", null);
	    double lat = (double) (YooApplication.getLatitude());
        double lng = (double) (YooApplication.getLongitude());
        if(lat != -1 && lng != -1){
        	currentLocation.setLat(lat); 
            currentLocation.setLng(lng); 
        	if(ActivityUtils.checkNetworkConnected(this, R.string.improve_location_search)){
    	        try {
    	        	Geocoder geocoder = new Geocoder(this, Locale.getDefault());
    	            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
    	            Log.i("Addresses : ", addresses.toString());
    	            Address result = addresses.get(0);
    	            String name = result.getAdminArea();
    	            currentLocation.setName(!StringUtils.isEmpty(name)? name : currentLocation.getName());
    	            currentLocation.setAddress(getAddress(result)); 
    	            
    	        } catch (IOException e) {
    	        	Log.e("Error Load crruent address", e.getMessage());
    	        }catch (Exception e) {
    	        	e.printStackTrace();
    	        }
            	loadNearbyPlace(currentLocation,  "");
            }
        }else{
        	Log.e("Location", "Location not avilable");
        }
	    
	   
	    reloadFragment();
	}
	
	private String getAddress(Address address){
		StringBuilder addr = new StringBuilder();
		if(!StringUtils.isEmpty(address.getAddressLine(0))){
			addr.append(address.getAddressLine(0) + ", ");
			addr.append(address.getAddressLine(1) + ", ");
			addr.append(address.getAddressLine(2) + "");
		}else if(!StringUtils.isEmpty(address.getAdminArea())){
			addr.append(address.getAdminArea() + ", ");
			addr.append(address.getCountryName() + " ");
		}
		return addr.toString();
	}

	
	
	private void loadNearbyPlace(YooLocation location, String key){
		
		StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
		sb.append("location="+ location.getLat() +"," + location.getLng());
		sb.append("&radius=5000");
		sb.append("&types="+ placeType);
		sb.append("&name="+ URLEncoder.encode(key));
		sb.append("&key=AIzaSyBx9x-NfJSxUCsJn8RQ-YRi1Y9aIHfoZXI");
		sb.append("&sensor=true");
		
		Log.i("Url Request", sb.toString());
		
        tasks = new PlacesTask().execute(sb.toString());	 
        
	}
	
	
	private class PlacesTask extends AsyncTask<String, Integer, String>{

		@Override
		protected String doInBackground(String... urlSr) {
			String data = "";
			try{
				// return downloadUrl(url[0]);
		        InputStream iStream = null;
		        HttpURLConnection urlConnection = null;
		        try{
		                URL url = new URL(urlSr[0]);    
		       
		                urlConnection = (HttpURLConnection) url.openConnection();   
		                urlConnection.connect();                
		                iStream = urlConnection.getInputStream();
		                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
		                StringBuffer sb  = new StringBuffer();
		                String line = "";
		                while( ( line = br.readLine())  != null){
		                    sb.append(line);
		                }
		                data = sb.toString();
		                br.close();
		        }catch(Exception e){
		                Log.e("Exception while downloading url", e.toString());
		        }finally{
		                iStream.close();
		                urlConnection.disconnect();
		        }
			}catch(Exception e){
				 Log.e("Error load places", e.toString());
			}
			return data;
		}

		@Override
		protected void onPostExecute(String result){	
			Log.i("Response Result", result);
			if(!StringUtils.isEmpty(result)){
				tasks = new ParserTask().execute(result);
			}
		}
	}
	
	
    
	private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

		@Override
		protected List<HashMap<String,String>> doInBackground(String... jsonData) {
		
			List<HashMap<String, String>> places = new ArrayList<HashMap<String,String>>();			
			PlaceJSONParser placeJsonParser = new PlaceJSONParser();
        
	        try{
	        	JSONObject jObject = new JSONObject(jsonData[0]);
	            places = placeJsonParser.parse(jObject);
	            
	        }catch(Exception e){
	           Log.e("Exception PaserTask",e.toString());
	        }
	        return places;
		}
		
		@Override
		protected void onPostExecute(List<HashMap<String,String>> list){			
			
			YooApplication.locations.clear();
			for (int i = 0; i < list.size(); i++) {
				HashMap<String, String> hmPlace = list.get(i);

				double lat = Double.parseDouble(hmPlace.get("lat"));
				double lng = Double.parseDouble(hmPlace.get("lng"));
				String name = hmPlace.get("place_name");
				String vicinity = hmPlace.get("vicinity");
				String type = hmPlace.get("types") + " " + name.toLowerCase(Locale.US);  

				YooLocation location = new YooLocation(name, vicinity, YooLocation.getIconType(type));
				location.setLat(lat);
				location.setLng(lng);
				YooApplication.locations.add(location);
			}	
			reloadFragment();
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.contact_detail, menu);
		
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		SearchView searchView = (SearchView) searchItem.getActionView();
		searchView.setQueryHint(getString(R.string.action_search));
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		
		SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				tasks.cancel(true);
				loadNearbyPlace(currentLocation, newText);
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				tasks.cancel(true);
				loadNearbyPlace(currentLocation, query);
				return true;
			}
		};
		searchView.setOnQueryTextListener(textChangeListener);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	private void reloadFragment(){
		try {
			LocationFragment fragment = LocationFragment.newInstance(currentLocation);
	        FragmentTransaction transaction = getFragmentManager().beginTransaction();
	        transaction.replace(R.id.location_layout, fragment);
	        transaction.commit();
		} catch (Exception e) {
			Log.w("", e.toString());
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
	    switch (itemId) {
	    	case android.R.id.home:
	    		finish();
	    	   return true;
	    }
		
		return super.onMenuItemSelected(featureId, item);
	}
	
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		SpinnerNavItem item = navSpinner.get(itemPosition);
		placeType = item.getKey();
		loadNearbyPlace(currentLocation, ""); 
		
		return false;
	}
	
	

	@Override
	protected void onDestroy() {
		stopSyncTask();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		stopSyncTask();
		super.onBackPressed();
	}
	
	private void stopSyncTask() {
		// stop background tasks.
		if (tasks != null && !tasks.isCancelled()) {
			tasks.cancel(true);
			tasks = null;
		}
		YooApplication.locations.clear();
		super.onDestroy();
	}
	
	

	
	

}
