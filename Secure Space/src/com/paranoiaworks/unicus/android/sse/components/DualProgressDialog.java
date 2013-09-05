package com.paranoiaworks.unicus.android.sse.components;

import android.app.Activity;
import android.app.Dialog;
import android.text.Spanned;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.R;

/**
 * Simple Progress Dialog
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.0
 */
public class DualProgressDialog extends Dialog {

	private Activity context;
	
	private TextView titleTextView;
	private ProgressBar progressBarA;
	private TextView progressBarATextView;
	
	private int progressA;
	private int secondaryProgressA;
	private int maxA;


	public DualProgressDialog(View v) 
	{
		this((Activity)v.getContext());
	}	
	
	public DualProgressDialog(Activity context) 
	{
		super(context);
		this.context = context;
		this.init();
	}
	
	private void init()
	{		
		this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.lc_dualprogress_dialog);
		this.setCancelable(false);
		this.setCanceledOnTouchOutside(false);
		titleTextView = (TextView)findViewById(R.id.DPBD_message);
		progressBarA = (ProgressBar)findViewById(R.id.DPBD_progressBarA);
		progressBarATextView = (TextView)findViewById(R.id.DPBD_progressBarAText);
	}
	
	public void setMessage(Spanned text)
	{
		titleTextView.setText(text);
	}
	
	public void setMessage(String text)
	{
		titleTextView.setText(text);
	}
	
	public void setProgress(int progress)
	{
		progressA = progress;
		if(progress <= 0) this.setSecondaryProgress(0); //reset
		progressBarA.setSecondaryProgress(progress); //swap progress and secondary progress
		double progressRelative = 0;
		if(maxA > 0) progressRelative = ((double)progressA / maxA) * 100;
		progressBarATextView.setText((int)Math.round(progressRelative) + "%");
	}
	
	public void setSecondaryProgress(int secondaryProgress)
	{
		secondaryProgressA = secondaryProgress;
		progressBarA.setProgress(secondaryProgress); //swap progress and secondary progress
	}
	
	public void setMax(int max)
	{
		maxA = max;
		progressBarA.setMax(max);
	}
}
