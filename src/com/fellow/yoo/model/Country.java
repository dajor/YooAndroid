package com.fellow.yoo.model;

public class Country implements Comparable<Country> {

	private String code;
	private String name;

	public Country(String code, String name) {
		super();
		this.code = code;
		this.name = name;
	}

	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}



	@Override
	public int compareTo(Country another) {
		return name.compareTo(another.name);
	}

}
