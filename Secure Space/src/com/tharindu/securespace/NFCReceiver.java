package com.tharindu.securespace;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

public class NFCReceiver extends Activity{

	private NdefMessage[] msgs;
	private PreferenceHelp prefHelp;
	private String filename = ServiceSettingsActivity.filename;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		Log.d("-MY-", "on resume");

		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {

			Log.d("-MY-", "ACTION_NDEF_DISCOVERED");
			
			Intent intent = getIntent();

			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
			}

			if (msgs[0] != null) {
				String result = "";
				byte[] payload = msgs[0].getRecords()[0].getPayload();
				// this assumes that we get back am SOH followed by host/code
				for (int b = 1; b < payload.length; b++) { // skip SOH
					result += (char) payload[b];
				}
				
				takeDecision(result);
				
//				Log.d("-MY-", "before toast");
//				Toast.makeText(getApplicationContext(),
//						"Tag Contains " + result, Toast.LENGTH_SHORT).show();
				
				finish();
				
			}

		}

	}
	
	private void takeDecision(String tagText){
		
		if(prefHelp == null){
			prefHelp = new PreferenceHelp(getApplicationContext(), filename );
		}
		
		String encTag = prefHelp.getPrefString(ServiceSettingsActivity.PREF_KEY_ENC_TAG);
		String decTag = prefHelp.getPrefString(ServiceSettingsActivity.PREF_KEY_DEC_TAG);
		
		if(tagText.equalsIgnoreCase("en"+encTag)){	//if encryptor tag
			
			Toast.makeText(getApplicationContext(),"Encryptor tag", Toast.LENGTH_SHORT).show();
			
		}else if(tagText.equalsIgnoreCase("en"+decTag)){	//if decryptor tag
			
			Toast.makeText(getApplicationContext(),"Decryptor tag", Toast.LENGTH_SHORT).show();
		}
		
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);

		Log.d("-MY-", "on new intent");
		
	}

	
}
