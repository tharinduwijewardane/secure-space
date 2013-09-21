package com.paranoiaworks.unicus.android.sse.components;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.ClipboardManager;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.tharindu.securespace.R;
import com.paranoiaworks.unicus.android.sse.adapters.BasicListAdapter;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.PasswordGenerator;

/**
 * Password Generator Dialog
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.2
 * @related PasswordGenerator.java
 */
public class PasswordGeneratorDialog extends Dialog {
	
    private static final List<String> charsetsList = new ArrayList<String>();
    private static final List<boolean[]> charsetsConfList = new ArrayList<boolean[]>();
    private static final List<Object> defaultSettings = new ArrayList<Object>();
    private static final int DEFAULT_LENGTH = 12;
	
	private SpinnerAdapter charsetSA;
	private Activity context;
    private Spinner charsetS;
    private EditText lengthET;
    private EditText passwordET;
    private Button setButton;
    private Button toClipboardButton;
    private Button generateButton;
    private CheckBox excludeCB;
    
    private Integer messageCode = null;
    private boolean buttonLock = false;
    
    static
    {
    	{charsetsList.add("123"); boolean[] t = {false, false, true, false}; charsetsConfList.add(t);}
    	{charsetsList.add("abc"); boolean[] t = {true, false, false, false}; charsetsConfList.add(t);}
    	{charsetsList.add("ABC"); boolean[] t = {false, true, false, false}; charsetsConfList.add(t);}
    	{charsetsList.add("123 + abc"); boolean[] t = {true, false, true, false}; charsetsConfList.add(t);}
    	{charsetsList.add("123 + ABC"); boolean[] t = {false, true, true, false}; charsetsConfList.add(t);}
    	{charsetsList.add("abc + ABC"); boolean[] t = {true, true, false, false}; charsetsConfList.add(t);}
    	{charsetsList.add("123 + abc + ABC"); boolean[] t = {true, true, true, false}; charsetsConfList.add(t);}
    	{charsetsList.add("ASCII 33-126"); boolean[] t = {true, true, true, true}; charsetsConfList.add(t);}
    	
    	defaultSettings.add(6);
    	defaultSettings.add(12);
    	defaultSettings.add(true);
    }
    
	public PasswordGeneratorDialog(View v) 
	{
		this((Activity)v.getContext());
	}	
	
	public PasswordGeneratorDialog(Activity context) 
	{
		super(context);
		this.context = context;
		this.init();
	}

	
	public PasswordGeneratorDialog(View v, int messageCode) 
	{
		this((Activity)v.getContext(), messageCode);
	}	
	
	public PasswordGeneratorDialog(Activity context, int messageCode) 
	{
		super(context);
		this.context = context;
		this.messageCode = messageCode;
		this.init();
	}
		
	private void init()
	{	
		final SettingDataHolder sdh = SettingDataHolder.getInstance();
		
		this.setContentView(R.layout.lc_passwordgenerator_dialog);
		this.setTitle(context.getResources().getString(R.string.passwordGeneratorDialog_passwordGenerator_text));
		charsetSA = new BasicListAdapter(context, charsetsList);
		charsetS = (Spinner)findViewById(R.id.PWGD_charsetSpinner);
		charsetS.setAdapter(charsetSA);
		setButton = (Button)findViewById(R.id.PWGD_setButton);
		toClipboardButton = (Button)findViewById(R.id.PWGD_toClipboardButton);
		generateButton = (Button)findViewById(R.id.PWGD_generateButton);
	    lengthET = (EditText)findViewById(R.id.PWGD_length);
	    passwordET = (EditText)findViewById(R.id.PWGD_passwordField);
	    excludeCB = (CheckBox)findViewById(R.id.PWGD_excludeCheckBox);
	    
	    if(this.messageCode == null) setButton.setVisibility(Button.GONE);
	    else toClipboardButton .setVisibility(Button.GONE);
	    
	    passwordET.setTransformationMethod(null);
	    excludeCB.setText(Html.fromHtml(context.getResources().getString(R.string.passwordGeneratorDialog_excludeCharacters)));
	    
	    List<Object> savedSettings = (List)sdh.getPersistentDataObject("PASSWORD_GENERATOR_SETTINGS");	    
	    List<Object> settings = savedSettings != null ? savedSettings : defaultSettings;
	    
	    charsetS.setSelection((Integer)settings.get(0));
	    lengthET.setText("");
	    lengthET.append(Integer.toString((Integer)settings.get(1)));
	    excludeCB.setChecked((Boolean)settings.get(2));
	    
    	this.setOnCancelListener(new DialogInterface.OnCancelListener() {
    		@Override
    		public void onCancel (DialogInterface dialogInterface) {
    			saveCurrentSettting(sdh);
    		}
    	});
	    
	    generate();
	    
	    generateButton.setOnClickListener(new android.view.View.OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	generate();
		    }
	    });
	    
	    toClipboardButton.setOnClickListener(new android.view.View.OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	setToSystemClipboard(passwordET.getText().toString().trim());
		    	ComponentProvider.getShowMessageDialog(
		    			context, 
		    			context.getResources().getString(R.string.common_copyToClipboard_text),
		    			context.getResources().getString(R.string.common_passwordCopiedToClipboard_text) + "<br/><br/>" + context.getResources().getString(R.string.common_copyToClipboardWarning),
		    			ComponentProvider.DRAWABLE_ICON_INFO_BLUE).show();
		    	return;
		    }
	    });
	    
	    setButton.setOnClickListener(new android.view.View.OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	buttonLock = true;
		    	if(messageCode != null)
        		{
	        		CryptActivity ca = (CryptActivity)context;
	        		ca.setMessage(new ActivityMessage(messageCode, passwordET.getText().toString().trim(), null));
        		}
		    	
        		cancel(); 
        		return;
		    }
	    });
	    
    	this.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {		
		        if (keyCode == KeyEvent.KEYCODE_SEARCH) {            
		        	return true;
		        }
		        return false;
			}
		});  
	}
	
	private void generate()
	{
    	if(buttonLock) return;
    	buttonLock = true;
    	
    	int position = charsetS.getSelectedItemPosition();
    	String lenS = lengthET.getText().toString().trim();
    	int length = lenS.length() > 0 ? Integer.parseInt(lenS) : DEFAULT_LENGTH;
    	if(length > 64) length = 64;
    	if(length < 4) length = 4;
    	lengthET.setText("");
    	lengthET.append(Integer.toString(length));
    	boolean[] conf = charsetsConfList.get(position);
    	PasswordGenerator pg = new PasswordGenerator(conf[0], conf[1], conf[2], conf[3], excludeCB.isChecked());
	    String password = pg.getNewPassword(length);
	    passwordET.setText(password);
	    
	    buttonLock = false;
	}
	
	private void saveCurrentSettting(SettingDataHolder sdh)
	{
    	List<Object> settingsObject = new ArrayList<Object>();
    	settingsObject.add(charsetS.getSelectedItemPosition());
    	String lenS = lengthET.getText().toString().trim();
    	settingsObject.add(lenS.length() > 0 ? Integer.parseInt(lenS) : DEFAULT_LENGTH);
    	settingsObject.add(excludeCB.isChecked());
    	
    	sdh.addOrReplacePersistentDataObject("PASSWORD_GENERATOR_SETTINGS", settingsObject);
    	sdh.save();
	}
	
    @SuppressWarnings("deprecation")
	private void setToSystemClipboard(String text)
    {
    	ClipboardManager ClipMan = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    	ClipMan.setText(text);
    }
}
