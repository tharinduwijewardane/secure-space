package com.paranoiaworks.unicus.android.sse;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.DataFormatException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StatFs;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.paranoiaworks.unicus.android.sse.adapters.FileEncArrayAdapter;
import com.paranoiaworks.unicus.android.sse.components.DualProgressDialog;
import com.paranoiaworks.unicus.android.sse.components.ImageToast;
import com.paranoiaworks.unicus.android.sse.components.PasswordDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleHTMLDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleWaitDialog;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.PasswordAttributes;
import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.misc.EncryptorException;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.misc.ProgressMessage;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;
import com.tharindu.securespace.R;

/**
 * File Encryptor activity class
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.11
 */ 
public class FileEncActivity extends CryptActivity {
	
	private int encryptAlgorithmCode;
	private boolean askOnLeave;
	private boolean nativeCodeDisabled;
	private boolean showRoot = false;
	private boolean compress = false;
	private boolean startFromFileSystem = false;
	private static boolean pbLock = false;
	private Map<String, Integer> scrollPositionMap = new HashMap<String, Integer>();
	private ProgressBarToken progressBarToken;
	private File currentDir;
	private List<File> availableVolumesList;
	private ArrayAdapter fileArrayAdapter;
	private List<CryptFile> currentFiles = new ArrayList<CryptFile>();
	private ListView filesListView;
	private CryptFile selectedItem;
	private PasswordDialog passwordDialog;
	private List<String> tips = new ArrayList<String>();
	
	private TextView topTextView;
	private TextView bottomTextView;
	private Button startEncDecButton;
	private Button helpButton;
	private Button toMainPageButton;
	private Button compressButton;
	private Button moreButton;
	private Dialog waitDialog;
	private int renderPhase = 0;
	
	
	private Thread dirSizeThread;
	private Thread encDecThread;
	private Thread volumeSizeThread;
	private Thread wipeThread;
	
	private static final int FEA_MESSAGE_DIALOG_FILEACTION= -3101;
	private static final int FEA_MESSAGE_DIALOG_FILEACTION_DELETE_CONFIRM = -3102;
	private static final int FEA_MESSAGE_DIALOG_FILEACTION_RENAME_CONFIRM = -3103;
	private static final int FEA_MESSAGE_DIALOG_FILEACTION_WIPE_CONFIRM = -3104;
	private static final int FEA_MESSAGE_RENDER_CANCEL_CONFIRM = -3105;
	
	private static final int FEA_MESSAGE_AFTERENCRYPT_REPORT = -3111;
	private static final int FEA_MESSAGE_AFTERENCRYPT_DELETE_ASK = -3112;
	
	public static final int FEA_PROGRESSHANDLER_SET_MAINMESSAGE = -3201;
	public static final int FEA_PROGRESSHANDLER_SET_INPUTFILEPATH = -3202;
	public static final int FEA_PROGRESSHANDLER_SET_OUTPUTFILEPATH = -3203;
	public static final int FEA_PROGRESSHANDLER_SET_ERRORMESSAGE = -3204;
	
	private static final int FEA_UNIVERSALHANDLER_SHOW_WAITDIALOG = -3301;
	private static final int FEA_UNIVERSALHANDLER_HIDE_WAITDIALOG = -3302;
	private static final int FEA_UNIVERSALHANDLER_REFRESH_FILELIST = -3303;
	private static final int FEA_UNIVERSALHANDLER_SHOW_DIRSIZE = -3304;
	private static final int FEA_UNIVERSALHANDLER_SHOW_VOLUMESIZE = -3305;
	
	
    @Override
	public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.la_fileenc);
    	encryptAlgorithmCode = settingDataHolder.getItemAsInt("SC_FileEnc", "SI_Algorithm");
    	askOnLeave = settingDataHolder.getItemAsBoolean("SC_Common", "SI_AskIfReturnToMainPage");
    	nativeCodeDisabled = settingDataHolder.getItemAsBoolean("SC_Common", "SI_NativeCodeDisable");
        
        //Intent - External File Path
    	final android.content.Intent intent = getIntent();
    	String externalFilePath = null;
    	if (intent != null) {
    		android.net.Uri data = intent.getData();
    		if (data != null) {
    			CryptFile tempFile = new CryptFile(data.getPath());
				if (tempFile.exists() && tempFile.isFile() && tempFile.isEncrypted()){
					externalFilePath = tempFile.getAbsolutePath();
				}
				else {
					ComponentProvider.getShowMessageDialog(this, null, getResources().getString(R.string.common_incorrectFile_text) + ": " 
							+ tempFile.getName(), ComponentProvider.DRAWABLE_ICON_INFO_RED).show();
				}
				startFromFileSystem = true;
    		}
    	}		
    	
    	//GUI parameters
        topTextView = (TextView) findViewById(R.id.FE_topTextView);
        bottomTextView = (TextView) findViewById(R.id.FE_bottomTextView);
        filesListView = (ListView) findViewById(R.id.FE_list);
		setTitle(getResources().getString(R.string.common_app_fileEncryptor_name));

        
        // Available directories
		availableVolumesList = Helpers.getExtDirectories(getApplicationContext()); 
    
        // Button - Start Encryption/Decryption of file
        startEncDecButton = (Button) findViewById(R.id.FE_startbtn);
        startEncDecButton.setEnabled(false);
        startEncDecButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	showPassworDialog();
		    }
	    });
        
        // More Button
        moreButton = (Button) findViewById(R.id.FE_moreButton);
        moreButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	showFileActionDialog();
		    }
	    });
        
        // Compress Button
        compressButton = (Button) findViewById(R.id.FE_compressButton);
        compressButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	compress = !compress;
		        Drawable img = null;
		        if(compress) img = getResources().getDrawable(R.drawable.compress_act);
		        else img = getResources().getDrawable(R.drawable.compress_inact);
		        compressButton.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null); 
		        String text = compress ? getResources().getString(R.string.fe_compression_on) : getResources().getString(R.string.fe_compression_off);
		        ImageToast it = new ImageToast(text, ImageToast.TOAST_IMAGE_INFO, (Activity)v.getContext());
		        it.setDuration(Toast.LENGTH_SHORT);
		        it.show();
		    }
	    });
        
        // To Main Menu Button
        toMainPageButton = (Button) findViewById(R.id.FE_toMainPageButton);
        toMainPageButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	setRunningCode(0);
		    	finish();
		    }
	    });
        
	    // Help Button
    	this.helpButton = (Button)this.findViewById(R.id.FE_helpButton);
	    this.helpButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	SimpleHTMLDialog simpleHTMLDialog = new SimpleHTMLDialog(v); //Help dialog
		    	simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_FileEncryptor));
		    	simpleHTMLDialog.show();
		    }
	    }); 
          
        
        //+ Create Top Buttons Line (shortcuts to available volumes)
	    LinearLayout rl = (LinearLayout)findViewById(R.id.FE_topLinearLayout);
        Button[] rootDirButtons = new Button[availableVolumesList.size()];
        final String deviceRootTag = "ROOT"; 
        for (int i = 0; i < availableVolumesList.size() && i < 3; ++i)
        {
        	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        	rootDirButtons[i] = (Button)getLayoutInflater().inflate(R.layout.lc_smallbutton_template, null);       	
        	
        	if(!(availableVolumesList.get(i).getAbsolutePath().length() < 2)) //no device ROOT
        	{
	        	rootDirButtons[i].setText(File.separator + availableVolumesList.get(i).getName());
	        	
        	} else {
        		if (!settingDataHolder.getItemAsBoolean("SC_FileEnc", "SI_ShowRoot") && availableVolumesList.size() > 1) continue;
        		rootDirButtons[i].setText(deviceRootTag);
        		rootDirButtons[i].setTag(deviceRootTag);
        	}
        	rootDirButtons[i].setId(i);   	
        	rl.addView((View)rootDirButtons[i]);    		
        	      	
        	rootDirButtons[i].setOnClickListener(new OnClickListener() 
    	    {
    		    @Override
    		    public void onClick(View v) 
    		    {
    		    	Button button = ((Button)v);		    	
    		    	if(!deviceRootTag.equals((String)button.getTag())) { 		    		
    		    		File tempRootDir = availableVolumesList.get(button.getId());
    		    		if (tempRootDir == null || currentDir.getAbsolutePath().equals(tempRootDir.getAbsolutePath())) return;
    		    		currentDir = tempRootDir;
    		    		showRoot = false;
    		    	} else {
    		    		currentDir = new File(File.separator); // device ROOT
    		    		showRoot = true;
    		    	}
    		    	updateCurrentFiles();
    		    	fileArrayAdapter.notifyDataSetChanged();
    		    	filesListView.setSelectionAfterHeaderView();
    		    }
    	    });
        }
        //- Create Top Buttons Line (shortcuts to available volumes)
        
        if(externalFilePath != null) currentDir = new File(new File(externalFilePath).getParent());
        else if(availableVolumesList.size() > 1) currentDir = availableVolumesList.get(1); // second dir in the list use as start dir
        else currentDir = availableVolumesList.get(0); //ROOT
        updateCurrentFiles();
        
        //+ Create File List View
        {
    		fileArrayAdapter = (new FileEncArrayAdapter(this, currentFiles));
    		RelativeLayout emptyView = (RelativeLayout)findViewById(R.id.FE_list_empty);
    		((TextView)emptyView.getChildAt(0)).setText(getResources().getString(R.string.fe_emptyDir_text));
    		filesListView.setEmptyView(emptyView);
    		filesListView.setAdapter(fileArrayAdapter);
    		
    		//click on item (file)
    		filesListView.setOnItemClickListener(new OnItemClickListener() 
    		{
    			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
    			{
    				CryptFile clickedFile = currentFiles.get(position);
    				File parentFile = clickedFile.getParentFile();
            	  
    				// if Directory
    				if (clickedFile.isDirectory())
    				{
    					scrollPositionMap.put(currentDir.getAbsolutePath(), filesListView.getFirstVisiblePosition());
    					currentDir = clickedFile;
    					updateCurrentFiles();
    					fileArrayAdapter.notifyDataSetChanged();  					
    					if(clickedFile.isBackDir()) setHistoricScrollPosition(clickedFile); 
    						else filesListView.setSelectionAfterHeaderView();
    				}
    				// if File
    				else if(clickedFile.isFile())
    				{
    					if(parentFile == null || !parentFile.canWrite())
    					{
    						ComponentProvider.getShowMessageDialog(view, 
    								null, 
    								getResources().getString(R.string.fe_parentDirectoryReadOnly), 
    								ComponentProvider.DRAWABLE_ICON_INFO_RED)
    								.show();
    						return;
    					}
            		  
    					setSelectedItem(clickedFile);
    					fileArrayAdapter.notifyDataSetChanged();
            		  
    					moreButton.setEnabled(true);
    					startEncDecButton.setEnabled(true);
    					if (clickedFile.isEncrypted()) startEncDecButton.setText(getResources().getString(R.string.fe_goButtonDecFile));
    					else startEncDecButton.setText(getResources().getString(R.string.fe_goButtonEncFile));
    				}
    			}
    		});
    		filesListView.setOnItemLongClickListener(new OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
                {
                	CryptFile clickedFile = currentFiles.get(position);
                	File parentFile = clickedFile.getParentFile();
                	
                	if (clickedFile.isBackDir()) return false;
                
                	// Directory
                	if (clickedFile.isDirectory())
                	{
          		    	if(parentFile == null || !parentFile.canWrite())
        		    	{
        		    		ComponentProvider.getShowMessageDialog(view, 
            					null, 
            					getResources().getString(R.string.fe_parentDirectoryReadOnly), 
            					ComponentProvider.DRAWABLE_ICON_INFO_RED)
            					.show();
        		    		return false;
        		    	}
          		    	
          		    	if(clickedFile.listFiles() == null)
        		    	{
        		    		ComponentProvider.getShowMessageDialog(view, 
            					null, 
            					getResources().getString(R.string.fe_directoryCannotBeSelected), 
            					ComponentProvider.DRAWABLE_ICON_INFO_RED)
            					.show();
        		    		return false;
        		    	}
                		
                		setSelectedItem(clickedFile);
                		fileArrayAdapter.notifyDataSetChanged();
                		
                		moreButton.setEnabled(true);
                		startEncDecButton.setEnabled(true);
                		startEncDecButton.setText(getResources().getString(R.string.fe_goButtonEncDir));
            			
                		final String dirPath = clickedFile.getAbsolutePath();
                		((FileEncArrayAdapter)fileArrayAdapter).removeDirSize(dirPath);
                		if(dirSizeThread != null) dirSizeThread.interrupt();
                		dirSizeThread = new Thread (new Runnable() 
            			{
            				public void run() 
            				{
            					Long dirSize = null;
								try {
									dirSize = Helpers.getDirectorySizeWithInterruptionCheck(new File(dirPath));
								} catch (InterruptedException e) {
									dirSize = -1l;
									//SSElog.d("DirSizeThread: ", e.getMessage());
								}
            					List message = new ArrayList();
            					message.add(dirPath);
            					message.add(dirSize);
            					universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_SHOW_DIRSIZE, message));
            				}
            			});
                		dirSizeThread.setPriority(Thread.MIN_PRIORITY);
                		dirSizeThread.start();        			
                	}
                	// File
                	else if(clickedFile.isFile())
                	{
        		    	if(!clickedFile.canWrite())
        		    	{
        		    		ComponentProvider.getShowMessageDialog(view, 
            					null, 
            					getResources().getString(R.string.fe_fileReadOnly), 
            					ComponentProvider.DRAWABLE_ICON_INFO_RED)
            					.show();
        		    		return false;
        		    	}
                		
                		List<String> itemList = new ArrayList<String>();
        		    	List<String> keyList = new ArrayList<String>();
        		    	itemList.add(getResources().getString(R.string.fe_fileactionDialog_renameFile));
        		    	keyList.add("fe_fileactionDialog_renameFile");
        		    	itemList.add(getResources().getString(R.string.fe_fileactionDialog_deleteFile));
        		    	keyList.add("fe_fileactionDialog_deleteFile");
        		    	itemList.add(getResources().getString(R.string.fe_fileactionDialog_wipeFile));
        		    	keyList.add("fe_fileactionDialog_wipeFile");      		    	
        		    	if(!clickedFile.isEncrypted())
        		    	{
	        		    	itemList.add(getResources().getString(R.string.fe_fileactionDialog_openFile));
	        		    	keyList.add("fe_fileactionDialog_openFile");
        		    	}
        		    	itemList.add(getResources().getString(R.string.fe_fileactionDialog_sendFile));
        		    	keyList.add("fe_fileactionDialog_sendFile");  
        		    	AlertDialog fileActionDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
        		    			view, 
        		    			getResources().getString(R.string.fe_file_dialogTitle),
        		    			itemList,
        		    			keyList,
        		    			FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION,
        		    			clickedFile);
        		    	if (fileActionDialog != null) fileActionDialog.show();
                	}
          
                	return true;
                }
              });	
        }
        //- Create File List View
        
        // External File Path
        if(externalFilePath != null)
        {
        	int externalFileIndex = getFileIndex(new File(externalFilePath));
        	if(externalFileIndex < 0) return;
        	CryptFile externalFile = currentFiles.get(externalFileIndex);
			setSelectedItem(externalFile);
			fileArrayAdapter.notifyDataSetChanged();
        	filesListView.setSelectionFromTop(externalFileIndex, 0);
  		  
			moreButton.setEnabled(true);
			startEncDecButton.setEnabled(true);
			if(externalFile.isEncrypted()) startEncDecButton.setText(getResources().getString(R.string.fe_goButtonDecFile));
			else startEncDecButton.setText(getResources().getString(R.string.fe_goButtonEncFile));
			showPassworDialog();
        }
    }
 
    
    /** Handle Message */
    protected void processMessage() //made protected by th
    {
        ActivityMessage am = getMessage();
        if (am == null) return;
        
        int messageCode = am.getMessageCode();
        //SSElog.d("Activity Message: ", ""  + messageCode);
        switch (messageCode) 
        {        
        	case CryptActivity.COMMON_MESSAGE_SET_ENCRYPTOR:
            	this.passwordAttributes = (PasswordAttributes)((List)am.getAttachement()).get(0);
            	this.encryptor = (Encryptor)((List)am.getAttachement()).get(1);
            	if(!nativeCodeDisabled) this.encryptor.enableNativeCodeEngine();
    	        this.resetMessage();
    	        
		    	try {				
		    		startEncDec();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					e.printStackTrace();
				}
    	        
            	break;
        	
        	case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION:
        		if (am.getMainMessage().equals("fe_fileactionDialog_renameFile"))
        		{
        			Dialog fileSetNameDialog = ComponentProvider.getFileSetNameDialog(this, (File)am.getAttachement(), FEA_MESSAGE_DIALOG_FILEACTION_RENAME_CONFIRM);
        			fileSetNameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        			fileSetNameDialog.show();
        		}
        		else if (am.getMainMessage().equals("fe_fileactionDialog_deleteFile"))
        		{
        			File tFile = (File)am.getAttachement();
            		String title = "";
            		if(tFile.isFile()) title = getResources().getString(R.string.fe_deleteFile_dialogTitle);
            			else if(tFile.isDirectory()) title = getResources().getString(R.string.fe_deleteFolder_dialogTitle);
            		
        			ComponentProvider.getBaseQuestionDialog(this, 
        					title, 
            				getResources().getString(R.string.common_question_delete).replaceAll("<1>", tFile.getName()), 
            				tFile.getAbsolutePath(), 
            				FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_DELETE_CONFIRM)
            				.show();
        		}
        		else if (am.getMainMessage().equals("fe_fileactionDialog_wipeFile"))
        		{
        			File tFile = (File)am.getAttachement();
            		String title = "";
            		if(tFile.isFile()) title = getResources().getString(R.string.fe_wipeFile_dialogTitle);
            			else if(tFile.isDirectory()) title = getResources().getString(R.string.fe_wipeFolder_dialogTitle);
            		
        			ComponentProvider.getBaseQuestionDialog(this, 
        					title, 
            				getResources().getString(R.string.common_question_wipe).replaceAll("<1>", tFile.getName()), 
            				tFile.getAbsolutePath(), 
            				FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_WIPE_CONFIRM)
            				.show();
        		}
        		else if (am.getMainMessage().equals("fe_fileactionDialog_openFile"))
        		{
        			File tFile = (File)am.getAttachement();
        			try {      
        				MimeTypeMap mime = MimeTypeMap.getSingleton();
        				String ext = Helpers.getFileExt(tFile).toLowerCase();
        				String type = mime.getMimeTypeFromExtension(ext);
        			    if(type == null) type = "*/*";
        				
        				Intent intent = new Intent();
        				intent.setAction(Intent.ACTION_VIEW);
        				intent.setDataAndType(Uri.fromFile(tFile), type);		              
						startActivity(Intent.createChooser(intent, getResources().getString(R.string.fe_fileactionDialog_openFile)));
					} catch (Exception e) {
						ComponentProvider.getImageToast(getResources().getString(R.string.fe_cannotPerformThisAction), 
								ImageToast.TOAST_IMAGE_CANCEL, this).show();
					}
        		}
        		else if (am.getMainMessage().equals("fe_fileactionDialog_sendFile"))
        		{
        			File tFile = (File)am.getAttachement();
        			try {      
        				MimeTypeMap mime = MimeTypeMap.getSingleton();
        				String ext = Helpers.getFileExt(tFile).toLowerCase();
        				if(ext.equalsIgnoreCase(CryptFile.ENC_FILE_EXTENSION) || ext.equalsIgnoreCase(PasswordVaultActivity.PWV_EXPORT_EXT)) ext = "zip"; // as archive behavior 
        				String type = mime.getMimeTypeFromExtension(ext);
        				if(type == null) type = "*/*";
        				
        				Intent intent = new Intent(Intent.ACTION_SEND);     				
        				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tFile));
        				intent.setType(type);
        				startActivity(Intent.createChooser(intent, getResources().getString(R.string.fe_fileactionDialog_sendFile)));
					} catch (Exception e) {
						ComponentProvider.getImageToast(getResources().getString(R.string.fe_cannotPerformThisAction), 
								ImageToast.TOAST_IMAGE_CANCEL, this).show();
					}
        		}
        		this.resetMessage();
        		break;
            	
        	case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_RENAME_CONFIRM:
        		updateCurrentFiles();
        		fileArrayAdapter.notifyDataSetChanged();
        		this.resetMessage();
        		break;
        		
        	case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_DELETE_CONFIRM:
				if(am.getAttachement().equals(new Integer(1)))
				{
            		final File tFile = new File(am.getMainMessage());
            		
            		if(tFile.isFile())
            		{
            			if(tFile.delete())
	            		{
		            		updateCurrentFiles();
		            		fileArrayAdapter.notifyDataSetChanged();
							ComponentProvider.getImageToast(this.getResources().getString(R.string.fe_fileDeleted), 
									ImageToast.TOAST_IMAGE_OK, this)
									.show();
	            		}
	            		else
	            		{
							ComponentProvider.getImageToast(this.getResources().getString(R.string.fe_fileNotDeleted), 
									ImageToast.TOAST_IMAGE_CANCEL, this)
									.show();
	            		}
            		} 
            		else if(tFile.isDirectory())
            		{
            			waitDialog = new SimpleWaitDialog(this);
            			waitDialog.setTitle(getResources().getString(R.string.common_deleting_text) + "...");
            			new Thread (new Runnable() 
            			{
            				public void run() 
            				{
            					PowerManager.WakeLock wakeLock;
            					PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            					wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FE_DELETE_DIR");
            					
            					wakeLock.acquire();
            					universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_SHOW_WAITDIALOG));
		            			Helpers.deleteDirectory(tFile);
		            			universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_HIDE_WAITDIALOG));
		            			universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_REFRESH_FILELIST));
		            			wakeLock.release();
            				}
            			}).start();
            		}
				}
        		this.resetMessage();
        		break;
        		
        	case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_WIPE_CONFIRM:
				if(am.getAttachement().equals(new Integer(1)))
				{
            		final File tFile = new File(am.getMainMessage());
            		
    		    	if(dirSizeThread != null) dirSizeThread.interrupt();
    		    	progressBarToken = new ProgressBarToken();          		
            		initProgressBar();
    		    	progressBarToken.setProgressHandler(progressHandler);
    		    	progressBarToken.getDialog().show();

					wipeThread = new Thread (new Runnable() {
						public void run() {			        	   
							if(pbLock) return;
					    	pbLock = true;
							PowerManager.WakeLock wakeLock;
							PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
							wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WIPE");
							
							wakeLock.acquire();
							try {
								Helpers.wipeFileOrDirectory(tFile, progressBarToken);
							} catch (InterruptedException e) {
			            	   sendPBTMessage(-401, e.getMessage());
							} catch (Exception e) {
			            	   sendPBTMessage(-400, e.getLocalizedMessage());
			            	   e.printStackTrace();
			               } finally {    
			            	   progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), -100));
			            	   wakeLock.release();
			               }
			           }
			        });
			        
			        // start wiping
					wipeThread.start();            
				}
        		this.resetMessage();
        		break;
        		
        	case FileEncActivity.FEA_MESSAGE_AFTERENCRYPT_REPORT:
				FinalMessageBean fmb = (FinalMessageBean)(am.getAttachement());
				String reportTitle = getResources().getString(R.string.fe_report_title);
				StringBuffer report = new StringBuffer();
				Dialog showMessageDialog = null;
				
				File iFile = new File(fmb.inputFilePath);
				File oFile = new File(fmb.outputFilePath);
				
				if(!fmb.errorMessage.equals(""))
				{
					showMessageDialog = ComponentProvider.getShowMessageDialog(this, 
	    					reportTitle, 
	    					fmb.errorMessage, 
	    					ComponentProvider.DRAWABLE_ICON_CANCEL
	    			);
					showMessageDialog.show();
					this.resetMessage();
					return;				
				}
				
				
				if(fmb.mainMessage.equals("encrypting"))
				{
					reportTitle = getResources().getString(R.string.fe_report_enc_title);
					if(iFile.isDirectory())
						report.append(getResources().getString(R.string.fe_report_inputFolder).replaceAll("<1>", iFile.getName()));
					else
						report.append(getResources().getString(R.string.fe_report_inputFile).replaceAll("<1>", iFile.getName()));
					report.append("<br/>");
					report.append(getResources().getString(R.string.fe_report_outputFile).replaceAll("<1>", oFile.getName()));
				}
				else if(fmb.mainMessage.equals("decrypting"))
				{
					reportTitle = getResources().getString(R.string.fe_report_dec_title);
					report.append(getResources().getString(R.string.fe_report_inputFile).replaceAll("<1>", iFile.getName()));
					report.append("<br/>");
					
					if(oFile.isDirectory())
						report.append(getResources().getString(R.string.fe_report_outputFolder).replaceAll("<1>", oFile.getName()));
					else
						report.append(getResources().getString(R.string.fe_report_outputFile).replaceAll("<1>", oFile.getName()));
				}
				
				 // wiping report
				if(fmb.files > 0 || fmb.folders > 0)
				{
					if(fmb.mainMessage.equals("encrypting")) report.append("<br/><br/>");
					report.append(getResources().getString(R.string.fe_report_wiped) + "<br/>");
					report.append(getResources().getString(R.string.fe_report_wipedFiles)
							.replaceAll("<1>", "" + fmb.deletedFiles).replaceAll("<2>", "" + fmb.files) + "<br/>");
					report.append(getResources().getString(R.string.fe_report_wipedFolders)
							.replaceAll("<1>", "" + fmb.deletedFolders).replaceAll("<2>", "" + fmb.folders));
				}
							
				boolean afterDelete = false; // afterDelete not used		
				if(afterDelete)
				{
					showMessageDialog = ComponentProvider.getShowMessageDialog(this, 
	    					reportTitle, 
	    					report.toString(), 
	    					ComponentProvider.DRAWABLE_ICON_OK,
	    					fmb.inputFilePath,
	    					FEA_MESSAGE_AFTERENCRYPT_DELETE_ASK
	    			);
				}
				else 
				{
					int iconCode = !fmb.mainMessage.equals("decrypting") && !fmb.mainMessage.equals("encrypting") 
						? ComponentProvider.DRAWABLE_ICON_INFO_BLUE : ComponentProvider.DRAWABLE_ICON_OK;
					showMessageDialog = ComponentProvider.getShowMessageDialog(this, 
	    					reportTitle, 
	    					report.toString(), 
	    					iconCode
	    			);
				}			
    			showMessageDialog.show();

        		this.resetMessage();
        		break;
        		
        	case FileEncActivity.FEA_MESSAGE_AFTERENCRYPT_DELETE_ASK:
        		File tFile = new File(am.getMainMessage());
        		String title = "";
        		if(tFile.isFile()) title = getResources().getString(R.string.fe_deleteFile_dialogTitle);
        		else if(tFile.isDirectory()) title = getResources().getString(R.string.fe_deleteFolder_dialogTitle);
        		
        		ComponentProvider.getBaseQuestionDialog(this, 
        				title, 
        				getResources().getString(R.string.common_question_delete).replaceAll("<1>", tFile.getName()), 
        				am.getMainMessage(), 
        				FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_DELETE_CONFIRM)
        				.show();
        		this.resetMessage();
        		break;
        		
        	case FileEncActivity.FEA_MESSAGE_RENDER_CANCEL_CONFIRM:
				if(am.getAttachement().equals(new Integer(1)))
				{
					if(renderPhase == 11) // temp files wiping
					{
						Toast toast = ComponentProvider.getImageToast(this.getResources().getString(R.string.fe_message_wipe_interrupted_tempfiles), 
							ImageToast.TOAST_IMAGE_INFO, this);
						toast.setDuration(Toast.LENGTH_LONG);
						toast.show();
						return;
					}
					
					if(encDecThread != null) encDecThread.interrupt();
					if(wipeThread != null) wipeThread.interrupt();

					if(renderPhase == 10) // stand alone wiping
					{
						Toast toast = ComponentProvider.getImageToast(this.getResources().getString(R.string.fe_message_wipe_interrupted_aftercurrent), 
							ImageToast.TOAST_IMAGE_INFO, this);
						toast.setDuration(Toast.LENGTH_LONG);
						toast.show();
					}
				} else {
					progressBarToken.getDialog().getWindow().setGravity(Gravity.CENTER);
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

    
    /** Encryption/Decryption Button click implementation */
    public void startEncDec() throws NoSuchAlgorithmException, InvalidKeySpecException 
    {	 
    	if(pbLock) return;
    	if(selectedItem == null || selectedItem.getAbsolutePath().trim().equals("")) return;
    	pbLock = true;

		encDecThread = new Thread (new Runnable() {
           public void run() {
        	   
    		   PowerManager.WakeLock wakeLock;
    		   PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    		   wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FE");
    		   wakeLock.acquire();

        	   try {
        		   doEncDec();
            	   progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), -200));
               } catch (DataFormatException e) {
            	   String message = e.getMessage();
            	   try {message = getResources().getString(Integer.parseInt(message));}catch(Exception ie){};
            	   sendPBTMessage(-401, message);
               } catch (EncryptorException e) {
            	   String message = e.getMessage();
            	   sendPBTMessage(-401, message);
               } catch (InterruptedException e) {
            	   sendPBTMessage(-401, e.getMessage());
               } catch (NoSuchAlgorithmException e) {
            	   sendPBTMessage(-401, getResources().getString(R.string.common_unknownAlgorithm_text));
               } catch (Exception e) {
            	   sendPBTMessage(-400, e.getMessage());
            	   e.printStackTrace();
               } finally {    
            	   progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), -100));
            	   wakeLock.release();
               }
           }
        });
        
        // start ENC/DEC executor thread
		encDecThread.start();              
    }

    
	/** Initialize ENC/DEC ProgressBar */
	private void initProgressBar()
	{
		final Dialog cancelDialog = ComponentProvider.getBaseQuestionDialog(
				this, 
				getResources().getString(R.string.fe_question_cancel_title), 
				getResources().getString(R.string.fe_question_cancel_question)
				.replaceAll("<1>", ""), "X", FileEncActivity.FEA_MESSAGE_RENDER_CANCEL_CONFIRM);
		
		final DualProgressDialog pd = new DualProgressDialog(this);
		pd.setCancelable(false);
		pd.setMessage("");
		//pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setProgress(0);
		pd.setMax(100);
		
		progressBarToken.setDialog(pd);
		progressBarToken.setCancelDialog(cancelDialog);
		progressBarToken.setIncrement(1);
		
		pd.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {		
		        if (keyCode == KeyEvent.KEYCODE_BACK) 
		        {            		
    				pd.getWindow().setGravity(Gravity.TOP);
		        	cancelDialog.getWindow().setGravity(Gravity.BOTTOM);
    				cancelDialog.show();
		        	return true;
		        }
		        return true;
			}
		});        
	}
	
	
	/** Encrypt/Decrypt selected Folder/File */
	private synchronized void doEncDec () throws Exception
	{	
		CryptFile inputFile = new CryptFile(selectedItem);

		if (!inputFile.exists())
		{
			throw new FileNotFoundException();
		}
			
		this.sendPBTMessage(FEA_PROGRESSHANDLER_SET_INPUTFILEPATH, inputFile.getAbsolutePath());
			
		if(inputFile.isEncrypted()) //decryption
		{
			try {
				this.sendPBTMessage(FEA_PROGRESSHANDLER_SET_MAINMESSAGE, "decrypting");
				long start = System.currentTimeMillis();
				
				encryptor.unzipAndDecryptFile(inputFile, progressBarToken);
				
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
		else // encryption
		{
			try {
				this.sendPBTMessage(FEA_PROGRESSHANDLER_SET_MAINMESSAGE, "encrypting");
				long start = System.currentTimeMillis();
				
				encryptor.zipAndEncryptFile(inputFile, compress, progressBarToken);

				long time = (System.currentTimeMillis() - start);
				//SSElog.d("Enc Time: " + time + " : " + !nativeCodeDisabled + " : " + inputFile.getName() + " : " + inputFile.length());
				if(settingDataHolder.getItemAsBoolean("SC_FileEnc", "SI_WipeSourceFiles")) Helpers.wipeFileOrDirectory(inputFile, progressBarToken);
			} catch (Exception e) {
				
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
	
	
	/** Update File List and other "current files related" variables */
	private void updateCurrentFiles()
    {  	
    	currentFiles.clear();
    	selectedItem = null;
    	moreButton.setEnabled(false);
    	startEncDecButton.setEnabled(false);
    	startEncDecButton.setText(getResources().getString(R.string.fe_goButton));
    	if(fileArrayAdapter != null)((FileEncArrayAdapter)fileArrayAdapter).clearDirSizeMap();
    	if(dirSizeThread != null) dirSizeThread.interrupt();

		if (currentDir.getParent() != null && (currentDir.getParent().length() > 1 || showRoot)) // restrict ROOT
		{
			CryptFile backDir = new CryptFile(currentDir.getParent());
			backDir.setBackDir(true);
			currentFiles.add(backDir);
		}
    	
    	if (currentDir.listFiles() != null)
		{
    		File[] tempList = currentDir.listFiles();
    		for (int j = 0; j < tempList.length; ++j)
    		{
    			currentFiles.add(new CryptFile(tempList[j]));
    		}
		}	
    	
    	topTextView.setText(getResources().getString(R.string.fe_currentDir_text) + " " + currentDir.getAbsolutePath());
    	
    	Collections.sort(currentFiles);
    	
    	bottomTextView.setEllipsize(TruncateAt.END); 	   	
    	if(currentFiles != null && currentFiles.size() > 0 && currentFiles.get(0) != null && currentFiles.get(0).isBackDir()) // is Top Dir then first tip?
    		bottomTextView.setText(getTip(1)); // 0 for random
    	else bottomTextView.setText(getTip(1)); 
    	   	
    	final String absulutePath = currentDir.getAbsolutePath();
    	if(!Helpers.getFirstDirFromFilepathWithLFS(absulutePath).equals((String)getTitleRightTag()))setTitleRight("");
    	if(volumeSizeThread != null) return;
		volumeSizeThread = new Thread (new Runnable() 
		{
			public void run()
			{
		    	String titleRight = "";
		    	String titleRightTag = Helpers.getFirstDirFromFilepathWithLFS(absulutePath);
		    	List message = new ArrayList(); 
				try {
					StatFs stat = new StatFs(absulutePath);
					long blockSize = (long)stat.getBlockSize();
					long sdAvailSize = (long)stat.getAvailableBlocks() * blockSize;
					long sdSize = (long)stat.getBlockCount() * blockSize;
					if(sdSize < 1) throw new Exception();
					titleRight = (Helpers.getFormatedFileSize(sdAvailSize) + File.separator + Helpers.getFormatedFileSize(sdSize));
				} catch (Exception e) {
			    	//e.printStackTrace();
				}
				message.add(titleRight);
				message.add(titleRightTag);
				universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_SHOW_VOLUMESIZE, message));
			}
		});
		volumeSizeThread.setPriority(Thread.MIN_PRIORITY);
		volumeSizeThread.start(); 
    }
	
    /** Restore "historic" ListView position using scrollPositionMap */
    private void setHistoricScrollPosition(CryptFile file)
    { 
		if(file.isBackDir())
		{
			Integer index = scrollPositionMap.get(file.getAbsolutePath());
			if(index != null) filesListView.setSelectionFromTop(index, 0);
		} else filesListView.setSelectionAfterHeaderView();
    }
    
    /** Get File position in parent directory */
    private int getFileIndex(File file)
    { 
    	int fileIndex = -1;
    	for(int i = 0; i < currentFiles.size(); ++i)
    	{
    		if(currentFiles.get(i).getAbsolutePath().equals(file.getAbsolutePath()))
    		{
    			fileIndex = i;
    			break;
    		}
    	}
    	return fileIndex;
    }
	
    /** Set this file as the Selected One */
    private void setSelectedItem(CryptFile file)
    { 
    	if(selectedItem != null) selectedItem.setSelected(false);
    	selectedItem = file;
    	selectedItem.setSelected(true);
    	bottomTextView.setEllipsize(TruncateAt.START);
		bottomTextView.setText(getResources().getString(R.string.fe_selected_text) + " " + selectedItem.getName());
    }
    	
	/** ShortCut for progressBarToken...sendMessage...  */
	public void sendPBTMessage(int message, Object attachement)
	{
		progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), 
				message, 
				attachement
				));
	}
	
	/** Get Tip Text (fe_tip_X) - code == 0 for random */
	private String getTip(int tipCode)
	{	
		if(tips.size() == 0)
		{
			int tipCounter = 0;	
			while(true)
			{
				++tipCounter;
				String resourceName = "fe_tip_" + tipCounter;
				String tempTip = getStringResource(resourceName);
				if(!tempTip.equals(resourceName)) tips.add(tempTip);
				else break;		
			}
		}
		
		String tip = "NULL";
		int tipIndex;
		
		if(tipCode > 0) tipIndex = tipCode - 1;
		else 
		{
			Random rand = new Random(System.currentTimeMillis());
			tipIndex = rand.nextInt(tips.size());
		}	
		try {tip = tips.get(tipIndex);} catch (Exception e) {}

		return tip;
	}
	
    /** Back Button - if current dir has a parent show the parent - else go to the main menu  */
    @Override
    public void onBackPressed()
    {
    	CryptFile cf = null;
    	if(currentFiles != null && currentFiles.size() > 0) cf = currentFiles.get(0);
    	if (cf != null && cf.isBackDir())
    	{
  		  currentDir = cf;
		  updateCurrentFiles();
		  fileArrayAdapter.notifyDataSetChanged();
		  setHistoricScrollPosition(cf); 
    	}
    	else
    	{	
			if(askOnLeave && !startFromFileSystem)
			{
	    		ComponentProvider.getBaseQuestionDialog(this, 
						getResources().getString(R.string.common_returnToMainMenuTitle),  
	    				getResources().getString(R.string.common_question_leave).replaceAll("<1>", getResources().getString(R.string.common_app_fileEncryptor_name)), 
	    				null, 
	    				COMMON_MESSAGE_CONFIRM_EXIT
	    				).show();
			}
			else setMessage(new ActivityMessage(COMMON_MESSAGE_CONFIRM_EXIT, null));
    	}
    }
    
    @Override
    protected void onStart ()
    {
        setRunningCode(RUNNING_FILEENCACTIVITY);
    	super.onStart();
    }
    
    @Override
    public void onDestroy() {
    	if(dirSizeThread != null) dirSizeThread.interrupt();
    	super.onDestroy();
    }
    
    /** Handle Menu Button, ignore Search Button, else as default */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {            
            return true;
        } 
        else if (keyCode == KeyEvent.KEYCODE_MENU) {
        	showFileActionDialog();
        	return true;
        } 
        else return super.onKeyDown(keyCode, event);
    }
    
    // Show Password dialog
    private void showPassworDialog()
    {
		if(isButtonsLockActivated()) return;
		activateButtonsLock();
		
		if(dirSizeThread != null) dirSizeThread.interrupt();
		CryptFile tempCF = (CryptFile)selectedItem;
		progressBarToken = new ProgressBarToken();
			
		if(tempCF.isEncrypted()) passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_ENTER_PASSWORD);
			else passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD);
		initProgressBar();
		progressBarToken.setProgressHandler(progressHandler);
		passwordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);
		passwordDialog.setWaitDialog(progressBarToken, false);
		
		passwordDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel (DialogInterface dialogInterface) {
				deactivateButtonsLock();
			}
		});
		
		if(passwordDialog.getDialogMode() == PasswordDialog.PD_MODE_ENTER_PASSWORD) 
			passwordDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		passwordDialog.show();
    }
    
    // Show FileAction (More) dialog
    private void showFileActionDialog()
    {
    	if(dirSizeThread != null) dirSizeThread.interrupt();
    	if(selectedItem == null || selectedItem.getAbsolutePath().trim().equals(""))
    	{
    		new ImageToast(getResources().getString(R.string.fe_fileactionDialog_noFileSelected), ImageToast.TOAST_IMAGE_CANCEL, this).show();
    		return;
    	}

    	String renameText = selectedItem.isFile() ?  getResources().getString(R.string.fe_fileactionDialog_renameFile) 
    			: getResources().getString(R.string.fe_fileactionDialog_renameFolder);
    	String deleteText = selectedItem.isFile() ?  getResources().getString(R.string.fe_fileactionDialog_deleteFile) 
    			: getResources().getString(R.string.fe_fileactionDialog_deleteFolder);
    	String wipeText = selectedItem.isFile() ?  getResources().getString(R.string.fe_fileactionDialog_wipeFile) 
    			: getResources().getString(R.string.fe_fileactionDialog_wipeFolder);
    	String titleText = selectedItem.isFile() ?  getResources().getString(R.string.fe_file_dialogTitle) 
    			: getResources().getString(R.string.fe_folder_dialogTitle);
    	
    	List<String> itemList = new ArrayList<String>();
    	List<String> keyList = new ArrayList<String>();
    	itemList.add(renameText);
    	keyList.add("fe_fileactionDialog_renameFile");
    	itemList.add(deleteText);
    	keyList.add("fe_fileactionDialog_deleteFile");
    	itemList.add(wipeText);
    	keyList.add("fe_fileactionDialog_wipeFile");
    	if(selectedItem.isFile())
    	{
	    	if(!selectedItem.isEncrypted())
	    	{
	    		itemList.add(getResources().getString(R.string.fe_fileactionDialog_openFile));
		    	keyList.add("fe_fileactionDialog_openFile");
	    	}
	    	itemList.add(getResources().getString(R.string.fe_fileactionDialog_sendFile));
	    	keyList.add("fe_fileactionDialog_sendFile"); 
    	}
    	AlertDialog fileActionDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
    			this, 
    			titleText,
    			itemList,
    			keyList,
    			FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION,
    			selectedItem);
    	if (fileActionDialog != null) fileActionDialog.show();
    }


	// Handler for the background ENC/DEC progress updating
    Handler progressHandler = new Handler() 
    {
    	FinalMessageBean finalMessageBean = new FinalMessageBean();
    	
    	public synchronized void handleMessage(Message msg) 
        {        	
        	if (msg.what == -1100){
        		int progressRel = ((ProgressMessage)msg.obj).getProgressRel();
        		//int secondaryProgressRel = ((ProgressMessage)msg.obj).getSecondaryProgressRel();
        		progressBarToken.getDialog().setProgress(progressRel);
        		return;
        	}
        	if (msg.what == -1011){ // compressing + encrypting (one pass - since 1.3)
        		progressBarToken.getDialog().setMessage(Html.fromHtml(getResources().getString(R.string.common_encrypting_text) 
        				+ " (<small>" + encryptor.getEncryptAlgorithmShortComment() + "</small>)"));
        		renderPhase = 2;
        		return;
        	}
        	if (msg.what == -1201){ // wiping only (Helpers.wipeFileOrDirectory(...))
        		progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_wiping_text));
        		renderPhase = 10;
        		return;
        	}
        	if (msg.what == -1202){ // temp files wiping
        		progressBarToken.getDialog().setMessage(getResources().getString(R.string.fe_wipingTempFiles));
        		renderPhase = 11;
        		return;
        	}
        	if (msg.what == -1211){ // directory stats from delete/wipe procedures
        		Helpers.DirectoryStats ds = null;
        		if(msg.obj != null) ds = ((Helpers.DirectoryStats)msg.obj);	
        		finalMessageBean.files = ds.allFiles;
        		finalMessageBean.folders = ds.allFolders;
        		finalMessageBean.deletedFiles = ds.okFiles;
        		finalMessageBean.deletedFolders = ds.okFolders;
        		return;
        	}
        	if (msg.what == 0){ // reset progress
        		progressBarToken.getDialog().setProgress(0);
        		return;
        	}
        	if (msg.what == -1000){ // generatingKey
        		progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_generatingKey_text));
        		renderPhase = 1;
        		return;
        	}
        	if (msg.what == -1001){ // compressing
        		progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_compressing_text));
        		renderPhase = 1;
        		return;
        	}
        	if (msg.what == -1002){ // encrypting
        		progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_encrypting_text) 
        				+ "  (" + encryptor.getEncryptAlgorithmShortComment() + ")");
        		renderPhase = 2;
        		return;
        	}
        	if (msg.what == -1003){ // decompressing
        		progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_decompressing_text));
        		renderPhase = 3;
        		return;
        	}
        	if (msg.what == -1004){ // decrypting
        		progressBarToken.getDialog().setMessage(Html.fromHtml(getResources().getString(R.string.common_decrypting_text) 
        				+ "  (<small>" +  encryptor.getDecryptAlgorithmShortComment() + "</small>)"));
        		renderPhase = 4;
        		return;
        	}
        	
        	if (msg.what == -400){ // handle unexpected errors         	
        		String error = "error";
        		if(msg.obj != null) error = ((String)msg.obj);
        		finalMessageBean.errorMessage = error; 		
        		return;
        	}
        	if (msg.what == -401){ // handle interruption and other "expected" events     	
        		String error = "interruption";
        		if(msg.obj != null) error = ((String)msg.obj);
        		finalMessageBean.errorMessage = error; 
        		return;
        	}
            if(msg.what == -200)
            {       	         	
        		//not used
            	return;
            }
            if(msg.what == -100) // finalize ENC/DEC/WIPE
            {       	
    		 	progressBarToken.getCancelDialog().cancel();    		 	
            	updateCurrentFiles();
    		 	fileArrayAdapter.notifyDataSetChanged();
            	progressBarToken.getDialog().cancel();
            	pbLock = false;
            	renderPhase = 0;
            	encDecThread = null;
            	wipeThread = null;
            	setMessage(new ActivityMessage(FEA_MESSAGE_AFTERENCRYPT_REPORT, null, finalMessageBean.clone()));
            	finalMessageBean.reset();
            	
            	return;
            }
            
            //+ Create "After ENC/DEC Report"
            if(msg.what == FEA_PROGRESSHANDLER_SET_MAINMESSAGE)
            {  
            	finalMessageBean.mainMessage = ((String)msg.obj);
            	return;
            }
            if(msg.what == FEA_PROGRESSHANDLER_SET_INPUTFILEPATH)
            {  
            	finalMessageBean.inputFilePath = ((String)msg.obj);
            	return;
            }
            if(msg.what == FEA_PROGRESSHANDLER_SET_OUTPUTFILEPATH)
            {  
            	finalMessageBean.outputFilePath = ((String)msg.obj);
            	return;
            }
            if(msg.what == FEA_PROGRESSHANDLER_SET_ERRORMESSAGE)
            {  
            	finalMessageBean.errorMessage = ((String)msg.obj);
            	return;
            }
          //- Create "After Encrypt Report"
        }
    };
    
    
    // Handler for miscellaneous background activities
    Handler universalHandler = new Handler() 
    {
        public void handleMessage(Message msg)  
        {
        	if (msg.what == FEA_UNIVERSALHANDLER_SHOW_WAITDIALOG)
        	{ 
        		if(waitDialog != null) waitDialog.show();
        		return;
        	}
        	if (msg.what == FEA_UNIVERSALHANDLER_HIDE_WAITDIALOG)
        	{ 
        		if(waitDialog != null) waitDialog.cancel();     		
        		return;
        	}
        	if (msg.what == FEA_UNIVERSALHANDLER_REFRESH_FILELIST)
        	{ 
            	updateCurrentFiles();
    		 	fileArrayAdapter.notifyDataSetChanged();
        		return;
        	}
        	if (msg.what == FEA_UNIVERSALHANDLER_SHOW_DIRSIZE)
        	{ 
        		List message = (List)msg.obj;
        		String path = (String)message.get(0);
        		Long size = (Long)message.get(1);
        		((FileEncArrayAdapter)fileArrayAdapter).setDirSize(path, size);
    		 	fileArrayAdapter.notifyDataSetChanged();
        		return;
        	}
        	if (msg.what == FEA_UNIVERSALHANDLER_SHOW_VOLUMESIZE)
        	{
        		List message = (List)msg.obj;
        		String titleRight = (String)message.get(0);
        		String titleRightTag = (String)message.get(1); 		
    	    	setTitleRight(titleRight);
    	    	setTitleRightTag(titleRightTag);
    	    	volumeSizeThread = null;
        		return;
        	}
        }
    };
	
    /** Keeps "After ENC/DEC" Report data */
	public static class FinalMessageBean implements Cloneable
    {    
        private String mainMessage;
        private String inputFilePath;
        private String outputFilePath;
        private String errorMessage;
        public int files;
        public int deletedFiles;
        public int folders;
        public int deletedFolders;
        
        {
        	reset();
        }
        
        public void reset()
        {
        	mainMessage = "";
        	inputFilePath = "";
        	outputFilePath = "";
        	errorMessage = "";
        	files = 0;
        	deletedFiles = 0;
        	folders = 0;
        	deletedFolders = 0;
        }
        
        @Override 
        public FinalMessageBean clone() 
        {
        	try {
        		final FinalMessageBean result = (FinalMessageBean) super.clone();
        		result.mainMessage = new String(mainMessage);
        		result.inputFilePath = new String(inputFilePath);
        		result.outputFilePath = new String(outputFilePath);
        		result.errorMessage = new String(errorMessage);
        		result.files = files;
        		result.deletedFiles = deletedFiles;
        		result.folders = folders;
        		result.deletedFolders = deletedFolders;
                return result;
            } catch (final CloneNotSupportedException ex) {
                throw new AssertionError();
            }         
        }
    } 
}


