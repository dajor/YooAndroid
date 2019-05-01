package com.fellow.yoo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fellow.yoo.adapter.MenuAdapter;
import com.fellow.yoo.bitmaputils.ImageCache.ImageCacheParams;
import com.fellow.yoo.bitmaputils.ImageFetcher;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.fragment.ChatFragment;
import com.fellow.yoo.fragment.ChatListFragment;
import com.fellow.yoo.fragment.ContactListFragment;
import com.fellow.yoo.fragment.ContactListFragment.Type;
import com.fellow.yoo.fragment.SettingsFragment;
import com.fellow.yoo.fragment.YooFragment;
import com.fellow.yoo.gcm.GcmIntentService;
import com.fellow.yoo.model.ContactInfo;
import com.fellow.yoo.model.MenuOption;
import com.fellow.yoo.model.RecentItem;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooRecipient;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.BadgeUtils;
import com.fellow.yoo.utils.StringUtils;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;

@SuppressLint("RtlHardcoded")
public class MainActivity extends YooActivity implements
		ContactListFragment.ContactListCallbacks, 
		ChatListFragment.ChatListCallbacks,
		PopupMenu.OnMenuItemClickListener{
	
	
	private static final String IMAGE_CACHE_DIR = "images";
    public static final String EXTRA_IMAGE = "extra_image";
    private ImageFetcher mImageFetcher;
    protected int mImageThumbSize;
    protected int mImageThumbSpacing;

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private int mCurrentSelectedPosition = 1;
	private List<MenuOption> options;
	private int maxRecent = 5;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		YooApplication.refreshOptionMenu = true;
		 
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		
		mainFrameLayout = (FrameLayout) findViewById(R.id.content_frame);
		
		initOptionMenus();
		mDrawerList.setAdapter(new com.fellow.yoo.adapter.MenuAdapter(MainActivity.this, options));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		
		
		initCatchImage();
		
		getMenuBtn().setVisibility(View.VISIBLE);
		getMenuBtn().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				initOptionMenus();
				if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
					mDrawerLayout.closeDrawer(Gravity.LEFT);
				} else {
					ActivityUtils.hideSoftKeyboad(v);
					mDrawerLayout.openDrawer(Gravity.LEFT);
				}
				updateMenu();
			}
		});
		
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// GcmIntentService.clearPushMessageId(this); 
		YooFragment fragment = new ChatListFragment();
		setFragment(fragment);
		
		
	}
	
	
	
	
	private void initOptionMenus(){
		
		if(YooApplication.refreshOptionMenu){
			options = new ArrayList<MenuOption>();
			options.add(new MenuOption(R.drawable.ic_settings, R.string.action_settings));
			options.add(new MenuOption(R.drawable.ic_chat, R.string.chat_list));
			options.add(new MenuOption(R.drawable.contacts_64, R.string.contact_list));
			options.add(new MenuOption(-1, R.string.recent));
			
			List<RecentItem> recent = new ArrayList<RecentItem>();
			for (YooUser user : new UserDAO().list()) {
				String chatDate = YooApplication.getRecentChat(user.toJID());
				if(!StringUtils.isEmpty(chatDate)){ 
					recent.add(new RecentItem(user.toJID(), user.toString(), chatDate, false));
				}
			}
			
			for (YooGroup group : new GroupDAO().list()) {
				String chatDate = YooApplication.getRecentChat(group.toJID());
				if(!StringUtils.isEmpty(chatDate)){
					recent.add(new RecentItem(group.toJID(), group.toString(), chatDate, true));
				}
			}
			
			Collections.sort(recent, new Comparator<RecentItem>() {
				@Override
				public int compare(RecentItem con1, RecentItem con2) {
					return con2.getChatDate().compareToIgnoreCase(con1.getChatDate());
				}
			});
			
			
			for (int i = 0; i < maxRecent; i++) {
				if(i < recent.size()){
					options.add(new MenuOption(recent.get(i)));
				}
			}
			
			YooApplication.refreshOptionMenu = false;
		}
	}
	
	private void initCatchImage(){
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
       
        ImageCacheParams cacheParams = new ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); 

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this, mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.app_icon);
        mImageFetcher.addImageCache(this, cacheParams);
        
	}
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	public void updateMenu() {
		mDrawerList.setAdapter(new MenuAdapter(getBaseContext(), options));
		mDrawerList.setItemChecked(mCurrentSelectedPosition, true);
	}

	
	public void reloadFragement(int position){
		setFragment(position); 
	}
	
	private void setFragment(int position) {
		
		if (position < 0) {
			return;
		}
		
		clearActionBarLayout();
		mCurrentSelectedPosition = position;
		int optionLabel = options.get(position).getLabel();
		Fragment fragment = null;
		if (optionLabel == R.string.contact_list) {
			fragment = ContactListFragment.newInstance(Type.AddressBookList);
		} else if (optionLabel == R.string.chat_list) {
			fragment = new ChatListFragment();
		} else if (optionLabel == R.string.action_settings) {
			fragment = new SettingsFragment();
		} else if (position > 3){
			MenuOption menuOption = options.get(position);
			// String jId = options.get(position).getRecentJid();
			// fragment = ChatListFragment.newInstance(options.get(position).getRecentJid());
			
			YooRecipient recipient = null; 
			if(menuOption.getRecentItem().isGroup()){
				recipient = new GroupDAO().findByJid(menuOption.getRecentJid()); 
			}else{
				recipient = new UserDAO().findByJid(menuOption.getRecentJid()); 
			}
			
			if(recipient != null){
				fragment = ChatFragment.newInstance(recipient, false);
				// pushFragment(chatFragment);
			}
		}

		if (fragment != null) {
			super.setFragment(fragment);
		} else {
			finish();
		}
	}

	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if(position != 3){ // do nothing when click on recent
				selectItem(position);
			}
		}
	}
	
	private void selectItem(final int position) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if(position != mCurrentSelectedPosition){
					setFragment(position);
				}
			}
		}, 250);
		
		// Highlight the selected item, and close the drawer
		mDrawerList.setItemChecked(position, true);
		mDrawerLayout.closeDrawer(mDrawerList);

	}

	
	
	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		
		if (item.getItemId() == R.id.action_chatlist_menu) {
			// display the chat menu :
			// start new chat, create group, broadcast
			View anchorView = findViewById(R.id.action_chatlist_menu);
			if (anchorView == null) {
				anchorView = findViewById(R.id.content_frame);
			}
			
			PopupMenu popup = new PopupMenu(this, anchorView);
			MenuInflater inflater = popup.getMenuInflater();
			inflater.inflate(R.menu.newchat, popup.getMenu());
			popup.setOnMenuItemClickListener(this);
			popup.show();
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onContactsSelected(List<ContactInfo> contacts) {
		/*Intent intent = new Intent(this, ContactDetailActivity.class);
		intent.putExtra("contactId", contacts.get(0).getContactId());
		startActivity(intent);*/
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*if (requestCode == SELECT_CONTACT) {
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent(this, ChatActivity.class);
				intent.putExtra("recipient", data.getSerializableExtra("recipient"));
				startActivity(intent);
			}
		}*/
	}
	
	@Override
	protected void onStop() {
		if(YooApplication.isLeaveApp()){
			YooApplication.setAppOn(false);
			ChatTools.sharedInstance().asyncDisconnect(false);
			
			int unreads = new ChatDAO().countUnreads();
	        BadgeUtils.setBadge(this, unreads);
	        GcmIntentService.setPushMessage(this, unreads); 
	        YooApplication.stopCall();
			
			if(YooApplication.isFirstRegister()){
				YooApplication.setFistRegister(false); 
				// stopTheApp();
			}
		}
		// android.os.Process.killProcess(android.os.Process.myPid());
		super.onStop();
	}
	
	@Override
	protected void onStart() {
		if(YooApplication.isLeaveApp()){
			YooApplication.setAppOn(true);
			ChatTools.sharedInstance().reLogin(this);
		}
		super.onStart();
	}


	@Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
        
        ChatTools.sharedInstance().reLogin(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
        int unreads = new ChatDAO().countUnreads();
        BadgeUtils.setBadge(this, unreads);
        GcmIntentService.setPushMessage(this, unreads); 
    }
    
    public ImageFetcher getImageFetcher() {
        return mImageFetcher;
    }

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		return false;
	}

	@Override
	public void onChatSelected(YooRecipient recipient) {
		
	}
    
	@Override
	public void onBackPressed() {
		
		super.onBackPressed();
	}
    


}
