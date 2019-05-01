package com.fellow.yoo.fragment;

import com.fellow.yoo.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public abstract class YooListFragment extends YooFragment {
	
	private RelativeLayout topHeader;
	private ListView listview;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	     super.onCreateView(inflater, container, savedInstanceState);
	     
	    View view = inflater.inflate(R.layout.fragment_list, container, false);
	    topHeader = (RelativeLayout) view.findViewById(R.id.top_header);
	    listview = (ListView) view.findViewById(android.R.id.list);
	    
	    TextView emptyView = (TextView) inflater.inflate(R.layout.row_empty, container, false);
	    listview.setEmptyView(emptyView);
	    
		listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
				YooListFragment.this.onListItemClick(adapter, view, position, arg); 
			}
		});
	     
	    return view;
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	
	protected ListView getListView(){
		return listview;
	}
	
	protected RelativeLayout getTopHeader(){
		return topHeader;
	}
	
	public void onListItemClick(AdapterView<?> adapter, View v, int position, long id) {
		
	}

}
