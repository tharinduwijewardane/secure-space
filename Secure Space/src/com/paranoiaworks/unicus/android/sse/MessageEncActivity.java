package com.paranoiaworks.unicus.android.sse;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.paranoiaworks.unicus.android.sse.components.ImageToast;
import com.paranoiaworks.unicus.android.sse.components.PasswordDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleHTMLDialog;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.PasswordAttributes;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.DBHelper;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.tharindu.securespace.R;

/**
 * Message Encryptor activity class
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.2
 */
public class MessageEncActivity extends CryptActivity {
	
	private int encryptAlgorithmCode;
	private boolean hideInfoMessages;
	private boolean askOnLeave;
	private Button toClipBoardDecButton;
	private Button fromClipBoardDecButton;
	private Button encryptButton;
	private Button decryptButton;
	private Button toClipBoardEncButton;
	private Button fromClipBoardEncButton;
	private Button passwordButton;
	private Button helpButton;
	private Button moreOptionsButton;
	private EditText encryptedEditText;
	private EditText decryptedEditText;
	
	private PasswordDialog passwordDialog;
	private Dialog moreDialog;
	private Dialog messageAskDialog;
	
	private static final int MEA_MESSAGE_MOREDIALOG = -2101;
	private static final int MEA_MESSAGE_MOREDIALOG_LOAD = -2102;
	//private static final int MEA_MESSAGE_MOREDIALOG_SAVE = -2103;
	private static final int MEA_MESSAGE_MOREDIALOG_DELETE = -2104;
	private static final int MEA_MESSAGE_MOREDIALOG_DELETE_CONFIRM = -2105;
	
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	//this.resolveActivityPreferences("SC_MessageEnc");
    	super.onCreate(savedInstanceState);
    	this.setContentView(R.layout.la_messageenc);
    	this.setTitle(getResources().getString(R.string.common_app_messageEncryptor_name));
    	encryptAlgorithmCode = settingDataHolder.getItemAsInt("SC_MessageEnc", "SI_Algorithm");
    	hideInfoMessages = settingDataHolder.getItemAsBoolean("SC_MessageEnc", "SI_HideInfoMessage");
    	askOnLeave = settingDataHolder.getItemAsBoolean("SC_Common", "SI_AskIfReturnToMainPage");
    	 
    	decryptedEditText = (EditText) findViewById(R.id.ME_decryptedEditText);
    	encryptedEditText = (EditText) findViewById(R.id.ME_encryptedEditText);
    	
    	
        // Button - Copy Decrypted Message to system clipboard
        toClipBoardDecButton = (Button) findViewById(R.id.ME_toClipBoardDecButton);
        toClipBoardDecButton.setOnClickListener(new OnClickListener() 
	    {
			@Override
		    public void onClick(View v) 
		    {
		    	Toast toast;
		    	String text = decryptedEditText.getText().toString().trim();
		    	if (text.equals(""))
		    	{
			    	toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.me_emptyInputArea), v);
			    	toast.show();
			    	return;
		    	}
		    	setToSystemClipboard(text);
		    	if(!hideInfoMessages)ComponentProvider.getImageToastInfo(getResources().getString(R.string.me_decryptedToClipboard), v).show();
		    }
	    }); 
        
        // Button - Paste text from clipboard to "decryptedEditText"
        fromClipBoardDecButton = (Button) findViewById(R.id.ME_fromClipBoardDecButton);
        fromClipBoardDecButton.setOnClickListener(new OnClickListener() 
	    {
			@Override
		    public void onClick(View v) 
		    {	    	
		    	Toast toast;
		    	String text = getFromSystemClipboard();
		    	if (text.equals(""))
		    	{
			    	toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.me_emptyClipboard), v);
			    	toast.show();
			    	return;
		    	}		    	
		    	decryptedEditText.setText(text);
		    	decryptedEditText.setSelection(decryptedEditText.length());	    	
		    }
	    });
        
        // Button - Encrypt
        encryptButton = (Button) findViewById(R.id.ME_encryptButton);
        encryptButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	Toast toast;
		    	String text = decryptedEditText.getText().toString().trim();
		    	if (text.equals(""))
		    	{
			    	toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.me_emptyEncTextArea), v);
			    	toast.show();
			    	return;
		    	}
		    	try {
		    		encryptedEditText.setText(encryptor.encryptString(text));
		    		encryptedEditText.setSelection(encryptedEditText.length());
				}  catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
		    }
	    });
        
        // Button - Decrypt
        decryptButton = (Button) findViewById(R.id.ME_decryptButton);
        decryptButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	Toast toast;
		    	String text = encryptedEditText.getText().toString().trim();
		    	if (text.equals(""))
		    	{
			    	toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.me_emptyDecTextArea), v);
			    	toast.show();
			    	return;
		    	}
		    	try {
					decryptedEditText.setText(encryptor.decryptString(text));
					decryptedEditText.setSelection(decryptedEditText.length());
				} catch (DataFormatException e) {
			    	toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.me_decryptChecksumError), v);
			    	toast.show();
				} catch (NoSuchAlgorithmException e) {
					toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.common_unknownAlgorithm_text), v);
					toast.show();
				} catch (Exception e) {
			    	toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.me_decryptError), v);
			    	toast.show();
				}
		    }
	    });
        
        // Button - Copy Encrypted Message to system ClipBoard
        toClipBoardEncButton = (Button) findViewById(R.id.ME_toClipBoardEncButton);
        toClipBoardEncButton.setOnClickListener(new OnClickListener() 
	    {
			@Override
		    public void onClick(View v) 
		    {
		    	Toast toast;
		    	String text = encryptedEditText.getText().toString().trim();
		    	if (text.equals(""))
		    	{
			    	toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.me_emptyInputArea), v);
			    	toast.show();
			    	return;
		    	}
		    	
		    	setToSystemClipboard(text);
		    	if(!hideInfoMessages)ComponentProvider.getImageToastInfo(getResources().getString(R.string.me_encryptedToClipboard), v).show();
		    }
	    }); 
        
        // Button - Paste text from clipboard to "encryptedEditText"
        fromClipBoardEncButton = (Button) findViewById(R.id.ME_fromClipBoardEncButton);
        fromClipBoardEncButton.setOnClickListener(new OnClickListener() 
	    {
			@Override
		    public void onClick(View v) 
		    {
		    	Toast toast;
		    	String text = getFromSystemClipboard();
		    	if (text.equals(""))
		    	{
			    	toast = ComponentProvider.getImageToastKO(getResources().getString(R.string.me_emptyClipboard), v);
			    	toast.show();
			    	return;
		    	}	
		    	encryptedEditText.setText(text);
		    	encryptedEditText.setSelection(encryptedEditText.length());
		    }
	    });
        
        // Button - Show "More menu"
        moreOptionsButton = (Button)findViewById(R.id.ME_moreOptionsButton);
        moreOptionsButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	List<String> itemList = new ArrayList<String>();
		    	List<String> keyList = new ArrayList<String>();
		    	itemList.add(getResources().getString(R.string.me_moreDialog_loadMessage));
		    	keyList.add("me_moreDialog_loadMessage");
		    	itemList.add(getResources().getString(R.string.me_moreDialog_saveMessage));
		    	keyList.add("me_moreDialog_saveMessage");
		    	itemList.add(getResources().getString(R.string.me_moreDialog_deleteMessage));
		    	keyList.add("me_moreDialog_deleteMessage");
		    	moreDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
		    			v, 
		    			getResources().getString(R.string.me_moreDialog_Title),
		    			itemList,
		    			keyList,
		    			MessageEncActivity.MEA_MESSAGE_MOREDIALOG);
		    	if (moreDialog != null) moreDialog.show();
		    }
	    });
        
        // Button - Show "Help SimpleHTMLDialog"
        helpButton = (Button)findViewById(R.id.ME_helpButton);
        helpButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	SimpleHTMLDialog simpleHTMLDialog = new SimpleHTMLDialog(v);
		    	simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_MessageEncryptor));
		    	simpleHTMLDialog.show();
		    }
	    });
        
        // Button - Set Password
        passwordButton = (Button) findViewById(R.id.ME_passwordButton);
        passwordButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	passwordDialog = new PasswordDialog(v, PasswordDialog.PD_MODE_SET_PASSWORD);
		    	passwordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);		    	
		    	passwordDialog.show();
		    }
	    });
        
        // "On Start Set Password" 
        passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD);
        passwordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);
        passwordDialog.show();
        
        // OnClick on EncryptedEditText
        encryptedEditText.setOnClickListener(new View.OnClickListener() 
        {
            @Override
            public void onClick(View v) {
            	encryptedEditText.setSelection(encryptedEditText.length());
            }
        });
    }
    
    /** Handle Message */
    protected void processMessage() //made protected by th
    {
        ActivityMessage am = getMessage();
        if (am == null) return;
        
        int messageCode = am.getMessageCode();    
        switch (messageCode) 
        {        
        	case CryptActivity.COMMON_MESSAGE_SET_ENCRYPTOR:
            	this.passwordAttributes = (PasswordAttributes)((List)am.getAttachement()).get(0);
            	this.encryptor = (Encryptor)((List)am.getAttachement()).get(1);
            	Drawable bd = getResources().getDrawable(this.passwordAttributes.getDrawableID());
    	        passwordButton.setBackgroundDrawable(bd);
    	        this.resetMessage();
            	break;
            
        	case MessageEncActivity.MEA_MESSAGE_MOREDIALOG:
            	if(am.getMainMessage().equals("me_moreDialog_saveMessage"))
            	{
            		if (encryptedEditText != null && !encryptedEditText.getText().toString().equals(""))
            		{
            			messageAskDialog = ComponentProvider.getMessageSetNameDialog(this, encryptedEditText.getText().toString().trim());
            			messageAskDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            			messageAskDialog.show();
            		}
            		else
            			ComponentProvider.getImageToast(
            					this.getResources().getString(R.string.me_emptyDecTextArea02), 
            					ImageToast.TOAST_IMAGE_CANCEL, this).show();
            	} 
            	else if (am.getMainMessage().equals("me_moreDialog_loadMessage")) 
            	{
            		List<String> itemList = DBHelper.getMessageNames();
            		moreDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
    		    			this, 
    		    			getResources().getString(R.string.me_moreDialog_loadMessage),
    		    			itemList,
    		    			null,
    		    			MessageEncActivity.MEA_MESSAGE_MOREDIALOG_LOAD);
    		    	if (moreDialog != null) moreDialog.show();
            	}
            	else if (am.getMainMessage().equals("me_moreDialog_deleteMessage")) 
            	{
            		List<String> itemList = DBHelper.getMessageNames();
            		moreDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
    		    			this, 
    		    			getResources().getString(R.string.me_moreDialog_deleteMessage),
    		    			itemList,
    		    			null,
    		    			MessageEncActivity.MEA_MESSAGE_MOREDIALOG_DELETE);
    		    	if (moreDialog != null) moreDialog.show();
            	}
            	
	            this.resetMessage();
	            break;
		            
	        case MessageEncActivity.MEA_MESSAGE_MOREDIALOG_LOAD:       	
				try {
					encryptedEditText.setText(DBHelper.getMessage(am.getMainMessage()));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
	        	this.resetMessage();
	            break;
	            
	        case MessageEncActivity.MEA_MESSAGE_MOREDIALOG_DELETE:
				ComponentProvider.getBaseQuestionDialog(this,
						this.getResources().getString(R.string.me_moreDialog_deleteMessage_dialogTitle),
						this.getResources().getString(R.string.common_question_delete)
						.replaceAll("<1>", am.getMainMessage()), am.getMainMessage(), MessageEncActivity.MEA_MESSAGE_MOREDIALOG_DELETE_CONFIRM
						).show();
	        	this.resetMessage();
	            break;
	            
	        case MessageEncActivity.MEA_MESSAGE_MOREDIALOG_DELETE_CONFIRM:
				if(am.getAttachement().equals(new Integer(1)))
				{
					DBHelper.deleteMessage(am.getMainMessage());
					ComponentProvider.getImageToast(this.getResources().getString
							(R.string.common_question_delete_confirm), ImageToast.TOAST_IMAGE_OK, this
							).show();
				}	        	
	        	this.resetMessage();
	            break;
	            
        	case COMMON_MESSAGE_CONFIRM_EXIT:
				if(am.getAttachement() == null || am.getAttachement().equals(new Integer(1)))
				{
		    		setRunningCode(0);
		    		finish();
				}
        		break;

            default: 
            	break;
        }
    }

    @Override
    protected void onStart ()
    {
        setRunningCode(RUNNING_MESSAGEENCACTIVITY);
    	super.onStart();
    }
    
    @Override
    public void onBackPressed()
    {
		if(askOnLeave)
		{
    		ComponentProvider.getBaseQuestionDialog(this, 
					getResources().getString(R.string.common_returnToMainMenuTitle),  
    				getResources().getString(R.string.common_question_leave)
    				.replaceAll("<1>", getResources().getString(R.string.common_app_messageEncryptor_name)), 
    				null, 
    				COMMON_MESSAGE_CONFIRM_EXIT
    				).show();
		}
		else setMessage(new ActivityMessage(COMMON_MESSAGE_CONFIRM_EXIT, null));
    }
    
    @Override
    public void onWindowFocusChanged(boolean b)
    {
    	if(this.encryptor == null)
    	{
    		setRunningCode(0);
    		this.finish();
    	}
    	super.onWindowFocusChanged(b);
    }
    
    @SuppressWarnings("deprecation")
	private void setToSystemClipboard(String text)
    {
    	ClipboardManager ClipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    	ClipMan.setText(text);
    }
    
    @SuppressWarnings("deprecation")
	private String getFromSystemClipboard()
    {
    	ClipboardManager ClipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    	String text = null;
    	try {
			text = ClipMan.getText().toString().trim();
		} catch (Exception e) {
			// Empty clipboard (Android 3.0+)
		}
    	if(text == null) text = "";
    	return text;
    }
}
