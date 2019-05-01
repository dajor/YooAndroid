package com.fellow.yoo.data.criteria;

import java.util.Arrays;
import java.util.List;


public class EqualsCriteria extends BaseCriteria {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1470618987825876510L;
	
	private String[] values;
	
	public EqualsCriteria(String column, String[] pValues) {
		super(column);
		values = pValues;
	}
	
	public EqualsCriteria(String column, String pValue) {
		super(column);
		values = new String [] {pValue};
	}

	@Override
	public String toSql() {
		if (values.length == 1) {
			return getColumn() + " = ?";
		}
		String ret = getColumn() + " IN (";
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				ret += ", ";
			}
			ret += "?";
		}
		ret += ")";
		return ret;
	}


	@Override	
	public List<String> getValues() {
		return Arrays.asList(values);
	}



}
