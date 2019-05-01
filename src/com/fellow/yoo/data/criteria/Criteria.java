package com.fellow.yoo.data.criteria;

import java.io.Serializable;
import java.util.List;

public interface Criteria extends Serializable {
	public String toSql();
	public List<String> getValues();
}
