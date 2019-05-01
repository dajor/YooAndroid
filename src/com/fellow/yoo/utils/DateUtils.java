package com.fellow.yoo.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.fellow.yoo.R;

import android.annotation.SuppressLint;
import android.content.Context;

@SuppressLint({ "SimpleDateFormat", "UseValueOf" })
public class DateUtils {

	
	public static Date substructDate(long seconds){
		//TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		Date date = new Date();
		return new Date(date.getTime() - seconds*1000); 
	}
	
	public static long substructTime(long millisecond){
		//TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		Date date = new Date();
		return date.getTime() - millisecond; 
	}
	
	public static long substructTimeAsSeconds(Date pDate){
		//TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		if(pDate != null){
			Date date = new Date();
			return (date.getTime() - pDate.getTime())/1000; 
		}
		return 0;
	}
	
	
	public static String formatDate(Date date) {		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		return sdf.format(date);
	}

	public static String formatTime(Date date) {	
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}
	
	public static long formatTimeStamp(Date date) {
		return date.getTime();
	}
	
	public static Date parseString(String s) {
		if (s == null)
			return null;
		// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS", Locale.US);
		try {
			if (s.indexOf(":") < 0){
				sdf = new SimpleDateFormat(new String("yyyy-MM-dd"));
			}
			
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			return sdf.parse(s);
		} catch (ParseException e) {
			return null;
		}
	}
	
	
	public static String formatDateLastOnline(Context context, Date lastDate) {
		String status = "";
		Date todayDate = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		if(sdf.format(todayDate).equals(sdf.format(lastDate))){
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			sdf = new SimpleDateFormat("KK:mm a");
			status = context.getString(R.string.today) + ", " + sdf.format(lastDate);
		}else{
			sdf = new SimpleDateFormat("MMM dd, yyyy");
			status = sdf.format(lastDate);
		}
	    return status;
	}
	
	
	
}
