package com.tharindu.securespace;

import com.tharindu.securespace.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Settings related to automatic services
 * @author Tharindu Wijewardane
 */

public class ServiceSettingsActivity extends Activity{
	
	public static String filename = "prefs_th";		//shared preferences filename
	
	EditText etPassword;
	EditText etLoginPasswd;	
	EditText etEncTag;
	EditText etDecTag;
	TextView tvFileState, tvAllowedLat, tvAllowedLon;
	Button bSelectLocation;
	ToggleButton tbLocation;
	ToggleButton tbNFC;
	Button bSaveSettings;
	PreferenceHelp prefHelp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.th_la_settings);
		
		if(prefHelp == null){
			prefHelp = new PreferenceHelp(getApplicationContext(), filename);
		}
		
		etLoginPasswd = (EditText) findViewById(R.id.etLoginPassword_th);
		etPassword = (EditText) findViewById(R.id.etPassword_th);
		etEncTag = (EditText) findViewById(R.id.etEncTag_th);
		etDecTag = (EditText) findViewById(R.id.etDecTag_th);
		
		tvFileState = (TextView) findViewById(R.id.tvFileState);
		tvAllowedLat = (TextView) findViewById(R.id.tvAllowedLat_th);
		tvAllowedLon = (TextView) findViewById(R.id.tvAllowedLon_th);
		
		bSelectLocation = (Button) findViewById(R.id.bSelectLocation_th);
		bSelectLocation.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent(v.getContext(), MapActivity.class);
		    	startActivityForResult(myIntent, 0);
		    	Log.d("-MY-", "map activity");				
			}
		});
		
		tbLocation = (ToggleButton) findViewById(R.id.tbLocation_th);
		tbLocation.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				if(tbLocation.isChecked()){
					Intent intent = new Intent(getApplicationContext(), GPSReceiver.class);
					startService(intent);
				}else{
					Intent intent = new Intent(getApplicationContext(), GPSReceiver.class);
					stopService(intent);
				}
				
			}
		});		

		tbNFC = (ToggleButton) findViewById(R.id.tbNFC_th);
		tbNFC.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(tbNFC.isChecked()){
					prefHelp.savePref(ConstVals.PREF_KEY_NFC_ENABLED, true);
				}else{
					prefHelp.savePref(ConstVals.PREF_KEY_NFC_ENABLED, false);				
				}
				
			}
		});
		
		bSaveSettings = (Button) findViewById(R.id.bSaveButton_th);
		bSaveSettings.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {				
				saveValues();				
			}
		});		

	}
	

	@Override
	protected void onResume() {
		super.onResume();
		
		displayValues();
		
	}


	void saveValues(){
		
		prefHelp.savePref(ConstVals.PREF_KEY_LOGIN_PASSWORD, etLoginPasswd.getText().toString());
		prefHelp.savePref(ConstVals.PREF_KEY_PASSWORD, etPassword.getText().toString());
		prefHelp.savePref(ConstVals.PREF_KEY_LOCATION_ENABLED, tbLocation.isChecked());
		prefHelp.savePref(ConstVals.PREF_KEY_NFC_ENABLED, tbNFC.isChecked());
		prefHelp.savePref(ConstVals.PREF_KEY_ENC_TAG, etEncTag.getText().toString());
		prefHelp.savePref(ConstVals.PREF_KEY_DEC_TAG, etDecTag.getText().toString());
		
	}
	
	void displayValues(){
		
		etLoginPasswd.setText(prefHelp.getPrefString(ConstVals.PREF_KEY_LOGIN_PASSWORD));
		etPassword.setText(prefHelp.getPrefString(ConstVals.PREF_KEY_PASSWORD));
		tbLocation.setChecked(prefHelp.getPrefBool(ConstVals.PREF_KEY_LOCATION_ENABLED));
		tbNFC.setChecked(prefHelp.getPrefBool(ConstVals.PREF_KEY_NFC_ENABLED));
		etEncTag.setText(prefHelp.getPrefString(ConstVals.PREF_KEY_ENC_TAG));
		etDecTag.setText(prefHelp.getPrefString(ConstVals.PREF_KEY_DEC_TAG));
		tvAllowedLat.setText("Latitute:  \t" + prefHelp.getPrefString(ConstVals.PREF_KEY_LAT));
		tvAllowedLon.setText("Longitude:\t" + prefHelp.getPrefString(ConstVals.PREF_KEY_LON));
		if(prefHelp.getPrefBool(ConstVals.PREF_KEY_ARE_FILES_ENCRYPTED)){
			tvFileState.setText("Encrypted");
		}else{
			tvFileState.setText("Decrypted");
		}
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		saveValues();		//user entered values will be saved when exiting or pausing the app
		
	}
	

}