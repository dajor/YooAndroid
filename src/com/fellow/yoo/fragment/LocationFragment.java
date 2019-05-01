package com.fellow.yoo.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fellow.yoo.R;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.adapter.LocationListAdapter;
import com.fellow.yoo.model.YooLocation;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.StringUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

@SuppressLint("InflateParams")
public class LocationFragment extends ListFragment {
	
	private YooLocation currentLocation;
	private List<YooLocation> datas = new ArrayList<YooLocation>();
	private MapView map;
	public static LocationFragment newInstance(YooLocation curr) {
		LocationFragment f = new LocationFragment();
	    Bundle args = new Bundle();
	    args.putSerializable("curr", curr);
	    f.setArguments(args);
	    return f;
	}
	
	
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		currentLocation = (YooLocation) getArguments().getSerializable("curr");
		datas = YooApplication.locations;
	
		// "Current Location", "Nearby Location"
		String curLocation = getString(R.string.current_location);
		String nearbyLocation = getString(R.string.nearby_location);
		List<String> sections = Arrays.asList(curLocation, nearbyLocation);
		Map<String, List<YooLocation>> data = new HashMap<String, List<YooLocation>>();
		List<YooLocation> currs = new ArrayList<YooLocation>();
		currs.add(currentLocation);
		data.put(curLocation, currs);
		
		// add nearby locations section.
		data.put(nearbyLocation, datas);
		
		setListAdapter(new LocationListAdapter(sections, data));
		
	}
	
	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		YooLocation locaton = (YooLocation) getListAdapter().getItem(position);
		if (locaton != null) {
			try {
				map = YooApplication.getMap(getActivity());
				if(map != null && map.getMap() != null){
					showMapViewDialog(locaton); 
				}else{
					Toast.makeText(getActivity(), getActivity().getString(R.string.install_google_play_service), Toast.LENGTH_SHORT).show(); 
				}
			} catch (Exception e) {
				Log.w("LocationFragment : Item Click >> ", e.toString());
				Toast.makeText(getActivity(), getActivity().getString(R.string.install_google_play_service), Toast.LENGTH_SHORT).show(); 
			}
		}
		
		((BaseAdapter)getListAdapter()).notifyDataSetChanged();
	}
	
	
	

	@SuppressLint("InflateParams")
	private void showMapViewDialog(final YooLocation yooLocation){
		
		
		LayoutInflater inflator = getActivity().getLayoutInflater(); 
		RelativeLayout scrollView = (RelativeLayout) inflator.inflate(R.layout.map_layout, null, false);
		
		final LinearLayout mapLayout = (LinearLayout) scrollView.findViewById(R.id.map_layout);
		int screenWidth = ActivityUtils.getScreenWidth(getActivity()); // getActivity().getWindowManager().getDefaultDisplay().getWidth();
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, screenWidth);
		mapLayout.setLayoutParams(params);
		
		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		map.setLayoutParams(params1); 
		mapLayout.addView(map);
		addMapMarker(map, yooLocation); 
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
		alert.setTitle(getString(R.string.map_view)); 
		alert.setView(scrollView);
		map.getMap().setOnMapLoadedCallback(new OnMapLoadedCallback(){
	        @Override
	        public void onMapLoaded() {
	           //  captureMapScreen(map, yooLocation); 
	        }
	    });
		
	
		alert.setPositiveButton(getString(R.string.post_record), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int whichButton) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("location", yooLocation);
				getActivity().setResult(Activity.RESULT_OK, returnIntent);
				getActivity().finish();
			}
		});
	
		alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() { 
			public void onClick(DialogInterface dialog,int whichButton) {
				
			}
		});
		
		alert.show();
	}
	
	
	private void addMapMarker(MapView map, YooLocation yooLocation){
        GoogleMap  googleMap = map.getMap(); 
        if(googleMap != null){
        	googleMap.clear();
            googleMap.setMyLocationEnabled(true);
            String zoom = "15";
            if(yooLocation != null){
                MarkerOptions markerOptions = new MarkerOptions();
                LatLng point = new LatLng(yooLocation.getLat(), yooLocation.getLng());
                
                // map.invalidate();
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(point));
                markerOptions.position(point);
                //markerOptions.title(yooLocation.getName());
                //markerOptions.snippet(yooLocation.getAddress());	
                
                String message = yooLocation.getName() + "\n" + yooLocation.getAddress();
                if(!StringUtils.isEmpty(message)){
                	String delimiter = "\\n";
                	markerOptions.title(message.split(delimiter)[0]);
                	if(message.split(delimiter).length > 1){ 
                		markerOptions.snippet(message.split(delimiter)[1]);	
                	}
                }
                
                Marker marker = googleMap.addMarker(markerOptions);
                marker.showInfoWindow();
                
                // Moving CameraPosition to last clicked position
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(point));
            }

            // Setting the zoom level in the map on last position  is clicked
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(Float.parseFloat(zoom)));
            map.onLowMemory();
            map.setClickable(false); 
            map.invalidate(); 
            
        }
	}

	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
}
