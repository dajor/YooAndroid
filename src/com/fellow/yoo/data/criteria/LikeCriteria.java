package com.fellow.yoo.data.criteria;

import java.util.ArrayList;
import java.util.List;

public class LikeCriteria extends BaseCriteria {

	private static final long serialVersionUID = 2949768517364653081L;
	String value;
	public LikeCriteria(String column, String value) {
		super(column);
		this.value = value;
	}

	@Override
	public String toSql() {
		return getColumn() + " LIKE ?" ;
	}

	@Override
	public List<String> getValues() {
		List<String> tmp = new ArrayList<String>();
		tmp.add(value + "%");
		return tmp;
	}

}
