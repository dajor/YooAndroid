package com.fellow.yoo.data.criteria;

public abstract class BaseCriteria implements Criteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2261454265343807595L;
	
	private String column;
	
	public BaseCriteria(String pColumn) {
		column = pColumn;
	}
	
	public String getColumn() {
		return column;
	}
}
