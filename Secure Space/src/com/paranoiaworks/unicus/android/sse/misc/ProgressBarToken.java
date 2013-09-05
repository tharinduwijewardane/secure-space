package com.paranoiaworks.unicus.android.sse.misc;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Handler;

import com.paranoiaworks.unicus.android.sse.components.DualProgressDialog;

/**
 * Helper object for communication between ProgressBar and executor Thread
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.1
 */
public class ProgressBarToken {
	
	DualProgressDialog dialog;
	Dialog cancelDialog;
	Handler progressHandler;
	int increment;	
	
	public DualProgressDialog getDialog() {
		return dialog;
	}
	
	public void setDialog(DualProgressDialog dialog) {
		this.dialog = dialog;
	}	

	public Dialog getCancelDialog() {
		return cancelDialog;
	}

	public void setCancelDialog(Dialog cancelDialog) {
		this.cancelDialog = cancelDialog;
	}

	public int getIncrement() {
		return increment;
	}
	
	public void setIncrement(int increment) {
		this.increment = increment;
	}
	
	public Handler getProgressHandler() {
		return progressHandler;
	}
	
	public void setProgressHandler(Handler progressHandler) {
		this.progressHandler = progressHandler;
	}
}
