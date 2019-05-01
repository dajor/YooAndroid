package com.fellow.yoo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class StringUtils {
	
	public static String toString(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder s = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			s.append(line + "\n");
		}
		return s.toString();
	}
	
	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}
	
	public static String uppercaseFirst(String s) {
		if (s == null || s.length() < 1) return s;
		return s.substring(0, 1).toUpperCase(Locale.US) + s.substring(1);
	}

	

}
