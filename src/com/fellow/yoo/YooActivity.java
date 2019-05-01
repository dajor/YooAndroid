package com.fellow.yoo;

import java.util.Date;
import java.util.List;
import java.util.Stack;

import com.fellow.yoo.chat.CallingListener;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.data.ContactManager;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.fragment.ChatFragment;
import com.fellow.yoo.fragment.ContactListFragment;
import com.fellow.yoo.fragment.YooFragment;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooMessage.CallStatus;
import com.fellow.yoo.model.YooRecipient;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.DateUtils;
import com.fellow.yoo.utils.RoundImageView;
import com.fellow.yoo.utils.StringUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class YooActivity extends Activity implements CallingListener{

	public Stack<Fragment> fragments;

	private TextView mTitle;
	private TextView mSubTitle;

	private ImageButton btnInfo;
	private ImageButton btnGroup;
	private ImageButton btnAddChat;
	private ImageButton mBackBtn;
	private ImageButton menuBtn;

	private Menu actionMenu;
	private ViewGroup actionBarLayout;
	private LinearLayout chatBarLayout;
	private ImageButton checkboxContact;
	protected FrameLayout mainFrameLayout;
	private boolean isChecked = true;
	private ImageButton btnSave;
	private TextView checkBoxLabel;

	@SuppressLint("InflateParams")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		fragments = new Stack<Fragment>();

		actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.action_bar, null);
		menuBtn = (ImageButton) actionBarLayout.findViewById(R.id.action_bar_home);

		// Set up your ActionBar
		getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().setDisplayShowTitleEnabled(false);
		getActionBar().setDisplayShowCustomEnabled(true);
		getActionBar().setCustomView(actionBarLayout);
		getActionBar().setTitle(""); 
		getActionBar().getCustomView().setMinimumHeight(ActivityUtils.dpToPixels(this, 60)); 
		

		mTitle = (TextView) actionBarLayout.findViewById(R.id.action_bar_title);
		mSubTitle = (TextView) actionBarLayout.findViewById(R.id.action_bar_sub_title);
		mBackBtn = (ImageButton) actionBarLayout.findViewById(R.id.action_bar_back);	
		mSubTitle.getLayoutParams();
		
		
		mBackBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				popFragment();
				ActivityUtils.hideSoftKeyboad(v);
			}
		});
		
		btnInfo = (ImageButton) actionBarLayout.findViewById(R.id.btn_info);
		btnInfo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				YooFragment last = (YooFragment) fragments.lastElement();
				if (last != null) {
					last.actionClicked(R.id.btn_info);
				}
			}
		});

		btnGroup = (ImageButton) actionBarLayout.findViewById(R.id.btn_group);
		btnGroup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				YooFragment last = (YooFragment) fragments.lastElement();
				if (last != null) {
					last.actionClicked(R.id.btn_group);
				}
			}
		});
		
		btnAddChat = (ImageButton) actionBarLayout.findViewById(R.id.btn_add_chart);
		btnAddChat.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				YooFragment last =  (YooFragment) fragments.lastElement();
				if (last != null) {
					last.actionClicked(R.id.btn_add_chart);
				}
			}
		});
		
		btnSave = (ImageButton) actionBarLayout.findViewById(R.id.btn_save);
		btnSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment last =  (Fragment) fragments.lastElement();
				if (last != null && last instanceof YooFragment) {
					((YooFragment)last).actionClicked(R.id.btn_save);
				}
			}
		});
		
		checkboxContact = (ImageButton) actionBarLayout.findViewById(R.id.btn_checkbox_contact);
		checkBoxLabel = (TextView) actionBarLayout.findViewById(R.id.lbl_checkbox_contact);
		checkboxContact.setImageResource(isChecked ? R.drawable.checkbox_on : R.drawable.checkbox_off); 
		checkboxContact.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isChecked = !isChecked;
				checkboxContact.setImageResource(isChecked ? R.drawable.checkbox_on : R.drawable.checkbox_off); 
				checkBoxLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
				YooFragment last =  (YooFragment) fragments.lastElement();
				if (last != null) {
					last.actionClicked(R.id.btn_checkbox_contact);
				}
			}
		});
		
		chatBarLayout = (LinearLayout) actionBarLayout.findViewById(R.id.chat_bar_layout);
		YooApplication.importAllContacts(this); 
		
		ChatTools.sharedInstance().setCallingDelegate(this); 
		YooApplication.checkCalliing();
	}
	
	public void updateChatViewBar(final YooRecipient recipient){
		/*new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				
			}
		}, 250);*/
		
		updateChatViewBarW(recipient); 
	}
	
	
	public void updateChatViewBarW(YooRecipient recipient){
		if(recipient == null){ // set broadcast title
			mTitle.setText(R.string.action_broadcast); 
			if(!YooApplication.broadcastUsers.isEmpty()){
				StringBuilder subTitle = new StringBuilder();
				for (YooUser user : YooApplication.broadcastUsers) { 
					if(subTitle.length() > 0){
						subTitle.append(", ");
					}
					subTitle.append(user.getAlias());
				}
				mSubTitle.setVisibility(View.VISIBLE);
				mSubTitle.setText(subTitle.toString()); 
			}
			return ;
		}
		boolean online = false;
		chatBarLayout.removeAllViews();
		RoundImageView callImageView = new RoundImageView(this);
		RoundImageView chatImageView = new RoundImageView(this);
		chatBarLayout.addView(callImageView); 
		chatBarLayout.addView(chatImageView); 
		callImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment last =  (Fragment) fragments.lastElement();
				if (last instanceof ChatFragment) {
					((ChatFragment) last).startCall();
				}
			}
		});
		chatImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment last =  (Fragment) fragments.lastElement();
				if (last instanceof ChatFragment) {
					((ChatFragment) last).openRcipientContact();
				}
			}
		});
		int padding = ActivityUtils.dpToPixels(this, 5);
		int photoSize = ActivityUtils.dpToPixels(this, 38);
		int phoneSize = ActivityUtils.dpToPixels(this, 45);
		chatImageView.setImageResource(R.drawable.ic_user);
		callImageView.setImageResource(R.drawable.phone_48); 
		if (recipient instanceof YooUser) {
			YooUser user = (YooUser) recipient;	
			byte [] bitmapData = user.getPicture();
			if(bitmapData != null){
				Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);	
				if(bitmap != null){
					chatImageView.setImageBitmap(bitmap);
				}
			}
			
			mTitle.setText(((YooUser) recipient).toString()); 
			online = ChatTools.sharedInstance().isVisibility((YooUser)recipient);
		} else {
			chatImageView.setImageResource(R.drawable.group_icon);
			mTitle.setText(((YooGroup) recipient).toString()); 
		}
		
		
		if(fragments.size() < 2){
			mBackBtn.setVisibility(View.GONE); 
		}
		mSubTitle.setText(online ? getString(R.string.online) : "");  
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(phoneSize, phoneSize);
		LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(photoSize, photoSize);
		callImageView.setPadding(0, 0, padding, 0); 
		callImageView.setLayoutParams(params);
		chatImageView.setLayoutParams(params2);
		chatBarLayout.setVisibility(View.VISIBLE); 
		mSubTitle.setVisibility(View.VISIBLE); 
		
	}
	
	public void refreshSubTitle(YooRecipient recipient){
		
		String subtitle = ""; 
		if (recipient instanceof YooUser) {
			boolean  online = ChatTools.sharedInstance().isVisibility((YooUser)recipient);
			if(online){
				mSubTitle.setText(getString(R.string.online));  
			}else{
				subtitle = YooApplication.getLastOnline(this, ((YooUser) recipient).getLastOnline());
			}
		} else if (recipient instanceof YooGroup) {
			YooGroup group = (YooGroup) recipient;
			if(group != null){ 
				List<String> groupMembers = new GroupDAO().listMembers(group.toJID());
				if(groupMembers.size() > 0){
					subtitle = new UserDAO().getUserGroupName(groupMembers);
				}else{
					subtitle = group.getMember();
				}
			}
		}
		
		if(!StringUtils.isEmpty(subtitle)){
			mSubTitle.setVisibility(View.VISIBLE); 
			mSubTitle.setText(subtitle);
		}
	}

	@Override
	public void onBackPressed() {
		if (fragments != null) {
			if (fragments.size() < 2) {
				super.onBackPressed();
			} else {
				popFragment();
			}
		}
		
	}

	public void updateActionBar() {
		
		clearActionBarLayout();
		if (fragments.size() > 1) {
			mBackBtn.setVisibility(View.VISIBLE);
		} else {
			mBackBtn.setVisibility(View.GONE);
		}

		if(fragments.lastElement() instanceof YooFragment){
			YooFragment last =  (YooFragment) fragments.lastElement();
			mTitle.setText(last.getTitle(getResources()));

			if (actionMenu != null && last != null) {
				MenuItem menuItem = actionMenu.findItem(R.id.actionButton);
				menuItem.setVisible(last.getActionTitle() != -1);
				if (last.getActionTitle() != -1) {
					menuItem.setTitle(last.getActionTitle());
					menuItem.setIcon(last.getActionIcon());
				}
			}
		}
	}

	public void setFragment(Fragment fragment) {
		fragments.clear();
		fragments.push(fragment);
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left); 
		ft.replace(R.id.content_frame, fragment);
		ft.commit();
		
		
		showTopLayout(fragment); 
		updateActionBar();
	}

	public void popFragment() {
		if (fragments.size() == 1){
			return;
		}
		
		fragments.pop();
		Fragment last = fragments.lastElement();
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right);
		ft.replace(R.id.content_frame, last);
		ft.commit();

		if(last instanceof ChatFragment){
			updateChatViewBar(((ChatFragment) last).getRecipient());
		}else{
			updateActionBar();
		}
		
		showTopLayout(last);
	}
	
	public void pushFragment(Fragment fragment) {
		showTopLayout(fragment); 
		
		fragments.push(fragment);
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
		ft.replace(R.id.content_frame, fragment);
		ft.commit();
		
		updateActionBar();
	}
	
	public void pushFragment(Fragment fragment, boolean pop) {
		if (fragments.size() > 1){
			fragments.pop();
		}
		pushFragment(fragment); 
	}
	
	private void showTopLayout(Fragment fragment){
		int dp = ActivityUtils.dpToPixels(this, 10);
		if(mainFrameLayout != null){
			if(fragment instanceof ContactListFragment){
				mainFrameLayout.setPadding(0, dp*0, 0, 0);
			}else{
				mainFrameLayout.setPadding(0, 0, 0, 0);
			}
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, R.id.actionButton, 0, R.string.app_name).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		actionMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.actionButton:
			YooFragment last = (YooFragment) fragments.lastElement();
			if (last != null) {
				last.actionClicked(item.getItemId());
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.findItem(R.id.actionButton);
		if (menuItem != null) {
			menuItem.setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	
	public ImageButton getMenuBtn() {
		return menuBtn;
	}
	

	public ImageView getBtnGroup() {
		return btnGroup;
	}

	public ImageView getBtnAddChat() {
		return btnAddChat;
	}

	public TextView getmTitle() {
		return mTitle;
	}
	
	public ImageView getBtnInfo() {
		return btnInfo;
	}
	
	public ImageView getBtnSave() {
		return btnSave;
	}
	
	public Boolean isChecked() {
		return isChecked;
	}
	
	public void setContactCheckBox(boolean visable) {
		checkboxContact.setVisibility(visable? View.VISIBLE : View.GONE);
		checkBoxLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE); 
	}

	public void clearActionBarLayout(){
		mSubTitle.setVisibility(View.GONE); 
		chatBarLayout.setVisibility(View.GONE); 
		btnInfo.setVisibility(View.GONE); 
		btnGroup.setVisibility(View.GONE); 
		btnAddChat.setVisibility(View.GONE); 
		btnSave.setVisibility(View.GONE); 
		checkboxContact.setVisibility(View.GONE); 
		checkBoxLabel.setVisibility(View.GONE); 
	}
	
	@Override
	public void requestCall(YooMessage yooMsg) {
		// ActivityUtils.displayToast("Request Call", this); 
		if(yooMsg.getTo() == null){
			this.runOnUiThread(new Runnable() {
		        @Override
		        public void run() {
		        	ActivityUtils.displayDiaMsg(getString(R.string.make_call, ""),  
		        			getString(R.string.error_no_conference_number), YooActivity.this); 
		        }
			});
		}else{
			YooApplication.refreshChatFragment(this); 
		}
	}
	
	private String callerName = "";
	private String phone = "";
	@Override
	public void requestCallFromRecipient(YooMessage yooMsg) {
		if(yooMsg.getFrom() != null){
			YooApplication.callReqId = yooMsg.getIdent();
			final Date startDate = yooMsg.getDate();
			callerName = yooMsg.getFrom().toString();
			phone = "";
			YooUser user = null;
			if(yooMsg.getFrom() instanceof YooGroup){
				YooGroup yGroup = (YooGroup) yooMsg.getFrom();
				user = new UserDAO().findByJid(yGroup.getMember() + "@" + ChatTools.YOO_DOMAIN);
				callerName = yGroup.toString();
				YooApplication.callMember = user;
			}else{
				user = new UserDAO().findByJid(yooMsg.getFrom().toJID());
			}
			if(user != null){
				callerName = user.toString();
				Contact contact = new ContactManager().getContactPhone(new Contact(user.getContactId()));
				if(contact != null && !contact.getPhones().isEmpty()){
					phone = contact.getPhones().get(0).getValue();
				}
			}
			
			try {
				this.runOnUiThread(new Runnable() {
			        @Override
			        public void run() {
			        	YooApplication.callingPhoneAlert(YooActivity.this, callerName, phone, startDate);
			        }
				});
			} catch (Exception e) {
				Log.e("YooActivity : Alert Call From Receiver.", e.getMessage());
			}
		}
	}

	@Override
	public void cancelCall(YooMessage yooMsg) {
		if(yooMsg != null){
			if(yooMsg.getFrom() instanceof YooGroup){
				//return;
			}
    		new ChatDAO().updateCallStatus(yooMsg.getCallReqId(), CallStatus.csCancelled); 
    		YooApplication.stopCall();
    		YooApplication.refreshChatFragment(this); 
		}
	}


	@Override
	public void acceptCallFromRecipient(YooMessage yooMsg, boolean accept) {
		YooMessage yooMessage = new ChatDAO().findIdent(yooMsg.getCallReqId());
		if(yooMessage == null){
			return;
		}
		long waiting = DateUtils.substructTimeAsSeconds(yooMessage.getDate());
		if(waiting < ChatTools.CALL_MAX_DELAY){
			if(accept && CallStatus.csNone.equals(yooMessage.getCallStatus())){ 
				if( yooMessage.getTo() != null){
					String telNumber = ChatTools.getConferenceNumber(yooMessage.getConferenceNumber(), YooApplication.getLogin()); 
					if(!com.fellow.yoo.utils.StringUtils.isEmpty(telNumber)){
						Intent callIntent = new Intent(Intent.ACTION_CALL);
						callIntent.setData(Uri.parse("tel:" + telNumber));
						startActivity(callIntent);
					}
				}	
			}
			
			if(yooMessage.getTo() instanceof YooGroup && !accept){
				YooGroup yGroup = (YooGroup) yooMessage.getTo();
				List<String> members = new GroupDAO().listMembers(yGroup.toJID());
				for (String member : members) {
					 System.out.println("all group memeber : " + member); 
				}
				System.out.println("from group memeber : " + yooMsg.getFrom().toJID()); 
				
				// when all members not yet decline. continue calling.
				if(!ChatTools.sharedInstance().declineCallMembers(yooMsg.getFrom().toJID())){
					return;
				}
			}
			
			new ChatDAO().updateCallStatus(yooMsg.getCallReqId(), accept ? CallStatus.csAccepted : CallStatus.csRejected); 
			YooApplication.stopCall();
			YooApplication.refreshChatFragment(this); 
		}else{
			new ChatDAO().updateCallStatus(yooMsg.getCallReqId(), CallStatus.csCancelled); 
		}
	}
	
	
	@Override
	public void acceptCall(YooMessage yooMsg, boolean accept) {
		
		String telNumber = ChatTools.getConferenceNumber(yooMsg.getConferenceNumber(), YooApplication.getLogin()); 
		if(!com.fellow.yoo.utils.StringUtils.isEmpty(telNumber)){
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:" + telNumber));
			startActivity(callIntent);
		}
	}
	
	public void startCall(YooRecipient yooUser){
		ChatFragment chatFragment = ChatFragment.newInstance(yooUser, false, true);
		pushFragment(chatFragment);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		ChatTools.sharedInstance().logout();
		
		if(YooApplication.isFirstRegister()){
			YooApplication.setFistRegister(false); 
			// stopTheApp();
			// android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	protected void stopTheApp(){
		/*Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);*/
		
		android.os.Process.killProcess(android.os.Process.myPid());
		AlarmManager alm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		alm.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()), 0));
		
	}
	
}
