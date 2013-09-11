package com.tharindu.securespace;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.zip.DataFormatException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.paranoiaworks.unicus.android.sse.*;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.misc.EncryptorException;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

public class EncDecManagerServive extends IntentService{

	private boolean nativeCodeDisabled;
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
	
	  /** 
	   * A constructor is required, and must call the super IntentService(String)
	   * constructor with a name for the worker thread.
	   */
	  public EncDecManagerServive() {
	      super("EncDecManagerIntentService");
	  }

	  /**
	   * The IntentService calls this method from the default worker thread with
	   * the intent that started the service. When this method returns, IntentService
	   * stops the service, as appropriate.
	   */
	  @Override
	  protected void onHandleIntent(Intent intent) {
	      // Normally we would do some work here, like download a file.
		  
		  Log.d("-MY-", "onHandledIntent");
	      
		  root = Environment.getExternalStorageDirectory().toString();
		  progressBarToken = new ProgressBarToken();
		  settingDataHolder = SettingDataHolder.getInstance();
		  nativeCodeDisabled = settingDataHolder.getItemAsBoolean("SC_Common", "SI_NativeCodeDisable");
		  encryptAlgorithmCode = settingDataHolder.getItemAsInt("SC_FileEnc", "SI_Algorithm");
		  
		  selectedItem = new CryptFile(root + "/securespace/aaa.txt");
	    	try {
				encryptorForServices = new EncryptorForService("abcd1234");
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
			} catch (InvalidKeySpecException e1) {
				e1.printStackTrace();
			}
	    	
	    	if(selectedItem == null || selectedItem.getAbsolutePath().trim().equals("")) return;
		  

	    			

		  
	  }
	  
	  private void encryptFiles(){
		  startEncDec(true);
	  }
	  
	  private void decryptFiles(){
		  startEncDec(false);
	  }
	  
	  /**  */
	    public void startEncDec(final boolean isEnc){
	    	
	    	if(pbLock) return;
	    	
	    	
	    	pbLock = true;

			encDecThread = new Thread (new Runnable() {
	           public void run() {
	        	   
	    		   PowerManager.WakeLock wakeLock;
	    		   PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    		   wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FE");
	    		   wakeLock.acquire();

	        	   try {
	        		   
	        		   if(isEnc){ 
	        			   doEnc(); 
	        		   }else{
	        			   doDec();
	        		   }
//	            	   progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), -200));
//	               } catch (DataFormatException e) {
//	            	   String message = e.getMessage();
//	            	   try {message = getResources().getString(Integer.parseInt(message));}catch(Exception ie){};
//	            	   sendPBTMessage(-401, message);
//	               } catch (EncryptorException e) {
//	            	   String message = e.getMessage();
//	            	   sendPBTMessage(-401, message);
//	               } catch (InterruptedException e) {
//	            	   sendPBTMessage(-401, e.getMessage());
//	               } catch (NoSuchAlgorithmException e) {
//	            	   sendPBTMessage(-401, getResources().getString(R.string.common_unknownAlgorithm_text));
	               } catch (Exception e) {
//	            	   sendPBTMessage(-400, e.getMessage());
	            	   e.printStackTrace();
	               } finally {    
//	            	   progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), -100));
	            	   wakeLock.release();
	               }
	           }
	        });
	        
	        // start ENC/DEC executor thread
			encDecThread.start();              
	    }

	    /** Encrypt selected Folder/File */
		private synchronized void doEnc() throws Exception
		{	
			CryptFile inputFile = new CryptFile(selectedItem);

			if (!inputFile.exists())
			{
				throw new FileNotFoundException();
			}
				
//			this.sendPBTMessage(FEA_PROGRESSHANDLER_SET_INPUTFILEPATH, inputFile.getAbsolutePath());
				
			if(! inputFile.isEncrypted()){ //start encryption. if not already encrypted
				
				Log.d("-MY-", "encryption");
				
				try {
//					this.sendPBTMessage(FEA_PROGRESSHANDLER_SET_MAINMESSAGE, "encrypting");
					long start = System.currentTimeMillis();
					Log.d("-MY-", "before calling encryptor");
					encryptorForServices.zipAndEncryptFile(inputFile, compress, progressBarToken);
					Log.d("-MY-", "after calling encryptor");

					long time = (System.currentTimeMillis() - start);
					//SSElog.d("Enc Time: " + time + " : " + !nativeCodeDisabled + " : " + inputFile.getName() + " : " + inputFile.length());
					if(settingDataHolder.getItemAsBoolean("SC_FileEnc", "SI_WipeSourceFiles")) Helpers.wipeFileOrDirectory(inputFile, progressBarToken);
				} catch (Exception e) {
					
					e.printStackTrace();
					
					switch (renderPhase)
	                {        
	                	case 2:
	                	{
	        	    		CryptFile tf = new CryptFile(selectedItem.getAbsolutePath() + "." + Encryptor.ENC_FILE_EXTENSION + "." + Encryptor.ENC_FILE_UNFINISHED_EXTENSION);
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
		private synchronized void doDec () throws Exception
		{	
			CryptFile inputFile = new CryptFile(selectedItem);

			if (!inputFile.exists())
			{
				throw new FileNotFoundException();
			}
				
//			this.sendPBTMessage(FEA_PROGRESSHANDLER_SET_INPUTFILEPATH, inputFile.getAbsolutePath());
				
			if(inputFile.isEncrypted()){ //decryption. if already encrypted
				
				Log.d("-MY-", "decryption");
				
				try {
//					this.sendPBTMessage(FEA_PROGRESSHANDLER_SET_MAINMESSAGE, "decrypting");
					long start = System.currentTimeMillis();
					
					encryptorForServices.unzipAndDecryptFile(inputFile, progressBarToken);
					
					long time = (System.currentTimeMillis() - start);
					//SSElog.d("Dec Time: " + time + " : " + !nativeCodeDisabled + " : " + inputFile.getName() + " : " + inputFile.length());
				
				} catch (InterruptedException e) {
					String message = e.getMessage();
					String[] messages = message.split("\\|\\|"); // try to split "message||wipefile path"
					if(!(messages == null || messages.length < 1 || messages[0] == null || messages[0].trim().equals(""))) message = messages[0];
					//SSElog.d("message", message);
					
	        		switch (renderPhase) // wipe uncompleted file
	                {        
	                	case 4: // decrypting
	                	{
	                		if(messages.length > 1) Helpers.wipeFileOrDirectory(new File(messages[1]), progressBarToken, true);
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
		
		
		
}
