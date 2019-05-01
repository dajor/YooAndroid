package com.fellow.yoo.adapter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fellow.yoo.MainActivity;
import com.fellow.yoo.R;
import com.fellow.yoo.YooActivity;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.model.Chat;
import com.fellow.yoo.model.YooRecipient;
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

public class ChatListAdapter extends SectionAdapter<Chat> {
	
	private Activity activity;

	public ChatListAdapter(Activity activity, List<String> sections, Map<String, List<Chat>> recipients) {
		super(sections, recipients);
		this.activity = activity;
	}

	@Override
	public View getView(Chat chat, View row, ViewGroup parent) {		
		/*if (row == null) {
			LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.row_chat, parent, false);
			row.setTag(chat); 
		}*/
		
		LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		row = inflater.inflate(R.layout.row_chat, parent, false);
		
		TextView text1 = (TextView) row.findViewById(android.R.id.text1);
		TextView text2 = (TextView) row.findViewById(android.R.id.text2);
		
		text1.setText(chat.getRecipient().toString());
		if(text2 != null){
			if (chat.getLastMsg() != null) {
				text2.setText(chat.getLastMsg().getMessage());
				text2.setVisibility(View.VISIBLE);
			} else {
				text2.setVisibility(View.GONE);
			}
		}
		
		TextView badge = (TextView) row.findViewById(R.id.badge);
		if(badge != null){
			if (chat.getUnread() > 0) {
				badge.setText(String.valueOf(chat.getUnread()));
				badge.setVisibility(View.VISIBLE);
			} else {
				badge.setVisibility(View.GONE);
			}
		}
		
		RoundImageView image = (RoundImageView) row.findViewById(android.R.id.icon);
		RoundImageView iconStatus = (RoundImageView) row.findViewById(R.id.icon_status);
		if(image != null){
			if (chat.getRecipient() instanceof YooUser) {
				YooUser yooUser = (YooUser)chat.getRecipient();
				if (yooUser.getPicture() != null) {
					byte [] bitmapData = ((YooUser)chat.getRecipient()).getPicture();
					Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);			
					image.setImageBitmap(bitmap);
				} else {
					image.setImageResource(R.drawable.ic_user);				
				}
				
				if(ChatTools.sharedInstance().isPresent(yooUser)){
					boolean online = ChatTools.sharedInstance().isVisibility(yooUser);
					iconStatus.setVisibility(online ? View.VISIBLE : View.GONE); 
				}
				
			} else {
				image.setImageResource(R.drawable.group_icon);
			}
			
			if(activity instanceof MainActivity){
				ImageView imageCall = (ImageView) row.findViewById(R.id.btn_call);
				imageCall.setVisibility(View.VISIBLE); 
				imageCall.setTag(chat.getRecipient());
				imageCall.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						YooRecipient yooUser = (YooRecipient) v.getTag();
						((YooActivity) activity).startCall(yooUser);
					}
				});
			}
		}
		 
		// iconStatus.setVisibility(chat.isOnline() ? View.VISIBLE : View.GONE); 
		
		return row;
	}

	@Override
	protected String formatHeader(String header) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
		Date date = null;
		try {
		    date = sdf.parse(header);
		} catch (ParseException e) {
			return "#";
		}
		DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(YooApplication.getAppContext());
		return dateFormat.format(date);
	}

	


}
