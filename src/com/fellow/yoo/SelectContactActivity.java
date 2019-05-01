package com.fellow.yoo;

import java.util.List;

import com.fellow.yoo.fragment.ContactListFragment;
import com.fellow.yoo.fragment.ContactListFragment.ContactListCallbacks;
import com.fellow.yoo.fragment.ContactListFragment.Type;
import com.fellow.yoo.model.ContactInfo;
import com.fellow.yoo.utils.ActivityUtils;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class SelectContactActivity extends Activity implements ContactListCallbacks {
	
	
	private boolean isChecked = true;
	private boolean shareContact = false;
	private MenuItem minuCheckItem;
	private ContactListFragment fragment;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_list);
		ContactListFragment.Type type = (ContactListFragment.Type) getIntent().getSerializableExtra("type"); 
		shareContact = ContactListFragment.Type.ShareContact.equals(type);
		fragment = ContactListFragment.newInstance(type);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.contact_list, fragment);
        transaction.commit();
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		if(ContactListFragment.Type.ShareContact.equals(type)){ 
			getActionBar().setTitle(getString(R.string.send_contact));  
		}
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
		    case android.R.id.home:
		        finish();
		        return true;
		    case R.id.action_check:
		    	isChecked = !isChecked;
		    	// minuCheckItem.setIcon(isChecked ? R.drawable.checkbox_on : R.drawable.checkbox_off);
		    	 minuCheckItem.setIcon(ActivityUtils.getIcon(this, isChecked ? R.drawable.checkbox_on : R.drawable.checkbox_off, 32));   
		    	
		    	fragment.actionClicked(R.id.btn_checkbox_contact);
		    	return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	public void onContactsSelected(List<ContactInfo> contacts) {
		if(contacts != null && !contacts.isEmpty()){
			Intent returnIntent = new Intent();
			ContactListFragment.Type type = (Type) getIntent().getSerializableExtra("type");
			if(ContactListFragment.Type.ShareContact.equals(type)){ 
				returnIntent.putExtra("contact", contacts.get(0).getContact());
			}else{
				returnIntent.putExtra("recipient", contacts.get(0).getUser());
			}
			setResult(RESULT_OK, returnIntent);
			finish();
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start_chat, menu);
		minuCheckItem = menu.findItem(R.id.action_check);
		minuCheckItem.setIcon(ActivityUtils.getIcon(this, R.drawable.checkbox_on, 32)); 
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		minuCheckItem.setVisible(shareContact ? true : false);
		return super.onPrepareOptionsMenu(menu);
	}
	
	public boolean isChecked(){
		return isChecked;
	}
	
}
