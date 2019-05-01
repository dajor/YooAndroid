package com.fellow.yoo.fragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import com.fellow.yoo.R;
import com.fellow.yoo.YooActivity;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.DateUtils;
import com.fellow.yoo.utils.StringUtils;
import com.imagezoom.ImageAttacher;
import com.imagezoom.ImageAttacher.OnMatrixChangedListener;
import com.imagezoom.ImageAttacher.OnPhotoTapListener;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ViewImageFragment extends YooFragment{
	
	
	private ImageView imageView;
	private String messageId;
	private Bitmap bitmap;
	
	public static ViewImageFragment newInstance(String messageId) {
		
		ViewImageFragment f = new ViewImageFragment();
	    Bundle args = new Bundle();
	    args.putString("messageId", messageId);
	    f.setArguments(args);
	    return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_view_image, container, false);
		
		ActivityUtils.hideSoftKeyboad(view); 
		messageId = getArguments().getString("messageId");
		((YooActivity) getActivity()).getBtnSave().setVisibility(View.VISIBLE); 
		
		YooMessage message = new ChatDAO().findById(messageId, true);
		
		imageView = (ImageView) view.findViewById(R.id.photo_zoom);
		imageView.setImageResource(R.drawable.bg2); 
		
		if(message != null && !message.getPictures().isEmpty()){
			bitmap = YooApplication.getBitmap(getActivity(), message.getIdent());
			if(bitmap == null){
				bitmap = BitmapFactory.decodeByteArray(message.getPictures().get(0), 0, message.getPictures().get(0).length);
				YooApplication.saveBitmap(getActivity(), message.getIdent(), bitmap);
			}
			imageView.setImageBitmap(bitmap); 
		}
		
		usingSimpleImage(imageView);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		view.setBackgroundColor(Color.WHITE); 
		super.onViewCreated(view, savedInstanceState);
	}
	
	@Override
	public void actionClicked(int actionId) {
		if(actionId == R.id.btn_save){
			if(bitmap != null){
				String title = DateUtils.formatDate(new Date()) + " " + messageId;
				String url = insertImage(getActivity().getContentResolver(), bitmap, title, "");
				if(!StringUtils.isEmpty(url)){
					ActivityUtils.displayToast(getString(R.string.image_saved), getActivity()); 
				}else{
					ActivityUtils.displayToast(getString(R.string.image_saved_fail), getActivity()); 
				}
			}
		}
	}
	

	@Override
	public String getTitle(Resources resources) {
		return resources.getString(R.string.picture);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}
	
	
	public void usingSimpleImage(ImageView imageView) {
        ImageAttacher mAttacher1 = new ImageAttacher(imageView);
        ImageAttacher.MAX_ZOOM = 3.0f; // Double the current Size
        ImageAttacher.MIN_ZOOM = 1.0f; // Half the current Size
        MatrixChangeListener mMaListener = new MatrixChangeListener();
        mAttacher1.setOnMatrixChangeListener(mMaListener);
        PhotoTapListener mPhotoTap = new PhotoTapListener();
        mAttacher1.setOnPhotoTapListener(mPhotoTap);
        
        
        if(bitmap != null){
        	int countBytes = bitmap.getByteCount()/1024;
        	if(countBytes < 1000){
            	// mAttacher1.setZoomable(false);
            }
        	System.out.println(" >>> bm.getWidth() >> : " + bitmap.getWidth());
        	System.out.println(" >>> bm.getHeight() >> : " + bitmap.getHeight());
        	System.out.println(" >>> bm.getByteCount() >> : " + countBytes);
        }
    }
	
	

    private class PhotoTapListener implements OnPhotoTapListener {

        @Override
        public void onPhotoTap(View view, float x, float y) {
        	//contestantPhotoZoomLayout.setVisibility(ImageView.GONE);
			//myPager.setVisibility(ScrollView.VISIBLE);
        }
        
        
    }

    private class MatrixChangeListener implements OnMatrixChangedListener {

        @Override
        public void onMatrixChanged(RectF rect) {

        }
    }
    
    
    
    
    
    public static final String insertImage(ContentResolver cr,  Bitmap source, 
            String title, 
            String description) {

        ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, title);
        values.put(Images.Media.DISPLAY_NAME, title);
        values.put(Images.Media.DESCRIPTION, description);
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        // Add the date meta data to ensure the image is added at the front of the gallery
        values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());

        Uri url = null;
        String stringUrl = null;    /* value to be returned */

        try {
            url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (source != null) {
                OutputStream imageOut = cr.openOutputStream(url);
                try {
                    source.compress(Bitmap.CompressFormat.JPEG, 100, imageOut);
                } finally {
                    imageOut.close();
                }

                long id = ContentUris.parseId(url);
                // Wait until MINI_KIND thumbnail is generated.
                Bitmap miniThumb = Images.Thumbnails.getThumbnail(cr, id, Images.Thumbnails.MINI_KIND, null);
                // This is for backward compatibility.
                storeThumbnail(cr, miniThumb, id, 50F, 50F,Images.Thumbnails.MICRO_KIND);
            } else {
                cr.delete(url, null, null);
                url = null;
            }
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
                url = null;
            }
        }

        if (url != null) {
            stringUrl = url.toString();
        }

        return stringUrl;
    }

   
    private static final Bitmap storeThumbnail(ContentResolver cr, Bitmap source,
            long id, float width, float height,int kind) {

        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0,
            source.getWidth(),
            source.getHeight(), matrix,
            true
        );

        ContentValues values = new ContentValues(4);
        values.put(Images.Thumbnails.KIND,kind);
        values.put(Images.Thumbnails.IMAGE_ID,(int)id);
        values.put(Images.Thumbnails.HEIGHT,thumb.getHeight());
        values.put(Images.Thumbnails.WIDTH,thumb.getWidth());

        Uri url = cr.insert(Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            thumbOut.close();
            return thumb;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }
    
    
    protected void createDirectoryAndSaveFile(Bitmap imageToSave, String fileName) {

        File direct = new File(Environment.getExternalStorageDirectory() + "/Yoo");
        String path = Environment.getExternalStorageDirectory().getPath() + "/Yoo/";
        if (!direct.exists()) {
            File wallpaperDirectory = new File(path);
            wallpaperDirectory.mkdirs();
        }

        File file = new File(new File(path), fileName);
        if (file.exists()) {
            // file.delete();
        	ActivityUtils.displayToast("Image aleady existed.", getActivity()); 
        	return;
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            ActivityUtils.displayToast("Image is saved", getActivity()); 
        } catch (Exception e) {
            e.printStackTrace();
        }
        ActivityUtils.displayToast("Save image failt", getActivity()); 
    }
    
    
    @Override
    public void onDestroy() { 
    	((YooActivity) getActivity()).getBtnSave().setVisibility(View.GONE); 
    	super.onDestroy();
    }
	
	
}
