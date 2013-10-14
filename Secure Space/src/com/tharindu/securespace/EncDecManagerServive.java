package com.tharindu.securespace;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.paranoiaworks.unicus.android.sse.*;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.misc.EncryptorException;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.utils.DBHelper;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

/**
 * Encryption/Decryption managing service
 * @author Tharindu Wijewardane
 */

public class EncDecManagerServive extends IntentService {

	private boolean nativeCodeDisabled;
	private SQLiteDatabase db;
	private SettingDataHolder settingDataHolder;
	private int encryptAlgorithmCode;
	private boolean pbLock;
	private Thread encDecThread;
	private CryptFile selectedItem;
	private EncryptorForService encryptorForServices;
	private boolean compress;
	private ProgressBarToken progressBarToken;
	private int renderPhase;
	private String root;
	private PreferenceHelp prefHelp;
	private ArrayList<String> selectedFileList;

	/**
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public EncDecManagerServive() {
		super("EncDecManagerIntentService");
	}

	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns,
	 * IntentService stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		// Normally we would do some work here, like download a file.

		Log.d("-MY-", "onHandledIntent");		

		initVariables(); // initialize variables

		if (selectedItem == null || selectedItem.getAbsolutePath().trim().equals("")){
			Log.d("-MY-", "Error: path error");
			return;
		}
			
		
		String requesterType = intent.getStringExtra(ConstVals.REQUESTER_TYPE_KEY);
		if(requesterType.equalsIgnoreCase(ConstVals.REQUEST_FROM_NFC)){ //intent coming from NFC section
			
			String tagType = intent.getStringExtra(ConstVals.TAG_TYPE_KEY);
			if(tagType.equalsIgnoreCase(ConstVals.TAG_TYPE_ENCRYPTOR)){	//encryptor tag
				encryptFiles();
			}else if(tagType.equalsIgnoreCase(ConstVals.TAG_TYPE_DECRYPTOR)){ //decryptor tag
				decryptFiles();
			}
			
		}else if(requesterType.equalsIgnoreCase(ConstVals.REQUEST_FROM_GPS)){ //intent comimg from GPS section
			
			String commandType = intent.getStringExtra(ConstVals.COMMAND_TYPE_KEY);
			if(commandType.equalsIgnoreCase(ConstVals.COMMAND_TYPE_ENCRYPT)){	//encrypt command
				encryptFiles();
			}else if(commandType.equalsIgnoreCase(ConstVals.COMMAND_TYPE_DECRYPT)){ //decrypt command
				decryptFiles();
			}
			
		}

	}

	//initialize variables  
	private void initVariables() {
		
		prefHelp = new PreferenceHelp(getApplicationContext()); //instantiating Preference helper class	
		String password = prefHelp.getPrefString(ConstVals.PREF_KEY_PASSWORD); //pw from user settings
		
		db = DBHelper.initDB(this.getApplicationContext()); //needed for settingDataHolder
    	try {
			while(!DBHelper.isDBReady()) Thread.sleep(100); // can be useful on some systems
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		settingDataHolder = SettingDataHolder.getInstance();
		nativeCodeDisabled = settingDataHolder.getItemAsBoolean("SC_Common", "SI_NativeCodeDisable");
		encryptAlgorithmCode = settingDataHolder.getItemAsInt("SC_FileEnc", "SI_Algorithm");
		
		root = Environment.getExternalStorageDirectory().toString(); // define root dir	
		progressBarToken = new ProgressBarToken(); //i have removed the usage of this. but kept for future decisions
		
		//if the list has been stored in shared preferences
		if(prefHelp.getPrefList(ConstVals.PREF_KEY_SELECTED_FILES_LIST) != null){
			selectedFileList = (ArrayList<String>) prefHelp.getPrefList(ConstVals.PREF_KEY_SELECTED_FILES_LIST);
		}else{
			selectedFileList = new ArrayList<String>();	//else creates a new list
		}

		selectedItem = new CryptFile(root + "/securespace/aaa.txt.enc"); //used for testing

		try {
			// encryptorForServices = new EncryptorForService(password, encryptAlgorithmCode); //initialize encryptor/decryptor
			encryptorForServices = new EncryptorForService(password); //initialize encryptor/decryptor. algo is default (AES)
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (InvalidKeySpecException e1) {
			e1.printStackTrace();
		}

	}

	private void encryptFiles() {
		startEncryptor();
	}

	private void decryptFiles() {
		startDecryptor();
	}

	//start encryptor thread
	public void startEncryptor() {

		Log.d("-MY-", "before starting encThread");
		encDecThread = new Thread(new Runnable() {
			public void run() {

				PowerManager.WakeLock wakeLock;
				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FE");				

				for (String path : selectedFileList) {
					CryptFile currentFile = new CryptFile(path);
					
					if (currentFile.exists()) {
						wakeLock.acquire();
						try {
							Log.d("-MY-", "doEnc path: " + path);
							doEnc(currentFile);
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							wakeLock.release();
						}
					}
				}
				
				//save state of the selected files
				prefHelp.savePref(ConstVals.PREF_KEY_ARE_FILES_ENCRYPTED, true);
								
			}
		});

		// start ENC executor thread
		encDecThread.start();
	}
	
	//start decryptor thread
	public void startDecryptor() {

		Log.d("-MY-", "before starting decThread");
		encDecThread = new Thread(new Runnable() {
			public void run() {

				PowerManager.WakeLock wakeLock;
				PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FE");
				
				for (String path : selectedFileList) {
					
					if(!path.endsWith(".enc")){
						path += ".enc";		//name of the encrypted file
					}
					
					File ff = new File(path); //work around by th
					if (ff.exists()) {
					CryptFile currentFile = new CryptFile(path);
					
						wakeLock.acquire();
						try {
							Log.d("-MY-", "doEnc path: " + path);
							doDec(currentFile);
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							wakeLock.release();
						}
					}
				}
				
				//save state of the selected files
				prefHelp.savePref(ConstVals.PREF_KEY_ARE_FILES_ENCRYPTED, false);
				
			}
		});

		// start DEC executor thread
		encDecThread.start();
	}

	/** Encrypt selected Folder/File */
	private synchronized void doEnc(CryptFile inputFile) throws Exception {

		if (!inputFile.exists()) {
			throw new FileNotFoundException();
		}	
		
		if (!inputFile.isEncrypted()) { //if not already encrypted

			Log.d("-MY-", "encryption");

			try {
				long start = System.currentTimeMillis();
				Log.d("-MY-", "before calling encryptor");
				encryptorForServices.zipAndEncryptFile(inputFile, compress,	progressBarToken);
				Log.d("-MY-", "after calling encryptor");

				long time = (System.currentTimeMillis() - start);
				// SSElog.d("Enc Time: " + time + " : " + !nativeCodeDisabled +
				// " : " + inputFile.getName() + " : " + inputFile.length());
				if (settingDataHolder.getItemAsBoolean("SC_FileEnc",
						"SI_WipeSourceFiles"))
					Log.d("-MY-", "wipe source requested");
					WipeSource.wipeFileOrDirectory(inputFile, progressBarToken); //WipeSource by th instead of Helpers
			} catch (Exception e) {

				e.printStackTrace();

				switch (renderPhase) {
				case 2: {
					CryptFile tf = new CryptFile(selectedItem.getAbsolutePath()
							+ "." + Encryptor.ENC_FILE_EXTENSION + "."
							+ Encryptor.ENC_FILE_UNFINISHED_EXTENSION);
					tf.delete();
					break;
				}

				default:
					break;
				}
				throw e;
			}
		}

		return;
	}

	/** Decrypt selected Folder/File */
	private synchronized void doDec(CryptFile inputFile) throws Exception {

		if (!inputFile.exists()) {
			throw new FileNotFoundException();
		}

		if (inputFile.isEncrypted()) { // decryption. if already encrypted

			Log.d("-MY-", "decryption");

			try {
				long start = System.currentTimeMillis();
				Log.d("-MY-", "before calling decryptor");
				encryptorForServices.unzipAndDecryptFile(inputFile,	progressBarToken);
				Log.d("-MY-", "after calling decryptor");
				long time = (System.currentTimeMillis() - start);
				// SSElog.d("Dec Time: " + time + " : " + !nativeCodeDisabled +
				// " : " + inputFile.getName() + " : " + inputFile.length());

			} catch (InterruptedException e) {
				String message = e.getMessage();
				String[] messages = message.split("\\|\\|"); // try to split
																// "message||wipefile path"
				if (!(messages == null || messages.length < 1
						|| messages[0] == null || messages[0].trim().equals("")))
					message = messages[0];
				// SSElog.d("message", message);

				switch (renderPhase) // wipe uncompleted file
				{
				case 4: // decrypting
				{
					if (messages.length > 1)
						Helpers.wipeFileOrDirectory(new File(messages[1]),
								progressBarToken, true);
					break;
				}

				default:
					break;
				}
				throw new InterruptedException(message);
			}
		}

		return;
	}

//	 @Override
//	    public void onDestroy() {
//	    	super.onDestroy();
//
//	    		// Wipeout application
//	    		try {
//					DBHelper.killDB();
//					android.os.Process.killProcess(android.os.Process.myPid());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//	    	
//	    }
	
	
	
}
