package com.fellow.yoo.adapter;

import java.util.List;
import java.util.Map;

import com.fellow.yoo.MainActivity;
import com.fellow.yoo.R;
import com.fellow.yoo.YooActivity;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.model.Contact;
import com.fellow.yoo.model.ContactInfo;
import com.fellow.yoo.model.YooGroup;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.RoundImageView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactListAdapter extends SectionAdapter<ContactInfo> {

	
	
	private List<ContactInfo> selected;
	private YooGroup group;
	private Activity activity;
	
	public ContactListAdapter(Activity activity, List<String> sections,
			Map<String, List<ContactInfo>> contacts, 
			List<ContactInfo> pSelected, YooGroup group) {
		super(sections, contacts);
		this.selected = pSelected;
		this.group = group;
		this.activity = activity;
		
	}

	@Override
	public View getView(ContactInfo contact, View row, ViewGroup parent) {
		if (row == null || row.getTag() != contact) {
			LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.row_contact, parent, false);
			row.setTag(contact);
		}
		
		TextView text = (TextView) row.findViewById(android.R.id.text1);
		text.setText(contact.toString());
		
		RoundImageView image = (RoundImageView) row.findViewById(android.R.id.icon);
		RoundImageView iconStatus = (RoundImageView) row.findViewById(R.id.icon_status);
		
		image.setImageResource(R.drawable.ic_user);
		if (contact.getUser() != null) {
			if (contact.getUser().getPicture() != null) {
				byte [] bitmapData = contact.getUser().getPicture();
				Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);			
				image.setImageBitmap(bitmap);
			}
			
			if(ChatTools.sharedInstance().isPresent(contact.getUser())){
				boolean online = ChatTools.sharedInstance().isVisibility(contact.getUser());
				iconStatus.setVisibility(online ? View.VISIBLE : View.GONE); 
			}
			
			if(activity instanceof MainActivity){
				ImageView imageCall = (ImageView) row.findViewById(R.id.btn_call);
				// imageCall.setVisibility(View.VISIBLE); 
				imageCall.setTag(contact.getUser());
				imageCall.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						YooUser yooUser = (YooUser) v.getTag();
						((YooActivity) activity).startCall(yooUser);
					}
				});
			}
		} 
		
		ImageView selImg = (ImageView) row.findViewById(R.id.row_selected);
		if(group != null){
			selImg.setImageResource(R.drawable.green_checkmark); 
		}
		
		selImg.setVisibility(isSelected(contact.getContact()) ? View.VISIBLE : View.GONE);
		
		return row;
	}
	
	private boolean isSelected(Contact contact){
		for (ContactInfo contactInfo : selected) {
			 if(contactInfo.getContact().getContactId() == contact.getContactId()){
				 return true;
			 }
		}
		return false;
	}
	
	
	public List<ContactInfo> getSelected() {
		return selected;
	}

	public void setSelected(List<ContactInfo> selected) {
		this.selected = selected;
	}

	

}
