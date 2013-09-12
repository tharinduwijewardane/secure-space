package com.tharindu.securespace;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Class to handle the saving and fetching shared preferences
 * @author Tharindu Wijewardane
 */

public class PreferenceHelp {

	public final String filename;
	SharedPreferences prefData;
	SharedPreferences.Editor editor;
	Context context;
	
	public PreferenceHelp(Context context) {
		this.filename = "prefs_th";	//default filename hard coded
		prefData = context.getSharedPreferences(filename, 0);
	}
	
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
	
	//retrieve a string list from shared preferences
	public List<String> getPrefList(String key){
		String csvList = prefData.getString(key, null);
		if(csvList != null){	//if not null (if has been saved earlier)
			String[] items = csvList.split(",");
			List<String> list = new ArrayList<String>();
			for(int i=0; i < items.length; i++){
			     list.add(items[i]);     
			}
			return list;
		}else{
			return null;
		}
		
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
	
	//store a string list from shared preferences
	public void savePref(String key, List<String> list){
		editor = prefData.edit();
		StringBuilder csvList = new StringBuilder();
		for(String s : list){
		      csvList.append(s);
		      csvList.append(",");
		}
		editor.putString(key, csvList.toString());
		editor.commit();
	}

}
