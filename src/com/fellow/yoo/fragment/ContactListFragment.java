package com.fellow.yoo.fragment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fellow.yoo.ContactGroupActivity;
import com.fellow.yoo.MainActivity;
import com.fellow.yoo.R;
import com.fellow.yoo.SelectContactActivity;
import com.fellow.yoo.YooActivity;
import com.fellow.yoo.adapter.ContactListAdapter;
import com.fellow.yoo.chat.ChatListener;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.ContactDAO;
import com.fellow.yoo.data.ContactManager;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.ContactInfo;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.StringUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SearchView;

public class ContactListFragment extends YooListFragment implements ChatListener {

	
	private List<ContactInfo> selected;
	private List<String> sections;
	private Map<String, List<ContactInfo>> data;
	// private View empty;
	private YooGroup yooGroup;
	private boolean checkYooContact = false;
	private SearchView searchView;
	private EditText searchEditText;
	private String searchKey;
	private boolean isGroupOwner;
	
	private ContactListCallbacks mCallbacks;

	public enum Type {
		ContactSelect, ContactMultiSelect, ContactReadOnly, AddressBookList, AddressBookSelect, ShareContact
	}


	public static ContactListFragment newInstance(Type type) {
		ContactListFragment f = new ContactListFragment();
		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putSerializable("type", type);
		f.setArguments(args);
		return f;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		selected = new ArrayList<ContactInfo>();
		ChatTools.sharedInstance().addListener(this);
		
		if (getActivity() instanceof YooActivity) {
			((YooActivity) getActivity()).setContactCheckBox(true); 
			((MainActivity) getActivity()).updateMenu();
		}else if (getActivity() instanceof ContactGroupActivity) {
			isGroupOwner = ((ContactGroupActivity) getActivity()).isGroupOwner();
		}
		
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		view.setBackgroundColor(Color.WHITE); 
		
		yooGroup = (YooGroup) getActivity().getIntent().getSerializableExtra("group");
		sections = new ArrayList<String>();
		data = new HashMap<String, List<ContactInfo>>();
		
		// setListAdapter(new ContactListAdapter(sections, data, selected, yooGroup));
		getListView().setAdapter(new ContactListAdapter(getActivity(), sections, data, selected, yooGroup));
				
		addSearchView();
		updateData();
		
		ActivityUtils.hideSoftKeyboad(getView()); 
		
		super.onViewCreated(view, savedInstanceState);
	}

	
	
	private void addSearchView(){
		searchView = new SearchView(getActivity());
		int sp = ActivityUtils.dpToPixels(getActivity(), 10);
		LinearLayout.LayoutParams params = (new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, sp * 5)); 
		searchView.setLayoutParams(params); 
		searchView.setGravity(Gravity.CENTER_HORIZONTAL); 
		
		SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
	
		searchEditText = (EditText) searchView.findViewById(getResources().getIdentifier("android:id/search_src_text", null, null));
		searchEditText.setTextColor(Color.DKGRAY); 
		searchEditText.setHint(R.string.search_hint);
		
		
		ImageView srButtonImage = (ImageView) searchView.findViewById(getResources().getIdentifier("android:id/search_button", null, null));  
		srButtonImage.setImageResource(android.R.drawable.ic_menu_search);  
		
		ImageView btnSearchMag = (ImageView) searchView.findViewById(getResources().getIdentifier("android:id/search_mag_icon", null, null));  
		btnSearchMag.setVisibility(View.VISIBLE); 
		
		searchView.setQueryHint(getString(R.string.action_search));
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
		if(!StringUtils.isEmpty(searchKey)){
			searchView.setIconifiedByDefault(true);
			searchView.setIconified(false);
		}
		
		SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				searchKey = newText;
				updateData();
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				searchKey = query;
				updateData();
				return true;
			}
		};
		
		searchView.setOnQueryTextListener(textChangeListener);
		getTopHeader().setVisibility(View.VISIBLE); 
		getTopHeader().removeAllViews();
		getTopHeader().addView(searchView);
	}
	
	
	@SuppressLint("UseSparseArrays")
	public void updateData() {
		
		Type type = (Type) getArguments().getSerializable("type");
		if(getActivity() instanceof YooActivity){
			checkYooContact = ((YooActivity) getActivity()).isChecked();
		}else if(getActivity() instanceof SelectContactActivity){
			checkYooContact = ((SelectContactActivity) getActivity()).isChecked();
		}
		
		List<Contact> contacts = null;
		if (StringUtils.isEmpty(searchKey)) {
			contacts = new ContactDAO().list();
		} else {
			contacts = new ContactDAO().filterByName(searchKey);
		}

		List<YooUser> users = new UserDAO().list();
		Map<Long, List<YooUser>> userMap = new HashMap<Long, List<YooUser>>();
		
		for (YooUser user : users) {
			List<YooUser> cUsers = userMap.get(user.getContactId());
			if (cUsers == null) {
				cUsers = new ArrayList<YooUser>();
				userMap.put(user.getContactId(), cUsers);
			}
			cUsers.add(user);
		}
		sections.clear();
		data.clear();
		
		List<String> groupMember = new ArrayList<String>();
		if(yooGroup != null){
			groupMember = new GroupDAO().listMembers(yooGroup.toJID());
		}
		
		if (type == Type.AddressBookList || type == Type.AddressBookSelect || type == Type.ShareContact) {
			for (Contact contact : contacts) {
				List<YooUser> contUsers = userMap.get(contact.getContactId());
				YooUser user = null;
				if(contUsers != null && contUsers.size() > 0){
					user = contUsers.get(0);
				}
				
				if(checkYooContact){ // add only Yoo Contact.
					if(user != null && !user.isMe()){
						addContactInfo(sections, data, new ContactInfo(contact, user));
					}
				}else{
					addContactInfo(sections, data, new ContactInfo(contact, user));
				}
			}	
		}else if (type == Type.ContactMultiSelect) { // contact list for group and broadcast.
			for (Contact contact : contacts) {
				List<YooUser> contUsers = userMap.get(contact.getContactId());
				YooUser yooUser = contUsers != null && contUsers.size() > 0 ? contUsers.get(0) : null;
				if(yooUser != null && !yooUser.isMe()){
					ContactInfo conInfo = new ContactInfo(contact, yooUser);
					if(groupMember.contains(yooUser.toJID())){
						if(isGroupOwner){
							selected.add(conInfo);
						}
					}
					
					if(isGroupOwner || groupMember.isEmpty()){
						addContactInfo(sections, data, conInfo);
					}else{
						if(groupMember.contains(yooUser.toJID())){ // group member, see only member list.
							addContactInfo(sections, data, conInfo);
						}
					}
					
				}
			}	
		}else{ // add only Yoo Contact.
			for (Contact contact : contacts) {
				List<YooUser> contUsers = userMap.get(contact.getContactId());
				if (contUsers != null) {
					for (YooUser user : contUsers) {
						if(!user.isMe()){
							addContactInfo(sections, data, new ContactInfo(contact, user));
						}
					}
				}
			}	
		}

		
		// 
		if(getActivity() instanceof ContactGroupActivity){
			((ContactGroupActivity) getActivity()).setSelected(selected); 
		}

		
		Collections.sort(sections);
		// update the list
		// ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
		((ContactListAdapter) getListView().getAdapter()).setSelected(selected);
		((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged();
	}


	private void addContactInfo(List<String> sections, Map<String, List<ContactInfo>> data, ContactInfo contact) {
		YooUser user = contact.getUser();
		if(user != null){
			if("registration".equals(user.getName()) || "registration".equals(user.getAlias())){
				return;
			}
		}
		String section = getHeader(contact.toString());
		if (!sections.contains(section)) {
			sections.add(section);
		}
		List<ContactInfo> sectionData = data.get(section);
		if (sectionData == null) {
			sectionData = new ArrayList<ContactInfo>();
			data.put(section, sectionData);
		}
		sectionData.add(contact);
	}
	
	
	
	@Override
	public void onListItemClick(AdapterView<?> adapter, View v, int position, long id) {
		// ContactInfo contact = (ContactInfo) getListAdapter().getItem(position);
		ContactInfo contact = (ContactInfo) getListView().getAdapter().getItem(position);
		if (contact != null) {
			if (getActivity() instanceof ContactGroupActivity) {
				if(!isGroupOwner){
					return;
				}
			}
			
			if (getActivity() instanceof YooActivity) {
				// getListView().setVisibility(View.GONE);
				if(new ContactManager().findById(contact.getContactId()) != null){
					ContactDetailFragment conDetailFragment = ContactDetailFragment.newInstance(contact.getContactId(), true);
					((YooActivity) getActivity()).pushFragment(conDetailFragment);
				}
			} else {
				Type type = (Type) getArguments().getSerializable("type");
				if (selected.contains(contact)) {
					selected.remove(contact);
				} else {
					selected.add(contact);
				}

				
				mCallbacks.onContactsSelected(selected);
				if (type == Type.AddressBookSelect || type == Type.ContactSelect || type == Type.AddressBookList) {
					selected.clear();
				} else {
					// ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
					if(isGroupOwner){
						((BaseAdapter) getListView().getAdapter()).notifyDataSetChanged();
					}
				}
			}
		}
	}


	public static interface ContactListCallbacks {
		void onContactsSelected(List<ContactInfo> contacts);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mCallbacks = (ContactListCallbacks) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException("Activity must implement ContactListCallbacks.");
		}
	}

	@Override
	public void friendListChanged(List<YooUser> newFriends) {
		Activity activity = getActivity();
		// activity might be null if the fragment has been detached ; test it
		// to avoid NullPointerException
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateData();
				}
			});
		}
	}
	
	@Override
	public String getTitle(Resources resources) {
		return resources.getString(R.string.contact_list);
	}
	
	
	@Override
	public void actionClicked(int actionId) {
		if(actionId == R.id.btn_checkbox_contact){
			updateData();
		}
	}
	

	@Override
	public void didReceiveMessage(YooMessage message) {
	}

	@Override
	public void didLogin(String login, String error) {
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		ChatTools.sharedInstance().removeListener(this);
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	
	
	@Override
	public void onResume() {
		super.onResume();
	}


	private static String getHeader(String s) {
		if (s == null || s.equals(""))
			return "#";
		s = flattenToAscii(s).toUpperCase(Locale.US);
		char c = s.charAt(0);
		if (c >= 'A' && c <= 'Z')
			return String.valueOf(c);
		return "#";

	}

	private static String flattenToAscii(String string) {
		char[] out = new char[string.length()];
		string = Normalizer.normalize(string, Normalizer.Form.NFD);
		int j = 0;
		for (int i = 0, n = string.length(); i < n; ++i) {
			char c = string.charAt(i);
			if (c <= '\u007F')
				out[j++] = c;
		}
		return new String(out);
	}


}
