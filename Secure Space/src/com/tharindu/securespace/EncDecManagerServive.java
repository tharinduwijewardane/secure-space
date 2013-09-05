package com.tharindu.securespace;

import android.app.IntentService;
import android.content.Intent;

public class EncDecManagerServive extends IntentService{

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
	      
		  
		  
	  }
	  
	  

		
}
