package com.fellow.yoo.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fellow.yoo.R;
import com.fellow.yoo.YooActivity;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.adapter.ContactDetailAdapter;
import com.fellow.yoo.chat.ChatListener;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.ContactManager;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.data.criteria.EqualsCriteria;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.LabelledValue;
import com.fellow.yoo.model.YooMessage;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.DateUtils;
import com.fellow.yoo.utils.RoundImageView;
import com.fellow.yoo.utils.StringUtils;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ContactDetailFragment extends YooListFragment implements ChatListener{
	
	private int inviteLeftId = -1;
	private int inviteRightId = -1;
	private Contact fullContact;
	private TextView labelLastOnline;
	private YooUser yooUser;
	private boolean showIcon;
	private RoundImageView bg_image; 
	private long contactId;
	// private String txtOnline;
	
	public static ContactDetailFragment newInstance(long contactId, boolean showIcon) {
		
		ContactDetailFragment f = new ContactDetailFragment();
	    Bundle args = new Bundle();
	    args.putLong("contactId", contactId);
	    args.putBoolean("showIcon", showIcon);
	    // args.putSerializable("contact", new ContactDAO().find(contactId));
	    f.setArguments(args);
	    return f;
	}
	
	
	
	@Override
	public void onResume() {
		System.err.println("onResume Contact detail."); 
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		ChatTools.sharedInstance().addListener(this);
		
		contactId = getArguments().getLong("contactId");
		showIcon = getArguments().getBoolean("showIcon", true);
		fullContact = new ContactManager().getDetail(contactId); 
		yooUser = new UserDAO().findByCriteria(new EqualsCriteria("contactId", String.valueOf(contactId)));
		if(yooUser != null){
			ChatTools.sharedInstance().requestLastOneline(yooUser.toJID()); 
			// txtOnline = YooApplication.getLastOnline(getActivity(), yooUser.getLastOnline());
		}
		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		view.setBackgroundColor(Color.WHITE); 
		super.onViewCreated(view, savedInstanceState);
	}

	
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ActivityUtils.hideSoftKeyboad(getView()); 
		
		if(getListView().getHeaderViewsCount() <= 0 && fullContact != null){
			LayoutInflater li = LayoutInflater.from(getActivity());
			View header = li.inflate(R.layout.header_contact, getListView(), false);
			bg_image = (RoundImageView) header.findViewById(R.id.icon_background);
			RoundImageView imageView = (RoundImageView) header.findViewById(android.R.id.icon);
			RelativeLayout inviteLayout = (RelativeLayout) header.findViewById(R.id.invite_layout);
			
			RoundImageView imageCall = (RoundImageView) header.findViewById(R.id.icon_call);
			RoundImageView imageChat = (RoundImageView) header.findViewById(R.id.icon_chat);
			
			labelLastOnline = (TextView) header.findViewById(android.R.id.text1);
			imageView.setImageResource(R.drawable.ic_user);
			imageView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {}
			});
			
			imageCall.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ChatFragment chatFragment = ChatFragment.newInstance(yooUser, false, true);
					((YooActivity) getActivity()).pushFragment(chatFragment);
				}
			});
			
			imageChat.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ChatFragment chatFragment = ChatFragment.newInstance(yooUser, false);
					((YooActivity) getActivity()).pushFragment(chatFragment);
				}
			});
			
			header.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {}
			});
			
			if(yooUser != null){
				if(showIcon){
					imageCall.setVisibility(View.VISIBLE);
					imageChat.setVisibility(View.VISIBLE); 
				}
				
				if (yooUser.getPicture() != null) {
					byte [] bitmapData = yooUser.getPicture();
					Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);			
					imageView.setImageBitmap(bitmap);
				}
				
				boolean online = ChatTools.sharedInstance().isVisibility(yooUser);
				if(!online){
					// ChatTools.sharedInstance().requestLastOneline(yooUser.toJID()); 
					String txtOnline = YooApplication.getLastOnline(getActivity(), yooUser.getLastOnline());
					if(!StringUtils.isEmpty(txtOnline)){
						labelLastOnline.setText(txtOnline);
						labelLastOnline.setVisibility(View.VISIBLE); 
					}
				}
				bg_image.setImageResource(online ? R.drawable.bg_online : R.drawable.bg_offline);
			}else{
				bg_image.setVisibility(View.GONE); 
			}
			
			if(fullContact.getEmails().size() > 0){
				inviteRightId = R.string.invite_by_email;
			}
			
			if(fullContact.getPhones().size() > 0){
				inviteLeftId = R.string.invite_by_sms;
			} 
			
			if(inviteRightId == -1 && inviteLeftId != -1){
				inviteRightId = inviteLeftId;
				inviteLeftId = -1;
			}else if(inviteLeftId == -1 && inviteRightId == -1){
				inviteLayout.setVisibility(View.GONE); 
			}
			
			TextView leftInviteTv = (TextView) header.findViewById(R.id.tv_invite_left);
			TextView rightInviteTv = (TextView) header.findViewById(R.id.tv_invite_right);
			if((inviteLeftId > 0  || inviteRightId > 0) && yooUser == null){
				inviteLayout.setVisibility(View.VISIBLE); 
				if(inviteLeftId > 0){
					leftInviteTv.setVisibility(View.VISIBLE); 
					ActivityUtils.setTextHtmlLink(getActivity(), leftInviteTv, getString(inviteLeftId), "#2E64FE"); 
				}
				if(inviteRightId > 0){
					rightInviteTv.setVisibility(View.VISIBLE); 
					ActivityUtils.setTextHtmlLink(getActivity(), rightInviteTv, getString(inviteRightId), "#2E64FE"); 
				}
				if(StringUtils.isEmpty(labelLastOnline.getText().toString())){
					labelLastOnline.setVisibility(View.GONE); 
				}
			}
			
			leftInviteTv.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startInvite(inviteLeftId);
				}
			});
			
			rightInviteTv.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startInvite(inviteRightId);
				}
			});
			
			
			getListView().addHeaderView(header);
			// getTopHeader().setVisibility(View.VISIBLE); 
			// getTopHeader().removeAllViews();
			// getTopHeader().addView(header);
		}
		
		List<String> sections = Arrays.asList(getString(R.string.contact_details), getString(R.string.contact_phones), getString(R.string.contact_emails));
		Map<String, List<String []>> data = new HashMap<String, List<String []>>();
		
		if(fullContact != null){
			data.put(getString(R.string.contact_details), 
					Arrays.asList(
						new String[] {getString(R.string.first_name), fullContact.getFirstName()}, 
						new String[] {getString(R.string.last_name), fullContact.getLastName()},
						new String[] {getString(R.string.company), fullContact.getCompany()}, 
						new String[] {getString(R.string.job_title), fullContact.getJobTitle()})); 
			
			List<String []> phones = new ArrayList<String []>();
			for (LabelledValue labVal : fullContact.getPhones()) {
				phones.add(new String [] {labVal.getTypeName(), labVal.getValue()});
			}
			
			List<String []> emails = new ArrayList<String []>();
			for (LabelledValue labVal : fullContact.getEmails()) {
				emails.add(new String [] {labVal.getTypeName(), labVal.getValue()});
			}
			
			//addEmptyText(phones);
			addEmptyText(emails);
			data.put(getString(R.string.contact_phones), phones);
			data.put(getString(R.string.contact_emails), emails);
		}
		
		// setListAdapter(new ContactDetailAdapter(sections, data));
		getListView().setAdapter(new ContactDetailAdapter(sections, data));
	}
	
	private void startInvite(int inviteId){
		
		if(inviteId == R.string.invite_by_sms){
			try {
				String number = fullContact.getPhones().get(0).getValue(); 
				// startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, null))); 
		        
		        Uri uri = Uri.parse("smsto:" + number); 
	            Intent it = new Intent(Intent.ACTION_SENDTO, uri); 
	            it.putExtra("sms_body", getString(R.string.invite_body)); 
	            startActivity(it); 
			} catch (Exception e) {
				Log.e("ContactDetailFragment : send invite by sms >> ", e.toString());
			}
		}else if(inviteId == R.string.invite_by_email){
			if(ActivityUtils.checkNetworkConnected(getActivity(), R.string.no_internet_connect)){
				String email = fullContact.getEmails().get(0).getValue();
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("message/rfc822");
				i.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
				i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invite_title)); 
				i.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_body));
				try {
				    startActivity(Intent.createChooser(i, getString(R.string.invite_title)));
				} catch (android.content.ActivityNotFoundException ex) {
				    Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Log.e("ContactDetailFragment : send invite by email >> ", e.toString());
				}
			}
		}
	}
	
	private void addEmptyText(List<String []> items){
		if(items.isEmpty()){
			items.add(new String [] {"", ""});
		}
	}
	

	@Override
	public String getTitle(Resources resources) {
		Contact contact = (Contact) getArguments().getSerializable("contact");
		if(contact != null){
			return contact.toString();
		}
		return resources.getString(R.string.contact);
	}
	

	@Override
	public void friendListChanged(List<YooUser> newFriends) {
		System.err.println("############### friendListChanged  ################"); 
		if(labelLastOnline != null && yooUser != null){
			for (YooUser frUser : newFriends) {
				if(frUser.toJID().equals(yooUser.toJID())){
					yooUser = new UserDAO().updateLastOnline(yooUser.toJID(), DateUtils.substructDate(0));
				}
			}
			
			Activity activity = getActivity();
			if(activity != null && activity instanceof YooActivity){
				YooActivity yooActivity = (YooActivity) activity;
				Fragment last = yooActivity.fragments.lastElement();
				if(last instanceof ContactDetailFragment){
					final FragmentTransaction ft = last.getFragmentManager().beginTransaction();
	    			ft.detach(last);
	    			ft.attach(last);
	    			ft.commit();
				}
			}
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
	public void onDestroy() {
		ChatTools.sharedInstance().removeListener(this);
		super.onDestroy();
	}
	
	
}
