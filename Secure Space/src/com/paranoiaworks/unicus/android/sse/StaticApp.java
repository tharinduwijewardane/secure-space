package com.paranoiaworks.unicus.android.sse;

import android.app.Application;
import android.content.Context;

/**
 * "Static Context" provider 
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.1
 */
public class StaticApp extends Application{

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext(){
        return mContext;
    }
    
	/** Get String Resource dynamically by Identifier */
	public static String getStringResource(String name)
    {
    	String resText = null;
    	int resID = getStringResID(name);
    	if(resID > 1) resText = mContext.getResources().getString(resID);
    	else resText = name;
    	
    	return resText;
    }
	
    private static int getStringResID(String name)
    {
    	return mContext.getResources().getIdentifier(name, "string", "com.paranoiaworks.unicus.android.sse");
    }
    
    /** Convert DP to PX */
    public static int dpToPx(float dp)
    {
    	float scale = mContext.getResources().getDisplayMetrics().density;
    	return (int)(dp * scale + 0.5f);
    }
    
    /** Convert PX to DP */
    public float pxToDp(int px)
    {
    	float scale = mContext.getResources().getDisplayMetrics().density;
    	return ((float)px - 0.5f) / scale;
    }
}

