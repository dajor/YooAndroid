package com.fellow.yoo.adapter;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SettingsAdapter extends SectionAdapter<String []> {

	public SettingsAdapter(List<String> sections, Map<String, List<String []>> items) {
		super(sections, items);
	}
	
	@Override
	public View getView(String [] item, View row, ViewGroup parent) {
		if (row == null || row.getTag() != item) {
			LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
			row.setTag(item);
		}
		TextView text1 = (TextView) row.findViewById(android.R.id.text1);
		text1.setText(getCustomText(item[0]));
		TextView text2 = (TextView) row.findViewById(android.R.id.text2);
		text2.setText(item[1]);

		return row;
	}

}
