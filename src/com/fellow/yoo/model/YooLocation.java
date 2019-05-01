package com.fellow.yoo.model;

import java.io.Serializable;

import com.fellow.yoo.R;
import com.fellow.yoo.utils.StringUtils;

public class YooLocation implements Serializable, Comparable<YooLocation> {
	
	
	private static final long serialVersionUID = 1L; 


	public enum LocationType {
	    // restaurants, hotels, cafés, bars
		restaurant, 
	    hotel,
	    coffee,
	    bar
	};


	private String name;
	private String address;
	private double lat;
	private double lng;
	private LocationType type;

	public YooLocation() {
		super();
	}
	
	public YooLocation(String name, String address, LocationType type) {
		super();
		this.name = name;
		this.address = address;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}



	public String getAddress() {
		return address;
	}



	public void setAddress(String address) {
		this.address = address;
	}



	public double getLat() {
		return lat;
	}



	public void setLat(double lat) {
		this.lat = lat;
	}



	public double getLng() {
		return lng;
	}



	public void setLng(double lng) {
		this.lng = lng;
	}



	@Override
	public int compareTo(YooLocation another) {
		return name.compareTo(another.name);
	}
	
	
	
	
	public LocationType getType() {
		return type;
	}



	public void setType(LocationType type) {
		this.type = type;
	}



	public int getIcon(LocationType type){
		if(LocationType.restaurant.equals(type)){
			return R.drawable.restaurant_64;
		}else if(LocationType.hotel.equals(type)){
			return R.drawable.hotel_64;
		}else if(LocationType.coffee.equals(type)){
			return R.drawable.coffee_64;
		}else if(LocationType.bar.equals(type)){
			return R.drawable.bar_64;
		}
		return R.drawable.current_64;
	}
	
	public static LocationType getIconType(String type){
		if(!StringUtils.isEmpty(type)){
			if(type.indexOf("hotel") != -1 || type.indexOf("lodging") != -1){
				return LocationType.hotel;
			}else if(type.indexOf("cafe") != -1){
				return LocationType.coffee;
			}else if(type.indexOf("bar") != -1){
				return LocationType.bar;
			}else if(type.indexOf("restaurant") != -1){
				return LocationType.restaurant;
			}
		}
		return null;
	}

}
