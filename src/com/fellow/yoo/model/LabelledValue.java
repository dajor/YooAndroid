package com.fellow.yoo.model;

import java.util.Locale;

public class LabelledValue {
	private int type;
	private String value;
	
	public LabelledValue(int pType, String pValue) {
		type = pType;
		value = pValue;
	}
	
	public LabelledValue(String pType, String pValue) {
		type = getTypeIndex(pType);
		value = pValue;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String pValue) {
		this.value = pValue;
	}
	
	public int getType() {
		return type;
	}
	
	public String getTypeNameLowerCase(){
		return getTypeName().toLowerCase(Locale.US);
	}
	
	public String getTypeName(){
		String label = "Other";
		if(type == 1){
			label = "Home";
		}else if(type == 2){
			label = "Mobile";
		}else if(type == 3){
			label = "Work";
		}
		return label;
	}
	
	public int getTypeIndex(String type){
    	// andriod : Mobile, Work, Home, Other
    	// IOS : home, work, Iphone, mobile, main
    	type = type.toLowerCase(Locale.US); 
    	if("home".equals(type)){
    		return 1;
    	}else if("mobile".equals(type)){
    		return 2;
    	}else if("work".equals(type)){
    		return 3;
    	}
    	return 0;
    }
    
}
