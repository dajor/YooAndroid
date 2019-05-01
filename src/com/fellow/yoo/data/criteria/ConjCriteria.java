package com.fellow.yoo.data.criteria;

import java.util.ArrayList;
import java.util.List;


public class ConjCriteria implements Criteria {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1148574928380071205L;

	public enum Conjunction {AND, OR};
	
	private List<Criteria> criterias;
	private Conjunction conjunction;
	
	public ConjCriteria(Conjunction pConjunction) {
		criterias = new ArrayList<Criteria>();
		conjunction = pConjunction;
	}

	public int size() {
		return criterias.size();
	}
	
	public void add(Criteria criteria) {
		criterias.add(criteria);
	}
	
	public void add(String column, String value) {
		criterias.add(new EqualsCriteria(column, value));
	}

	@Override
	public List<String> getValues() {
		List<String> values = new ArrayList<String>();
		for (Criteria criteria : criterias) {
			values.addAll(criteria.getValues());
		}
		return values;
	}

	
	@Override
	public String toSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Criteria criteria : criterias) {
			if (sb.length() > 1) {
				sb.append(" " + conjunction.name() + " ");
			}
			sb.append(criteria.toSql());
		}
		sb.append(")");
		return sb.toString();
	}

}
