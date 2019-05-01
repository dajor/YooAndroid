package com.fellow.yoo.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Environment;
import android.util.Log;
import android.view.View;

public class CatchFileUtils {
	

	public static String getFileName(String url) {
		String str = "";
		if (!StringUtils.isEmpty(url)) {
			str = url.replace("http://", "");
			str = str.replace("/", "_");
			System.out.println(" ======FileName======= " + str);
		}
		return str;
	}
	
	public static boolean hasSDCard() { // SD????????
		String status = Environment.getExternalStorageState();
		return status.equals(Environment.MEDIA_MOUNTED);
	}

	public static String getSDCardPath() {
		File path = Environment.getExternalStorageDirectory();
		return path.getAbsolutePath(); 
	}

	
	// =========================Save to Files directory under project folder===========
	// ================================================================================
	// save to memory file, will remove when close application
	public static String getDirectoryFilename(Context context, String fileName) {
		return context.getFilesDir().getAbsolutePath() + "/" + fileName;
	}

	public static void saveToDirectoryFile(Context context, String fileName, Bitmap bmp) {
		saveToFile(getDirectoryFilename(context, fileName), bmp);
	}

	public static Bitmap loadFromDirectoryFile(Context context, String fileName) {
		return loadFromFile(getDirectoryFilename(context, fileName));
	}
	
	// ================================================================================
	// ================================================================================
	public static void saveToFile(String filenameAndPath, final Bitmap bmp) {
		try {
			System.out.println("filenameAndPath : " + filenameAndPath); 
			FileOutputStream out = new FileOutputStream(filenameAndPath);
			if(filenameAndPath.indexOf(".png")!=-1){
				bmp.compress(CompressFormat.PNG, 100, out);
			}else{
				bmp.compress(CompressFormat.JPEG, 100, out);
			}
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Bitmap loadFromFile(String filename) {
		Bitmap tmp = null;
		try {
			System.out.println("filename : " + filename); 
			File f = new File(filename);
			if (!f.exists()) {
				return null;
			}
			tmp = BitmapFactory.decodeFile(filename);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(tmp != null){
			System.out.println(" ==========  bitmap size : =======" 
					+ (tmp.getRowBytes() * tmp.getHeight())/1024 + " kb");
		}
		
		return tmp;
	}
	
	
	public static Bitmap captureMapviews(Context context, View mapViewLayout){
		int width = ActivityUtils.dpToPixels(context, 400);
		int cropWidth = ActivityUtils.dpToPixels(context, 400);
		Bitmap mBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
		try {
			Canvas canvas = new Canvas(mBitmap);
			mapViewLayout.draw(canvas);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Bitmap newBitMap = Bitmap.createScaledBitmap(mBitmap, cropWidth, cropWidth, true);
			newBitMap.compress(CompressFormat.JPEG, 85, os);
			return newBitMap;
		} catch (Exception e) {
			Log.v("Error Capture Mapviews", e.getMessage());
		}
		
		return null;
	}
	
	
	
}