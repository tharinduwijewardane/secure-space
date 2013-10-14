package com.tharindu.securespace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.tharindu.securespace.R;
import com.paranoiaworks.unicus.android.sse.StaticApp;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.misc.ProgressMessage;

public class WipeSource {
	
	public static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";
	public static final String DATE_FORMAT_DATEONLY = "dd/MM/yyyy";
	public static final String REGEX_REPLACEALL_LASTDOT = "\\.(?!.*\\.)";
	
	public static final String UNIX_FILE_SEPARATOR = "/";
	public static final String WINDOWS_FILE_SEPARATOR = "\\";
	
	
	
	
	
    public static void wipeFileOrDirectory(File file, ProgressBarToken progressBarToken) throws IOException, InterruptedException 
    {        
    	wipeFileOrDirectory(file, progressBarToken, false);
    }
	
	public static void wipeFileOrDirectory(File file, ProgressBarToken progressBarToken, boolean tempFilesWiping) throws IOException, InterruptedException 
	{        		
//		Handler progressHandler = progressBarToken.getProgressHandler();
//		if(tempFilesWiping) progressHandler.sendMessage(Message.obtain(progressHandler, -1202)); // temp files wiping
//		else progressHandler.sendMessage(Message.obtain(progressHandler, -1201)); // normal wiping
//        progressHandler.sendMessage(Message.obtain(progressHandler, 0));    
        ProgressMessage hm = new ProgressMessage();
        hm.setProgressAbs(0);
        DirectoryStats ds = getDirectoryStats(file);
    	
    	if(file.isDirectory())
    	{   		
    		ds.allFolders++;
//    		hm.setFullSize(getDirectorySize(file));
//    		if(hm.getFullSize() > 2147483647) 
//    			throw new IllegalStateException(StaticApp.getStringResource("fe_message_wipe_folder_2GBlimit").replaceAll("<1>", getFormatedFileSize(hm.getFullSize())));
    		boolean ok = wipeDirectory(file, progressBarToken, hm, ds);
    		if(ok) ds.okFolders++;
    	}
    	else if(file.isFile())
    	{
    		ds.allFiles++;
//    		hm.setFullSize(file.length());
//    		if(hm.getFullSize() > 2147483647) 
//    			throw new IllegalStateException(StaticApp.getStringResource("fe_message_wipe_file_2GBlimit").replaceAll("<1>", getFormatedFileSize(hm.getFullSize())));
    		boolean ok = wipeFile(file, progressBarToken, hm);
    		if(ok) ds.okFiles++;
    	}
//    	if(!tempFilesWiping) progressHandler.sendMessage(Message.obtain(progressHandler, -1211, ds)); // send DirectoryStats;
    }
    
    private static boolean wipeDirectory(File directory, ProgressBarToken progressBarToken, ProgressMessage hm, DirectoryStats ds) throws IOException, InterruptedException
    {
		String[] list = directory.list();

		if (list != null) 
		{
			for (int i = 0; i < list.length; i++) 
			{
				File entry = new File(directory, list[i]);

				if (entry.isDirectory())
				{
					ds.allFolders++;
					if (wipeDirectory(entry, progressBarToken, hm, ds)) ds.okFolders++;
				}
				else
				{
					ds.allFiles++;
					if (wipeFile(entry, progressBarToken, hm)) ds.okFiles++;
				}
				if (Thread.interrupted())
				{
					throw new InterruptedException("Canceled by User.");
				}
			}
		}
		
		return directory.delete();
    }
    
    private static boolean wipeFile(File file, ProgressBarToken progressBarToken, ProgressMessage hm) throws IOException
    {
    	Handler progressHandler = progressBarToken.getProgressHandler();
    	//SSElog.d("Wiping", "File: " + file.getName() + " " + file.length());
    	final int BUFFER_SIZE = 262144;
    	final long FILE_SIZE = file.length();
    	RandomAccessFile rwFile = null;
    	FileChannel rwChannel = null;
    	boolean wiped = false;
    	
    	byte[] nullBytes = new byte[BUFFER_SIZE];
    	try {  
    		if(!file.canWrite()) return false;
    		rwFile = new RandomAccessFile(file, "rw");
    		rwChannel = rwFile.getChannel();
    		
    		for(long i = 0; i * BUFFER_SIZE < FILE_SIZE; ++i)
    		{
    			long bytesLeft = FILE_SIZE - (i * BUFFER_SIZE);
    			int currentBufferSize = bytesLeft > BUFFER_SIZE ? BUFFER_SIZE : (int)bytesLeft;
    			MappedByteBuffer buffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, i * BUFFER_SIZE, currentBufferSize);     
    			buffer.clear();
    			if(currentBufferSize != BUFFER_SIZE) nullBytes = new byte[currentBufferSize];
    			buffer.put(nullBytes);  
    			//byte[] randomBytes = new byte[currentBufferSize];     
    			//new Random().nextBytes(randomBytes);     
    			//buffer.put(randomBytes);     
    			buffer.force();

//				hm.setProgressAbs(hm.getProgressAbs() + currentBufferSize);
//				progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
    		}
    		wiped = true;
    		
    		return wiped;
    	} catch (IOException ioe){  
    		throw new IOException("Wiping: " + ioe.getLocalizedMessage()); 		
    	} finally {  
    		if(rwChannel != null) rwChannel.close();
    		try{rwFile.close();}catch(Exception e){}; 
    		if(wiped)file.delete();
    	}  
    }
	
	
		
	public static DirectoryStats getDirectoryStats(File directory)
	{
		WipeSource h = new WipeSource();
		return h.getDirectoryStatsInner(directory);
	}
	
	private DirectoryStats getDirectoryStatsInner(File directory)
	{
		DirectoryStats ds =  new DirectoryStats();
		return ds;
	}
	
	public class DirectoryStats 
	{ 
		public int allFolders = 0, allFiles = 0;
		public int okFolders = 0, okFiles = 0;
	}
	
	
}
