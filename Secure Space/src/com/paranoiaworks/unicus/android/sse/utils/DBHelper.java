package com.paranoiaworks.unicus.android.sse.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.paranoiaworks.unicus.android.sse.dao.ApplicationStatusBean;

/**
 * Provides all SQL Database related services
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.1.1
 */ 
public class DBHelper {

	private static SQLiteDatabase db = null;
	private static Context ct = null;
	private static Lock lock = new ReentrantLock(); //over restricted - but some Android versions have quite odd "DB related concurrency behavior"
	
	/** Initialize DB and return SQLiteDatabase object, if exists already - return the existing one */
	public static SQLiteDatabase initDB(Context context)
	{
		lock.lock();
		try {
			if (db != null && db.isOpen()) return db;

			db = context.openOrCreateDatabase("SSE_APP.db", Context.MODE_PRIVATE, null);
			ct = context;
			createTables();
		} finally {
			lock.unlock();
		}
		return db;
	}
	
	/** Get SQLiteDatabase object */
	public static boolean isDBReady()
	{
		lock.lock();
		try {
			return db == null ? false : true;
		} finally {
			lock.unlock();
		}	
	}
	
	/** Get Context */
	public static Context getContext()
	{
		return ct;
	}
	
	
	/** Close DB and clear other related object references */
	public static void killDB()
	{
		lock.lock();
		try {
			if(db != null)
			{
				db.close();
				db = null;
			}
			ct = null;
		} finally {
			lock.unlock();
		}		
	}

	
	/** Get binary data from BLOB_REP table - used mainly for serialized objects */
	public static byte[] getBlobData(String id)
	{	
		return getBlobData(id, null);
	}
	
	public static byte[] getBlobData(String id, StringBuffer hashStamp)
	{		
		lock.lock();
		try {
			StringBuffer sql = new StringBuffer();
			String[] ida = {id};
			
			sql.append(" select BLOBDATA, STAMPHASH from BLOB_REP ");
			sql.append(" where ID = ? ");
			Cursor cursor = db.rawQuery(sql.toString(), ida);
			if (cursor.getCount() < 1) 
			{
				cursor.close();
				return null;
			}
			cursor.moveToNext();
			byte[] output = cursor.getBlob(cursor.getColumnIndex("BLOBDATA"));
			if(hashStamp != null) hashStamp.append(cursor.getString(cursor.getColumnIndex("STAMPHASH")));
			cursor.close();
			if(output == null) output = new byte[1]; // in case of "DB wrong import" inconsistent
			return output;
		} finally {
			lock.unlock();
		}		
	}
	
	
	/** Delete data from BLOB_REP table */
	public static void deleteBlobData(String id)
	{		
		lock.lock();
		try {
			String sql = "delete from BLOB_REP where ID = ?";
			Object[] obj = {id};
			db.execSQL(sql.toString(), obj);
		} finally {
			lock.unlock();
		}	
	}
	
	
	/** Insert or Update(if exists) data in BLOB_REP table */
	public static void insertUpdateBlobData(String id, byte[] input)
	{
		insertUpdateBlobData(id, input, null);
	}
	
	public static void insertUpdateBlobData(String id, byte[] input, String stampHash)
	{
		lock.lock();
		try {
			StringBuffer sql = new StringBuffer();
			
			Object[] obj = {input, stampHash, id };
			if(getBlobData(id) == null)
			{
				sql.append("insert into BLOB_REP (BLOBDATA, STAMPHASH, ID) values(?, ?, ?);");
			} 
			else 
			{
				sql.append("update BLOB_REP set BLOBDATA = ?, STAMPHASH = ?, TIMESTAMP = current_timestamp where id = ?;");
			}
			db.execSQL(sql.toString(), obj);
		} finally {
			lock.unlock();
		}	
	}
	
	
	/** Get list of message names stored in DB - for Message Encryptor */
	public static List<String> getMessageNames()
	{	
		lock.lock();
		Cursor cursor = null;
		try {
			ArrayList<String> names = new ArrayList<String>();
			StringBuffer sql = new StringBuffer();
			
			sql.append(" select NAME from MESSAGE_ARCHIVE order by lower(NAME)");
			
			cursor = db.rawQuery(sql.toString(), null);
			if (cursor.getCount() < 1) return null;
			int columnIndex = cursor.getColumnIndex("NAME");
			while (cursor.moveToNext())
			{
				names.add(cursor.getString(columnIndex));
			}
			return names;		
		} finally {
			cursor.close();
			lock.unlock();
		}		
	}
	
	
	/** Get message from DB - for Message Encryptor */
	public static String getMessage(String name) throws UnsupportedEncodingException
	{		
		lock.lock();
		try {
			StringBuffer sql = new StringBuffer();
			String[] ida = {name};
			
			sql.append(" select MESSAGE from MESSAGE_ARCHIVE ");
			sql.append(" where NAME = ? ");
			Cursor cursor = db.rawQuery(sql.toString(), ida);
			if (cursor.getCount() < 1) return null;
			cursor.moveToNext();
			byte[] output = cursor.getBlob(cursor.getColumnIndex("MESSAGE"));
			cursor.close();
			return new String(output, "UTF8");
		} finally {
			lock.unlock();
		}	
	}
	
	
	/** Delete message from DB - for Message Encryptor */
	public static void deleteMessage (String name)
	{		
		lock.lock();
		try {
			StringBuffer sql = new StringBuffer();
			Object[] obj = {name};
			
			sql.append(" delete from MESSAGE_ARCHIVE ");
			sql.append(" where NAME = ? ");
			db.execSQL(sql.toString(), obj);
		} finally {
			lock.unlock();
		}
	}
	
	
	/** Insert message to DB - for Message Encryptor */
	public static void insertMessage(String name, String message)
	{	
		lock.lock();
		try {
		StringBuffer sql = new StringBuffer();
			byte[] messageB = message.trim().getBytes("UTF8");
			
			Object[] obj = {name, messageB};
			sql.append("insert into MESSAGE_ARCHIVE values(?, ?, null);");
			db.execSQL(sql.toString(), obj);
		} catch (UnsupportedEncodingException e) {
			// Exception
		} finally {
			lock.unlock();
		}	
	}
	
	
	/** Get ApplicationStatusBean - for "Main Menu" report */
	public static ApplicationStatusBean getAppStatus()
	{
		lock.lock();
		Cursor cursor = null;
		ApplicationStatusBean asb = new ApplicationStatusBean();
		
		try {
			ArrayList<String> variables = new ArrayList<String>();
			StringBuffer sql = new StringBuffer();
					
			sql.append(" select * from APP_STATUS ");
		
			cursor = db.rawQuery(sql.toString(), null);
			if (cursor.getCount() < 1) // application first run
			{
				updateAppStatus();
				cursor = db.rawQuery(sql.toString(), null);
			}
			cursor.moveToNext();
			asb.setPresentRun(cursor.getLong(cursor.getColumnIndex("PRESENT_RUN")));
			asb.setLastRun(cursor.getLong(cursor.getColumnIndex("LAST_RUN")));
			asb.setNumberOfRuns(cursor.getInt(cursor.getColumnIndex("NUMBER_OF_RUNS")));
			asb.setFirstRun(cursor.getLong(cursor.getColumnIndex("FIRST_RUN")));
			asb.setField1(cursor.getString(cursor.getColumnIndex("FIELD1")));
			asb.setChecksum(cursor.getString(cursor.getColumnIndex("CHECKSUM")));
			
			variables.add(Long.toString(asb.getPresentRun()));
			variables.add(Long.toString(asb.getLastRun()));
			variables.add(Integer.toString(asb.getNumberOfRuns()));
			variables.add(Long.toString(asb.getFirstRun()));
			variables.add(asb.getField1());
			if(asb.getChecksum().equals(getVariablesChecksum(variables)))
				asb.setChecksumOk(true);
			return asb;
		} catch (Exception e) {
			// application first run
			return asb;
		} finally {
			cursor.close();
			lock.unlock();
		}	
	}
	
	
	/** Insert or Update(if exists) ApplicationStatus in DB */
	public synchronized static void updateAppStatus()
	{
		lock.lock();
		Cursor cursor = null;
		try {
			StringBuffer sql = new StringBuffer();
			
			sql.append(" select * from APP_STATUS ");
			cursor = db.rawQuery(sql.toString(), null);
			
			if (cursor.getCount() < 1)
			{ 
				ArrayList<String> variables = new ArrayList<String>();
				sql = new StringBuffer();
				sql.append("insert into APP_STATUS values(?, ?, ?, ?, ?, ?);");
				long nowTemp = Calendar.getInstance().getTimeInMillis();
				variables.add(Long.toString(nowTemp));
				variables.add(Long.toString(nowTemp));
				variables.add(Integer.toString(1));
				variables.add(Long.toString(nowTemp));
				variables.add(null);
				variables.add(getVariablesChecksum(variables));
				String[] vrs = new String[variables.size()];
				variables.toArray(vrs);
				
				db.execSQL(sql.toString(), vrs);
				
			} else {
				cursor.moveToNext();
				ArrayList<String> variables = new ArrayList<String>();
				sql = new StringBuffer();
				sql.append("update APP_STATUS set PRESENT_RUN = ?, ");
				sql.append(" LAST_RUN = ?, ");
				sql.append(" NUMBER_OF_RUNS = ?, ");
				sql.append(" FIRST_RUN = ?, ");
				sql.append(" FIELD1 = ?, ");
				sql.append(" CHECKSUM = ?; ");
				variables.add(Long.toString(Calendar.getInstance().getTimeInMillis()));
				variables.add(Long.toString(cursor.getLong(cursor.getColumnIndex("PRESENT_RUN"))));			
				variables.add(Integer.toString(cursor.getInt(cursor.getColumnIndex("NUMBER_OF_RUNS")) + 1));
				variables.add(Long.toString(cursor.getLong(cursor.getColumnIndex("FIRST_RUN"))));
				variables.add(null);
				variables.add(getVariablesChecksum(variables));
				String[] vrs = new String[variables.size()];
				variables.toArray(vrs);
				
				db.execSQL(sql.toString(), vrs);
			}
			
		} finally {
			cursor.close();
			lock.unlock();
		}	
	}
	
	
	/** ApplicationStatus Checksum */
	private static String getVariablesChecksum(List<String> variables)
	{
		lock.lock();
		try {
			String checkSum = "CheckSum";	
			for (int i = 0; i < variables.size(); ++i) checkSum += variables.get(i) + ":";
			checkSum = new String(Encryptor.getMD5Hash(checkSum));
			
			return checkSum;
		} finally {
			lock.unlock();
		}	
	}
	
	
	/** Create all application Tables - if not exist */
	private synchronized static void createTables()
	{
		StringBuffer sql = new StringBuffer();
		
		sql.append("CREATE TABLE IF NOT EXISTS ");
		sql.append(" APP_STATUS ");
		sql.append(" (");
		sql.append(" PRESENT_RUN INTEGER, LAST_RUN INTEGER, NUMBER_OF_RUNS INTEGER, FIRST_RUN INTEGER, FIELD1 VARCHAR, CHECKSUM VARCHAR(32)");
		sql.append(" );");
		db.execSQL(sql.toString());
		
		sql = new StringBuffer();		
		sql.append("CREATE TABLE IF NOT EXISTS ");
		sql.append(" BLOB_REP ");
		sql.append(" (");
		sql.append(" ID VARCHAR PRIMARY KEY ASC, CLOBDATA TEXT, ");
		sql.append(" BLOBDATA BLOB, ");
		sql.append(" STAMPHASH VARCHAR(32), ");
		sql.append(" TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP ");
		sql.append(" );");		
		db.execSQL(sql.toString());
		
		sql = new StringBuffer();		
		sql.append("CREATE TABLE IF NOT EXISTS ");
		sql.append(" MESSAGE_ARCHIVE ");
		sql.append(" (");
		sql.append(" NAME VARCHAR(32) PRIMARY KEY ASC, MESSAGE BLOB , ");
		sql.append(" FIELD1 VARCHAR ");
		sql.append(" );");		
		db.execSQL(sql.toString());
	}
}
