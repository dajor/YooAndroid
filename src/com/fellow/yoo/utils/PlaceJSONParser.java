package com.fellow.yoo.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class PlaceJSONParser {
	
	
	public List<HashMap<String,String>> parse(JSONObject jObject){		
		JSONArray jPlaces = null;
		try {			
			jPlaces = jObject.getJSONArray("results");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return getPlaces(jPlaces);
	}
	
	
	private List<HashMap<String, String>> getPlaces(JSONArray jPlaces){
		int placesCount = jPlaces.length();
		List<HashMap<String, String>> placesList = new ArrayList<HashMap<String,String>>();
		HashMap<String, String> place = null;	
		
		for (int i = 0; i < placesCount; i++) {
			try {
				place = getPlace((JSONObject) jPlaces.get(i));
				placesList.add(place);

			} catch (JSONException e) {
				Log.i("JSONException get place", e.toString());
			}
		}
		
		return placesList;
	}
	
	
	private HashMap<String, String> getPlace(JSONObject jPlace){

		HashMap<String, String> place = new HashMap<String, String>();
		String latitude = "";
		String longitude = "";
		String types = "";	
		
		try {
			
			if(!jPlace.isNull("types")){
				types = jPlace.getJSONArray("types").toString(); 
			}	
			
			latitude = jPlace.getJSONObject("geometry").getJSONObject("location").getString("lat");
			longitude = jPlace.getJSONObject("geometry").getJSONObject("location").getString("lng");			
			
			
			place.put("icon", getJSonString(jPlace, "icon")); 
			place.put("place_name", getJSonString(jPlace, "name"));
			place.put("vicinity", getJSonString(jPlace, "vicinity"));
			place.put("types", types);
			place.put("lat", latitude);
			place.put("lng", longitude);
			
		} catch (JSONException e) {			
			Log.i("JSONException get place", e.toString());
		}		
		return place;
	}
	
	public static String getJSonString(JSONObject jObj, String key) throws JSONException{
		
		if(!jObj.isNull(key)){ 
			return jObj.getString(key).toString();
		}
		return "";
	}
}
