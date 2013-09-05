package com.tharindu.securespace;

// this class handles the saving and fetching shared preferences

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PreferenceHelp {

	public final String filename;
	SharedPreferences prefData;
	SharedPreferences.Editor editor;
	Context context;
	
	
	public PreferenceHelp(Context context, String filename) {
		this.filename = filename;
		prefData = context.getSharedPreferences(filename, 0);
	}

	public String getPrefString(String key){
		return prefData.getString(key, "0");
	}
	
	public int getPrefInt(String key){
		return (int) Double.parseDouble(prefData.getString(key, "0"));
	}
	
	public boolean getPrefBool(String key){
		return prefData.getBoolean(key, true);
	}

	public void savePref(String key, String value) {
		editor = prefData.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public void savePref(String key, int value) {
		editor = prefData.edit();
		editor.putString(key, String.valueOf(value));
		editor.commit();
	}
	
	public void savePref(String key, boolean value) {
		editor = prefData.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

}
