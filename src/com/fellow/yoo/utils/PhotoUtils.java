package com.fellow.yoo.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

public class PhotoUtils {
	
public static String getBase64FromFile(Bitmap bm) {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		byte[] byteArrayImage = baos.toByteArray();
		String encodedImage = Base64.encodeToString(byteArrayImage, Base64.NO_WRAP);
		return encodedImage.toString();
	}
	// getExternalStorageDirectory
	public static File createImageFile(Activity activity) throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		// File storageDir = activity.getCacheDir();
		File image = File.createTempFile(imageFileName, /* prefix */
				".jpg", /* suffix */
				storageDir /* directory */
		);

		return image;
	}
	
	public static Bitmap loadImageFromDevice(Activity activity, String photoPath) {
		Bitmap bitmap = configureImage(activity, photoPath);
		int dimension = getSquareBitmap(bitmap);
		return ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
	}

	

	@SuppressWarnings("deprecation")
	public static Bitmap configureImage(Activity activity, String photoPath) {

		int targetW = 500;
		int targetH = 500;

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(photoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW / targetW, photoH / targetH);
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);

		int orientation = getCameraPhotoOrientation(activity, photoPath);
		if (orientation > 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(orientation);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
		return bitmap;
	}

	public static String getRealPathFromURI(Context ctx, Uri contentURI) {
		String result;
		Cursor cursor = ctx.getContentResolver().query(contentURI, null, null, null, null);
		if (cursor == null) { // Source is Dropbox or other similar local file
								// path
			result = contentURI.getPath();
		} else {
			cursor.moveToFirst();
			int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
			result = cursor.getString(idx);
			cursor.close();
		}
		return result;
	}
	
	
	public static int getSquareBitmap(Bitmap bitmap) {
		// If the bitmap is wider than it is tall
		// use the height as the square crop dimension
		if (bitmap.getWidth() >= bitmap.getHeight()) {
			return bitmap.getHeight();
		}
		// If the bitmap is taller than it is wide
		// use the width as the square crop dimension
		else {
			return bitmap.getWidth();
		}
	}
	
	
    public static int getCameraPhotoOrientation(Context context, String imagePath) {
        int rotate = 0;
        try {
            context.getContentResolver().notifyChange(Uri.fromFile(new File(imagePath)), null);

            ExifInterface exif = new ExifInterface(
            		imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            }



            Log.v("PhotoUtils", "Exif orientation: " + orientation);
        } catch (Exception e) {
        	Log.e("PhotoUtils", e.getMessage(), e);
        }
       return rotate;
    }
}
