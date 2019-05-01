package com.fellow.yoo.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static DatabaseHelper instance;

	private static final String DB_NAME = "Yoo.DB";
	
	public DatabaseHelper(Context pContext) {
		super(pContext, DB_NAME, null, 1);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	
	public static void init(Context context) {
		instance = new DatabaseHelper(context);
	}
	
	public static DatabaseHelper getInstance() {
		// fail early
		if (instance == null) throw new IllegalStateException("DatabaseHelper not initialized");
		return instance;
	}
	
}
