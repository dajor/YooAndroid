package com.fellow.yoo.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fellow.yoo.R;
import com.fellow.yoo.YooApplication;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public abstract class SectionAdapter<A> extends BaseAdapter {
	
	protected final static List<String> customTexts = 
			Arrays.asList("User Information", "Chat Settings" ,"Statistics" ,"Background", "Statistics", "Messages", "Networks", 
					"Profile Name", "Status");
	
	private List<String> sections;
	private Map<String, List<A>> items;
	private SparseArray<String> headers;
	private List<A> data;
	
	
	public SectionAdapter(List<String> pSections, Map<String, List<A>> pItems) {
		sections = pSections;
		items = pItems;
		data = new ArrayList<A>();
		headers = new SparseArray<String>();
		computeSections();
	}
	
	private void computeSections() {
		data.clear();
		headers.clear();
		for (String section : sections) {
			headers.put(data.size(), section);
			data.add(null);
			data.addAll(items.get(section));
		}
	}
	
	@Override
	public void notifyDataSetChanged() {
		computeSections();
		super.notifyDataSetChanged();
	}


	@Override
	public int getCount() {
		return data.size();
	}

	
	@Override
	public A getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	public abstract View getView(A item, View row, ViewGroup parent);
	
	@Override
	public boolean isEnabled(int position) {
		String header = headers.get(position);
		return header == null;
	}

	public View getView(int position, View row, ViewGroup parent) {
		String header = headers.get(position);
		if (header != null) {
			header = getCustomText(header);
			if (row == null || row.getTag() != header) {
				LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.row_header, parent, false);
				row.setTag(header);
			}
			TextView text = (TextView) row.findViewById(android.R.id.text1);
			text.setText(formatHeader(header));
		} else {
			A item = getItem(position);
			row = getView(item, row, parent);
		}
		return row;
	}

	// override this to format the header
	protected String formatHeader(String header) {
		return header;
	}
	
	
	
	public static String getCustomText(String textValue){
		if(customTexts.contains(textValue)){
			if(YooApplication.getAppContext() != null){
				Context context = YooApplication.getAppContext();
				int key = -1;
				if("User Information".equals(textValue)){
					key = R.string.user_information;
				}else if("Chat Settings".equals(textValue)){
					key = R.string.chat_settings;
				}else if("Statistics".equals(textValue)){
					key = R.string.statistics;
				}else if("Background".equals(textValue)){
					key = R.string.background;
				}else if("Statistics".equals(textValue)){
					key = R.string.statistics;
				}else if("Messages".equals(textValue)){
					key = R.string.stats_messages;
				}else if("Networks".equals(textValue)){
					key = R.string.stats_network;
				}else if("Profile Name".equals(textValue)){
					key = R.string.nickname;
				}else if("Status".equals(textValue)){
					key = R.string.user_status;
				}
				if(key != -1){
					return context.getString(key);
				}
			};
		}
		
		return textValue;
	}

}
