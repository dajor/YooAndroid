package com.fellow.yoo.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.fellow.yoo.data.criteria.Criteria;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;


public abstract class BaseDAO {
	
	
	private SQLiteDatabase database;
	
	public BaseDAO() {
		database = DatabaseHelper.getInstance().getWritableDatabase();
	}
	
	protected SQLiteDatabase getDatabase() {
		return database;
	}
	
	
	public abstract void initTables();
	
	protected void createTable(String table, Map<String, String> columns) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE " + table + "(");
		Boolean first = true;
		for (String column : columns.keySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(column + " " + columns.get(column));
		}
		sb.append(")");
		execSQL(sb.toString());
	}
	
	protected void checkTable(String table, Map<String, String> columns) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		boolean first = true;
		for (String column : columns.keySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(column);
		}
		sb.append(" FROM " + table);
		sb.append(" LIMIT 1");
		try {
			Cursor cursor = database.rawQuery(sb.toString(), null);
			cursor.moveToFirst();
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}

		} catch (SQLiteException e1) {
			// add the missing columns
			List<String> missing = new ArrayList<String>();
			for (String column : columns.keySet()) {
				try {
					rawQuery("SELECT " + column + " FROM " + table + " LIMIT 1", null);
				} catch (SQLiteException e2) {
					missing.add(column);
				}
			}
			if (missing.size() == columns.size()) {
				createTable(table, columns);
			} else {
				for (String column : missing) {
					addColumn(table, column, columns.get(column));
				}
			}
		}
	}
	
	protected void addColumn(String table, String column, String type) {
		execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
	}
	
	protected void createIndex(String table, boolean unique, List<String> columns) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE ");
		if (unique) {
			sb.append("UNIQUE ");
		}
		sb.append("INDEX IF NOT EXISTS idx_" + table);
		for (String column : columns) {
			sb.append("_" + column);
		}
		sb.append(" ON " + table + "(");
		boolean first = true;
		for (String column : columns) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(column);
		}
		sb.append(")");
		execSQL(sb.toString());
	}
	
	protected List<Map<String, String>> select(String table, Criteria criteria, String orderBy, int count) {
		List<String> bindArgs = getBindArgs(criteria);
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM " + table);
		sb.append(getWhere(criteria));
		if (orderBy != null && orderBy.length() > 0) {
			sb.append(" ORDER BY " + orderBy );
		}
		if (count > 0) {
			sb.append(" LIMIT " + count);
		}
		
		return rawQuery(sb.toString(), bindArgs.toArray(new String [] {}));
	}
	
	protected int selectCount(String sql) {
		Log.d("BaseManager", "PSQL: " + fillParameters(sql, new String[]{}));
		Cursor cursor = database.rawQuery(sql, new String[]{});
		
		return cursor.getCount();
	}
	
	protected Map<String, String> selectOne(String table, Criteria criteria) {
		List<Map<String, String>> records = select(table, criteria, null, 1);
		if (records.size() > 0) {
			return records.get(0);
		}
		return null;
	}
	
	protected void execSQL(String query) {
		Log.d("BaseManager", "SQL: " + query);
		database.execSQL(query);
	}
	
	protected void execSQL(String query, String [] bindArgs) {
		Log.d("BaseManager", "PSQL: " + fillParameters(query, bindArgs));
		database.execSQL(query, bindArgs);
	}
	
	protected List<Map<String, String>> rawQuery(String query, String [] bindArgs) {
		Log.d("BaseManager", "PSQL: " + fillParameters(query, bindArgs));
		List<Map<String, String>> records = new ArrayList<Map<String, String>>();
		Cursor cursor = database.rawQuery(query, bindArgs);
		if (cursor.moveToFirst()) {
			do {
				Map<String, String> record = new HashMap<String, String>();
				for (int j = 0; j < cursor.getColumnCount(); j++) {
					record.put(cursor.getColumnName(j), cursor.getString(j));
				}
				records.add(record);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return records;
	}
	
	protected void update(String table, Map<String, String> item, Criteria criterias) {
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE " + table + " SET ");
		List<String> bindArgs = new ArrayList<String>();
		boolean first = true;
		for (String field : item.keySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(field + " = ?");
			bindArgs.add(item.get(field));
		}
		sb.append(getWhere(criterias));
		bindArgs.addAll(getBindArgs(criterias));
		execSQL(sb.toString(), bindArgs.toArray(new String [] {}));
	}
	
	protected long insert(String table, Map<String, String> item) {
		Log.d("BaseManager", "Insert " + dump(item));
		ContentValues values = new ContentValues();
		for (String field : item.keySet()) {
			values.put(field, item.get(field));
		}
		return database.insert(table, null, values);
	}
	
	protected void delete(String table, Criteria criteria) {
		String query = "DELETE FROM " + table + getWhere(criteria);
		execSQL(query, getBindArgs(criteria).toArray(new String [] {}));
	}
	
	private String getWhere(Criteria criterias) {
		if (criterias != null) {
			return " WHERE " + criterias.toSql();
		} else {
			return "";
		}
	}

	private List<String> getBindArgs(Criteria criterias) {
		List<String> bindArgs = new ArrayList<String>();
		if (criterias != null) {
			bindArgs.addAll(criterias.getValues());
		}
		return bindArgs;
	}
	
	private String fillParameters(String query, String [] bindArgs) {
		if (bindArgs != null) {
			try {
				for (String arg : bindArgs) {
					if (arg == null) {
						query = query.replaceFirst("\\?", "null");
					} else {
						query = query.replaceFirst("\\?", Matcher.quoteReplacement("'" + arg + "'"));
					}
				}
			} catch (Exception e) {
				Log.w("BaseManager", "Error formatting logs : " + e.getMessage());
			}
		}
		return query;
	}
	
	protected static String dump(Map<String, String> item) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (String field : item.keySet()) {
			if (item.containsKey(field)) {
				if (sb.length() > 1) {
					sb.append(", ");
				}
				sb.append(field).append(" : ").append(item.get(field));
			}
		}
		sb.append(']');
		return sb.toString();
	}
}
