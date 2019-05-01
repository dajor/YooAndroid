package com.fellow.yoo.adapter;

import java.util.List;

import com.fellow.yoo.R;
import com.fellow.yoo.YooApplication;
import com.fellow.yoo.chat.ChatTools;
import com.fellow.yoo.data.ChatDAO;
import com.fellow.yoo.model.MenuOption;
import com.fellow.yoo.model.YooUser;
import com.fellow.yoo.utils.ActivityUtils;
import com.fellow.yoo.utils.BadgeUtils;
import com.fellow.yoo.utils.RoundImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class MenuAdapter extends ArrayAdapter<MenuOption> {

	protected Context context;
	public MenuAdapter(Context context, List<MenuOption> options) {
		super(context, R.layout.menu_row, options);
		
		this.context = context;
	}

	@Override
	public MenuOption getItem(int position) {
		return super.getItem(position);
	}

	public View getView(int position, View row, ViewGroup parent) {
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.menu_row, parent, false);
		}

		MenuOption option = getItem(position);
		ImageView imageForward = (ImageView) row.findViewById(R.id.row_forward);
		TextView chatCount = (TextView) row.findViewById(R.id.chat_count);
		

		imageForward.setVisibility(View.GONE); 
		chatCount.setVisibility(View.GONE);
		
		
		if (((ListView) parent).isItemChecked(position) && option.getLabel() != R.string.recent) {
			row.setBackgroundColor(parent.getResources().getColor(R.color.transparentTheme_color));
		} else {
			row.setBackgroundColor(parent.getResources().getColor(R.color.darkgray));
		}

		if(option.getLabel() == R.string.chat_list){
			int unreadChats = new ChatDAO().countUnreads();
			if (unreadChats > 0) {
				chatCount.setVisibility(TextView.VISIBLE);
				chatCount.setText(String.valueOf(unreadChats));
			}else{
				chatCount.setVisibility(TextView.GONE);
			}
			BadgeUtils.setBadge(context, unreadChats);
		}	
		

		LinearLayout iconlayout = (LinearLayout) row.findViewById(R.id.menuIconLayout);
		ImageView image = new ImageView(getContext()); 
		TextView label = (TextView) row.findViewById(R.id.menuText);
		TextView label2 = (TextView) row.findViewById(R.id.menuSubText);
		label2.setText(""); 

		int iconWidth = ActivityUtils.dpToPixels(getContext(), 40);
		int sp = ActivityUtils.dpToPixels(getContext(), 10);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(iconWidth, iconWidth);
		if (option.getLabel() == R.string.action_settings) {
			iconWidth = ActivityUtils.dpToPixels(getContext(), 55);
			params = new LinearLayout.LayoutParams(iconWidth, iconWidth);
			params.setMargins(sp, sp, sp, sp);
			image = new RoundImageView(getContext());			
			image.setLayoutParams(params);
			image.setImageResource(R.drawable.ic_user); 
			YooUser yooUser = YooApplication.getUserLogin();
			String nickname = yooUser.getAlias();
			String status = ChatTools.sharedInstance().getStatus();
			
			int rowHight = ActivityUtils.dpToPixels(getContext(), 73);
			row.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, rowHight));
			if (yooUser != null && yooUser.getPicture() != null) {
				byte [] bitmapData = yooUser.getPicture();
				Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);			
				image.setImageBitmap(bitmap);
			} 	
			
			label.setText(nickname);
			label2.setText(status);

		}else if (option.getLabel() == R.string.recent || option.getLabel() == R.string.recent_item) {
			iconlayout.setVisibility(View.GONE);
			
			if (option.getRecentItem() != null) {
				label.setText(option.getLabelString());
				label.setPadding(sp*2, 0, sp, 0);  
				// row.setBackgroundColor(Color.DKGRAY); 
			}else{
				label.setText(R.string.recent); 
				label.setPadding(sp, sp*2, sp, sp);   
				label.setTextColor(Color.GRAY); 
			}
		}else {
			params.setMargins(sp, sp, sp, sp);
			image.setImageResource(option.getImage());
			label.setText(getContext().getString(option.getLabel()));
		}
		
		image.setLayoutParams(params);
		iconlayout.addView(image);

		return row;
	}

}
