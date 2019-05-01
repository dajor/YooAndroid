package com.fellow.yoo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.edmodo.cropper2.CropImageView;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.FileUtils;
import com.fellow.yoo.utils.PhotoUtils;
import com.fellow.yoo.utils.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class PostPictureActivity extends Activity {
	
	
	protected final static int REQUEST_CHANGE_PHOTO = 1000;
	protected static final int DEFAULT_ASPECT_RATIO_VALUES = 10;
	protected static final int ROTATE_NINETY_DEGREES = 90;
	protected static final String ASPECT_RATIO_X = "ASPECT_RATIO_X";
	protected static final String ASPECT_RATIO_Y = "ASPECT_RATIO_Y";
	protected static final int ON_TOUCH = 1;
	
	public static int RESULT_LOAD_IMAGE = 1;
	public static final int CAMERA_REQUEST = 1888;
	
	protected int rotateIdx = 1;

	// Instance variables
	protected int mAspectRatioX = DEFAULT_ASPECT_RATIO_VALUES;
	protected int mAspectRatioY = DEFAULT_ASPECT_RATIO_VALUES;

	private Bitmap selectedBitmap;
	protected Bitmap croppedImage;
	
	private Button btnCrop, btnBrowse, btnReset, btnRotate, btnPost;
	private ImageView postImage;
	
	private String mCurrentPhotoPath;
	private CropImageView cropImageView;
	private ImageView btnBack;
	private boolean firstCrop = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post_picture);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(getString(R.string.post_picture));  
		
		btnBrowse = (Button) findViewById(R.id.button_browse);
		btnReset = (Button) findViewById(R.id.button_reset);
		btnPost = (Button) findViewById(R.id.button_post);
		btnRotate = (Button) findViewById(R.id.button_rotate);
		btnCrop = (Button) findViewById(R.id.button_crop);
		btnBack = (ImageView) findViewById(R.id.btnBack);
		
		btnReset.setVisibility(View.VISIBLE); 
		
		cropImageView = (CropImageView) findViewById(R.id.cropImageView);
		postImage = (ImageView) findViewById(R.id.post_image);
		postImage.setBackgroundColor(Color.LTGRAY); 
		cropImageView.setBackgroundColor(Color.LTGRAY); 
		postImage.setScaleType(ScaleType.FIT_CENTER);
		// cropImageView.setScaleType(ScaleType.CENTER_INSIDE);
		// cropImageView.getImageView();
		// cropImageView.setVisibility(View.VISIBLE); 
		
		// cropImageView.setImageBitmap(selectedBitmap);
		// cropImageView.setImageResource(R.drawable.bg2); 
		// cropImageView.setVisibility(View.VISIBLE); 
		// btnBack.setVisibility(View.VISIBLE);
		
		cropImageView.setAspectRatio(5, 10);
		cropImageView.setFixedAspectRatio(true);
		cropImageView.setGuidelines(1);
		// cropImageView.setCropShape(CropImageView.CropShape.OVAL);
		// cropImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		
		
		int padding = ActivityUtils.dpToPixels(this, 10);
		int width = ActivityUtils.getScreenWidth(this);
		int height = ActivityUtils.getScreenHeight(this);
		
		postImage.setLayoutParams(new LinearLayout.LayoutParams(width - padding * 2, height/2));
		// cropImageView.getImageView().setLayoutParams(new LinearLayout.LayoutParams(width - padding * 2, height/2));
		cropImageView.setLayoutParams(new LinearLayout.LayoutParams(width - padding, height/2));
		

		// cropImageView.setMinimumWidth(width);
		// cropImageView.setImageResource(R.drawable.background_black); 
		// cropImageView.getCroppedImage();
		cropImageView.setMinimumHeight(width);
		
		
		cropImageView.setFixedAspectRatio(false);
		cropImageView.setAspectRatio(DEFAULT_ASPECT_RATIO_VALUES, DEFAULT_ASPECT_RATIO_VALUES);
		// Sets aspectRatioX
		// final TextView aspectRatioX = (TextView) findViewById(R.id.aspectRatioX);
		cropImageView.setGuidelines(ON_TOUCH);
		
		postImage.setOnClickListener(new OnClickListener() {
	        @Override
	        public void onClick(final View view) {
	        	if(selectedBitmap != null){ // click image to edit picture
	        		cropImageView.setVisibility(View.VISIBLE);
		        	btnCrop.setVisibility(View.VISIBLE); 
		        	btnRotate.setVisibility(View.VISIBLE); 
		        	postImage.setVisibility(View.GONE);
		        	btnBack.setVisibility(View.VISIBLE); 
		        	
		        	if(firstCrop){
		        		cropImageView.setImageBitmap(selectedBitmap); 
			        	cropImageView.refreshDrawableState();
			        	firstCrop = false;
		        	}
		        	
		        	
		        	enableButton(false); 
	        	}
	        }
	    });
		
		 btnBrowse.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
					selectPhoto();
				}
			});
		 
		
		 btnReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
				if(selectedBitmap != null){
					croppedImage = null;
					postImage.setImageBitmap(selectedBitmap); 
				}
			}
		});
		
		btnPost.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
				Bitmap bitmap = selectedBitmap; // ((BitmapDrawable) postImage.getDrawable()).getBitmap();
				if(croppedImage != null){
					bitmap = croppedImage;
				}
				if(bitmap != null){
					Intent returnIntent = new Intent();
					setResult(RESULT_OK, returnIntent);
					try {
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
						YooApplication.postImageByte = stream.toByteArray();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					finish();
				}
			}
		});
		
		btnRotate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
				cropImageView.rotateImage(ROTATE_NINETY_DEGREES);
			}
		});
		
		btnCrop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
				cropImageView.setVisibility(View.GONE);
	        	btnCrop.setVisibility(View.GONE); 
	        	btnRotate.setVisibility(View.GONE); 
	        	postImage.setVisibility(View.VISIBLE);
	        	
	        	croppedImage = cropImageView.getCroppedImage();
	        	postImage.setImageBitmap(croppedImage);
	        	
	        	enableButton(true); 
	        	
	        	btnBack.setVisibility(View.GONE); 
			}
		});
		
		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException {
				cropImageView.setVisibility(View.GONE);
	        	btnCrop.setVisibility(View.GONE); 
	        	btnRotate.setVisibility(View.GONE); 
	        	postImage.setVisibility(View.VISIBLE);
	        	enableButton(true); 
	        	btnBack.setVisibility(View.GONE); 
			}
		});
	}
	
	


	private void selectPhoto(){
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
	        File photoFile = null;
	        try {
	            photoFile = createImageFile();
	        } catch (IOException ex) {
	            Log.i("ChatFragment", "error while taking photo", ex);
	        }		    	
	        
	        if (photoFile != null) {
	        	requestPostPicture();
	        }
	    }
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == REQUEST_CHANGE_PHOTO && resultCode == Activity.RESULT_OK) {
			Uri uri  = null;
			if (data != null && StringUtils.isEmpty(mCurrentPhotoPath)) { 
				// when request get picture form local device
				uri = data.getData();
				if (uri != null && this != null) {
					mCurrentPhotoPath = PhotoUtils.getRealPathFromURI(this, uri);
				}
			} // else when capture new photo by camera
			
			if (!StringUtils.isEmpty(mCurrentPhotoPath) && this != null) {
				try {
					if(uri != null){
						selectedBitmap = FileUtils.loadSelectedPhoto(this, uri);
					}else{
						selectedBitmap = PhotoUtils.loadImageFromDevice(this, mCurrentPhotoPath);
					}
					
					if (selectedBitmap != null) {
						postImage.setImageBitmap(selectedBitmap); 
						cropImageView.setImageBitmap(selectedBitmap); 
						cropImageView.refreshDrawableState();
					} else {
						Toast.makeText(this, getResources().getString(R.string.unknown_image), Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					Log.w("PostPictureActivity", "Photo not found", e);
					Toast.makeText(this, getResources().getString(R.string.load_photo_failed), Toast.LENGTH_SHORT).show();
				}
			}
	    }
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	
	// private android.hardware.Camera mCameraDevice;
	private void requestPostPicture() {
		mCurrentPhotoPath = ""; // FEATURE_CAMERA_FRONT,
		PackageManager packageManager = this.getPackageManager();
		if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
				packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			
			final AlertDialog.Builder dia = new AlertDialog.Builder(this);
			ListView list = new ListView(this);
			List<String> options = Arrays.asList(getString(R.string.FROM_GALLERY), getString(R.string.TAKE_PHOTO));
			list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options));
			dia.setView(list);
			dia.setNegativeButton(getString(R.string.cancel), null);
			final Dialog newDia = dia.create();
			newDia.show();

			list.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View v, int pos, long arg3) {
					if (pos == 1) {
						mCurrentPhotoPath = getCapturePhotoPath();
					} else {
						pickImage();
					}
					newDia.dismiss();
				}
			});
		} else {
			pickImage();
		}
	}
	
	
	private void pickImage() {
		Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(Intent.createChooser(intent, ""), REQUEST_CHANGE_PHOTO);
	}
	

	private File createImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + "_";
	    File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	    File image = File.createTempFile(imageFileName, ".jpg", storageDir );

	    // Save a file: path for use with ACTION_VIEW intents
	    mCurrentPhotoPath = image.getAbsolutePath();
	    return image;
	}
	
	
	private String getCapturePhotoPath() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
			File photoFile = null;
			try {
				photoFile = PhotoUtils.createImageFile(this);
			} catch (IOException ex) {
				Log.i(ex.getClass().getName(), "error while taking photo", ex);
			}
			// Continue only if the File was successfully created
			if (photoFile != null) {
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
				startActivityForResult(takePictureIntent, REQUEST_CHANGE_PHOTO);
				return photoFile.getAbsolutePath();
			}
		}
		return null;
	}
	
	
	private void enableButton(boolean visible){
		/*btnBrowse.setEnabled(status);
		btnPost.setEnabled(status);
		btnReset.setEnabled(status);*/
		btnBrowse.setVisibility(visible? View.VISIBLE : View.GONE);
		btnPost.setVisibility(visible? View.VISIBLE : View.GONE);
		btnReset.setVisibility(visible? View.VISIBLE : View.GONE);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case android.R.id.home:
	        finish();
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// getMenuInflater().inflate(R.menu.record_message, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	
	
	
		/* public static void buttonPickMyPhotoClick(Activity activity){
			Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		activity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMAGE);
	}

	public static void buttonCapturePhoto(){
		// Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE); 
	    // startActivityForResult(cameraIntent, CAMERA_REQUEST); 
	}

	public static void loadCapturePhoto(Activity activity, Intent data, String title, String tags){
     	try {
     		Bitmap photo = (Bitmap) data.getExtras().get("data"); 
     		if(photo != null){
             	
     		} 
 		} catch (Exception e) {
 			Toast.makeText(activity, "Internal error", Toast.LENGTH_LONG).show();
 			Log.e(e.getClass().getName(), e.getMessage(), e);
 		}
     }

*/



	
}