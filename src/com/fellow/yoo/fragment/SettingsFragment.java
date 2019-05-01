package com.fellow.yoo.fragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fellow.yoo.MainActivity;
import com.fellow.yoo.R;
import com.fellow.yoo.RegisterActivity;
import com.fellow.yoo.YooActivity;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.adapter.SettingsAdapter;
import com.fellow.yoo.chat.ChatListener;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.PhotoUtils;
import com.fellow.yoo.utils.RecyclingImageView;
import com.fellow.yoo.utils.RoundImageView;
import com.fellow.yoo.utils.StringUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsFragment extends YooListFragment implements ChatListener{
	
	@Override
	public void actionClicked(int actionId) {
		if(actionId == R.id.btn_info){
			yooInfo();
		}
	}

	protected final static int REQUEST_CHANGE_PHOTO = 1000;
	
	private String mCurrentPhotoPath;
	private String nickname = "";
	private String errMessge = "";
	private String status = "";
	private YooUser yooUser;
	
	protected final static String[] backgrounds = new String[]{"Default", "Space", "Blue", "Grass", "Sand", "Snow" , "Lava" ,"Wood" , "Trees", "empty"};
	protected final static Integer[] backgroundIds = new Integer[]{ R.drawable.bg1_sm, 
			R.drawable.bg2_sm, R.drawable.bg3_sm, R.drawable.bg4_sm, R.drawable.bg5_sm, 
			R.drawable.bg6_sm , R.drawable.bg7_sm ,R.drawable.bg8_sm, R.drawable.bg9_sm };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		ChatTools.sharedInstance().addListener(this); 
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		((MainActivity) getActivity()).updateMenu();
				
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ChatTools.sharedInstance().reLogin(getActivity());
		
		((YooActivity) getActivity()).getBtnInfo().setVisibility(View.VISIBLE); 
		
		yooUser = YooApplication.getUserLogin();
		nickname = yooUser.getAlias();
		status = ChatTools.sharedInstance().getStatus();
		LayoutInflater li = LayoutInflater.from(getActivity());
			
		if(getListView().getHeaderViewsCount() <= 0){
			View header = li.inflate(R.layout.header_profile, getListView(), false);
			RoundImageView imageView = (RoundImageView)header.findViewById(android.R.id.icon);
			RelativeLayout hideLayout = (RelativeLayout) header.findViewById(R.id.hide_status_layout);
			hideLayout.setVisibility(View.VISIBLE); 
			if (yooUser != null && yooUser.getPicture() != null) {
				byte [] bitmapData = yooUser.getPicture();
				Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);			
				imageView.setImageBitmap(bitmap);
			} else {
				imageView.setImageResource(R.drawable.ic_user);
			}		
			TextView label = (TextView) header.findViewById(android.R.id.text1);
			label.setVisibility(View.GONE);	
			
			imageView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					changePhoto();
				}
			});
			
			Switch switchButton = (Switch) header.findViewById(R.id.switch_hide_status);
			switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			    	if(ActivityUtils.checkNetworkConnected(getActivity(), R.string.no_internet_connect)){
			    		ChatTools.sharedInstance().sendPrensenc(isChecked); 
				        SharedPreferences preferences = getActivity().getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
						SharedPreferences.Editor editor = preferences.edit();
						editor.putBoolean("hideStatus", isChecked);
						editor.commit();
			    	}else{
			    		buttonView.setChecked(!isChecked); 
			    	}
			    }
			});
			
			SharedPreferences preferences = getActivity().getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
			switchButton.setChecked(preferences.getBoolean("hideStatus", false)); 
			
			getListView().addHeaderView(header);
		}
		
		if(getListView().getFooterViewsCount() <= 0){
			
			RelativeLayout footer = new RelativeLayout(getActivity());
			footer.setGravity(Gravity.CENTER);
			Button resetBtn = new Button(getActivity());
			resetBtn.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
			resetBtn.setText(R.string.reset_all);
			resetBtn.setOnClickListener(new OnClickListener() {			
				@Override
				public void onClick(View v) {
					showResetDialog();				
				}
			});
			footer.addView(resetBtn);
			getListView().addFooterView(footer);
		}
		updateData();
		
	}
	
	
	
	private void updateData(){
		
		List<String> sections = Arrays.asList("User Information", "Chat Settings", "Statistics");
		Map<String, List<String []>> data = new HashMap<String, List<String []>>();
		data.put("User Information", 
				Arrays.asList( new String[] {"Profile Name", nickname}, new String[] {"Status", status}));
		List<String []> bg = new ArrayList<String []>();
		bg.add(new String [] {"Background", YooApplication.getBckGroundName()});
		data.put("Chat Settings", bg);
		StringBuilder messgges = new StringBuilder();
		messgges.append(new ChatDAO().countRecieved(yooUser) + " " + getString(R.string.states_received) + " / ");
		messgges.append(new ChatDAO().countSent(yooUser) + " " + getString(R.string.states_send));
		StringBuilder networks = new StringBuilder();
		/**send/receive message in bytes**/
		long messageSend = ChatTools.getSendReceivedBytes(true);
		long messageReceive = ChatTools.getSendReceivedBytes(false);
		networks.append(getDisplayBytes(messageReceive)).append(" " + getString(R.string.states_received) + " / ");
		networks.append(getDisplayBytes(messageSend)).append(" " + getString(R.string.states_send));
		/**finish block**/
		data.put("Statistics", Arrays.asList( new String[] {"Messages", messgges.toString()}, new String[] {"Networks", networks.toString()}));
 		
		// setListAdapter(new SettingsAdapter(sections, data));
		getListView().setAdapter(new SettingsAdapter(sections, data));
		
	}
	
	private String getDisplayBytes(long bytes){
		StringBuilder displayBytes = new StringBuilder();
		if(bytes > 1000000){
			bytes = bytes/1024;
			displayBytes.append(bytes).append(" Kbytes");
			if(bytes > 1000000){
				bytes = bytes/1024;
				displayBytes.append(bytes).append(" Mbytes");
			}
		}else{
			displayBytes.append(bytes).append(" bytes");
		}
		return displayBytes.toString();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == REQUEST_CHANGE_PHOTO && resultCode == Activity.RESULT_OK) {
			
			if (data != null && StringUtils.isEmpty(mCurrentPhotoPath)) { 
				// when request get picture form local device
				Uri uri = data.getData();
				if (uri != null && getActivity() != null) {
					mCurrentPhotoPath = PhotoUtils.getRealPathFromURI(getActivity(), uri);
				}
			} // else when capture new photo by camera
			
			if (!StringUtils.isEmpty(mCurrentPhotoPath) && getActivity() != null) {
				try {
					Bitmap bitmap = PhotoUtils.loadImageFromDevice(getActivity(), mCurrentPhotoPath);
					if (bitmap != null) {
						yooUser.setAlias(nickname); 
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
						yooUser.setPicture(stream.toByteArray());
						new UserDAO().upsert(yooUser);
						SharedPreferences preferences = getActivity().getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
						SharedPreferences.Editor editor = preferences.edit();
						editor.putString("nickname", nickname);
						editor.commit();
						
						ChatTools.sharedInstance().setNicknameAndPicture(nickname, yooUser.getPicture()); 
						
						((MainActivity) getActivity()).updateMenu();
						
						((MainActivity) getActivity()).reloadFragement(0);
						
					} else {
						Toast.makeText(getActivity(), getResources().getString(R.string.unknown_image), Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					Log.w("SettingsFragment", "Photo not found", e);
					Toast.makeText(getActivity(), getResources().getString(R.string.load_photo_failed), Toast.LENGTH_SHORT).show();
				}
			}
	    }
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onListItemClick(AdapterView<?> adapter, View v, int position, long id) {
		if(position == 0){
			changePhoto();
		}else if(position == 2){
			changeNickName();
		}else if(position == 5){
			selectBackground();
		}
		super.onListItemClick(adapter, v, position, id);
	}
	
	
	@Override
	public void onResume() {
		if(backgroundDialog != null && backgroundDialog.isShowing()){
			backgroundDialog.dismiss();
		}
		super.onResume();
	}

	public void yooInfo(){
		
		AlertDialog.Builder alertInfo = new AlertDialog.Builder(getActivity());
		alertInfo.setTitle(R.string.about);
		LayoutInflater li = LayoutInflater.from(getActivity());
		View infoLayout = li.inflate(R.layout.yoo_info, getListView(), false);
		RecyclingImageView imageView = (RecyclingImageView) infoLayout.findViewById(android.R.id.icon);
		imageView.setImageResource(R.drawable.logo);
		TextView label = (TextView) infoLayout.findViewById(android.R.id.text1);
		TextView label2 = (TextView) infoLayout.findViewById(R.id.text2);
		TextView label3 = (TextView) infoLayout.findViewById(R.id.text3);
		TextView label4 = (TextView) infoLayout.findViewById(R.id.text4);
		
		String verName = "";
		try {
			verName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
		} catch (Exception e) {
			Log.w("SettingFragment : VersionName >> ", e.toString());
		}
		
		label.setText(getString(R.string.yoo_info, verName)); 
		label2.setText(getString(R.string.yoo_info2, verName)); 
		ActivityUtils.setTextHtmlLink(getActivity(), label3, getString(R.string.yoo_info_link), "#0174DF"); 
		label4.setText(getString(R.string.yoo_info3)); 
		
		label3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = getString(R.string.yoo_info_link);
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(browserIntent);
			}
		});
		
		alertInfo.setView(infoLayout);
		
		alertInfo.setPositiveButton(R.string.contact_us, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int whichButton) {
				String email = "sales@fellow-consulting.de";
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("message/rfc822");
				i.putExtra(Intent.EXTRA_EMAIL  , new String[]{email});
				i.putExtra(Intent.EXTRA_SUBJECT, "Yoo"); 
				i.putExtra(Intent.EXTRA_TEXT   , "");
				try {
				    startActivity(Intent.createChooser(i, "Yoo"));
				} catch (android.content.ActivityNotFoundException ex) {
				    Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Log.e("SettingFragment : send invite by email >> ", e.toString());
				}
			}
		});
		
		alertInfo.setNegativeButton(R.string.action_close, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int whichButton) {
				
			}
		});
		
		alertInfo.show();
	}
	
	private AlertDialog backgroundDialog;
	@SuppressLint("InflateParams")
	private void selectBackground(){
		
		LayoutInflater inflator = getActivity().getLayoutInflater(); 
		ScrollView scrollView = (ScrollView) inflator.inflate(R.layout.popup_layout, null, false);
		
		backgroundDialog = new AlertDialog.Builder(getActivity()).create();
		backgroundDialog.setTitle(R.string.select_background);
		backgroundDialog.setView(scrollView);
		
		LinearLayout mainLayout = (LinearLayout) scrollView.findViewById(R.id.edit_layout);
		mainLayout.removeAllViews();
		int scWidth = ActivityUtils.getScreenWidth(getActivity());
		int padding = ActivityUtils.dpToPixels(getActivity(), 10);
		int imageSize = scWidth/2 - padding * 3;
		for (int i = 0; i <= 10; i++) { 
			final View separteLineH = new View(getActivity());
			final View separteLineV = new View(getActivity());
			separteLineH.setLayoutParams(new RelativeLayout.LayoutParams(padding/4, padding/4));
			separteLineV.setLayoutParams(new RelativeLayout.LayoutParams(padding/4, padding/4));
			mainLayout.addView(separteLineV);
			LinearLayout rowLayout = new LinearLayout(getActivity());
			rowLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)); 
			addImageView(rowLayout, i, padding, imageSize); // add left Image
			rowLayout.addView(separteLineH); // add separate.
			addImageView(rowLayout, i+1, padding, imageSize); // add right image
			
			mainLayout.addView(rowLayout);
			
			i++; // increase 1
		}
		
		/*alert.setPositiveButton(R.string.action_close, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int whichButton) {
				
			}
		});*/
		backgroundDialog.show();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		
		super.onSaveInstanceState(outState);
	}
	
	private void addImageView(LinearLayout rowLayout, final int i, int padding, int imageSize){
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageSize, imageSize);
		if(i < backgrounds.length){
			final String imageName = backgrounds[i];
			if(!StringUtils.isEmpty(imageName)){
				final RecyclingImageView imageView = new RecyclingImageView(getActivity());
				// imageView.setBackgroundResource(YooApplication.background.get(imageName)); 
				imageView.setPadding(padding/2, padding/2, padding/2, padding/2);
				imageView.setLayoutParams(params);
				imageView.setTag(imageName);
				if(!"empty".equals(imageName)){
					imageView.setBackgroundResource(backgroundIds[i]);  
					
					imageView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							String background = (String) v.getTag();
							YooApplication.setBckGround(background);
							((MainActivity) getActivity()).reloadFragement(0);
							backgroundDialog.dismiss();
						}
					});
				}
				
				
				
				rowLayout.addView(imageView);
			}
		}
		
	}
	
	
	private void changeNickName() {
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

		alert.setTitle(R.string.nickname_change);
		alert.setMessage(R.string.enter_nickname_name);

		// Set an EditText view to get user input 
		LinearLayout diLayout = new LinearLayout(getActivity());
		diLayout.setOrientation(LinearLayout.VERTICAL);
		final EditText input = new EditText(getActivity());
		input.setText("");  // move cursor to the end of text.
		input.append(nickname);
		diLayout.addView(input);
		alert.setView(diLayout);
		
		if(!StringUtils.isEmpty(errMessge)){
			final TextView txtMessage = new TextView(getActivity());
			txtMessage.setText(getString(R.string.nickname_length)); 
			txtMessage.setTextColor(Color.RED);
			txtMessage.setText(errMessge);
			int padding = ActivityUtils.dpToPixels(getActivity(), 10);
			txtMessage.setPadding(padding, padding, padding, padding);
			diLayout.addView(txtMessage); 
		}
		
		alert.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				nickname = input.getText().toString();
				if (nickname.length() > 2) {
					yooUser.setAlias(nickname); 
					new UserDAO().upsert(yooUser);
					ChatTools.sharedInstance().setNicknameAndPicture(nickname, yooUser.getPicture()); 
					((MainActivity) getActivity()).reloadFragement(0);
				}else{
					errMessge = getString(R.string.groupname_length);
					changeNickName();
				}
			}
		});

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				
			}
		});
		alert.show();
	}
	
	private void changePhoto(){
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
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
	
	// private android.hardware.Camera mCameraDevice;
	private void requestPostPicture() {
	
		/*try {
		  mCameraDevice = android.hardware.Camera.open();
		} catch (RuntimeException e) {
		  Log.e("SettingFragment : requestPostPicture", "fail to connect Camera", e);
		  // Throw exception
		}*/
		
		mCurrentPhotoPath = ""; // FEATURE_CAMERA_FRONT,
		PackageManager packageManager = getActivity().getPackageManager();
		if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
				packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
			
			final AlertDialog.Builder dia = new AlertDialog.Builder(getActivity());
			ListView list = new ListView(getActivity());
			List<String> options = Arrays.asList(getString(R.string.FROM_GALLERY), getString(R.string.TAKE_PHOTO));
			list.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, options));
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
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    File image = File.createTempFile(imageFileName, ".jpg", storageDir );

	    // Save a file: path for use with ACTION_VIEW intents
	    mCurrentPhotoPath = image.getAbsolutePath();
	    return image;
	}
	
	
	private String getCapturePhotoPath() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
			File photoFile = null;
			try {
				photoFile = PhotoUtils.createImageFile(getActivity());
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
	
	private void showResetDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

		alert.setTitle(R.string.reset_all);
		alert.setMessage(R.string.confirm_reset);


		alert.setPositiveButton(R.string.reset_all, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				resetAll();
			}
		});

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// do nothing
			}
		});

		alert.show();
	}
	
	private void resetAll() {
		SharedPreferences preferences = getActivity().getSharedPreferences("YooPreferences", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor = preferences.edit();

		editor.putString("login", "");
		editor.putString("password", "");
		editor.putString("nickname", "");
		editor.putString("countryCode", "");
		editor.putString("callingCode", "");
		editor.putString("phoneNumber", "");
		editor.putString("background", "Default");
	    editor.commit();
	    
	    SharedPreferences preferences_r = getActivity().getSharedPreferences("YooPreferences_Recent", Context.MODE_PRIVATE);  
		SharedPreferences.Editor editor_r = preferences_r.edit();
		editor_r.clear();
		editor_r.commit();
		
	    
	    new ChatDAO().purge();
	    new UserDAO().purge();
	    new GroupDAO().purge();
	    
	    Intent intent = new Intent(getActivity(), RegisterActivity.class);
	    getActivity().startActivity(intent);
	    getActivity().finish();
	    
	    ChatTools.sharedInstance().logout();
	}

	@Override
	public String getTitle(Resources resources) {
		return resources.getString(R.string.action_settings);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		view.setBackgroundColor(Color.WHITE); 
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void friendListChanged(List<YooUser> newFriends) {
	}

	@Override
	public void didReceiveMessage(YooMessage message) {
	}

	@Override
	public void didLogin(String login, String error) {
		Log.w("SettingFragment", " ######## refresh didLogin ######### ");
		if(StringUtils.isEmpty(error) || "REFRESH".equals(error)){
			Activity activity = getActivity();
			if(activity != null && activity instanceof YooActivity){
				YooActivity yooActivity = (YooActivity) activity;
				Fragment last = yooActivity.fragments.lastElement();
				if(last instanceof SettingsFragment){
					final FragmentTransaction ft = last.getFragmentManager().beginTransaction();
	    			ft.detach(last);
	    			ft.attach(last);
	    			ft.commit();
				}
			}
		}
	}

	@Override
	public void didReceiveRegistrationInfo(String user, String password) {
	}

	@Override
	public void didReceiveUserFromPhone(Map<String, String> info) {
	}

	@Override
	public void addressBookChanged() {
	}
	
	@Override
	public void onDestroy() {
		ChatTools.sharedInstance().removeListener(this); 
		super.onDestroy();
	}
	
}
