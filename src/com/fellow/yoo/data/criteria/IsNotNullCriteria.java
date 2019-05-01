package com.fellow.yoo.data.criteria;

import java.util.ArrayList;
import java.util.List;

public class IsNotNullCriteria extends BaseCriteria {

	private static final long serialVersionUID = 1L;
	
	public IsNotNullCriteria(String pColumn) {
		super(pColumn);
	}

	@Override
	public String toSql() {
		return getColumn() + " IS NOT NULL";
	}

	@Override
	public List<String> getValues() {
		return new ArrayList<String>();
	}

	

}
