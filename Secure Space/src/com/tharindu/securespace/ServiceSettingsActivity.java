package com.tharindu.securespace;

import com.paranoiaworks.unicus.android.sse.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

/**
 * Settings related to automatic services
 * @author Tharindu Wijewardane
 */

public class ServiceSettingsActivity extends Activity{
	
	public static String filename = "prefs_th";		//shared preferences filename
	
	EditText etPassword;
	EditText etEncTag;
	EditText etDecTag;
	ToggleButton tbLocation;
	ToggleButton tbNFC;
	Button bSaveSettings;
	PreferenceHelp prefHelp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.th_la_settings);
		
		etPassword = (EditText) findViewById(R.id.etPassword_th);
		etEncTag = (EditText) findViewById(R.id.etEncTag_th);
		etDecTag = (EditText) findViewById(R.id.etDecTag_th);
		
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
				// TODO Auto-generated method stub
				
			}
		});
		
		bSaveSettings = (Button) findViewById(R.id.bSaveButton_th);
		bSaveSettings.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {				
				saveValues();				
			}
		});
		
		if(prefHelp == null){
			prefHelp = new PreferenceHelp(getApplicationContext(), filename);
		}
				
		displayValues();
	}

	void saveValues(){
		
		prefHelp.savePref(ConstVals.PREF_KEY_PW, etPassword.getText().toString());
		prefHelp.savePref(ConstVals.PREF_KEY_LOCATION_ENABLED, tbLocation.isChecked());
		prefHelp.savePref(ConstVals.PREF_KEY_NFC_ENABLED, tbNFC.isChecked());
		prefHelp.savePref(ConstVals.PREF_KEY_ENC_TAG, etEncTag.getText().toString());
		prefHelp.savePref(ConstVals.PREF_KEY_DEC_TAG, etDecTag.getText().toString());
		
	}
	
	void displayValues(){
		
		etPassword.setText(prefHelp.getPrefString(ConstVals.PREF_KEY_PW));
		tbLocation.setChecked(prefHelp.getPrefBool(ConstVals.PREF_KEY_LOCATION_ENABLED));
		tbNFC.setChecked(prefHelp.getPrefBool(ConstVals.PREF_KEY_NFC_ENABLED));
		etEncTag.setText(prefHelp.getPrefString(ConstVals.PREF_KEY_ENC_TAG));
		etDecTag.setText(prefHelp.getPrefString(ConstVals.PREF_KEY_DEC_TAG));
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		saveValues();		//user entered values will be saved when exiting or pausing the app
		
	}
	

}