package com.android.joro.testapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

//**Class wich is responsible for storing the results data in a database.
//**Database helper is written here, and other methods that allow the access of database
//**information.
/**
 * @author Joro
 *
 */
public class ResultsData {
	private static final String TAG = "Results Data";

	// Database info
	static final int VERSION = 1;
	static final String DATABASE = "results.db";
	static final String TABLE = "results";

	// Column names of the database
	// Most likely will not need an ID. I can organize them by the created_at
	// key
	public static final String C_ID = "_id";
	public static final String C_CREATED_AT = "created_at";
	public static final String C_PULSE = "pulse";
	public static final String C_OXY = "oxy";
	public static final String C_USER = "user";
	public static final String C_UPLOADED = "uploaded";

	// Useful shortcut strings and arrays
	private static final String GET_ALL_ORDER_BY = C_CREATED_AT + " DESC";
	private static final String[] MAX_CREATED_AT_COLUMNS = { "max("
			+ ResultsData.C_CREATED_AT + ")" };
	private static final String[] DB_TEXT_COLUMNS = { C_ID, C_CREATED_AT,
			C_USER, C_PULSE, C_OXY };

	private DbHelper mDbHelper;
	private SQLiteDatabase db;
	private final Context mCtx;

	// Database Helper
	/**
	 * @author Joro
	 *
	 */
	class DbHelper extends SQLiteOpenHelper {

		/**
		 * @param context
		 */
		public DbHelper(Context context) {
			super(context, DATABASE, null, VERSION);
			// TODO Auto-generated constructor stub
		}

		
		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			Log.i(TAG, "Creating database: " + DATABASE);
			db.execSQL("create table " + TABLE + " (" + C_ID
					+ " integer primary key autoincrement, " + C_CREATED_AT
					+ " text, " + C_USER + " text, " + C_PULSE + " text, "
					+ C_OXY + " text, " + C_UPLOADED + " text)");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			db.execSQL("drop table if exists " + TABLE);
			this.onCreate(db);
		}

	}

	// Constructor of Results Data
	/**
	 * @param context
	 */
	public ResultsData(Context context) {
		this.mCtx = context;
		Log.i(TAG, "db initialized");
	}

	// Sets dbHelper and the database
	/**
	 * @return
	 * @throws SQLException
	 */
	public ResultsData open() throws SQLException {
		mDbHelper = new DbHelper(mCtx);
		db = mDbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * 
	 */
	public void close() {
		this.mDbHelper.close();

	}

	/**
	 * @param created
	 */
	public void uploadedResults(String[] created) {
		/*
		 * String command = new String("UPDATE "+TABLE+" SET " + C_UPLOADED +
		 * " = true " + "WHERE " + C_CREATED_AT + " = " + created);
		 * db.execSQL(command);
		 */

		ContentValues values = new ContentValues();
		values.put(C_UPLOADED, "true");
		String where = new String(C_CREATED_AT + " = ?");
		db.update(TABLE, values, where, created);
	}

	// @return rowId or -1 if failed
	/**
	 * @param createdAt
	 * @param user
	 * @param pulse
	 * @param oxy
	 * @param uploaded
	 * @return
	 */
	public long addResult(String createdAt, String user, String pulse,
			String oxy, String uploaded) {

		ContentValues values = new ContentValues();
		values.put(C_CREATED_AT, createdAt);
		values.put(C_USER, user);
		values.put(C_PULSE, pulse);
		values.put(C_OXY, oxy);
		values.put(C_UPLOADED, uploaded);
		return db.insert(TABLE, null, values);

	}

	/*
	 * public long insertOrIgnore(ContentValues values){ Log.d(TAG,
	 * "InsertOrIgnore on " +values);
	 * 
	 * try{ return db.insertWithOnConflict(TABLE, null, values,
	 * SQLiteDatabase.CONFLICT_IGNORE); }finally{ db.close(); } }
	 */

	// @return cursor of databse results organized by time
	/**
	 * @return
	 */
	public Cursor getResultsByTime() {
		return db.query(TABLE, null, null, null, null, null, GET_ALL_ORDER_BY);
	}
		
	/**
	 * @return
	 */
	public Cursor getResultsByUploadStatus() {
		String sql = new String("SELECT " + C_CREATED_AT + ", " + C_PULSE
				+ ", " + C_OXY + ", " + C_UPLOADED + " " + "FROM " + TABLE
				+ " " + "WHERE " + C_UPLOADED + " = ? " + "ORDER BY "
				+ GET_ALL_ORDER_BY);
		String[] argument = new String[] { "false" };

		return db.rawQuery(sql, argument);
	}

	// @return cursor pointing to the last recorded status (by time)
	// This may not be used but is good to have just in case.
	/**
	 * @return
	 */
	public long getLatestStatusAtTime() {
		SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
		try {
			// This asks for the latest time
			Cursor cursor = db.query(TABLE, MAX_CREATED_AT_COLUMNS, null, null,
					null, null, null);
			try {
				// The moveToNext method will return false
				// if the cursor is already past the last entry in the result
				// set.
				return cursor.moveToNext() ? cursor.getLong(0) : Long.MIN_VALUE;

			} finally {
				cursor.close();
			}

		} finally {
			db.close();
		}
	}

	// @return cursor pointing to result of given row id
	/**
	 * @param rowId
	 * @return
	 * @throws SQLException
	 */
	public Cursor fetchResult(long rowId) throws SQLException {

		Cursor mCursor =

		db.query(true, TABLE, DB_TEXT_COLUMNS, C_ID + "=" + rowId, null, null,
				null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/** 
	 * @return
	 */
	public boolean dataToUpdate() {
		Cursor mCursor = db.rawQuery("SELECT " + C_CREATED_AT + ", "
				+ C_UPLOADED + " " + "FROM " + TABLE + " " + "WHERE "
				+ C_UPLOADED + " = ? " + "ORDER BY " + GET_ALL_ORDER_BY,
				new String[] { "false" });

		return mCursor.moveToFirst();
	}

	/**
	 * 
	 */
	public void clearResults() {
		db.delete(TABLE, null, null);
	}

	// CHANGES WILL NEED TO BE MADE TO THIS METHOD!
	/*
	 * public String getStatusTextById(long id){ SQLiteDatabase db =
	 * this.mDbHelper.getReadableDatabase(); try{ Cursor cursor =
	 * db.query(TABLE, DB_TEXT_COLUMNS, C_ID + "=" + id, null, null, null,
	 * null); try{ //The moveToNext method will return false //if the cursor is
	 * already past the last entry in the result set. return cursor.moveToNext()
	 * ? cursor.getString(0): null;
	 * 
	 * }finally{ cursor.close(); }
	 * 
	 * }finally{ db.close(); } }
	 */

}
