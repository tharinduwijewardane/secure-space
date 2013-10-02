package com.tharindu.securespace;

import android.app.Activity;
import android.content.Intent;
import android.media.ExifInterface;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

/**
 * Activity to receive NFC tag data
 * @author Tharindu Wijewardane
 */

/* an activity is used here because android doesn't support 
 * getting an intent to a service when a NFC tag discovered. 
 * And there is no broadcast receiver for NFC tag discovering */

public class NFCReceiver extends Activity{
	
	private NdefMessage[] msgs;
	private PreferenceHelp prefHelp;
	private String filename = ServiceSettingsActivity.filename;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		
		if(prefHelp == null){
			prefHelp = new PreferenceHelp(getApplicationContext(), filename );
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d("-MY-", "on resume");
		
		//exit if enc/dec based on NFC is not enabled by user
		if(! prefHelp.getPrefBool(ConstVals.PREF_KEY_NFC_ENABLED)){
			Log.d("-MY-", "enc/dec based on NFC not enabled in settings");
			finish();
			return;
		}

		//if NDEF (nfc data exchange format) type NFC tag discovered
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {						
			
			Log.d("-MY-", "ACTION_NDEF_DISCOVERED");
			
			Intent intent = getIntent();

			//extract NDEF data
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
			}

			//build a string from tag data
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
				
			}

		}
		
		/* finishes activity at the end of work. 
		 * becoz this activity is not used for user interaction */
		finish();
		return;
	}
	
	// checks for known tags (stored encryptor/decryptor tag)
	private void takeDecision(String tagText){		
		
		String encTag = prefHelp.getPrefString(ConstVals.PREF_KEY_ENC_TAG);
		String decTag = prefHelp.getPrefString(ConstVals.PREF_KEY_DEC_TAG);
		
		if(tagText.equalsIgnoreCase("en"+encTag)){	//if encryptor tag
			
			Toast.makeText(getApplicationContext(),"Encryptor tag", Toast.LENGTH_SHORT).show();
			Intent i = new Intent(getApplicationContext(), EncDecManagerServive.class);
			i.putExtra(ConstVals.REQUESTER_TYPE_KEY, ConstVals.REQUEST_FROM_NFC);
			i.putExtra(ConstVals.TAG_TYPE_KEY, ConstVals.TAG_TYPE_ENCRYPTOR);
			startService(i);
			
		}else if(tagText.equalsIgnoreCase("en"+decTag)){	//if decryptor tag
			
			Toast.makeText(getApplicationContext(),"Decryptor tag", Toast.LENGTH_SHORT).show();
			Intent i = new Intent(getApplicationContext(), EncDecManagerServive.class);
			i.putExtra(ConstVals.REQUESTER_TYPE_KEY, ConstVals.REQUEST_FROM_NFC);
			i.putExtra(ConstVals.TAG_TYPE_KEY, ConstVals.TAG_TYPE_DECRYPTOR);
			startService(i);
			
		}
		
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Log.d("-MY-", "on new intent");
		
	}
	
}
