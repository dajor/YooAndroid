package com.fellow.yoo.data.criteria;

import java.util.Arrays;
import java.util.List;


public class NotEqualsCriteria extends BaseCriteria {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -5906438601804240426L;
	
	private String param;
	
	public NotEqualsCriteria(String pColumn, String pParam) {
		super(pColumn);
		param = pParam;
	}

	@Override
	public String toSql() {
		return "(" + getColumn() + " != ? OR " + getColumn() + " IS NULL)" ;
	}


	@Override
	public List<String> getValues() {
		return Arrays.asList(param);
	}

}
