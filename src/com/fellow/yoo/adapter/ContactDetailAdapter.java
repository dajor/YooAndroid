package com.fellow.yoo.adapter;

import java.util.List;
import java.util.Map;

import com.fellow.yoo.utils.StringUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ContactDetailAdapter extends SectionAdapter<String []> {

	public ContactDetailAdapter(List<String> sections, Map<String, List<String []>> recipients) {
		super(sections, recipients);
	}
	
	@Override
	public View getView(String [] item, View row, ViewGroup parent) {
		if (row == null || row.getTag() != item) {
			LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
			row.setTag(item);
		}
		
		TextView tvLabel = (TextView) row.findViewById(android.R.id.text1);
		TextView tvValue = (TextView) row.findViewById(android.R.id.text2);
	
		tvLabel.setText(item[0]);
		tvValue.setText(item[1]);
		
		if(StringUtils.isEmpty(item[0])){
			tvLabel.setVisibility(View.GONE); 
		}

		return row;
	}

}
