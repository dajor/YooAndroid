package com.fellow.yoo;

import java.util.ArrayList;
import java.util.List;

import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.data.GroupDAO;
import com.fellow.yoo.data.UserDAO;
import com.fellow.yoo.fragment.ContactListFragment;
import com.fellow.yoo.fragment.ContactListFragment.Type;
import com.fellow.yoo.model.ContactInfo;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.StringUtils;

import android.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContactGroupActivity extends Activity implements ContactListFragment.ContactListCallbacks {


	private List<ContactInfo> selected;
	private YooGroup yooGroup;
	private boolean broadcast = false;
	private boolean isGroupOwner = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_group);
		
		selected = new ArrayList<ContactInfo>();
		yooGroup = (YooGroup)getIntent().getSerializableExtra("group");
		broadcast = getIntent().getBooleanExtra("broadcast", false);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		if(broadcast){
			getActionBar().setTitle(getString(R.string.action_broadcast)); 
			isGroupOwner = true;
		}else{
			if(yooGroup != null){
				List<YooUser> users = new ArrayList<YooUser>();
				for (String userJid : new GroupDAO().listMembers(yooGroup.toJID())) {
					YooUser user = new UserDAO().findByJid(userJid);
					if(user != null){
						users.add(user);
					}
				}
				checkGroupOwner();
			}else{
				isGroupOwner = true;
			}
			
			if(yooGroup != null && !isGroupOwner){
				getActionBar().setTitle(getString(R.string.group_members)); 
			}else{
				getActionBar().setTitle(getString(yooGroup != null? R.string.edit_group : R.string.new_group)); 
			}
			
		}
		
		ContactListFragment fragment = ContactListFragment.newInstance(Type.ContactMultiSelect);
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(R.id.contact_list, fragment);
		transaction.commit();
		
	}
	
	private void checkGroupOwner(){
		if(yooGroup != null){
			String loginName = YooApplication.getLogin().replace("@" + ChatTools.YOO_DOMAIN, "");
			String groupName = yooGroup.getName();
			if(groupName != null && groupName.split("-")[0].equals(loginName)){
				isGroupOwner = true;
			}
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home:
			finish();
			return true;
		
		case R.id.action_done:	 
			if(broadcast){
				if(selected.size() > 0){
					YooApplication.broadcastUsers.clear();
	                for (ContactInfo contact : selected) {
	                	YooUser user = contact.getUser();
	                    if (user != null) {
	                    	YooApplication.broadcastUsers.add(user);
	                    }
	                }
	                new ChatDAO().removeChatBroadcast();
	                Intent returnIntent = new Intent();
	        		setResult(RESULT_OK, returnIntent);
                	finish();
                }else{
                	ActivityUtils.displayDiaMsg(getString(R.string.action_broadcast),
							getString(R.string.group_member), this); 
                }
			}else{
				if(yooGroup != null){ // edit group member
	                if(selected.size() > 0){
	                	List<String> jids = new ArrayList<String>();
		                for (ContactInfo contact : selected) {
		                	YooUser user = contact.getUser();
		                    if (user != null) {
		                    	jids.add(user.toJID());
		                    }
		                }
	                	ChatTools.sharedInstance().editGroup(yooGroup, jids);
	                	finish();
	                }else{
	                	ActivityUtils.displayDiaMsg(getString(R.string.edit_group),
								getString(R.string.group_member), this); 
	                	
	                }
				}else{ // create new group
					if(selected.size() > 0){
						showGroupDialog(); 
					}else{
						ActivityUtils.displayDiaMsg(getString(R.string.new_group),
								getString(R.string.group_member), this); 
					}
					  
				}
			}
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private String groupName = "";
	private String errMessge = "";
	private void showGroupDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.new_group);
		alert.setMessage(R.string.enter_group_name);

		// Set an EditText view to get user input 
		LinearLayout diLayout = new LinearLayout(this);
		diLayout.setOrientation(LinearLayout.VERTICAL);
		final EditText input = new EditText(this);
		input.setText(""); 
		input.append(groupName); // move cursor to the end of text.
		diLayout.addView(input);
		alert.setView(diLayout);
		
		if(!StringUtils.isEmpty(errMessge)){
			final TextView txtMessage = new TextView(this);
			txtMessage.setText(getString(R.string.groupname_length)); 
			txtMessage.setTextColor(Color.RED);
			txtMessage.setText(errMessge);
			int padding = ActivityUtils.dpToPixels(this, 10);
			txtMessage.setPadding(padding, padding, padding, padding);
			diLayout.addView(txtMessage); 
		}
		
		alert.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				groupName = input.getText().toString();
				if (groupName.length() > 2) {
					List<String> jids = new ArrayList<String>();
	                for (ContactInfo contact : selected) {
	                	YooUser user = contact.getUser(); // new UserDAO().find(contact.getUser().getName(), ChatTools.YOO_DOMAIN);
	                    if (user != null) {
	                    	jids.add(user.toJID());
	                    	Log.w(" ##### ContactGroupActivity Add Group Member >> ", user.toJID());
	                    }else{
	                    	Log.w(" ##### ContactGroupActivity Add Group Member >> ", "User not found ");
	                    }
	                }
	                ChatTools.sharedInstance().createGroup(groupName, jids);
	                
	                Intent returnIntent = new Intent();
	        		setResult(RESULT_OK, returnIntent);
	        		finish();
				}else{
					errMessge = getString(R.string.groupname_length);
					showGroupDialog();
				}
			}
		});

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		});
		
		
		//Button pos =  ((AlertDialog.Builder) alert).getButton(AlertDialog.BUTTON_POSITIVE);
		//pos.setEnabled(true);

		alert.show();
	}

	@Override
	public void onContactsSelected(List<ContactInfo> contacts) {
		selected = contacts;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.create_group, menu);
		
		return true;
	}
	

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.getItem(0);
		if(broadcast){
			item.setIcon(ActivityUtils.getIcon(this, R.drawable.ic_chat, 35)); 
			// item.setIcon(R.drawable.ic_chat);
		}else{
			item.setIcon(ActivityUtils.getIcon(this, drawable.ic_menu_save, 50)); 
			if(!isGroupOwner){
				item.setEnabled(false);
				item.setVisible(false);
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	public List<ContactInfo> getSelected() {
		return selected;
	}


	public void setSelected(List<ContactInfo> selected) {
		this.selected = selected;
	}

	public boolean isGroupOwner() {
		return isGroupOwner;
	}


}
