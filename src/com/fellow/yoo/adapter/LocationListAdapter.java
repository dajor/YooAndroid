package com.fellow.yoo.adapter;

import java.util.List;
import java.util.Map;

import com.fellow.yoo.R;
import com.fellow.yoo.model.YooLocation;
import com.fellow.yoo.utils.StringUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class LocationListAdapter extends SectionAdapter<YooLocation> {

	
	public LocationListAdapter(List<String> sections, Map<String, List<YooLocation>> locations) {
		super(sections, locations);
	}

	@Override
	public View getView(YooLocation location, View row, ViewGroup parent) {
		if (row == null || row.getTag() != location) {
			LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.row_location, parent, false);
			row.setTag(location);
		}
		
		TextView text1 = (TextView) row.findViewById(android.R.id.text1);
		TextView text2 = (TextView) row.findViewById(android.R.id.text2);
		text1.setText(location.getName());
		text2.setText(location.getAddress());
		if(StringUtils.isEmpty(location.getAddress())){
			text2.setVisibility(TextView.GONE);
		}
		
		ImageView image = (ImageView) row.findViewById(android.R.id.icon);
		image.setImageResource(R.drawable.ic_latitude);
		image.setImageResource(location.getIcon(location.getType())); 
		return row;
	}

}
