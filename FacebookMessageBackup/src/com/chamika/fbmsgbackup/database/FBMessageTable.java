package com.chamika.fbmsgbackup.database;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.chamika.fbmsgbackup.model.FBMessage;
import com.chamika.fbmsgbackup.utils.AppLogger;

public class FBMessageTable extends SQLiteOpenHelper {

	private static final String TAG = "FBMessageTable";

	private String threadId;
	private String tableName;

	private static final String DATABASE_NAME = "msgbackup.db";
	private static final int DATABASE_VERSION = 2;

	// Database table
	public static final String TABLE_PREFIX = "thread_";
	public static final String COLUMN_ID = "_id";

	public static final String COLUMN_BODY = "body";
	public static final String COLUMN_MESSAGE_ID = "messageId";
	public static final String COLUMN_TIME = "time";
	public static final String COLUMN_AUTHOR = "author";

	private static final String SELECTION_IN = COLUMN_ID + " BETWEEN ? AND ? ";
	private static final String ORDER_BY_ID = COLUMN_ID + " ASC";

	public FBMessageTable(Context context, String threadId) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.threadId = threadId;
		this.tableName = TABLE_PREFIX + threadId;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		// Database creation SQL statement
		// String DATABASE_CREATE_QUERY = "CREATE TABLE IF NOT EXISTS " +
		// tableName + "(" + COLUMN_ID
		// + " integer primary key autoincrement, " + COLUMN_BODY + " text , " +
		// COLUMN_MESSAGE_ID
		// + " text not null," + COLUMN_AUTHOR + " text not null," + COLUMN_TIME
		// + " text not null" + ");";

		String DATABASE_CREATE_QUERY = getTableCreateQuery();

		database.execSQL(DATABASE_CREATE_QUERY);

	}

	private String getTableCreateQuery() {
		return "CREATE TABLE IF NOT EXISTS " + tableName + "(" + COLUMN_ID + " integer primary key, " + COLUMN_BODY
				+ " text , " + COLUMN_MESSAGE_ID + " text not null," + COLUMN_AUTHOR + " text not null," + COLUMN_TIME
				+ " text not null" + ");";
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			AppLogger.log(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_PREFIX + threadId);
		onCreate(database);
	}

	public void createTableIfNotExist() {
		SQLiteDatabase db = null;

		try {
			db = this.getWritableDatabase();
			db.execSQL(getTableCreateQuery());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.close();
		}

	}

	/**
	 * returns cursor with data order - time, authorId,message body
	 * 
	 * @param threadId
	 * @param lower
	 * @param upper
	 * @return null if error occurred
	 */
	public Cursor getExportMessages(String threadId, long lower, long upper) {
		SQLiteDatabase db = this.getReadableDatabase();

		try {
			Cursor cursor = db.query(this.tableName, new String[] { COLUMN_TIME, COLUMN_AUTHOR, COLUMN_BODY },
					SELECTION_IN, new String[] { String.valueOf(lower), String.valueOf(upper) }, null, null,
					ORDER_BY_ID);

			if (cursor != null) {
				cursor.moveToFirst();
			}

			return cursor;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void closeReadableDatabase() {
		this.getReadableDatabase().close();
	}

	public void addAll(List<FBMessage> messages, long startIndex) {
		SQLiteDatabase db = null;

		try {
			db = this.getWritableDatabase();
			if (db != null && messages != null) {
				ContentValues values = new ContentValues();
				db.beginTransaction();

				for (FBMessage msg : messages) {
					values.put(COLUMN_ID, startIndex++);
					values.put(COLUMN_BODY, msg.getBody());
					values.put(COLUMN_AUTHOR, msg.getAuthor());
					values.put(COLUMN_MESSAGE_ID, msg.getMessageId());
					values.put(COLUMN_TIME, msg.getFormattedTime());

					db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
					values.clear();
				}

				db.setTransactionSuccessful();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (db != null && db.inTransaction()) {
				db.endTransaction();
				db.close();
			}
		}

	}
}
