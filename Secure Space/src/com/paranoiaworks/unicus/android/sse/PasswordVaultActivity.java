package com.paranoiaworks.unicus.android.sse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.paranoiaworks.unicus.android.sse.adapters.ColorListAdapter;
import com.paranoiaworks.unicus.android.sse.adapters.PWVFolderAdapter;
import com.paranoiaworks.unicus.android.sse.adapters.PWVItemArrayAdapter;
import com.paranoiaworks.unicus.android.sse.components.ImageToast;
import com.paranoiaworks.unicus.android.sse.components.PWVNewEditFolderDialog;
import com.paranoiaworks.unicus.android.sse.components.PWVScreenLockDialog;
import com.paranoiaworks.unicus.android.sse.components.PasswordDialog;
import com.paranoiaworks.unicus.android.sse.components.PasswordGeneratorDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleHTMLDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleWaitDialog;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.PasswordAttributes;
import com.paranoiaworks.unicus.android.sse.dao.Vault;
import com.paranoiaworks.unicus.android.sse.dao.VaultFolder;
import com.paranoiaworks.unicus.android.sse.dao.VaultItem;
import com.paranoiaworks.unicus.android.sse.utils.ColorHelper;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.DBHelper;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;
import com.tharindu.securespace.R;

/**
 * Password Vault activity class
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.12
 */ 
public class PasswordVaultActivity extends CryptActivity {
	
	private int encryptAlgorithmCode;
	private boolean askOnLeave;
	private boolean lockOnPause;
	private boolean showBottomMenu;
	private int screenLockedPosition = -1;
	private Vault vault = null;
	private PWVFolderAdapter iconAdapter = null;
	private ViewAnimator layoutSwitcher;
	private SpinnerAdapter itemColorSpinnerAdapter = new ColorListAdapter(this, ColorHelper.getColorList(), ColorListAdapter.ICONSET_ITEMS);
	private Dialog waitDialog;
	private PasswordDialog changePasswordDialog;
	private Dialog moreDialog;
	private Toast commonToast;
	
	// Start Layout
	private LinearLayout layoutStartButtons;
	private Button toMainPageButton;
	private Button helpMeButton;
		
	// Folders Layout
	private List<VaultItem> currentItems = new ArrayList<VaultItem>();
	private LinearLayout foldersBottomMenu;
	private Button foldersMoreButton;
	private Button foldersNewFolderButton;
	private Button foldersHelpButton;
	
	// Items Layout
	private VaultFolder currentFolder;	
	private ArrayAdapter itemsArrayAdapter;
	private ListView itemsListView;	
		
	// Item Detail Layout
	private VaultItem currentItem;	
	private EditText itemNameEditText;
	private EditText itemPasswordEditText;
	private EditText itemCommentEditText;
	private Spinner itemColorSpinner;
	private Button itemDeleteButton;
	private Button itemEditSaveButton;
	private Button itemMoveToButton;
	private Button passwordGeneratorButton;
	private Button passwordToClipboardButton;

	
	public static final String PWV_DBPREFIX = "PASSWORD_VAULT";
	public static final String PWV_EXPORT_EXT = "pwv";
	
	public static final int PWV_MESSAGE_FOLDER_NEW = -1101;
	public static final int PWV_MESSAGE_FOLDER_SAVE = -1102;
	public static final int PWV_MESSAGE_FOLDER_DELETE = -1103;
	public static final int PWV_MESSAGE_FOLDER_DELETE_CONFIRM = -1104;
	public static final int PWV_MESSAGE_ITEM_DELETE_CONFIRM = -1105;
	public static final int PWV_MESSAGE_ITEM_MOVETOFOLDER = -1106;
	public static final int PWV_MESSAGE_ITEM_SAVE_CONFIRM = -1107;
	public static final int PWV_MESSAGE_MOREDIALOG = -1201;
	public static final int PWV_MESSAGE_MOREDIALOG_IMPORT = -1202;
	public static final int PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM = -1203;
	public static final int PWV_MESSAGE_MOREDIALOG_RESET_CONFIRM = -1204;
	public static final int PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM_XML = -1205;
	public static final int PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM_XML_PASSWORD = -1206;
	public static final int PWV_MESSAGE_PWGDIALOG_SET = -1301;
	public static final int PWV_MESSAGE_PWGDIALOG_SET_CONFIRM = -1302;
	public static final int PWV_MESSAGE_SCREENLOCK_UNLOCK = -1401;
	
	private static final int PWV_LAYOUT_START = 0;
	private static final int PWV_LAYOUT_FOLDERS = 1;
	private static final int PWV_LAYOUT_ITEMS = 2;
	private static final int PWV_LAYOUT_ITEMDETAIL = 3;
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.setTheme(R.style.ThemeAltB);
    	super.onCreate(savedInstanceState);
    	this.setContentView(R.layout.la_passwordvault);
    	this.setTitle(getResources().getString(R.string.common_app_passwordVault_name));
    	encryptAlgorithmCode = settingDataHolder.getItemAsInt("SC_PasswordVault", "SI_Algorithm");
    	askOnLeave = settingDataHolder.getItemAsBoolean("SC_Common", "SI_AskIfReturnToMainPage");
    	lockOnPause = settingDataHolder.getItemAsBoolean("SC_PasswordVault", "SI_LockScreen");
    	showBottomMenu = settingDataHolder.getItemAsBoolean("SC_PasswordVault", "SI_ShowMenu");
    	
    	layoutSwitcher = (ViewAnimator) findViewById(R.id.vaultLayoutSwitcher);
    	initLayoutStart();
    	
    	commonToast = new ImageToast("***", ImageToast.TOAST_IMAGE_CANCEL, this);
    }
    
    
    /** Handle Message */
    protected void processMessage() //made protected by th
    {
        ActivityMessage am = getMessage();
        if (am == null) return;
        
        int messageCode = am.getMessageCode();   
        String mainMessage = am.getMainMessage();
        switch (messageCode) 
        {        
        	case CryptActivity.COMMON_MESSAGE_SET_ENCRYPTOR:
            	this.passwordAttributes = (PasswordAttributes)((List)am.getAttachement()).get(0);
            	this.encryptor = (Encryptor)((List)am.getAttachement()).get(1);
            	
            	if(mainMessage.equals("enter"))
            	{
	            	try {
	        			vault = loadVaultfromDB();
	        		} catch (Exception e) {
	        			getStartScreenPasswordDialog().show();
	        			Toast tt = new ImageToast(
	        					this.getResources().getString(R.string.pwv_failedOnEnter), 
	        					ImageToast.TOAST_IMAGE_CANCEL, this);
		    			tt.show();
		    			encryptor = null;
		    			this.resetMessage();
		    			return;
	        		}
	        		
	        		if (vault == null)
	        		{
	        			vault = getVaultOnFirstRun(null);
	        			try {
							saveVaultToDB();
						} catch (IOException e) {
							e.printStackTrace();
						}
	        		}
            	     	
	        		vault.notifyFolderDataSetChanged();
	        		initLayoutFolders();
	            	initLayoutItems();
	            	initLayoutItemDetail();
	
	    	        this.resetMessage();
	    	        layoutSwitcher.showNext();
            	}
            	
            	if(mainMessage.equals("change") && vault != null && encryptor != null)
            	{        		
            		try {
						saveVaultToDB();
					} catch (IOException e) {
		        		Toast tt = new ImageToast(e.getMessage(), ImageToast.TOAST_IMAGE_CANCEL, this);
		        		tt.show();
						e.printStackTrace();
					}
					
					changePasswordDialog = null;
	        		Toast tt = new ImageToast(
	        				this.getResources().getString(R.string.passwordDialog_passwordChanged), 
	        				ImageToast.TOAST_IMAGE_OK, 
	        				this);
	        		tt.show();
            	}
            	
            	if(mainMessage.startsWith("xmlimport") && vault != null && encryptor != null)
            	{        		
            		try {
						DBHelper.deleteBlobData(PWV_DBPREFIX);
						saveVaultToDB();					
						
						vault.notifyFolderDataSetChanged();
		        		initLayoutFolders();
		            	initLayoutItems();
		            	initLayoutItemDetail();
		            	
		            	layoutSwitcher.setDisplayedChild(PWV_LAYOUT_FOLDERS);
		            	
						ComponentProvider.getImageToast(this.getResources().getString(R.string.pwv_moreDialog_importVault_Loaded)
								 .replaceAll("<1>", am.getMainMessage().substring(am.getMainMessage().indexOf(":") + 1)), ImageToast.TOAST_IMAGE_OK, this).show();
						
					} catch (Exception e) {
		        		Toast tt = new ImageToast(e.getMessage(), ImageToast.TOAST_IMAGE_CANCEL, this);
		        		tt.setDuration(Toast.LENGTH_LONG);
		        		tt.show();
						e.printStackTrace();
					}
					this.resetMessage();
            	}
            	
            	if(waitDialog != null) waitDialog.cancel();
            	waitDialog = new SimpleWaitDialog(this);
            	break;
            
        	case PWV_MESSAGE_FOLDER_NEW:
        		vault.addFolder((VaultFolder)am.getAttachement());
    			try {
    				saveVaultToDB();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}     		
        		vault.notifyFolderDataSetChanged();
        		iconAdapter.notifyDataSetChanged();
        		
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_FOLDER_SAVE:
    			try {
    				saveVaultToDB();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}     		
        		vault.notifyFolderDataSetChanged();
        		iconAdapter.notifyDataSetChanged();
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_FOLDER_DELETE:
        		ComponentProvider.getBaseQuestionDialog(
        				this, 
        				getResources().getString(R.string.common_delete_text) + " " + getResources().getString(R.string.common_folder_text), 
        				getResources().getString(R.string.common_question_delete)
						.replaceAll("<1>", vault.getFolderByIndex((Integer)am.getAttachement()).getFolderName()), 
						(Integer)am.getAttachement() + ":" + (String)am.getMainMessage(), PWV_MESSAGE_FOLDER_DELETE_CONFIRM).show();
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_FOLDER_DELETE_CONFIRM:
				if(am.getAttachement().equals(new Integer(1)))
				{
	        		String[] mm = am.getMainMessage().split(":");
					vault.removeFolderWithIndex(Integer.parseInt(mm[0]), mm[1]);
	        		try {
	    				saveVaultToDB();
	    			} catch (IOException e) {
	    				e.printStackTrace();
	    			}     		
	        		vault.notifyFolderDataSetChanged();
	        		iconAdapter.notifyDataSetChanged();	
	        		ComponentProvider.getImageToast(
	        				this.getResources().getString(R.string.common_question_delete_confirm), 
	        				ImageToast.TOAST_IMAGE_OK, this).show();
				}       		
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_ITEM_DELETE_CONFIRM:
				if(am.getAttachement().equals(new Integer(1)))
				{
	        		String[] mm = am.getMainMessage().split(":"); // item index, item security hash
			    	currentFolder.removeItemWithIndex(Integer.parseInt(mm[0]), mm[1]);	    	
			    	try {
						saveVaultToDB();
					} catch (IOException e) {
						e.printStackTrace();
					}
					resetItemsList();
					makeLayoutItemDetailReadOnly();
					currentItem = null;
					layoutSwitcher.showPrevious();
			    	itemDeleteButton.setEnabled(true);
			    	itemMoveToButton.setEnabled(true);
			    	itemEditSaveButton.setEnabled(true);
					}       		
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_ITEM_SAVE_CONFIRM:
				if(am.getAttachement().equals(new Integer(1))) {
					String mode = am.getMainMessage();
					handleItemSave(mode);
				}
				else {
					leaveItemDetailLayout();
				}
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_ITEM_MOVETOFOLDER:				
				String[] mm = am.getMainMessage().split(":"); // destination folder index : destination folder hash : item index : item security hash
			    VaultItem itemToMove = currentFolder.getItemByIndex(Integer.parseInt(mm[2]));
			    VaultFolder destinationFolder = vault.getFolderByIndex(Integer.parseInt(mm[0]));
			    destinationFolder.addItem(itemToMove);
				currentFolder.removeItemWithIndex(Integer.parseInt(mm[2]), mm[3]);		    	
			    try {
					saveVaultToDB();
				} catch (IOException e) {
					e.printStackTrace();
				}
				resetItemsList();
				makeLayoutItemDetailReadOnly();
				currentItem = null;
				layoutSwitcher.showPrevious();
			    itemDeleteButton.setEnabled(true);
			    itemMoveToButton.setEnabled(true);
			    itemEditSaveButton.setEnabled(true);
			    
				ComponentProvider.getShowMessageDialog(this, 
						null, 
						getResources().getString(R.string.pwv_itemMoveToFolderReport).replaceAll("<1>", itemToMove.getItemName()).replaceAll("<2>", destinationFolder.getFolderName()), 
						ComponentProvider.DRAWABLE_ICON_OK)
						.show();	
      		
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_MOREDIALOG:
            	if (am.getMainMessage().equals("pwv_moreDialog_changePassword")) 
            	{
    		    	changePasswordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_CHANGE_PASSWORD);
    		    	changePasswordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);
    		    	changePasswordDialog.setParentMessage("change");
    		    	changePasswordDialog.setCurrentDecryptSpec(encryptor.getDecKeyHash(), encryptor.getDecryptAlgorithmCode());
    		    	changePasswordDialog.setWaitDialog(waitDialog, false);
    		    	changePasswordDialog.show();
            	}
            	else if (am.getMainMessage().equals("pwv_moreDialog_importVault")) 
            	{
            		File importExportDir = Helpers.getImportExportDir();            		
            		if(importExportDir == null)
            		{
            			Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(this, 
            					getResources().getString(R.string.pwv_moreDialog_importVault), 
            					getResources().getString(R.string.pwv_moreDialog_importExportVault_Invalid)
            					.replaceAll("<1>", Helpers.getImportExportPath()), 
            					ComponentProvider.DRAWABLE_ICON_CANCEL);
            			showMessageDialog.show();
            			return;
            		}
            		
            		List<String> fileListPWV = Arrays.asList(importExportDir.list(
            				Helpers.getOnlyExtFilenameFilter(PasswordVaultActivity.PWV_EXPORT_EXT)));
            		List<String> fileListXML = Arrays.asList(importExportDir.list(
            				Helpers.getOnlyExtFilenameFilter("xml")));
            		
            		List<String> fileList = new ArrayList<String>();
            		fileList.addAll(fileListPWV);
            		fileList.addAll(fileListXML);
            		
            		if(!(fileList.size() > 0))
            		{
            			Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(this, 
            					getResources().getString(R.string.pwv_moreDialog_importVault), 
            					getResources().getString(R.string.pwv_moreDialog_importVault_NoFilesToImport)
            					.replaceAll("<1>", Helpers.getImportExportPath()), 
            					ComponentProvider.DRAWABLE_ICON_CANCEL);
            			showMessageDialog.show();
            			return;
            		}
            		
            		AlertDialog fileListDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
    		    			this, 
    		    			getResources().getString(R.string.pwv_moreDialog_importVault_dialogTitle),
    		    			fileList,
    		    			null,
    		    			PasswordVaultActivity.PWV_MESSAGE_MOREDIALOG_IMPORT);
    		    	if (fileListDialog != null) fileListDialog.show();
            	}
            	else if (am.getMainMessage().equals("pwv_moreDialog_exportVault") || am.getMainMessage().equals("pwv_moreDialog_exportVaultXML"))
            	{
            		File importExportDir = Helpers.getImportExportDir();
            		if(importExportDir == null)
            		{
            			Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(this, 
            					getResources().getString(R.string.pwv_moreDialog_exportVault), 
            					getResources().getString(R.string.pwv_moreDialog_importExportVault_Invalid)
            					.replaceAll("<1>", Helpers.getImportExportPath()), 
            					ComponentProvider.DRAWABLE_ICON_CANCEL);
            			showMessageDialog.show();
            			return;
            		}
            		if(!importExportDir.canWrite())
            		{
            			Toast t = new ImageToast(
            					"Export failed. Import dir <b>" + Helpers.getImportExportPath() + "</b> is read only.",
            					ImageToast.TOAST_IMAGE_CANCEL, this);
            			t.show();
            			return;
            		}
			    	
            		try {
						if(vault != null) saveVaultToDB();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
            		Dialog setVaultNameDialog = ComponentProvider.getVaultSetNameDialog(this, importExportDir, am.getMainMessage().equals("pwv_moreDialog_exportVaultXML") ? vault : null);
            		setVaultNameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        			setVaultNameDialog.show();
            	}
            	else if (am.getMainMessage().equals("pwv_moreDialog_resetVault")) 
            	{
            		AlertDialog cad = ComponentProvider.getCriticalQuestionDialog(this,
            				getResources().getString(R.string.pwv_moreDialog_resetVault), 
            				getResources().getString(
            						R.string.pwv_moreDialog_resetVault_ResetCriticalQuestion), 
            						null, 
            						PWV_MESSAGE_MOREDIALOG_RESET_CONFIRM);
            		cad.show();
            		cad.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
            	}
            	else if (am.getMainMessage().equals("pwv_moreDialog_enterPassword")) 
            	{
            		getStartScreenPasswordDialog().show();
            	}
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_MOREDIALOG_IMPORT:
        		String fileName = (String)am.getMainMessage();
        		String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        		int importAction = ext.equalsIgnoreCase(PWV_EXPORT_EXT) ? PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM : PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM_XML;
        		
        		if(DBHelper.getBlobData(PWV_DBPREFIX) != null)
        		{
	        		AlertDialog cad = ComponentProvider.getCriticalQuestionDialog(this, 
	        				getResources().getString(R.string.common_importVault_text), 
	        				getResources().getString(R.string.pwv_moreDialog_importVault_ImportCriticalQuestion)
							.replaceAll("<1>", fileName), fileName, importAction);
	        		cad.show();
	        		cad.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
        		}
        		else
        		{
	        		AlertDialog cad = ComponentProvider.getBaseQuestionDialog(this, 
	        				getResources().getString(R.string.common_importVault_text), 
	        				getResources().getString(R.string.pwv_moreDialog_importVault_ImportQuestion)
							.replaceAll("<1>", fileName), fileName, importAction);
	        		cad.show();
        		}
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM:
        		if(am.getAttachement().equals(new Integer(1)))
        		{
        			File importFile = new File(Helpers.getImportExportPath() + File.separator + am.getMainMessage());
        			
        			if(importFile.length() > 2097152)
        			{
	        			ComponentProvider.getImageToast("Sorry - file <1> is too large to import.<br/> 2MB max."
								 .replaceAll("<1>", am.getMainMessage()), ImageToast.TOAST_IMAGE_CANCEL, this).show();
	        			return;
        			}
        			
        			byte[] fisBuffer = new byte[(int) importFile.length()];
        			    			
        			try {
        				FileInputStream fis = new FileInputStream(importFile);
						fis.read(fisBuffer);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					int offset = 4;
					byte[] hash = Helpers.getSubarray(fisBuffer, 0, offset);
					byte[] data = Helpers.getSubarray(fisBuffer, offset, fisBuffer.length - offset);
					 
					if(!Arrays.equals(hash, Encryptor.getShortHash(data)))
					{
						ComponentProvider.getImageToast(this.getResources().getString(R.string.pwv_moreDialog_importVault_Corrupted)
								 .replaceAll("<1>", am.getMainMessage()), ImageToast.TOAST_IMAGE_CANCEL, this).show();
						return;
					}
					
					DBHelper.insertUpdateBlobData(PWV_DBPREFIX, data, "IMPORTED");
					
					ComponentProvider.getImageToast(this.getResources().getString(R.string.pwv_moreDialog_importVault_Loaded)
							 .replaceAll("<1>", am.getMainMessage()), ImageToast.TOAST_IMAGE_OK, this).show();
					 
					this.finish();
        		}       		
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM_XML:
        		if(am.getAttachement().equals(new Integer(1)))
        		{
        			File importFile = new File(Helpers.getImportExportPath() + File.separator + am.getMainMessage());
        			
        			Vault tempvault = null;

					try {
						tempvault = Vault.getInstance(Helpers.loadStringFromFile(importFile));
					} catch (Exception e) {
						ComponentProvider.getShowMessageDialog(this, this.getResources().getString(R.string.pwv_moreDialog_importVault), 
								this.getResources().getString(R.string.pwv_moreDialog_importVault_NotValid)
									.replaceAll("<1>", am.getMainMessage()) + "<br/><br/>"+ e.getLocalizedMessage(),ComponentProvider.DRAWABLE_ICON_CANCEL).show();
					} 
				
					if(tempvault != null)
					{
						vault = tempvault;
						PasswordDialog xmlPD = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD);
						xmlPD.setEncryptAlgorithmCode(encryptAlgorithmCode);
						xmlPD.setParentMessage("xmlimport:" + am.getMainMessage());
						xmlPD.setBlockCancellation(true);
						xmlPD.show();
					}					
        		}
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_MOREDIALOG_RESET_CONFIRM:
        		if(am.getAttachement().equals(new Integer(1)))
        		{
	        		DBHelper.deleteBlobData(PWV_DBPREFIX);
	        		this.finish();
        		}
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_PWGDIALOG_SET:
        		if(itemPasswordEditText.getText().toString().trim().equals(""))
        		{
        			itemPasswordEditText.setText("");
        			itemPasswordEditText.append(am.getMainMessage());
        		}
        		else {
        			ComponentProvider.getBaseQuestionDialog(
        					this, 
        					this.getResources().getString(R.string.passwordGeneratorDialog_passwordGenerator_text), 
        					this.getResources().getString(R.string.passwordGeneratorDialog_replacePasswordQuestion), 
        					am.getMainMessage(),
        					PWV_MESSAGE_PWGDIALOG_SET_CONFIRM).show();
        		}
        			
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_PWGDIALOG_SET_CONFIRM:
        		if(am.getAttachement().equals(new Integer(1)))
        		{
        			itemPasswordEditText.setText("");
        			itemPasswordEditText.append(am.getMainMessage());
        		}
        		this.resetMessage();
        		break;
        		
        	case PWV_MESSAGE_SCREENLOCK_UNLOCK: 		
        		if(screenLockedPosition > 0) layoutSwitcher.setDisplayedChild(screenLockedPosition);
        		screenLockedPosition = -1;
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
    
    
    /** Create "Login to Password Vault Layout" */
    private void initLayoutStart()
    {
    	PasswordDialog startDialog = getStartScreenPasswordDialog();
    	startDialog.show();

    	toMainPageButton = (Button) findViewById(R.id.PWVS_toMainPage);
    	helpMeButton = (Button) findViewById(R.id.PWVS_helpMe);
    	layoutStartButtons = (LinearLayout) findViewById(R.id.PWVS_buttons);
		
	    this.toMainPageButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	setRunningCode(0);
		    	finish();
		    }
	    });
	    
	    // Help me get in! button
	    this.helpMeButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	boolean existsVault = DBHelper.getBlobData(PWV_DBPREFIX) != null;
		    	List<String> itemList = new ArrayList<String>();
		    	List<String> keyList = new ArrayList<String>();
		    	itemList.add(getResources().getString(R.string.pwv_moreDialog_enterPassword));
		    	keyList.add("pwv_moreDialog_enterPassword");
		    	if(existsVault){
			    	itemList.add(getResources().getString(R.string.pwv_moreDialog_resetVault));
			    	keyList.add("pwv_moreDialog_resetVault");
		    	}
		    	itemList.add(getResources().getString(R.string.pwv_moreDialog_importVault));
		    	keyList.add("pwv_moreDialog_importVault");
		    	if(existsVault){
			    	itemList.add(getResources().getString(R.string.pwv_moreDialog_exportVault));
			    	keyList.add("pwv_moreDialog_exportVault");
		    	}

		    	moreDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
		    			v, 
		    			getResources().getString(R.string.pwv_start_helpMe),
		    			itemList,
		    			keyList,
		    			PasswordVaultActivity.PWV_MESSAGE_MOREDIALOG);
		    	if (moreDialog != null) moreDialog.show();
		    }
	    });
    }
    
    
    /** Create Password Folders Layout */
    private void initLayoutFolders()
    {	
    	iconAdapter = new PWVFolderAdapter(this, vault);
		foldersBottomMenu = (LinearLayout) findViewById(R.id.PWVL_Folders_buttons);
		if(showBottomMenu) foldersBottomMenu.setVisibility(LinearLayout.VISIBLE);
		foldersMoreButton = (Button) findViewById(R.id.PWVL_Folders_moreButton);
		foldersNewFolderButton = (Button) findViewById(R.id.PWVL_Folders_newFolderButton);
		foldersHelpButton = (Button) findViewById(R.id.PWVL_helpButton);
		GridView gridview = (GridView) findViewById(R.id.PWVL_Folders_gridview);
        gridview.setAdapter(iconAdapter);
            

        gridview.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                currentFolder = vault.getFolderByIndex(position);
                resetItemsList();
                layoutSwitcher.showNext();
                }
            });
        
        gridview.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
            	TextView tw = (TextView) v.findViewById(R.id.iconTextPW);
		    	PWVNewEditFolderDialog nefd = new PWVNewEditFolderDialog(
		    			v, 
		    			vault,
		    			position,
		    			PWVNewEditFolderDialog.PWVFD_MODE_SHOW_FOLDER);
		    	nefd.setOriginalHash((String)tw.getTag());
		    	nefd.show();

    			try {
    				saveVaultToDB();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
            	vault.notifyFolderDataSetChanged();
    			iconAdapter.notifyDataSetChanged();
            return true;
            }
        });
        
	    this.foldersMoreButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	List<String> itemList = new ArrayList<String>();
		    	List<String> keyList = new ArrayList<String>();
		    	itemList.add(getResources().getString(R.string.pwv_moreDialog_changePassword));
		    	keyList.add("pwv_moreDialog_changePassword");
		    	itemList.add(getResources().getString(R.string.pwv_moreDialog_importVault));
		    	keyList.add("pwv_moreDialog_importVault");
		    	itemList.add(getResources().getString(R.string.pwv_moreDialog_exportVault));
		    	keyList.add("pwv_moreDialog_exportVault");
		    	itemList.add(getResources().getString(R.string.pwv_moreDialog_exportVaultXML));
		    	keyList.add("pwv_moreDialog_exportVaultXML");
		    	moreDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
		    			v, 
		    			getResources().getString(R.string.me_moreDialog_Title),
		    			itemList,
		    			keyList,
		    			PasswordVaultActivity.PWV_MESSAGE_MOREDIALOG);
		    	if (moreDialog != null) moreDialog.show();
		    }
	    });
	    
	    this.foldersNewFolderButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	if(isButtonsLockActivated()) return;
		    	activateButtonsLock();
		    	
		    	PWVNewEditFolderDialog nefd = new PWVNewEditFolderDialog(v, vault, null, PWVNewEditFolderDialog.PWVFD_MODE_NEW_FOLDER);
		    	nefd.setTitle(getResources().getString(R.string.pwv_newFolder_text));
		    	
		    	nefd.setOnCancelListener(new DialogInterface.OnCancelListener() {
		    		@Override
		    		public void onCancel (DialogInterface dialogInterface) {
		    			deactivateButtonsLock();
		    		}
		    	});
		    	
		    	nefd.show();
		    }
        });
	    
	    this.foldersHelpButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	SimpleHTMLDialog simpleHTMLDialog = new SimpleHTMLDialog(v);
		    	simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_PasswordVault));
		    	simpleHTMLDialog.show();			
		    }
	    });
    }
    
    
    /** Create Password Items Layout */
    private void initLayoutItems()
    {
    	itemsListView = (ListView) findViewById(R.id.PWVL_Items_listView);
        
        itemsArrayAdapter = (new PWVItemArrayAdapter(this, currentItems));
        ((PWVItemArrayAdapter)itemsArrayAdapter).setFontSizeMultiplier(Integer.parseInt(
    			settingDataHolder.getItemValueName("SC_PasswordVault", "SI_PasswordListFontSize")) / 100.0F);
        itemsListView.setAdapter(itemsArrayAdapter);   		
        itemsListView.setOnItemClickListener(new OnItemClickListener() 
        {
            // click on item
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
            {
            	VaultItem tvi = null;
            	
            	if (position == 0) // first item - return to folder layout
            	{
            		layoutSwitcher.showPrevious();
            		return;
            	}           	
            	--position;
            	
            	try {
					tvi = currentFolder.getItemByIndex(position);
					tvi.setSelected(!tvi.isSelected());
					itemsArrayAdapter.notifyDataSetChanged();
				} catch (IndexOutOfBoundsException e) { // create new item (last position in the items list)		
					currentItem = new VaultItem();
					makeLayoutItemDetailEditable();
					itemDeleteButton.setEnabled(false);
					itemMoveToButton.setEnabled(false);
					itemEditSaveButton.setTag("new");
			    	List tagMessage = new ArrayList();
			    	tagMessage.add(currentItem.getItemSecurityHash());
			    	tagMessage.add(position);
			    	itemNameEditText.setTag(tagMessage); // hash + position
			    	prepareLayoutItemDetailForShow();
			    	layoutSwitcher.showNext();
				} 
            }
          });
		
        itemsListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
            {
            	VaultItem tvi = null;
            	String itemHash = (String)view.findViewById(R.id.PWVI_itemName).getTag(); // hash
            	
            	if (position == 0) // first item - return to folder layout
            	{
            		layoutSwitcher.showPrevious();
            		return true;
            	}           	
            	--position;
            	
            	try {
					tvi = currentFolder.getItemByIndex(position);
					currentItem = tvi;
					
					if(!itemHash.equals(currentItem.getItemSecurityHash())) return false;
	
				} catch (IndexOutOfBoundsException e) { // create new item (last position in the items list)
					currentItem = new VaultItem();
					makeLayoutItemDetailEditable();
					itemDeleteButton.setEnabled(false);
					itemMoveToButton.setEnabled(false);
					itemEditSaveButton.setTag("new");
					
				} finally {
			    	List tagMessage = new ArrayList();
			    	tagMessage.add(currentItem.getItemSecurityHash());
			    	tagMessage.add(position);
			    	itemNameEditText.setTag(tagMessage); // hash + position
			    	prepareLayoutItemDetailForShow();
			    	layoutSwitcher.showNext();
				}
       	
            	return true;
            }
          });
    }
    
    
    /** Create Item Detail Layout */
    private void initLayoutItemDetail()
    {
    	setLayoutItemDetailOrientation();
    	
    	itemNameEditText = (EditText)findViewById(R.id.PWVD_name);
    	itemPasswordEditText = (EditText)findViewById(R.id.PWVD_password);
    	itemCommentEditText = (EditText)findViewById(R.id.PWVD_comment);
    	itemColorSpinner = (Spinner)findViewById(R.id.PWVD_colorCombo);
    	itemDeleteButton = (Button)findViewById(R.id.PWVD_buttonDelete);
    	itemEditSaveButton = (Button)findViewById(R.id.PWVD_buttonEditSave);
    	itemMoveToButton= (Button)findViewById(R.id.PWVD_buttonMoveTo);
    	passwordGeneratorButton = (Button)findViewById(R.id.PWVD_passwordGeneratorButton);
    	passwordToClipboardButton = (Button)findViewById(R.id.PWVD_passwordToClipboardButton);
    	
    	itemNameEditText.setTransformationMethod(null);
    	itemPasswordEditText.setTransformationMethod(null);
    	itemEditSaveButton.setTag("edit");
    	itemColorSpinner.setAdapter(itemColorSpinnerAdapter);

	    this.itemEditSaveButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	String mode = (String)v.getTag();
		    	handleItemSave(mode);
		    }
	    });
	    
	    this.itemDeleteButton.setOnClickListener(new OnClickListener()
	    {
		    @Override
		    public synchronized void onClick(View v)
		    {
		    	if(isButtonsLockActivated()) return;
		    	activateButtonsLock();
		    	
		    	List tagMessage = (List)itemNameEditText.getTag();
		    	
        		Dialog deleteDialog = ComponentProvider.getBaseQuestionDialog(v, getResources().getString(R.string.common_delete_text) + " " + getResources().getString(R.string.common_item_text), 
        				getResources().getString(R.string.common_question_delete)
						.replaceAll("<1>", currentFolder.getItemByIndex((Integer)tagMessage.get(1)).getItemName()), (Integer)tagMessage.get(1) + ":" + (String)tagMessage.get(0), PWV_MESSAGE_ITEM_DELETE_CONFIRM);			
		    
        		deleteDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
		    		@Override
		    		public void onCancel (DialogInterface dialogInterface) {
		    			deactivateButtonsLock();
		    		}
		    	});
        		
        		deleteDialog.show();
		    }
	    });
	    
	    this.itemMoveToButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	if(isButtonsLockActivated()) return;
		    	activateButtonsLock();
		    	
		    	List tagMessage = (List)itemNameEditText.getTag();
		    	List<String> itemList = new ArrayList<String>();
		    	List<String> keyList = new ArrayList<String>();
		    	
		    	for(int i = 0; i < vault.getFolderCount(); ++i)
		        {   
		    		VaultFolder tempFolder = vault.getFolderByIndex(i);
		    		itemList.add(tempFolder.getFolderName());
		    		keyList.add(Integer.toString(i) + ":" + 
		    					tempFolder.getFolderSecurityHash() + ":" + 
		    					(Integer)tagMessage.get(1) + ":" + 
		    					(String)tagMessage.get(0));
		        }
		    	Dialog moveToFolderDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
		    			v, 
		    			getResources().getString(R.string.common_moveToFolder_text),
		    			itemList,
		    			keyList,
		    			PasswordVaultActivity.PWV_MESSAGE_ITEM_MOVETOFOLDER);
		    	
		    	moveToFolderDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
		    		@Override
		    		public void onCancel (DialogInterface dialogInterface) {
		    			deactivateButtonsLock();
		    		}
		    	});
		    	
		    	moveToFolderDialog.show();		
		    }
	    });
	    
	    this.passwordGeneratorButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	new PasswordGeneratorDialog(v, PWV_MESSAGE_PWGDIALOG_SET).show();
		    }
	    });
	    
	    this.passwordToClipboardButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
		    	setToSystemClipboard(itemPasswordEditText.getText().toString());
		    	
		    	ComponentProvider.getShowMessageDialog(v, 
		    			getResources().getString(R.string.common_copyToClipboard_text),
		    			getResources().getString(R.string.common_passwordCopiedToClipboard_text) + "<br/><br/>" + getResources().getString(R.string.common_copyToClipboardWarning),
		    			ComponentProvider.DRAWABLE_ICON_INFO_BLUE).show();
		    }
	    });
	    
	    makeLayoutItemDetailReadOnly();
    }
    
    
    /** Serialize, Compress and Encrypt given Vault Object */
    private byte[] serializeVault(Vault passwordVault) throws IOException
    {
		byte[] serializedVault;	
    	String crcZipCompress = "";

    	StringBuffer crc = new StringBuffer();
    	serializedVault = Encryptor.zipObject(passwordVault, crc);
    	crcZipCompress = crc.toString();

		byte[] output = encryptor.encryptWithCRC(serializedVault, crcZipCompress);
		return output;
    }
    
    
    /** Decrypt, Decompress and Deserialize given serialized Vault Object */
    private Vault deserializeVault(byte[] serializedVault) throws Exception
    {
		String crcZipDecompress = "";
		String crcZipFromFile = "";
		
		StringBuffer crcf = new StringBuffer();
		byte[] decrypted = encryptor.decryptWithCRC(serializedVault, crcf);
		crcZipFromFile = crcf.toString();
		
    	Vault unzipedVault;

    	StringBuffer crcd = new StringBuffer();
    	unzipedVault = (Vault)Encryptor.unzipObject(decrypted, crcd);
    	crcZipDecompress = crcd.toString();

		if(!crcZipFromFile.equals(crcZipDecompress)) 
			throw new InvalidParameterException(
					"CRC Failed!");
		return unzipedVault;
    }
    
    
    /** Load Vault Object from Application Database */
    private synchronized Vault loadVaultfromDB() throws Exception
    {
		byte[] dbVault;	
		StringBuffer dbhs = new StringBuffer();
		
		dbVault = DBHelper.getBlobData(PWV_DBPREFIX, dbhs);
    	if(dbVault == null) return null;
		
    	Vault tempVault = deserializeVault(dbVault);
    	tempVault.setStampHashFromDB(dbhs.toString());
    	return tempVault;
    }
    
    
    /** Save Vault Object to Application Database */
    private synchronized void saveVaultToDB() throws IOException
    {
		String oldStampHash = vault.getCurrentStampHash();
		String newStampHash = vault.generateNewStampHash();
		String dbStampHash = null;
		byte[] serializedVault = serializeVault(vault);

		StringBuffer dbhs = new StringBuffer();
		byte[] blobData = DBHelper.getBlobData(PWV_DBPREFIX, dbhs);

		if (oldStampHash == null && blobData != null) // important - don't save FirstRun vault if exist db version
			throw new IllegalStateException(
					"DB inconsistent: current object cannot be saved.");

		if (!dbhs.toString().equals("")) dbStampHash = dbhs.toString();
		
		if (!(oldStampHash == null && dbStampHash == null) && !oldStampHash.equals(dbStampHash))
			throw new IllegalStateException(
					"DB invalid HashStamp: current object cannot be saved.");

		DBHelper.insertUpdateBlobData(PWV_DBPREFIX, serializedVault, newStampHash);
    }
    
    
    /** Set and Reset Item Detail Variables before show */
    private void prepareLayoutItemDetailForShow()
    {
		itemNameEditText.setText(currentItem.getItemName());
    	itemPasswordEditText.setText(currentItem.getItemPassword());
    	itemCommentEditText.setText(currentItem.getItemComment());
    	itemColorSpinner.setSelection(ColorHelper.getColorPosition(currentItem.getColorCode()));
    }
    
    
    /** Prepare Item Detail Layout for View */
    private void makeLayoutItemDetailReadOnly()
    {
	    makeReadOnlyEditText(itemNameEditText);
	    makeReadOnlyEditText(itemPasswordEditText);
	    makeReadOnlyEditText(itemCommentEditText);
	    itemColorSpinner.setEnabled(false);
	    itemColorSpinner.setBackgroundResource(R.drawable.d_edittext_readonly);
	    itemEditSaveButton.setText(getResources().getString(R.string.common_edit_text));
	    itemEditSaveButton.setTag("edit");
	    passwordGeneratorButton.setVisibility(Button.GONE);
	    passwordToClipboardButton.setVisibility(Button.VISIBLE);
    }
    
    
    /** Prepare Item Detail Layout for Edit */
    private void makeLayoutItemDetailEditable()
    {
	    makeEditableEditText(itemNameEditText);
	    makeEditableEditText(itemPasswordEditText);
	    makeEditableEditText(itemCommentEditText);
	    itemColorSpinner.setEnabled(true);
	    itemColorSpinner.setBackgroundResource(R.drawable.d_edittext);
	    itemEditSaveButton.setText(getResources().getString(R.string.common_save_text));
	    itemEditSaveButton.setTag("save");
	    passwordGeneratorButton.setVisibility(Button.VISIBLE);
	    passwordToClipboardButton.setVisibility(Button.GONE);
    }
  
    
    /** Update Item List and other "current items related" variables */
    private void resetItemsList()
    {
        currentItems.clear();
        VaultItem tvi = VaultItem.getSpecial(VaultItem.SPEC_GOBACKITEM, currentFolder.getFolderName());
        tvi.setColorCode(currentFolder.getColorCode());
        currentFolder.notifyItemDataSetChanged();
        currentItems.add(tvi);
        currentItems.addAll(currentFolder.getItemList());
        currentItems.add(VaultItem.getSpecial(VaultItem.SPEC_NEWITEM));
        
        itemsArrayAdapter.notifyDataSetChanged();
    }
    
    
    /** Helper method for "makeLayoutItemDetailReadOnly" method */
    private void makeReadOnlyEditText(EditText et)
    {
    	et.setFocusable(false);
    	et.setFocusableInTouchMode(false);
    	et.setEnabled(false);
    	et.setBackgroundResource(R.drawable.d_edittext_readonly);
    	et.setTextColor(Color.BLACK);
    }
    
    
    /** Helper method for "makeLayoutItemDetailEditable" method */
    private void makeEditableEditText(EditText et)
    {
    	et.setFocusable(true);
    	et.setFocusableInTouchMode(true);
    	et.setEnabled(true);
    	et.setBackgroundResource(R.drawable.d_edittext);
    }
    
    
    /** Solve differences between Portrait and Landscape orientation (Item Detail Layer) */ 
    private void setLayoutItemDetailOrientation()
    {
    	TableLayout lMTL = (TableLayout) this.findViewById(R.id.PWVD_mainTopLeft);
    	TableLayout lMBR = (TableLayout) this.findViewById(R.id.PWVD_mainBottomRight);
    	FrameLayout lC = (FrameLayout) this.findViewById(R.id.PWVD_centerer);
    	
    	int orientation = this.getResources().getConfiguration().orientation;
    	if(orientation == Configuration.ORIENTATION_PORTRAIT)
    	{   	
    		{
	    		RelativeLayout.LayoutParams relativeParams = 
	    			new RelativeLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);    		
	    		relativeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	    		
	    		lMTL.setLayoutParams(relativeParams);
    		}
    		{
	    		RelativeLayout.LayoutParams relativeParams = 
	    			new RelativeLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);   		
	    		relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	    		relativeParams.addRule(RelativeLayout.BELOW, lC.getId());
	    		
	    		lMBR.setLayoutParams(relativeParams);
    		}
    		{
	    		RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(0, 0);
	    		relativeParams.addRule(RelativeLayout.BELOW, lMTL.getId());

	    		lC.setLayoutParams(relativeParams);
    		}
    	}
    	if(orientation == Configuration.ORIENTATION_LANDSCAPE)
    	{
    		{
	    		RelativeLayout.LayoutParams relativeParams = 
	    			new RelativeLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
	    		relativeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	    		relativeParams.addRule(RelativeLayout.LEFT_OF, lC.getId());
	    		
	    		lMTL.setLayoutParams(relativeParams);
    		}
    		{
	    		RelativeLayout.LayoutParams relativeParams = 
	    			new RelativeLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);	    		
	    		relativeParams.addRule(RelativeLayout.RIGHT_OF, lC.getId());
	    		relativeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	    		
	    		lMBR.setLayoutParams(relativeParams);
    		}
    		{
	    		RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(0, 0); 		
	    		relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
	    		
	    		lC.setLayoutParams(relativeParams);
    		}
    	}
    }
    
    private PasswordDialog getStartScreenPasswordDialog()
    {
    	PasswordDialog passwordDialog;
    	byte[] testVault = DBHelper.getBlobData(PWV_DBPREFIX);
    	if(testVault == null)
    		passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD);
    	else {
    		passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_ENTER_PASSWORD);
    		passwordDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    	}
    	passwordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);
    	passwordDialog.setParentMessage("enter");
    	return passwordDialog;
    }
    
    /** Before PasswordVaultActivity Exit */
    private void handleExit()
    {
		if(askOnLeave)
		{
    		ComponentProvider.getBaseQuestionDialog(this, 
					getResources().getString(R.string.common_returnToMainMenuTitle),  
    				getResources().getString(R.string.common_question_leave).replaceAll("<1>", getResources().getString(R.string.common_app_passwordVault_name)), 
    				null, 
    				COMMON_MESSAGE_CONFIRM_EXIT
    				).show();
		}
		else setMessage(new ActivityMessage(COMMON_MESSAGE_CONFIRM_EXIT, null));
    }
    
    /**  Check, Add and Save Item */
    private void handleItemSave(String mode)
    {
    	if (itemNameEditText.getText().toString().trim().equals(""))
    	{
    		new ImageToast(getResources().getString(R.string.common_enterTheName_text), ImageToast.TOAST_IMAGE_CANCEL, this).show();
    		return;
    	}
    	if (itemPasswordEditText.getText().toString().trim().equals(""))
    	{
    		new ImageToast(getResources().getString(R.string.common_enterThePassword_text), ImageToast.TOAST_IMAGE_CANCEL, this).show();
    		return;
    	}
    	
    	if(mode.equals("new"))
    	{
    		currentFolder.addItem(currentItem);
    		itemDeleteButton.setEnabled(true);
    		itemMoveToButton.setEnabled(true);
    	}
    	
    	if(mode.equals("edit"))
    	{
    		makeLayoutItemDetailEditable();
    		itemDeleteButton.setEnabled(false);
    		itemMoveToButton.setEnabled(false);
    	    return;
    	}
    	
    	itemDeleteButton.setEnabled(false);
    	itemEditSaveButton.setEnabled(false);
    	itemMoveToButton.setEnabled(false);
    	
    	List tagMessage = (List)itemNameEditText.getTag();
    	String itemHash = (String)tagMessage.get(0);
    	int position = (Integer)tagMessage.get(1);
    	
    	if(!((String)tagMessage.get(0)).equals(currentItem.getItemSecurityHash())) 
    		throw new IllegalStateException("hash doesn't match");
    	
    	currentItem.setItemName(itemNameEditText.getText().toString().trim());
    	currentItem.setItemPassword(itemPasswordEditText.getText().toString().trim());
    	currentItem.setItemComment(itemCommentEditText.getText().toString().trim());
    	currentItem.setColorCode(ColorHelper.getColorList().get(itemColorSpinner.getSelectedItemPosition()).colorCode);
    	currentItem.setDateModified();

    	try {
			saveVaultToDB();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
		tagMessage.clear();
		tagMessage.add(currentItem.getItemSecurityHash());
		tagMessage.add(position);	
		
		leaveItemDetailLayout();
    }
    
    /**  Leave Item Detail Layout */
    private void leaveItemDetailLayout()
    {
		currentFolder.notifyItemDataSetChanged();
		makeLayoutItemDetailReadOnly();
		resetItemsList();
		layoutSwitcher.showPrevious();
    	itemDeleteButton.setEnabled(true);
    	itemMoveToButton.setEnabled(true);
    	itemEditSaveButton.setEnabled(true);
    }
    
    @Override
    protected void onStart()
    {
        setRunningCode(RUNNING_PASSWORDVAULTACTIVITY);
    	super.onStart();
    }
    
    @Override
    protected void onPause(){
        super.onPause();
        
        if(lockOnPause && layoutSwitcher.getDisplayedChild() > 0)
        {
	        screenLockedPosition = layoutSwitcher.getDisplayedChild();
	        layoutStartButtons.setVisibility(LinearLayout.GONE);
	        layoutSwitcher.setDisplayedChild(0);
        }
    }
    
    @Override
    protected void onResume(){
        super.onResume();

        if(screenLockedPosition > 0)
        {
	        PWVScreenLockDialog sld = new PWVScreenLockDialog(this, encryptor.getDecKeyHash(), encryptor.getDecryptAlgorithmCode());
	        sld.show();
        }
    }
    
    /** Back Button - navigate back in the Password Vault Layers  
     *  if Folders or Start Layer, return to Main Menu
     */
    @Override
    public void onBackPressed()
    {
        switch (layoutSwitcher.getDisplayedChild()) 
        {        
        	case PWV_LAYOUT_FOLDERS:
        	{
        		handleExit();
        		break;
        	}    		
        	case PWV_LAYOUT_ITEMS:
        	{
        		layoutSwitcher.showPrevious();
        		break;
        	}
        	case PWV_LAYOUT_ITEMDETAIL:
        	{
            	boolean itemChanged = 
            	!(
            		currentItem.getItemName().equals(itemNameEditText.getText().toString().trim()) &&
            		currentItem.getItemPassword().equals(itemPasswordEditText.getText().toString().trim()) &&
            		currentItem.getItemComment().equals(itemCommentEditText.getText().toString().trim()) &&
            		(currentItem.getColorCode() == ColorHelper.getColorList().get(itemColorSpinner.getSelectedItemPosition()).colorCode ||
            		currentItem.getColorCode() == -1)       		
            	);
            	
            	if(itemChanged)
            	{
            		ComponentProvider.getBaseQuestionDialog(this, 
            				getResources().getString(R.string.common_save_text),  
            				getResources().getString(R.string.common_question_saveChanges), 
            				(String)itemEditSaveButton.getTag(), 
            				PWV_MESSAGE_ITEM_SAVE_CONFIRM
            				).show();
            	}
            	else leaveItemDetailLayout();
        		break;
        	}
        	case PWV_LAYOUT_START:
        	{
        		handleExit();
        		break;
        	}
        	default: 
            	break;
        }
    }
    
    /** Menu + Search Buttons */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {            
            return true;
        } 
        else if (keyCode == KeyEvent.KEYCODE_MENU) {
        	if(foldersBottomMenu.getVisibility() == LinearLayout.GONE) foldersBottomMenu.setVisibility(LinearLayout.VISIBLE);
        	else foldersBottomMenu.setVisibility(LinearLayout.GONE);
        	return true;
        }
        else return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void onConfigurationChanged(Configuration c)
    {
    	setLayoutItemDetailOrientation();
    		
    	super.onConfigurationChanged(c);
    }
    
    @Override
    public void onWindowFocusChanged(boolean b)
     {
    	if(this.encryptor == null)
    	{
    		layoutStartButtons.setVisibility(LinearLayout.VISIBLE);
    	}
    	super.onWindowFocusChanged(b);
    }  
    
    Handler waitForSaveHandler = new Handler() 
    {
        public void handleMessage(Message msg)  
        {
        	if (msg.what == -100)
        	{            	
        		if(waitDialog != null) waitDialog.cancel();
        		return;
        	}
        	if (msg.what == -400)
        	{  
        		if(waitDialog != null) waitDialog.cancel();
        		Exception e = (Exception)msg.obj;
        		commonToast.setText(e.getMessage());
        		((ImageToast)commonToast).setImage(ImageToast.TOAST_IMAGE_CANCEL);
        		commonToast.show();
        		e.printStackTrace();
        	}
        }
    };
    
    /** Create default Vault Object on the first run */
    private Vault getVaultOnFirstRun(Vault v)
    {
    	Vault vault;
    	if(v == null) vault = Vault.getInstance();
    		else vault = v;
    	
    	//Items
    	VaultItem v00 = new VaultItem();
    	v00.setItemName(getResources().getString(R.string.pwv_data_item_00));
    	v00.setItemPassword(getResources().getString(R.string.pwv_data_item_00_password));
    	v00.setItemComment(getResources().getString(R.string.pwv_data_item_00_comment));
    	v00.setDateModified();
    	v00.setColorCode(Color.rgb(255, 0, 0));
    	waitPlease(3);
    	
    	VaultItem v01 = new VaultItem();
    	v01.setItemName(getResources().getString(R.string.pwv_data_item_01));
    	v01.setItemPassword(getResources().getString(R.string.pwv_data_item_01_password));
    	v01.setItemComment(getResources().getString(R.string.pwv_data_item_00_comment));
    	v01.setDateModified();
    	v01.setColorCode(Color.rgb(0, 0, 255));
    	
    	VaultItem v02 = new VaultItem();
    	v02.setItemName(getResources().getString(R.string.pwv_data_item_02));
    	v02.setItemPassword(getResources().getString(R.string.pwv_data_item_02_password));
    	v02.setItemComment(getResources().getString(R.string.pwv_data_item_02_comment));
    	v02.setDateModified();
    	v02.setColorCode(Color.rgb(255, 255, 0));
    	
    	//Folders
    	VaultFolder v0 = new VaultFolder();
    	v0.setFolderName(getResources().getString(R.string.pwv_data_folder_00));
    	v0.setColorCode(Color.rgb(255, 255, 0));
    	waitPlease(3);
    	vault.addFolder(v0);
    	
    	VaultFolder v1 = new VaultFolder();
    	v1.setFolderName(getResources().getString(R.string.pwv_data_folder_01));
    	v1.setColorCode(Color.rgb(0, 121, 240));
    	waitPlease(3);
    	vault.addFolder(v1);  	
    	
    	VaultFolder v2 = new VaultFolder();
    	v2.setFolderName(getResources().getString(R.string.pwv_data_folder_02));
    	v2.setColorCode(Color.rgb(0, 0, 255));
    	vault.addFolder(v2);
    	waitPlease(3);
    	
    	VaultFolder v3 = new VaultFolder();
    	v3.setFolderName(getResources().getString(R.string.pwv_data_folder_03));
    	v3.setColorCode(Color.rgb(255, 0, 0));
    	v3.addItem(v00);
    	v3.addItem(v01);
    	v3.addItem(v02);
    	vault.addFolder(v3);
    	
    	return vault;
    }
    
    private void waitPlease(int ms)
    {
    	try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    @SuppressWarnings("deprecation")
	private void setToSystemClipboard(String text)
    {
    	ClipboardManager ClipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    	ClipMan.setText(text);
    }

}
