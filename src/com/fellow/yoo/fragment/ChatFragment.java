package com.fellow.yoo.fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fellow.yoo.ContactGroupActivity;
import com.fellow.yoo.LocationActivity;
import com.fellow.yoo.PostPictureActivity;
import com.fellow.yoo.R;
import com.fellow.yoo.RecordingActivity;
import com.fellow.yoo.SelectContactActivity;
import com.fellow.yoo.YooActivity;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.chat.ChatListener;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.chat.Updatable;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.data.ContactDAO;
import com.fellow.yoo.data.ContactManager;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooLocation;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooMessage.CallStatus;
import com.fellow.yoo.model.YooMessage.YooMessageType;
import com.fellow.yoo.model.YooRecipient;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.CustomScrollView;
import com.fellow.yoo.utils.DateUtils;
import com.fellow.yoo.utils.RecyclingImageView;
import com.fellow.yoo.utils.RecyclingMapView;
import com.fellow.yoo.utils.StringUtils;
import com.fellow.yoo.utils.Triangle;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressLint("RtlHardcoded")
public class ChatFragment extends Fragment implements ChatListener, Updatable, PopupMenu.OnMenuItemClickListener {
	
	

	private EditText chatText;
	protected final static int REQUEST_POST_PHOTO = 1000;
	protected final static int REQUEST_POST_LOCATION = 2000;
	protected final static int REQUEST_SHARE_CONTACT = 3000;
	protected final static int REQUEST_POST_VOICE_MESSAGE = 4000;
	protected final static int REQUEST_MAKE_CALL = 5000;
	
	protected final static int EDIT_GROUP = 100;
	private List<String> messageIds; // for improve loading history chat.
	
	// private String mCurrentPhotoPath;
	private CustomScrollView chatScrollView;
	private MapView map;
	private int limitChat = 0;
	private boolean loadingOldChat = false;
	private boolean isManyImage = false;
	private boolean broadcast = false;
	private boolean makeCall = false;
	private Map<String, YooUser> members;
	public static ChatFragment newInstance(YooRecipient recipient, boolean brodcast) {
		return newInstance(recipient, brodcast, false);
	}
	
	public static ChatFragment newInstance(YooRecipient recipient, boolean brodcast, boolean makeCall) {
		ChatFragment f = new ChatFragment();
	    Bundle args = new Bundle();
	    args.putSerializable("recipient", recipient);
	    args.putBoolean("broadcast", brodcast);
	    args.putBoolean("makeCall", makeCall);
	    f.setArguments(args);
	    return f;
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ChatTools.sharedInstance().addListener(this);
		
		broadcast = getArguments().getBoolean("broadcast");
		makeCall = getArguments().getBoolean("makeCall", false);
		if(!broadcast){
			ChatTools.sharedInstance().markAsRead(getRecipient());
			members = new HashMap<String, YooUser>();
			YooRecipient recipient = getRecipient();
			if(recipient instanceof YooGroup){
				YooGroup group = ((YooGroup) recipient);
				for (String member : new GroupDAO().listMembers(group.toJID())) { 
					YooUser mUser = new UserDAO().findByJid(member);
					if(mUser != null){
						members.put(member, mUser);
					}
				}
			}
		}
	}
	
	private List<YooMessage> resendMessages;
	private void resendMessage() {
		for (YooMessage message : resendMessages) {
			if(message.getFrom() != null){
				if(message.getFrom().isMe() && !message.isSent()){
					// send(message);
					ChatTools.sharedInstance().sendMessage(message);
					System.out.println(" ## resendMessage >>> " + message.getIdent() + " --> " + message.getMessage()); 
				}
			}
		}
		resendMessages.clear();
	}
	
	
	private void sendTextMessage() {
	    String trimmed = chatText.getText().toString().trim();
	    if (trimmed.length() > 0) {
	        YooMessage yooMsg = buildNewMessage();
	        yooMsg.setType(YooMessageType.ymtText);
	        yooMsg.setMessage(trimmed);
	        send(yooMsg);
	        chatText.setText("");
	    }else{
	    	updateData();
	    }
	}

	private void send(YooMessage yooMsg) {

	    if (broadcast) {
	    	yooMsg.setRead(true);
	    	ChatTools.sharedInstance().addInHistory(yooMsg, true); // add broadcast history.
	    	for (YooUser user : YooApplication.broadcastUsers) { 
	    		yooMsg.setTo(user); 
				ChatTools.sharedInstance().sendMessage(yooMsg); 
			}
	    } else {
	        ChatTools.sharedInstance().sendMessage(yooMsg);
	    }
	        
	   
	    resetChatLimit();
	    updateData();
	    if(yooMsg.getTo() != null){
	    	YooApplication.addRecentChat(yooMsg.getTo().toJID()); 
	    }
	        
	}
	
	private YooMessage buildNewMessage() {
		YooRecipient recipient = (YooRecipient)getArguments().getSerializable("recipient");
	    YooMessage yooMsg = new YooMessage();
	    yooMsg.setTo(recipient);
	    
	    // TODO : set location
	    // yooMsg.location = [LocationTools sharedInstance].location.coordinate;
	    // TODO : set thread
	    // yooMsg.thread = self.thread;
	    return yooMsg;
	}
	
	
	
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		YooApplication.setLeaveApp(false); //  = false;
		switch (item.getItemId()) {
		case R.id.post_picture:
		    /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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
		    }*/
			
			openPostPicture();
		    
		    pauseMediaPlayer();
			return true;
		case R.id.post_location:
			Intent intent = new Intent(getActivity(), LocationActivity.class);
			startActivityForResult(intent, REQUEST_POST_LOCATION);
			pauseMediaPlayer();
			return true;
			
		case R.id.post_voice_message:
			intent = new Intent(getActivity(), RecordingActivity.class);
			startActivityForResult(intent, REQUEST_POST_VOICE_MESSAGE);
			pauseMediaPlayer();
			return true;	
			
		case R.id.post_contact:
			intent = new Intent(getActivity(), SelectContactActivity.class);
			intent.putExtra("type", ContactListFragment.Type.ShareContact); 
			startActivityForResult(intent, REQUEST_SHARE_CONTACT);
			pauseMediaPlayer();
			return true;
			
		/*case R.id.post_make_call:
			startCall();
			return true;	*/
			
		default:
			return false;
		}
	}
	
	public void startCall(){
		if(!YooApplication.hasCalling){
			pauseMediaPlayer();
			ChatTools.sharedInstance().makeCall(getRecipient());
			YooApplication.hasCalling = true;
			// YooApplication.callWaiting(ChatTools.CALL_MAX_DELAY, true); 
		}else{
			ActivityUtils.displayDiaMsg(getString(R.string.calling, getRecipient().toString()), 
					getString(R.string.two_calls), getActivity());
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if(requestCode == REQUEST_SHARE_CONTACT && resultCode == Activity.RESULT_OK){
			if(data != null){
				Contact contact = (Contact) data.getSerializableExtra("contact");
				if(contact != null){
					YooMessage message = buildNewMessage();
			        message.setType(YooMessageType.ymtContact);
			        Long contactId = Long.valueOf(contact.getContactId());
			        message.setShared(contactId.intValue());  
			        send(message);
				}
			}
		}else if(requestCode == REQUEST_POST_VOICE_MESSAGE && resultCode == Activity.RESULT_OK){
			if(data != null){
				String recordTime = data.getStringExtra("record_time");
				byte[] recordSound = data.getByteArrayExtra("record_sound");
				YooMessage message = buildNewMessage();
		        message.setType(YooMessageType.ymtSound);
		        message.setSound(recordSound);
		        message.setMessage(recordTime); 
		        send(message);
			}
		}else if(requestCode == REQUEST_POST_LOCATION && resultCode == Activity.RESULT_OK){
			if(data != null){
				YooLocation yLoc = (YooLocation) data.getSerializableExtra("location");
				YooMessage message = buildNewMessage();
		        message.setType(YooMessageType.ymtLocation);
		        message.setLocation(new double[]{yLoc.getLat(), yLoc.getLng()}); 
		        message.setMessage(yLoc.getName() + "\n" + yLoc.getAddress()); 
		        send(message);
			}
	        
		} else  if(requestCode == REQUEST_POST_PHOTO && resultCode == Activity.RESULT_OK) {
			
			if (YooApplication.postImageByte != null) {
				YooMessage message = buildNewMessage();
				message.setType(YooMessageType.ymtPicture);
				message.getPictures().add(YooApplication.postImageByte);
				send(message);
				
				YooApplication.postImageByte = null;
			} 
			
			
	    }
	}
	
	
	@Override
	public void onDestroy() {
		if(mediaPlayer != null && mediaPlayer.isPlaying()){
			mediaPlayer.stop();
		}
		
		// to release memory leaks
		Log.e("ChartFragment", " OnDestroy Set chat view null");
		chatScrollView = null;
		ChatTools.sharedInstance().removeListener(this);
		super.onDestroy();
		
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onDestroyView() {
		Log.d("ChatFragment", "onDestroyView");
        
        // If the View object still exists, delete references to avoid memory leaks
        if (chatScrollView != null) {
        	chatScrollView.setVisibility(View.GONE); 
        	chatScrollView.setOnClickListener(null);
            this.chatScrollView = null;
        }
		super.onDestroyView();
	}

	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_chat, container, false);
		chatText = (EditText)view.findViewById(R.id.chatedit);
		
		Button send = (Button) view.findViewById(R.id.chatsend);
		send.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sendTextMessage();
			}
		});
		chatText.requestFocus();
		
		ImageButton attach = (ImageButton) view.findViewById(R.id.chatpost);
		attach.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				PopupMenu popup = new PopupMenu(getActivity(), v);
				MenuInflater inflater = popup.getMenuInflater();
				inflater.inflate(R.menu.post, popup.getMenu());
				
				/*if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
					popup.getMenu().removeItem(R.id.post_picture);
				}*/
				popup.setOnMenuItemClickListener(ChatFragment.this);
				popup.show();
			}
		});
		
		//view.setBackgroundColor(Color.WHITE); 
		view.setBackgroundResource(YooApplication.getBckGroundSource());
		
		return view;
	}
	
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		resetChatLimit();
		updateData();
		// ActivityUtils.hideSoftKeyboad(view);
		if(makeCall){
			makeCall = false;
			startCall();
		}
	}
	
	
	@Override
	public void onResume() {
		YooApplication.setLeaveApp(true);
		// ActivityUtils.hideSoftKeyboad(chatScrollView);
		resetChatLimit();
		((YooActivity) getActivity()).updateChatViewBar(getRecipient());
		
		if(!broadcast){
			YooRecipient recipient = getRecipient();
			if(recipient instanceof YooUser){
				((YooActivity) getActivity()).refreshSubTitle(getRecipient()); 
				boolean  online = ChatTools.sharedInstance().isVisibility((YooUser)recipient);
				if(!online){
					ChatTools.sharedInstance().requestLastOneline(recipient.toJID()); 
				}
			}else if(recipient instanceof YooGroup){
				((YooActivity) getActivity()).refreshSubTitle(getRecipient()); 
			}
		}
		
		super.onResume();
	}
	
	
	@SuppressWarnings("deprecation")
	public void updateData() {
		final YooRecipient recipient = (YooRecipient) getArguments().getSerializable("recipient");
		// YooUser loginUser = YooApplication.getUserLogin();
		if(getView() == null){
			return;
		}
		
		 if(resendMessages == null){
			 resendMessages = new ArrayList<YooMessage>();
		 }
		
		int padding = ActivityUtils.dpToPixels(getActivity(), 8);
		chatScrollView = (CustomScrollView) getView().findViewById(R.id.chatview);
		LinearLayout vlayout;
		if(messageIds.isEmpty()){
			chatScrollView.removeAllViews();
			chatScrollView.getViewTreeObserver().addOnScrollChangedListener(new OnScrollChangedListener() {

			    @Override
			    public void onScrollChanged() {
			    	if(chatScrollView != null){
			    		Activity activity = getActivity();
						if (activity != null) {
							activity.runOnUiThread(new Runnable() {
						        @Override
						        public void run() {
						        	int scrollY = chatScrollView.getScrollY(); 
							        if(scrollY < 0  && loadingOldChat == false){ 
							        	loadingOldChat = true;
							        	chatScrollView.setScrollY(10); 
							        	int count = new ChatDAO().count(recipient);
							        	if(count >= limitChat){
							        		chatScrollView.setScrollToBottom(false); 
							        		if(isManyImage){
							        			limitChat += ChatTools.MAX_USER_HISTORY/2;
							        		}else{
							        			limitChat += ChatTools.MAX_USER_HISTORY;
							        		}
							        		updateData();
							        	}
							        	Log.w("ChatFragment : Load more chat ", " >> " + limitChat);
							        }
						        }
							});
						}	
			    	}
			    }
			});
			
			// chatScrollView.setBackgroundResource(YooApplication.getBckGroundSource());
			
			vlayout = new LinearLayout(getActivity());
			vlayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			vlayout.setOrientation(LinearLayout.VERTICAL);
			chatScrollView.addView(vlayout);
		}else{
			vlayout = (LinearLayout) chatScrollView.getChildAt(0);
		}
		
		
		boolean first = true;
		String currentDate = null;
		List<YooMessage> messages = null;
		if(broadcast){
			messages = ChatTools.sharedInstance().messagesForBroadcast(true, limitChat);
		}else{
			messages = ChatTools.sharedInstance().messagesForRecipient(recipient, true, limitChat);
		}
		Collections.reverse(messages);
		
		DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(getActivity());
		DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
		
		// for (int i = 0; i < messages.size(); i++) {
		// YooMessage message = messages.get(i);
		for (YooMessage message : messages) {
			if(messageIds.contains(message.getIdent())){
				continue;
			}
			// messageIds.add(message.getIdent());
			// resendMessage(message); // check to re send message.
			if(!message.isSent() && message.getFrom().isMe()){
				resendMessages.add(message);
			}
			
			String callName = "";
			String callPhone = "";
			boolean greenCheck = message.isAck() && message.isSent() && message.isReadByOther() ? true : false;
			
			String msgDate = dateFormat.format(message.getDate());
			if (currentDate == null || !currentDate.equals(msgDate)) {
				// Add date header
				currentDate = msgDate;
				RelativeLayout row = new RelativeLayout(getActivity());
				RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				row.setLayoutParams(params1);
				row.setGravity(Gravity.CENTER_HORIZONTAL);
				row.setPadding(padding, first ? padding : 0, padding, padding);
				
				TextView header = new TextView(getActivity());
				RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				header.setLayoutParams(params2);
				header.setPadding(padding, padding/2, padding, padding/2);
				header.setBackgroundResource(R.drawable.date_header);
				header.setTextColor(getResources().getColor(android.R.color.white));
				header.setText(currentDate);
				header.setTextSize(12);
				
				row.addView(header);
				vlayout.addView(row);
				first = false;
			}
			
			RelativeLayout row = new RelativeLayout(getActivity());
			row.setTag(message.getIdent());
			
			RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			row.setLayoutParams(params1);
			row.setPadding(padding, first ? padding : 0, padding, padding);

			int color = !message.getFrom().isMe() ? android.R.color.white : R.color.lightyellow;
			int bubble_color = !message.getFrom().isMe() ? R.drawable.bubble : R.drawable.bubble_yellow;
			if (message.getType() == YooMessageType.ymtContact) { 
				color = R.color.share_contact;
				bubble_color = R.drawable.bubble_share;
			}else if (message.getType() == YooMessageType.ymtSound || message.getType() == YooMessageType.ymtCallRequest) { 
				color = R.color.voice_message;
				bubble_color = R.drawable.bubble_sound;
			}
			
			Triangle triangle = new Triangle(getActivity());
			triangle.setId(1000); // need to set ID, else LEFT_OF/RIGHT_OF will not work.
			RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(padding, padding);
			params3.setMargins(0, message.getType().equals(YooMessageType.ymtText)?padding : padding*2, 0, 0);
			
			if (message.getFrom().isMe()) {
				params3.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			} else {
				params3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				triangle.setToLeft(true);
			}	
			
			triangle.setColor(getResources().getColor(color));
			triangle.setLayoutParams(params3);
			
			RelativeLayout bubble = new RelativeLayout(getActivity());
			RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			if (message.getFrom().isMe()) {
				params2.addRule(RelativeLayout.LEFT_OF, triangle.getId());
			} else {
				params2.addRule(RelativeLayout.RIGHT_OF, triangle.getId());
			}
			bubble.setLayoutParams(params2);
			bubble.setBackgroundResource(bubble_color);
			
			boolean isGroupChat = message.getFrom() instanceof YooGroup;
			boolean longMessage = message.getMessage() == null || message.getMessage().length() > 10;
			int lineP = message.getMessage() == null ? 0 : message.getMessage().length();
			int screenWidth = ActivityUtils.getScreenWidth(getActivity());
			int screenHeight = ActivityUtils.getScreenHeight(getActivity());
			if(screenWidth > screenHeight){
				screenWidth = screenHeight;
			}
			View main = null;
			
			if (message.getType() == YooMessageType.ymtText) { 
				TextView text = new TextView(getActivity());
				text.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
				text.setPadding(padding, padding/2, longMessage ? padding : padding/2, longMessage ? 0 : padding/2);
				text.setText(message.getMessage());		
				text.setTextSize(16);
				bubble.addView(text);
				main = text;
				if("Test 12".equals(message.getMessage())){
					System.out.println("check flag status .... "); 
				}
			} else if (message.getType() == YooMessageType.ymtContact) {
				
				LinearLayout layout = new LinearLayout(getActivity());
				params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
				params2 = new RelativeLayout.LayoutParams(padding*3, padding*3);
	
				params1.setMargins(0, 0, 0, 0); 
				params2.setMargins(0, padding, 0, 0);
				
				TextView text = new TextView(getActivity());
				layout.setPadding(padding, 0, 0, 0);
				text.setGravity(Gravity.CENTER_VERTICAL); 
				text.setPadding(padding, padding/2, padding, 0);
				text.setTextSize(16);
				
				ImageView image = new RecyclingImageView(getActivity());
				image.setImageResource(R.drawable.arrow_forward_thin); 
				image.setAlpha(90);
				layout.setLayoutParams(params1); 
				image.setLayoutParams(params2);
				
				layout.addView(text);
				layout.addView(image);
				bubble.addView(layout); 
				main = layout;
				longMessage = true;
				layout.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); 
				
				/*Contact contact = new ContactDAO().find(message.getShared());
	            if (contact == null) {
	                contact = new ContactManager().findById(message.getShared());
	            }*/
				
				Contact contact = YooApplication.getContact(Long.valueOf(message.getShared()));
	            // Log.w("ChatFragment >> share contact Id : ", "" + message.getShared());  
	            
	            StringBuffer shareCon = new StringBuffer(getActivity().getString(R.string.share_contact) + " : ");
	            shareCon.append(contact != null ? contact.toString() : "");
	            text.setText(shareCon.toString());
	            lineP = shareCon.length();
				layout.setTag(message.getShared());
				layout.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						openDetailContact((Integer)v.getTag()); 
					}
				});
				
			} else if (message.getType() == YooMessageType.ymtSound) {
				LinearLayout voidLayout = new LinearLayout(getActivity());
				voidLayout.setGravity(Gravity.CENTER_HORIZONTAL); 
				params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
				
				params2.setMargins(padding, padding, padding, padding); 
				voidLayout.setPadding(padding, padding, padding, padding); 
				voidLayout.setLayoutParams(params2); 
				
				ImageView image = new RecyclingImageView(getActivity());
				image.setImageResource(R.drawable.audio_file_64); 
				
				voidLayout.addView(image);
				TextView text = new TextView(getActivity());
				voidLayout.addView(text);
				voidLayout.setPadding(0, 0, 0, padding);
				text.setPadding(padding, padding, padding, 0);
				text.setText(message.getMessage()); 
				text.setSingleLine(true); 
				image.setTag(message); 
				lineP = 20;
				bubble.addView(voidLayout); 
				main = voidLayout;
				
				addPlaySoundListener(voidLayout); 
				
			} else if (message.getType() == YooMessageType.ymtPicture) {
				if (message.getPictures().size() > 0) {
					ImageView image = new RecyclingImageView(getActivity());
					int wPadding = isGroupChat? padding * 8 : padding * 2;
					
					Bitmap bitmap = YooApplication.getBitmap(getActivity(), message.getIdent());
					if(bitmap == null){
						bitmap = BitmapFactory.decodeByteArray(message.getPictures().get(0), 0, message.getPictures().get(0).length);
						YooApplication.saveBitmap(getActivity(), message.getIdent(), bitmap);
					}
					
					int imageWidth = screenWidth - wPadding;
					RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(imageWidth, // getImageHeight(bitmap, screenWidth - mHeight));
					RelativeLayout.LayoutParams.WRAP_CONTENT);
					if(bitmap.getWidth() < imageWidth){
						imageParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
						params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
						image.setScaleType(ScaleType.CENTER_INSIDE);
						 lineP = bitmap.getWidth();
					}else{
						params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);						
						image.setScaleType(ScaleType.FIT_XY);
						lineP = imageWidth; 
					}
					params2.setMargins(0, padding, 0, padding);
					if (message.getFrom().isMe()) {
						params2.addRule(RelativeLayout.LEFT_OF, triangle.getId());
					} else {
						params2.addRule(RelativeLayout.RIGHT_OF, triangle.getId());
					}
					imageParams.setMargins(isGroupChat? 0 : padding, padding, padding, 0); 
					image.setLayoutParams(imageParams);
					image.setImageBitmap(bitmap);
					
					bubble.setLayoutParams(params2);
					bubble.addView(image);
					main = image;
					longMessage = true;
					
					
					// click to view and save image
					final String messageId = message.getYooId();
					image.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							openImageView(messageId);
						}
					});
				}
			}else if (message.getType() == YooMessageType.ymtLocation) {
				String mapKey = "map_" + message.getLocation()[0] + "_" + message.getLocation()[1]; 
				Bitmap bitmap = YooApplication.getBitmap(getActivity(), mapKey);
				int mHeight = isGroupChat? padding * 6 : padding * 2;
				int imageWidth = screenWidth - mHeight;
				lineP = imageWidth; 
				if(bitmap != null){
					ImageView image = new RecyclingImageView(getActivity());
					RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(screenWidth - mHeight, RelativeLayout.LayoutParams.WRAP_CONTENT);
					imageParams.setMargins(isGroupChat? 0 : padding, padding, padding, 0); 
					image.setLayoutParams(imageParams);
					image.setScaleType(ScaleType.FIT_XY);
					if(bitmap != null){
						image.setImageBitmap(bitmap);
					}
					
					// params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
					params2 = new RelativeLayout.LayoutParams(screenWidth - mHeight, RelativeLayout.LayoutParams.WRAP_CONTENT);
					params2.setMargins(padding, padding, padding, padding);
					if (message.getFrom().isMe()) {
						params2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					}
					bubble.setLayoutParams(params2);
					bubble.addView(image);
					main = image;
				}else{
					map = YooApplication.getMap(getActivity());
					RelativeLayout mapLayout = new RelativeLayout(getActivity());
					mHeight = isGroupChat? padding * 7 : padding * 4;
					//params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
					//params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, screenWidth - mHeight);
					//params3 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
					
					params1 = new RelativeLayout.LayoutParams(screenWidth - mHeight, RelativeLayout.LayoutParams.MATCH_PARENT);
					params2 = new RelativeLayout.LayoutParams(screenWidth - mHeight, screenWidth - mHeight);
					params3 = new RelativeLayout.LayoutParams(screenWidth - mHeight, RelativeLayout.LayoutParams.MATCH_PARENT);
					if (message.getFrom().isMe()) {
						params2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						params3.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					}
					params2.setMargins(0, 0, 0, 0); 
					params1.setMargins(padding, padding, padding, padding*3/2); 
					params3.setMargins(padding, padding, padding, padding); 

					mapLayout.setLayoutParams(params2); 
					bubble.setLayoutParams(params3);
					
					final YooMessage messageF = message;
					if(map != null && map.getMap() != null){ 
						map.getMap().setOnMapLoadedCallback(new OnMapLoadedCallback(){
					        @Override
					        public void onMapLoaded() {
					            captureMapScreen(map, messageF); 
					        }
					    });
									
						map.setLayoutParams(params1);
						mapLayout.addView(map); 
						bubble.addView(mapLayout); 
						main = mapLayout;
						addMapMarker(map, message);
						((RelativeLayout) main).setGravity(Gravity.BOTTOM);
						
					}else{ // device not support google play service.
						TextView tv = new TextView(getActivity());
						tv.setPadding(padding, padding, padding, padding);
						tv.setText(R.string.install_google_play_service); 
						mapLayout.addView(tv); 
						bubble.addView(mapLayout); 
						main = mapLayout;
						((RelativeLayout) main).setGravity(Gravity.BOTTOM);
					}
				}
			}else if (message.getType() == YooMessageType.ymtCallRequest) {
				
				
				LinearLayout layout = new LinearLayout(getActivity());
				LinearLayout layoutText = new LinearLayout(getActivity());
				LinearLayout layoutButton = new LinearLayout(getActivity());
				params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
				
				TextView text = new TextView(getActivity());
				text.setGravity(Gravity.CENTER_VERTICAL); 
				text.setPadding(padding/2, padding, 0, padding);
				text.setGravity(Gravity.CENTER); 
				text.setTextSize(16);
				
				layout.setLayoutParams(params1); 
				layoutText.setPadding(padding, 0, 0, 0);
				layoutText.setGravity(Gravity.TOP); 
				layoutButton.setGravity(Gravity.CENTER_HORIZONTAL); 
				layout.setOrientation(LinearLayout.VERTICAL);
				layoutText.addView(text); 
				layout.addView(layoutText); 
				layout.addView(layoutButton); 
				
				main = layout;
				bubble.addView(layout); 
				longMessage = true;
				layout.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); 
				
				boolean isMe = message.getFrom().isMe();
				// Log.w("ChatFragment >> call Request : ", "" + message.getConferenceNumber());  
				if(message.getTo() instanceof YooGroup && isMe){
					callName = getRecipient().toString();
				}else{
					callName = getRecipient().toString();
					YooUser callingUser = getCallingUser(message);
					if(callingUser != null){
						if(callingUser.getContactId() > 0){
							Contact contact = new  ContactDAO().find(callingUser.getContactId());
							contact = new ContactManager().getContactPhone(contact);
							if(contact != null && contact.getPhones().size() > 0){
								callPhone = contact.getPhones().get(0).getValue();
							}else{
								callPhone = "";
							}
						}
						callName = callingUser.toString();
					}
				}
				
				/*if(message.getFrom() instanceof YooGroup){
					YooGroup yGroup = (YooGroup) message.getFrom();
					YooUser user = new UserDAO().findByJid(yGroup.getMember() + "@" + ChatTools.YOO_DOMAIN);
					if(user != null){
						
					}
				}*/

				long seconds = DateUtils.substructTimeAsSeconds(message.getDate());
			    if((seconds > ChatTools.CALL_MAX_DELAY && message.getCallStatus() != CallStatus.csAccepted)
						|| message.getCallStatus() == CallStatus.csRejected 
						|| message.getCallStatus() == CallStatus.csCancelled ){
					layoutText.addView(initCallStatusLayout(isMe, padding, R.drawable.call_red));   
					greenCheck = false;
				}else if(message.getCallStatus() == CallStatus.csAccepted){
					layoutText.addView(initCallStatusLayout(isMe, padding, R.drawable.call_green));   
				}else{
					layoutText.addView(initCallStatusLayout(isMe, padding, -1));   
					// cancel button
					if(seconds < ChatTools.CALL_MAX_DELAY){
						if(isMe){
							layoutButton.addView(initCancelButton(callName, callPhone, padding));
							YooApplication.hasCalling = true;
							YooApplication.callWaiting(ChatTools.CALL_MAX_DELAY - seconds, getActivity()); 
						}else {
							YooApplication.callingPhoneAlert(getActivity(), callName, callPhone, message.getDate()); 
						}
					}else{
						new ChatDAO().updateCallStatus(message.getIdent(), CallStatus.csCancelled);
					}
					greenCheck = false;
				}
			    
			    YooApplication.callReqId = message.getIdent();
			    if(isMe){
			    	text.setText(getActivity().getString(R.string.calling, callName));
				}else {
			    	text.setText(getActivity().getString(R.string.call_from, callName, callPhone));
			    	text.setPadding(0, padding, 0, padding);
				}
			    lineP = lineP + 20;
			} else if (message.getType() == YooMessageType.ymtCallStatus) {
				continue;
			}
			
			LinearLayout timePanel = new LinearLayout(getActivity());
			timePanel.setOrientation(LinearLayout.HORIZONTAL);
			timePanel.setPadding(0, 0, padding, padding/2);
			RelativeLayout.LayoutParams paramsPanel = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			if (main != null) {
				main.setId(2000);
				if (longMessage) {
					paramsPanel.addRule(RelativeLayout.BELOW, main.getId());
					paramsPanel.addRule(RelativeLayout.ALIGN_RIGHT, main.getId());
				} else {
					paramsPanel.addRule(RelativeLayout.RIGHT_OF, main.getId());
					paramsPanel.addRule(RelativeLayout.ALIGN_BOTTOM, main.getId());
				}
			}
			timePanel.setLayoutParams(paramsPanel);
			 
			TextView timeTxt = new TextView(getActivity());
			timeTxt.setTextSize(11);
			timeTxt.setTextColor(getResources().getColor(android.R.color.darker_gray));
			timeTxt.setSingleLine(true);
			timeTxt.setText(timeFormat.format(message.getDate()));			
			timePanel.addView(timeTxt);
	
			if (message.getFrom().isMe()) {
				int checkIcon = greenCheck ? R.drawable.green_checkmark : R.drawable.check_mark_gray_64;
				if(message.getCallStatus() == CallStatus.csRejected){
					checkIcon = R.drawable.checkmark_red;
				}
				if (message.isSent()) {
					LinearLayout.LayoutParams imgLayout = new LinearLayout.LayoutParams(padding, padding);
					imgLayout.gravity = Gravity.CENTER_VERTICAL;
					imgLayout.setMargins(padding/3, 0, 0, 0);
					ImageView checkImg = new ImageView(getActivity());
					checkImg.setLayoutParams(imgLayout);
					checkImg.setImageResource(checkIcon);
					timePanel.addView(checkImg);
				}
				
				if (message.isAck()) {
					LinearLayout.LayoutParams imgLayout = new LinearLayout.LayoutParams(padding, padding);
					imgLayout.gravity = Gravity.CENTER_VERTICAL;					
					ImageView checkImg2 = new ImageView(getActivity());
					checkImg2.setLayoutParams(imgLayout);
					checkImg2.setImageResource(checkIcon);
					timePanel.addView(checkImg2);
				}
				
				if (message.isAck() && message.getType() == YooMessageType.ymtCallRequest 
						&& (message.getCallStatus() == CallStatus.csAccepted || message.getCallStatus() == CallStatus.csRejected
								|| message.getCallStatus() == CallStatus.csCancelled || message.isSent())) { 
					LinearLayout.LayoutParams imgLayout = new LinearLayout.LayoutParams(padding, padding);
					imgLayout.gravity = Gravity.CENTER_VERTICAL;					
					ImageView checkImg3 = new ImageView(getActivity());
					checkImg3.setLayoutParams(imgLayout);
					checkImg3.setImageResource(checkIcon);
					timePanel.addView(checkImg3);
				}
				
			}
			
			if (main instanceof RecyclingMapView) {
				((RelativeLayout) main).addView(timePanel);
				((RelativeLayout)main).bringChildToFront(timePanel); 
				timePanel.setBackgroundColor(getResources().getColor(R.color.transparent));  
				paramsPanel.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, main.getId());
				paramsPanel.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, main.getId());
			}else{
				bubble.addView(timePanel);
				bubble.bringChildToFront(timePanel); 
			}
			
			
			// add group members icon and alias
			if(message.getFrom() instanceof YooGroup && !(message.getFrom().isMe()) ){
				
				boolean image = message.getType() == YooMessageType.ymtPicture || message.getType() == YooMessageType.ymtLocation;
				
				RecyclingImageView groupIcon = new RecyclingImageView(getActivity());
				groupIcon.setImageResource(R.drawable.ic_user);
				groupIcon.setLayoutParams(new RelativeLayout.LayoutParams(padding*5, padding*5)); 
				
				RelativeLayout layoutTitle = new RelativeLayout(getActivity());
				layoutTitle.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				layoutTitle.setBackgroundResource(color);
				TextView tvTtitle = new TextView(getActivity());
				tvTtitle.setPadding(padding, padding, padding, padding); 
				layoutTitle.addView(tvTtitle);
				
				View line = new View(getActivity());
				line.setBackgroundColor(Color.LTGRAY); 
				
				YooGroup sender = (YooGroup) message.getFrom();
				YooUser mUser = members.get(sender.getMember() + "@" + ChatTools.YOO_DOMAIN);
				String memberName = sender.getMember();
				if(mUser != null){
					memberName = mUser.getAlias();
					if(mUser.getPicture() != null){
						Bitmap bitmap = BitmapFactory.decodeByteArray(mUser.getPicture(), 0, mUser.getPicture().length);
						groupIcon.setImageBitmap(bitmap); 
					}
				}
				
				RelativeLayout.LayoutParams bParams = (RelativeLayout.LayoutParams) bubble.getLayoutParams();
				RelativeLayout.LayoutParams tParams = (RelativeLayout.LayoutParams) triangle.getLayoutParams();
				
				bParams.setMargins(image? padding : 0, padding * 5, 0, 0);
				tParams.setMargins(padding*5, padding*2, 0, padding);
				tvTtitle.setText(memberName);
				if(memberName.length() > lineP){
					lineP = memberName.length();
				}
				if(!image){
					lineP = padding * lineP;
				}
				RelativeLayout.LayoutParams lParams = new RelativeLayout.LayoutParams(lineP, padding / 8);
				lParams.setMargins(padding, padding*4, padding, padding); 
				line.setLayoutParams(lParams); 
				line.setAlpha(70); 
				
				RelativeLayout.LayoutParams lTParams = (RelativeLayout.LayoutParams) layoutTitle.getLayoutParams();
				lTParams.setMargins(padding*6, padding, 0, 0);
				
				bubble.setBackgroundResource(bubble_color);
				layoutTitle.setBackgroundResource(bubble_color);
				layoutTitle.addView(bubble); 
				layoutTitle.addView(line); 
				row.addView(layoutTitle);
				row.addView(groupIcon);
			}else{
				row.addView(bubble);
			}
			
			row.addView(triangle);
			vlayout.addView(row);
			
			/*if(loadingOldChat){
				vlayout.addView(row, 0);
			}else{
				vlayout.addView(row);
			}*/
		} 
		
		if(loadingOldChat){
			loadingOldChat = false;
			chatScrollView.pageScroll(ScrollView.FOCUS_UP);
		}
		
		resendMessage();
	} // end update chats
	
	private YooUser getCallingUser(YooMessage message){
		YooUser callingUser = null;
		if(message.getFrom() instanceof YooGroup){
			YooGroup sender = (YooGroup) message.getFrom();
			callingUser = members.get(sender.getMember() + "@" + ChatTools.YOO_DOMAIN);
		}else{
			if(!message.getFrom().isMe()){
				callingUser = new UserDAO().findByJid(message.getFrom().toJID());
			}else {
				callingUser = new UserDAO().findByJid(message.getTo().toJID());
			}
		}
		return callingUser;
	}
	
	private TextView initCancelButton(final String name, final String phone, int padding){
		TextView btnCancel = new TextView(getActivity());
		btnCancel.setPadding(padding*4, padding/2, padding*4, padding/2);
		btnCancel.setBackgroundColor(Color.GRAY); 
		btnCancel.setTextSize(18);
		btnCancel.setText(R.string.cancel); 
		btnCancel.setGravity(Gravity.CENTER); 
		btnCancel.setTextColor(Color.WHITE);  
		btnCancel.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ChatTools.sharedInstance().cancelCall(YooApplication.callReqId);
				YooApplication.stopCall();
				updateData();
			}
		});
		return btnCancel;
	}
	
	
	@SuppressLint("InflateParams")
	private LinearLayout initCallStatusLayout(boolean isMe, int padding, int icon){
		LayoutInflater li = LayoutInflater.from(getActivity());
		LinearLayout statusLayout = (LinearLayout) li.inflate(R.layout.call_progressbar, null, false);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
		int ltPadding = isMe ? padding : 0;
		statusLayout.setPadding(ltPadding, padding, ltPadding, 0); 
		if(icon > 0){
			LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(padding*3, padding*3);
			params2.setMargins(0, 0, padding, 0);
			ImageView image = new RecyclingImageView(getActivity());
			image.setImageResource(icon); 
			
			statusLayout.setGravity(Gravity.TOP); 
			statusLayout.setLayoutParams(params);
			image.setLayoutParams(params2);
			statusLayout.addView(image); 
		}else{
			ProgressBar progressBar = (ProgressBar) statusLayout.findViewById(R.id.progress_bar); 
			progressBar.setVisibility(View.VISIBLE); 
		}
		return statusLayout; 
	}
	
	
	
	private MediaPlayer mediaPlayer;
	//private Timer timer;
	//private TextView textRecord;
	private void addPlaySoundListener(LinearLayout voiceLayout){
		
		ImageView image = (ImageView) voiceLayout.getChildAt(0);
		
		image.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				YooMessage yoMsg =  (YooMessage) v.getTag();
				// LinearLayout voiceLayout = (LinearLayout) v.getParent();
				// textRecord = ((TextView) voiceLayout.getChildAt(1));
				
				if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
					if(yoMsg.getSound() != null) {
						String outputFile = getSoundAudioSource(yoMsg.getSound()); // yoMsg.getMessage();
						try {
							mediaPlayer = new MediaPlayer();
							mediaPlayer.setDataSource(outputFile);
							mediaPlayer.prepare();
							mediaPlayer.start();
							System.err.println("m.getCurrentPosition() : " + mediaPlayer.getDuration()); 
						}catch (IOException e) {
							Log.e("IOException", e.toString());
						}
					}
				}else{
					pauseMediaPlayer();
				}
			}
		});
	}
	
	private void pauseMediaPlayer(){
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
			return;
		}
	}
	
	protected String getSoundAudioSource(byte[] audioSoundByteArray) {
		FileOutputStream fileOutputStream;
		String outputFile = "";
	    try {
	    	// String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
	        File tempMpAudio = File.createTempFile("yooVoiceMassage", ".3gp", getActivity().getCacheDir());
	        tempMpAudio.deleteOnExit();
	        fileOutputStream = new FileOutputStream(tempMpAudio);
	        fileOutputStream.write(audioSoundByteArray);
	        fileOutputStream.close();
	        Log.w("tempMpAudio.getAbsolutePath() : ", tempMpAudio.getAbsolutePath());
	        outputFile = tempMpAudio.getAbsolutePath();
	       
	        // Toast.makeText(getActivity(), "Playing audio message", Toast.LENGTH_LONG).show();
	    } catch (IOException e) {
	        Log.e("Play Audio byte array : ", e.toString());
	    }
	    
	    return outputFile;
	    
	}
	
	private void addMapMarker(MapView map, YooMessage yooMsg){
        GoogleMap  googleMap = map.getMap(); 
        if(googleMap != null){
        	googleMap.clear();
            googleMap.setMyLocationEnabled(true);
            String zoom = "15";
            if(yooMsg != null){
                MarkerOptions markerOptions = new MarkerOptions();
                LatLng point = new LatLng(yooMsg.getLocation()[0], yooMsg.getLocation()[1]); 
                
                // map.invalidate();
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(point));
                markerOptions.position(point);
                String message = yooMsg.getMessage();
                markerOptions.position(point);
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
	
	
	public void captureMapScreen(MapView mMap, final YooMessage message) {
		SnapshotReadyCallback callback = new SnapshotReadyCallback() {
	            @Override
	            public void onSnapshotReady(Bitmap snapshot) {
	            	Bitmap mapBitmap = snapshot;
	            	String mapKey = "map_" + message.getLocation()[0] + "_" + message.getLocation()[1];
			    	YooApplication.saveBitmap(getActivity(), mapKey, mapBitmap);
	            }
	        };
	        mMap.getMap().snapshot(callback);
	}
	
	private void openPostPicture(){
		Intent intent = new Intent(getActivity(), PostPictureActivity.class);
		intent.putExtra("rcipient", getRecipient());
		startActivityForResult(intent, REQUEST_POST_PHOTO);
	}
	
	public void openRcipientContact(){
		if(!broadcast){
			YooRecipient recipient = getRecipient();
			if(recipient instanceof YooUser){
				openDetailContact(((YooUser) recipient).getContactId());
			}if(recipient instanceof YooGroup){
				YooApplication.setLeaveApp(false); // = false;
				Intent intent = new Intent(getActivity(), ContactGroupActivity.class);
				intent.putExtra("group", recipient); 
				startActivityForResult(intent, EDIT_GROUP);
			}
		}
	}
	
	public void openDetailContact(long contactId){
		Contact contact = new ContactManager().findById(contactId);
		if(contact != null){
			ContactDetailFragment conDetailFragment = ContactDetailFragment.newInstance(contactId, false);
			((YooActivity) getActivity()).pushFragment(conDetailFragment);
		}
	}
	
	public void openImageView(String messageId){
		// Contact contact = new ContactManager().findById(messageId);
		ViewImageFragment fra = ViewImageFragment.newInstance(messageId);
		((YooActivity) getActivity()).pushFragment(fra);
	}
	
	public YooRecipient getRecipient(){
		return (YooRecipient)getArguments().getSerializable("recipient");
	}

	private void resetChatLimit(){
		messageIds = new ArrayList<String>();
		if(broadcast){
			limitChat = ChatTools.MAX_USER_HISTORY;
		}else{
			isManyImage = new ChatDAO().isManyImage(getRecipient(), 10);
			limitChat = isManyImage ? ChatTools.MAX_USER_HISTORY/4 : ChatTools.MAX_USER_HISTORY;
			
			if(chatScrollView != null){
		    	chatScrollView.setScrollToBottom(true);
			}
		}
	}

	@Override
	public void friendListChanged(List<YooUser> newFriends) {
		System.err.println("############### friendListChanged  ################"); 
		if(chatScrollView != null && !broadcast){
			for (YooUser yooUser : newFriends) {
				if(yooUser.toJID().equals(getRecipient().toJID())){
					final YooUser yooUser2 = new UserDAO().updateLastOnline(getRecipient().toJID(), DateUtils.substructDate(0));
					getArguments().putSerializable("recipient", yooUser2);
					refreshSubTitle();
				}
			}
		}
	}
	
	
	@Override
	public void didReceiveMessage(YooMessage message) {

		System.err.println("############### didReceiveMessage  ################ >> " + message.toString()); 
		boolean refresh = message.getFrom().isMe(); // ack message
		// new message received from the other
		YooRecipient recipient = (YooRecipient)getArguments().getSerializable("recipient");
		// new message received from the other
		if(message.getType() != YooMessageType.ymtMessageRead && message.getFrom().toJID().equals(recipient.toJID())){
			//if (self.isViewLoaded && self.view.window) {
	        //    [[ChatTools sharedInstance] markAsRead:self.recipient];
	        //}
			
			ChatTools.sharedInstance().markAsRead(recipient);
        	resetChatLimit();
			refresh = true;
		}
		
		if (refresh) {
			refreshReceiveMessage(recipient);
		}
	}
	
	public void refreshReceiveMessage(YooRecipient recipient){
		if(recipient == null){
			recipient = getRecipient();
		}
		Activity activity = getActivity();
		if (activity != null && chatScrollView != null) {
			activity.runOnUiThread(new Runnable() {
		        @Override
		        public void run() {
		        	updateData();
		        }
			});
		}	
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	}


	@Override
	public void didLogin(String login, String error) {
		Log.w("ChatFramgment", " ######## refresh didLogin ######### error : >> " + error);
		if(YooApplication.isAppOn()){
			if("REFRESH".equals(error) || StringUtils.isEmpty(error)){
				// YooApplication.refreshChatFragment(getActivity()); 
				refreshReceiveMessage(getRecipient());
				refreshSubTitle();
			}
		}
	}
	
	private void refreshSubTitle(){
		YooRecipient recipient = getRecipient();
		Activity activity = getActivity();
		if(getRecipient() != null &&  recipient instanceof YooUser && YooApplication.isAppOn()){
			
			if (activity != null && chatScrollView != null) {
				activity.runOnUiThread(new Runnable() {
			        @Override
			        public void run() {
			        	((YooActivity) getActivity()).refreshSubTitle(getRecipient()); 
			        }
				});
			}	
			
		}
	}


	@Override
	public void didReceiveRegistrationInfo(String user, String password) {
		// do nothing
	}


	@Override
	public void didReceiveUserFromPhone(Map<String, String> info) {
		// do nothing
	}


	@Override
	public void addressBookChanged() {
		// do nothing
	}
	
	@Override
	public void onDestroyOptionsMenu() {
		YooApplication.stopCall();
		super.onDestroyOptionsMenu();
	}
	
	
}
