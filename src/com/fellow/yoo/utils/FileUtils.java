package com.fellow.yoo.utils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class FileUtils {
	
	public static Bitmap loadSelectedPhoto(Activity activity, Uri selectedImageUri) throws Exception {
		Bitmap bitmap = null;
		String filePath = null;
		// OI FILE Manager
		String filemanagerstring = selectedImageUri.getPath();

		// MEDIA GALLERY
		String selectedImagePath = FileUtils.getPath(activity, selectedImageUri);

		if (selectedImagePath != null) {
			filePath = selectedImagePath;
		} else if (filemanagerstring != null) {
			filePath = filemanagerstring;
		} else {
			Toast.makeText(activity, "Unknown path", Toast.LENGTH_LONG).show();
			Log.e("Bitmap", "Unknown path");
		}

		if (filePath != null) {
			bitmap = FileUtils.decodeFile(activity, selectedImageUri, filePath);
		} else {
			bitmap = null;
		}
		return bitmap;
	}
	
	public static String getPath(Activity activity, Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		@SuppressWarnings("deprecation")
		Cursor cursor = activity.managedQuery(uri, projection, null, null, null);
		if (cursor != null) {
			// HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
			// THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		}
		return null;
	}

	public static Bitmap decodeFile(Activity activity, Uri photoUri, String filePath) {
		// Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, o);

		// The new size we want to scale to
		final int REQUIRED_SIZE = 1024;

		// Find the correct scale value. It should be the power of 2.
		int width_tmp = o.outWidth, height_tmp = o.outHeight;
		int scale = 1;
		while (true) {
			if (width_tmp < REQUIRED_SIZE && height_tmp < REQUIRED_SIZE)
				break;
			width_tmp /= 2;
			height_tmp /= 2;
			scale *= 2;
		}

		// Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		Bitmap bitmap = BitmapFactory.decodeFile(filePath, o2);
		
		int orientation = getOrientation(activity, photoUri);
		if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, 
            		bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
		return bitmap;
	}
	
	public static int getOrientation(Context context, Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor == null || cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }

}
