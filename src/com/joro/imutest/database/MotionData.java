package com.joro.imutest.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MotionData {

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "SignalData";
	private static final String DATA_TABLE_NAME = "signal";
	private static final String KEY_ID = "id";
	private static final String C_TIMESTAMP = "time";
	private static final String C_AXIS_Y_VALUE = "yvalue";
	private static final String C_AXIS_X_VALUE = "xvalue";
	private static final String C_AXIS_Z_VALUE = "zvalue";
	private static final String DATA_TABLE_CREATE = "create table "
			+ DATA_TABLE_NAME + " (" + KEY_ID + " INTEGER PRIMARY KEY, "
			+ C_TIMESTAMP + " text, " + C_AXIS_X_VALUE + " text, "
			+ C_AXIS_Y_VALUE + " text, " + C_AXIS_Z_VALUE + " text)";
	private static final String GET_ORDER_BY = C_TIMESTAMP + " DESC";
	Context mContext;
	SQLiteOpenHelper dbOpenHelper;
	SQLiteDatabase db;

	class MotionDataOpenHelper extends SQLiteOpenHelper {

		public MotionDataOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATA_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}

	public MotionData(Context context) {
		mContext = context;
	}

	public void open() throws SQLException {
		dbOpenHelper = new MotionDataOpenHelper(mContext);
		db = dbOpenHelper.getWritableDatabase();
	}

	public void close() throws SQLException {
		dbOpenHelper.close();
	}

	public long add(float timestamp, float axisX, float axisY,
			float axisZ) {
		ContentValues values = new ContentValues();
		values.put(C_TIMESTAMP, String.valueOf(timestamp));
		values.put(C_AXIS_X_VALUE, String.valueOf(axisX));
		values.put(C_AXIS_Y_VALUE, String.valueOf(axisY));
		values.put(C_AXIS_Z_VALUE, String.valueOf(axisZ));

		return db.insert(DATA_TABLE_NAME, null, values);
	}

	public Cursor getAll() {
		return db.query(DATA_TABLE_NAME, null, null, null, null, null,
				GET_ORDER_BY);
	}

	public void clearAll() {
		db.delete(DATA_TABLE_NAME, null, null);
	}
}
