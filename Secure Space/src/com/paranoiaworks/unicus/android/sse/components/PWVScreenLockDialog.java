package com.paranoiaworks.unicus.android.sse.components;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.PasswordVaultActivity;
import com.tharindu.securespace.R;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;

/* Screen Lock Dialog for Password Vault
* 
* @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
* @version 1.0.0
*/ 
public class PWVScreenLockDialog extends Dialog {
	
	private Activity context;
	
	private Button unlockButton;
	private Button leaveButton;
	private CheckBox showPasswordCB;
	private EditText passwordET;
	
	private boolean leave = false;
	private boolean unicodeAllowed = false;
	
	private String decKeyHash = null;
	private int decAlgCode = -1;

	public PWVScreenLockDialog(View v, String decKeyHash, int decAlgCode) 
	{
		this((Activity)v.getContext(), decKeyHash, decAlgCode);
	}	
	
	public PWVScreenLockDialog(Activity context, String decKeyHash, int decAlgCode) 
	{
		super(context);
		this.context = context;
		this.decKeyHash = decKeyHash;
		this.decAlgCode = decAlgCode;
		this.init();
	}
	
	private void init()
	{		
		this.setContentView(R.layout.lc_pwv_screenlock_dialog);
		this.setTitle(context.getResources().getString(R.string.common_locked_text));
		this.setCancelable(false);
		this.setCanceledOnTouchOutside(false);
		
		unicodeAllowed = SettingDataHolder.getInstance().getItemAsBoolean("SC_Common", "SI_AllowUnicodePasswords");
		
		unlockButton = (Button)this.findViewById(R.id.PWVSLD_unlockButton);
		leaveButton = (Button)this.findViewById(R.id.PWVSLD_leaveButton);
		passwordET = (EditText)this.findViewById(R.id.PWVSLD_passwordEditText);
		showPasswordCB = (CheckBox)this.findViewById(R.id.PWVSLD_showPasswordCheckBox);
		
    	if(!unicodeAllowed) passwordET.setFilters(new InputFilter[] { filter });
		
		unlockButton.setOnClickListener(new android.view.View.OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	String currentPassword = passwordET.getText().toString().trim();
		    	String testKeyHash = null;
		    	try {
					testKeyHash = (new Encryptor(currentPassword, decAlgCode, unicodeAllowed)).getEncKeyHash();
				} catch (Exception e) {
					e.printStackTrace();
				}
		    	
				if(decKeyHash.equals(testKeyHash))
				{		    	
			    	CryptActivity ca = (CryptActivity)context;
			    	ca.setMessage(new ActivityMessage(PasswordVaultActivity.PWV_MESSAGE_SCREENLOCK_UNLOCK, null));
			    	cancel();
				}
				else
				{
	        		Toast tt = new ImageToast(context.getResources().getString(R.string.passwordDialog_invalidPassword), ImageToast.TOAST_IMAGE_CANCEL, context);
	        		tt.setDuration(Toast.LENGTH_SHORT);
	        		tt.show();
				}
		    }
	    });
		
		leaveButton.setOnClickListener(new android.view.View.OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	leave = true;
		    	cancel();
		    }
	    });
		
    	this.setOnCancelListener(new DialogInterface.OnCancelListener() {
    		@Override
    		public void onCancel (DialogInterface dialogInterface) {
		    	if(leave)
		    	{
	    			CryptActivity ca = (CryptActivity)context;
			    	ca.setMessage(new ActivityMessage(PasswordVaultActivity.COMMON_MESSAGE_CONFIRM_EXIT, null));
		    	}
    		}
    	});
		
    	showPasswordCB.setOnCheckedChangeListener(new OnCheckedChangeListener()
    	{
    	    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    	    {
    	        if (isChecked)
    	        {
    	        	passwordET.setTransformationMethod(null);
    	        	if(passwordET.length() > 0) passwordET.setSelection(passwordET.length());
    	        } else {
    	        	passwordET.setTransformationMethod(new PasswordTransformationMethod());
    	        	if(passwordET.length() > 0) passwordET.setSelection(passwordET.length());
    	        }
    	    }
    	});
	}
	
	// Only ASCII 32...126 allowed
    InputFilter filter = new InputFilter()
	{
	    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) 
	    { 
	    	if (source.length() < 1) return null;
	    	char last = source.charAt(source.length() - 1);
        	if(last > 126 || last < 32) 
        	{
				Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(context, 
						context.getResources().getString(R.string.passwordDialog_title_incorrectCharacter), 
						context.getResources().getString(R.string.passwordDialog_incorrectCharacterReport), 
    					ComponentProvider.DRAWABLE_ICON_INFO_BLUE
    			);
				showMessageDialog.show();
        		
        		return source.subSequence(0, source.length() - 1);
        	}
        	return null;
	    }  
	};
}
