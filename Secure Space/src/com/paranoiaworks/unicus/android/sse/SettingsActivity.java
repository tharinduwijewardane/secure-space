package com.paranoiaworks.unicus.android.sse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.components.SimpleHTMLDialog;
import com.paranoiaworks.unicus.android.sse.components.VerboseWaitDialog;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.utils.AlgorithmBenchmark;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor.AlgorithmBean;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;
import com.paranoiaworks.unicus.android.sse.utils.SSElog;

/**
 * Settings activity class
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.4
 * @related SettingDataHolder.java, data.xml
 */ 
public class SettingsActivity extends CryptActivity  {	

	private List<SettingCategory> settings;
	private List<View> viewsContainer = new ArrayList<View>();
	
	private LinearLayout settingsLL;
	private Button saveButton;
	private Button helpButton;
	private String currentValueText = "";
	
	private static final int S_HANDLE_SELECTOR = -4001;
	private static final int S_HANDLE_CHECKBOX = -4002;
	private static final int S_HANDLE_TEXT = -4003;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	this.setContentView(R.layout.la_settings);
    	setTitle(getResources().getString(R.string.common_app_settings_name));
    	
    	loadSetting(); //load Settings
    	
    	currentValueText = getResources().getString(R.string.common_current_text) + ": " ;
    	  	
        // Create settings list (graphical representation)
    	settingsLL = (LinearLayout)findViewById(R.id.S_settingsList);
    	for(SettingCategory settingCategory : settings)
        {     		
        	TextView catView = (TextView)getLayoutInflater().inflate(R.layout.lct_settingcategory_name, null);
        	catView.setText(getStringResource(settingCategory.categoryName));
        	settingsLL.addView((View)catView);
        	
        	for(int i = 0; i < settingCategory.itemsList.size(); ++i)
            {
        		SettingItem settingItem = settingCategory.itemsList.get(i);
        		View delimiter = getLayoutInflater().inflate(R.layout.lct_delimiter_thin_grey, null);
        		settingsLL.addView((View)getViewForItem(settingItem));
            	if(i + 1 < settingCategory.itemsList.size()) settingsLL.addView(delimiter);
            }
        }
        
        // Save Button
        saveButton = (Button)findViewById(R.id.S_saveButton);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	saveSettings();
	    		setRunningCode(0);
	    		finish();
		    }
	    });
        
	    // Help Button
        helpButton = (Button)findViewById(R.id.S_helpButton);
        helpButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	SimpleHTMLDialog simpleHTMLDialog = new SimpleHTMLDialog(v);
		    	simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_Settings));
		    	simpleHTMLDialog.show();
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
	        case S_HANDLE_SELECTOR:
	        {
	        	View clickedView = (View)am.getAttachement();
		    	LinearLayout leftView = (LinearLayout)((RelativeLayout)clickedView).getChildAt(0);
		    	LinearLayout rightView = (LinearLayout)((RelativeLayout)clickedView).getChildAt(1);
		    	View dataHolderView = ((LinearLayout)(rightView.getChildAt(0))).getChildAt(0);
		    	TextView currentValueTextView = (TextView)((LinearLayout)(leftView.getChildAt(0))).getChildAt(2);    	
		    	SettingItem si = (SettingItem)dataHolderView.getTag();
	        	
		    	si.itemValue = (String)am.getMainMessage();
		    	currentValueTextView.setText(currentValueText + si.itemValueNames[Integer.parseInt(si.itemValue)]);
		    	currentValueTextView.setMinWidth(10); //force refresh
		    	handleAlteration();
	        	
	        	this.resetMessage();
	            break;
	        }
	            
	        case S_HANDLE_CHECKBOX:
	        {
	        	View clickedView = (View)am.getAttachement();
		    	LinearLayout rightView = (LinearLayout)((RelativeLayout)clickedView).getChildAt(1);
		    	View dataHolderView = ((LinearLayout)(rightView.getChildAt(0))).getChildAt(0);
		    	SettingItem si = (SettingItem)dataHolderView.getTag();
		    	
				CheckBox tempView = (CheckBox)dataHolderView;
				si.itemValue = Boolean.toString(!tempView.isChecked());
				tempView.setChecked(Boolean.parseBoolean(si.itemValue));
		    	
	        	this.resetMessage();
	            break;
	        }
	        
	        case S_HANDLE_TEXT:
	        {
	        	View clickedView = (View)am.getAttachement();
		    	LinearLayout leftView = (LinearLayout)((RelativeLayout)clickedView).getChildAt(0);
		    	LinearLayout rightView = (LinearLayout)((RelativeLayout)clickedView).getChildAt(1);
		    	View dataHolderView = ((LinearLayout)(rightView.getChildAt(0))).getChildAt(0);
		    	TextView currentValueTextView = (TextView)((LinearLayout)(leftView.getChildAt(0))).getChildAt(2);    	
		    	SettingItem si = (SettingItem)dataHolderView.getTag();
	        	
		    	si.itemValue = (String)am.getMainMessage();
		    	currentValueTextView.setText(currentValueText + si.itemValue);
		    	currentValueTextView.setMinWidth(10); //force refresh
		    	handleAlteration();
	        	
	        	this.resetMessage();
	            break;
	        }
	        
        	case COMMON_MESSAGE_CONFIRM_EXIT:
				if(am.getAttachement() != null && am.getAttachement().equals(new Integer(1)))
				{
					saveSettings();
				}
				setRunningCode(0);
	    		finish();
        		break;
	
	        default: 
	        	break;
        }
	}
    
	
    /** 
     * Create main settings object ("List<SettingCategory> settings")  
	 * using SettingDataHolder.java and resource file Data.xml
	 */
	private void loadSetting()
	{
		Map<String, SettingCategory> categories = new HashMap<String, SettingCategory>();
		Map<String, String> comments = new HashMap<String, String>();
		Resources resources = this.getResources();
		String[] settingsCategoriesTemp = resources.getStringArray(R.array.settings_categories);
    	String[] settingsItemsTemp = resources.getStringArray(R.array.settings_items);
    	String[] settingsCommentsTemp = resources.getStringArray(R.array.settings_comments);
    	
        for(String settingCategory : settingsCategoriesTemp) 
        {
        	String[] parameters = settingCategory.split("\\|\\|");
        	SettingCategory sc = new SettingCategory();
        	sc.categoryName = parameters[0];
        	sc.categorySequence = Integer.parseInt(parameters[1]);
        	
        	categories.put(sc.categoryName , sc);
        }
        
        for(String settingsComments : settingsCommentsTemp) 
        {
        	String[] parameters = settingsComments.split("\\|\\|");
        	String id = parameters[0] + ":" + parameters[1];
        	
        	comments.put(id , parameters[2]);
        }
        
        for(String settingItem : settingsItemsTemp)
        {
        	String[] parameters = settingItem.split("\\|\\|");
        	SettingItem si = new SettingItem();
        	si.parentCategoryName = parameters[0];
        	si.itemName = parameters[1];
        	si.itemSequence = Integer.parseInt(parameters[2]);
        	si.itemType = parameters[3];
        	si.itemValueNames = parameters[4].split("\\|");
        	si.itemValue = settingDataHolder.getItem(si.parentCategoryName, si.itemName);
        	
        	String commentTemp = comments.get(si.getFullyQualifiedItemName());
        	if(commentTemp != null) si.itemComment = commentTemp;
        	
        	SettingCategory sc = categories.get(si.parentCategoryName);
        	if(sc != null) sc.itemsList.add(si);
        }
        
        List<SettingCategory> categoriesList = new ArrayList<SettingCategory>(categories.values());
        
        Collections.sort(categoriesList);
        for(SettingCategory settingCategory : categoriesList)
        {
        	Collections.sort(settingCategory.itemsList);
        }
        
        settings = categoriesList;
	}
    
	
	/** Create Graphical Representation of SettingItem Object */
	private View getViewForItem(SettingItem si)
	{
		RelativeLayout returnViewWrapper = (RelativeLayout)getLayoutInflater().inflate(R.layout.lct_settingitem_root, null);
		LinearLayout returnViewLeft = (LinearLayout)returnViewWrapper.getChildAt(0);
		LinearLayout returnViewRight = (LinearLayout)returnViewWrapper.getChildAt(1);
		LinearLayout itemTexts = (LinearLayout)getLayoutInflater().inflate(R.layout.lct_settingitem_texts, null);
		
		TextView itemTitle = (TextView)itemTexts.getChildAt(0);
		TextView itemComment = (TextView)itemTexts.getChildAt(1);
		TextView currentValue = (TextView)itemTexts.getChildAt(2);
		
    	itemTitle.setText(getStringResource(si.itemName));

		if(si.itemComment != null)
		{
			itemComment.setVisibility(TextView.VISIBLE);
			itemComment.setText(getStringResource(si.itemComment));
		}
		returnViewLeft.addView(itemTexts);
		
		returnViewWrapper.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	//LinearLayout leftView = (LinearLayout)((RelativeLayout)v).getChildAt(0);
		    	LinearLayout rightView = (LinearLayout)((RelativeLayout)v).getChildAt(1);
		    	View dataHolderView = ((LinearLayout)(rightView.getChildAt(0))).getChildAt(0);	    						  
		    	SettingItem si = (SettingItem)dataHolderView.getTag();	    	
		    	
				if(dataHolderView.getClass().equals(CheckBox.class)) // for checkbox
				{
					setMessage(new ActivityMessage(S_HANDLE_CHECKBOX, "", v));
				}
				else if(dataHolderView.getClass().equals(ImageView.class) && si.itemType.equalsIgnoreCase("selector")) // for selector
				{
			       	List<String> itemList = (Arrays.asList(si.itemValueNames));
			    	List<String> keyList = new ArrayList<String>();
			    	
			    	for(int i = 0; i < itemList.size(); ++i) keyList.add(Integer.toString(i));

			    	AlertDialog selectionDialog = (AlertDialog)ComponentProvider.getItemSelectionDialog(
			    			v, 
			    			getStringResource(si.itemName),
			    			itemList,
			    			keyList,
			    			S_HANDLE_SELECTOR,
			    			v,
			    			Integer.parseInt(si.itemValue)
			    			);
			    	selectionDialog.show();
				}
				else if(dataHolderView.getClass().equals(ImageView.class) && si.itemType.equalsIgnoreCase("text"))  // for text
				{
					Dialog enterTextDialog = ComponentProvider.getTextSetDialog(v,
							getStringResource(si.itemName), 
							si.itemValue, 
							S_HANDLE_TEXT, 
							v
							);
					enterTextDialog.show();
				}

		    	//SSElog.d(dataHolderView.getTag().toString() + " : " + dataHolderView.getClass().getName());	
		    }
	    });
		
		if(si.itemType.equalsIgnoreCase("checkbox"))  // for checkbox
		{
			LinearLayout rl = (LinearLayout)getLayoutInflater().inflate(R.layout.lct_settingitem_checkbox, null);
			CheckBox view = (CheckBox)rl.getChildAt(0);
			view.setChecked(Boolean.parseBoolean(si.itemValue));
			view.setTag(si.getTag());
			currentValue.setVisibility(TextView.GONE);
			
			view.setOnCheckedChangeListener(new OnCheckedChangeListener()
	    	{
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	    	    {
					handleAlteration();
	    	    }
	    	});
			
			returnViewRight.addView(rl);
			viewsContainer.add(view);
		}
		else if(si.itemType.equalsIgnoreCase("selector"))  // for selector
		{
			LinearLayout rl = (LinearLayout)getLayoutInflater().inflate(R.layout.lct_settingitem_selector, null);
			ImageView view = (ImageView)rl.getChildAt(0);
			currentValue.setText(currentValueText + si.itemValueNames[Integer.parseInt(si.itemValue)]);
			view.setTag(si.getTag());			
		
			returnViewRight.addView(rl);
			viewsContainer.add(view);
		}
		else if (si.itemType.equalsIgnoreCase("text"))  // for text
		{
			LinearLayout rl = (LinearLayout)getLayoutInflater().inflate(R.layout.lct_settingitem_entertext, null);
			ImageView view = (ImageView)rl.getChildAt(0);
			currentValue.setText(currentValueText + si.itemValue);
			view.setTag(si.getTag());
			
			returnViewRight.addView(rl);
			viewsContainer.add(view);
		}
		return returnViewWrapper;
	}
	
	
    /** Is current ("memory") version of Settings saved in the application DB? */
    private boolean checkForAlteration()
	{
    	boolean altered = false;
		
		for(View tempView : viewsContainer)
    	{
    		SettingItem itemFromView = (SettingItem)tempView.getTag();
    		String itemValueFromView = itemFromView.itemValue;
    		String itemValueFromHolder = settingDataHolder.getItem(itemFromView.parentCategoryName, itemFromView.itemName);
    		
    		if(!itemValueFromView.equals(itemValueFromHolder))
    		{
    			altered = true;
    			break;
    		}
    	}
		return altered;
	}
	
	
    /** Save current Settings to application DB */
    private void saveSettings()
	{
    	for(View tempView : viewsContainer)
    	{
    		SettingItem item = (SettingItem)tempView.getTag();
    		settingDataHolder.addOrReplaceItem(item.parentCategoryName, item.itemName, item.itemValue);
    	}
    	
		settingDataHolder.save();
	}
	
	
	/** Enable Save Button - if current Setting is not saved in the application DB */
	private void handleAlteration()
	{
    	if(checkForAlteration())
    		saveButton.setEnabled(true);
    	else 
    		saveButton.setEnabled(false); 
	}
	
    /** Keeps Setting Category Data */
	private static class SettingCategory implements Comparable<SettingCategory>
    {    
        protected String categoryName = null;
        protected Integer categorySequence = -1;
        protected List<SettingItem> itemsList = new ArrayList<SettingItem>();
        
    	public int compareTo(SettingCategory category)
    	{ 		
    		return this.categorySequence.compareTo(category.categorySequence);
    	}
    }
	
	/** Keeps Setting Item Data */
	private static class SettingItem implements Comparable<SettingItem>
    {    
    	protected String parentCategoryName = null;
    	protected String itemName = null;
        protected Integer itemSequence  = -1;
        protected String itemType = null;
        protected String[] itemValueNames = null;
        protected String itemValue = null;
        protected String itemComment = null;
        
    	public String getFullyQualifiedItemName()
    	{ 		
    		return parentCategoryName + ":" + itemName;
    	}
        
        public SettingItem getTag()
    	{ 		
    		return this;
    	}
        
    	public int compareTo(SettingItem item)
    	{ 		
    		return this.itemSequence.compareTo(item.itemSequence);
    	}
    }
	
    @Override
    protected void onStart ()
    {
        setRunningCode(RUNNING_SETTINGSACTIVITY);
    	super.onStart();
    }
    
    /** Back Button - exit to Main Menu, if Settings are altered ask for save and exit to Main Menu */
    @Override
    public void onBackPressed()
    {
		if(checkForAlteration())
		{
    		ComponentProvider.getBaseQuestionDialog(this, 
					getResources().getString(R.string.common_save_text),  
    				getResources().getString(R.string.common_question_saveChanges), 
    				null, 
    				COMMON_MESSAGE_CONFIRM_EXIT
    				).show();
		}
		else setMessage(new ActivityMessage(COMMON_MESSAGE_CONFIRM_EXIT, null));
    }
}
