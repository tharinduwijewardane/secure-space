package com.paranoiaworks.unicus.android.sse.utils;

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

import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.StaticApp;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.misc.ProgressMessage;

public class Helpers {
	
	public static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";
	public static final String DATE_FORMAT_DATEONLY = "dd/MM/yyyy";
	public static final String REGEX_REPLACEALL_LASTDOT = "\\.(?!.*\\.)";
	
	public static final String UNIX_FILE_SEPARATOR = "/";
	public static final String WINDOWS_FILE_SEPARATOR = "\\";
	
	public static List<File> getExtDirectories (Context context)
	{		
		List<File> dirList = new ArrayList<File>();
		List<Integer> removeFlagsList = new ArrayList<Integer>();
		String[] possibleDirs = context.getResources().getString(R.string.extdir_list).split(";");
		
		boolean mExternalStorageAvailable;
		boolean mExternalStorageWriteable;
		String state = Environment.getExternalStorageState();
		
		dirList.add(new File(File.separator)); // ROOT

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		File extDir = null;
		if (mExternalStorageAvailable && mExternalStorageWriteable)
		{
			extDir = Environment.getExternalStorageDirectory();
			dirList.add(extDir);
		}
		
		for (int i = 0; i < possibleDirs.length; ++i)
		{	
			if (extDir != null && extDir.getAbsolutePath().equals(possibleDirs[i])) continue;
			File testFile = new File(possibleDirs[i]);
			if (testFile.exists() && testFile.isDirectory()) dirList.add(testFile);
		}
		
		
		
		//+ remove equals
		try {
			for (int i = 0; i < dirList.size(); ++i)
			{
				File testFileA = dirList.get(i);
				for (int j = 0; j < dirList.size(); ++j)
				{
					if(i == j) continue;
					File testFileB = dirList.get(j);
					if(testFileA.getAbsolutePath().indexOf(testFileB.getAbsolutePath()) < 0) continue;
					if(!Arrays.deepEquals(testFileA.list(), testFileB.list())) continue;
					if(testFileA.getAbsolutePath().length() > testFileB.getAbsolutePath().length()) removeFlagsList.add(i);			
				}
			}
			
			Collections.reverse(removeFlagsList);		
			for (int i = 0; i < removeFlagsList.size(); ++i) dirList.remove((int)removeFlagsList.get(i));		
		} catch (Exception e) {
			e.printStackTrace();
		}
		//- remove equals

		return dirList;	
	}
	
	public static String getImportExportPath()
	{
		SettingDataHolder sdh = SettingDataHolder.getInstance();         		
		String importExportPath = sdh.getItem("SC_Common", "SI_ImportExportPath");		
		return importExportPath;
	}
	
	public static File getImportExportDir()
	{       		
		String importExportPath = getImportExportPath();
		File importExportDir = null;
		
    	try {
    		File tempDir = new File(importExportPath);
    		if(!tempDir.exists()) tempDir.mkdir();
    		if(tempDir.exists() && tempDir.canRead()) importExportDir = tempDir;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return importExportDir;
	}
	
	public static byte[] xorit(byte[] text, byte[] passPhrase)
	{		
		if (passPhrase.length == 0) passPhrase = "x".getBytes();
		byte[] outputBuffer = new byte[text.length];
		int counter = 0;
		for (int i = 0; i < text.length; ++i)
		{
			byte a = text[i];
			byte b = passPhrase[counter];
			outputBuffer[i] = (byte)(a ^ b);	
			++counter;
			if (counter == passPhrase.length) counter = 0;
		}		
		return outputBuffer;
	}
	
	public static byte[] concat(byte[]... args) 
	{
		int fulllength = 0;
		for (byte[] arrItem : args) 
		{
			fulllength += arrItem.length;
        }
		byte[] retArray = new byte[fulllength];
		int start = 0;
		for (byte[] arrItem : args) 
		{
			System.arraycopy(arrItem, 0, retArray, start, arrItem.length);
			start += arrItem.length;
		}
		return retArray;
	}
	
	public static byte[] getSubarray(byte[] array, int offset, int length) 
	{
		byte[] result = new byte[length];
		System.arraycopy(array, offset, result, 0, length);
		return result;
	}

	public static String removeExt (String fileName, String extension)
    {
    	String name = fileName;
    	if (fileName.endsWith("." + extension))
    		name = name.substring(0, name.lastIndexOf('.')); 		
    	return name;
    }
	
	public static String getFirstDirFromFilepath (String filepath)
    {
    	String[] temp = filepath.split(File.separator);
    	if(temp[0].equals("") && temp.length > 1) return temp[1];
    	return temp[0];
    }
	
	public static String getFirstDirFromFilepathWithLFS(String filepath) //leading file separator (/...)
    {
		if(regexGetCountOf(filepath, File.separator) == 1) return filepath;
		String[] temp = filepath.split(File.separator);
    	if(temp[0].equals("") && temp.length > 1) return File.separator + temp[1];
    	return File.separator + temp[0];
    }
	
	public static String[] listToStringArray (List<String> strings)
    {
		String[] sList = new String[strings.size()];
		for(int i = 0; i < strings.size(); ++i)
		sList[i] = strings.get(i);
		return sList;
    }
	
	public static String[] fileListToNameStringArray (List<File> files)
    {
		String[] sList = new String[files.size()];
		for(int i = 0; i < files.size(); ++i)
		sList[i] = files.get(i).getName();
		return sList;
    }
	
	public static long getDirectorySize(File directory) 
	{
		int totalFolder = 0, totalFile = 0;
		long foldersize = 0;

		totalFolder++; 
		File[] filelist = directory.listFiles();
		if(filelist == null) return -1;
		for (int i = 0; i < filelist.length; i++) 
		{
			if (filelist[i].isDirectory()) 
			{
				foldersize += getDirectorySize(filelist[i]);
			} else {
				totalFile++;
				foldersize += filelist[i].length();
			}
		}
		return foldersize;
	}
	
	public static long getDirectorySizeWithInterruptionCheck(File directory) throws InterruptedException 
	{
		int totalFolder = 0, totalFile = 0;
		long foldersize = 0;

		totalFolder++; 
		File[] filelist = directory.listFiles();
		if(filelist == null) throw new InterruptedException("DirectorySize: FileList is NULL");
		for (int i = 0; i < filelist.length; i++) 
		{
			if (filelist[i].isDirectory()) 
			{
				long tempSize = getDirectorySizeWithInterruptionCheck(filelist[i]);
				if(tempSize == -1) return -1;
				foldersize += tempSize;
			} else {
				totalFile++;
				foldersize += filelist[i].length();
			}
			if (Thread.interrupted())
			{
				throw new InterruptedException("DirectorySize: Thread Interrupted");
			}
		}
		return foldersize;
	}
	
	public static boolean deleteDirectory(File directory) 
	{
		if (directory == null)
			return false;
		if (!directory.exists())
			return true;
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();

		if (list != null) 
		{
			for (int i = 0; i < list.length; i++) 
			{
				File entry = new File(directory, list[i]);

				if (entry.isDirectory())
				{
					if (!deleteDirectory(entry))
						return false;
				}
				else
				{
					if (!entry.delete())
						return false;
				}
			}
		}
		return directory.delete();
	}
	
    public static void wipeFileOrDirectory(File file, ProgressBarToken progressBarToken) throws IOException, InterruptedException 
    {        
    	wipeFileOrDirectory(file, progressBarToken, false);
    }
	
	public static void wipeFileOrDirectory(File file, ProgressBarToken progressBarToken, boolean tempFilesWiping) throws IOException, InterruptedException 
	{        		
		Handler progressHandler = progressBarToken.getProgressHandler();
		if(tempFilesWiping) progressHandler.sendMessage(Message.obtain(progressHandler, -1202)); // temp files wiping
		else progressHandler.sendMessage(Message.obtain(progressHandler, -1201)); // normal wiping
        progressHandler.sendMessage(Message.obtain(progressHandler, 0));    
        ProgressMessage hm = new ProgressMessage();
        hm.setProgressAbs(0);
        DirectoryStats ds = getDirectoryStats(file);
    	
    	if(file.isDirectory())
    	{   		
    		ds.allFolders++;
    		hm.setFullSize(getDirectorySize(file));
    		if(hm.getFullSize() > 2147483647) 
    			throw new IllegalStateException(StaticApp.getStringResource("fe_message_wipe_folder_2GBlimit").replaceAll("<1>", getFormatedFileSize(hm.getFullSize())));
    		boolean ok = wipeDirectory(file, progressBarToken, hm, ds);
    		if(ok) ds.okFolders++;
    	}
    	else if(file.isFile())
    	{
    		ds.allFiles++;
    		hm.setFullSize(file.length());
    		if(hm.getFullSize() > 2147483647) 
    			throw new IllegalStateException(StaticApp.getStringResource("fe_message_wipe_file_2GBlimit").replaceAll("<1>", getFormatedFileSize(hm.getFullSize())));
    		boolean ok = wipeFile(file, progressBarToken, hm);
    		if(ok) ds.okFiles++;
    	}
    	if(!tempFilesWiping) progressHandler.sendMessage(Message.obtain(progressHandler, -1211, ds)); // send DirectoryStats;
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

				hm.setProgressAbs(hm.getProgressAbs() + currentBufferSize);
				progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
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
	
	public static String getFormatedFileSize(long fileSize) 
	{
		NumberFormat formatter = NumberFormat.getInstance();
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		double fileSizeD = fileSize;
		if(fileSizeD < 1024) return ((long)fileSizeD + " B");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " kB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " MB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " GB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " TB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " PB");
		return (formatter.format(fileSizeD / 1024) + " EB");	
	}
	
	public static String getFormatedDate(long time) 
	{
		return getFormatedDate(time, null);
	}
	
	public static String getFormatedDate(long time, String pattern) 
	{
		if(pattern == null) pattern = DATE_FORMAT;
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(time);
	}
	
	public static String replaceLastDot(String text, String replacement) 
	{
		return text.replaceAll(REGEX_REPLACEALL_LASTDOT, replacement);
	}
	
	public static int regexGetCountOf(byte[] input, String regex) 
	{   
		return regexGetCountOf(new String(input), regex);
	}
	
	public static int regexGetCountOf(String input, String regex) 
	{            	
		int count = 0;
		Pattern p = Pattern.compile(regex);   
		Matcher m = p.matcher(input);
		while (m.find()) ++count;
		return count;
	}
	
	public static String convertToUnixFileSeparator(String path)
	{
		path = path.replaceAll(Pattern.quote(File.separator), UNIX_FILE_SEPARATOR);
		return path;
	}
	
	public static String convertToCurrentFileSeparator(String path)
	{
		path = path.replaceAll(Pattern.quote(UNIX_FILE_SEPARATOR), Matcher.quoteReplacement(File.separator));
		path = path.replaceAll(Pattern.quote(WINDOWS_FILE_SEPARATOR), Matcher.quoteReplacement(File.separator));
		return path;
	}
	
	public Set<Integer> getOnlyNavigationKeySet()
	{
		Set<Integer> keySet = new HashSet<Integer>();
		keySet.add(android.view.KeyEvent.KEYCODE_ENTER);
		return keySet;
	}
	
	public static String getFileExt(File file)
	{
		if(file == null) return null;
		return file.getName().substring(file.getName().lastIndexOf(".") + 1);
	}
	
	public static FilenameFilter getOnlyExtFilenameFilter(String extension)
	{
		Helpers h = new Helpers();
		return h.getOnlyExtFF(extension);
	}
	
	private FilenameFilter getOnlyExtFF(String extension)
	{
		OnlyExt oe = new OnlyExt(extension);
		return oe;
	}
	
	private class OnlyExt implements FilenameFilter 
	{ 
		String ext;	
		public OnlyExt(String ext) 
		{ 
			this.ext = "." + ext; 
		}
		
		public boolean accept(File dir, String name) 
		{ 
			return name.endsWith(ext); 
		} 
	}
		
	public static DirectoryStats getDirectoryStats(File directory)
	{
		Helpers h = new Helpers();
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
	
	public static void saveStringToFile(File file, String text) throws IOException
	{
        try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF8");
			out.write(text);
			out.flush();
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
	}
	
	public static String loadStringFromFile(File file) throws IOException
	{
		StringBuilder text = new StringBuilder();
		
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

			String line = bufferedReader.readLine();
			while(line != null){
				text.append(line.trim());
				text.append("\n");
				line = bufferedReader.readLine();
			}      
               
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } 
        return text.toString();
	}
	
    public static void setLayoutOrientationA(LinearLayout mainWrapper, ViewGroup topWrapper, ViewGroup bottomWrapper)
    {    	
    	int orientation = StaticApp.getContext().getResources().getConfiguration().orientation;
    	if(orientation == Configuration.ORIENTATION_PORTRAIT)
    	{   	
    		mainWrapper.setOrientation(LinearLayout.VERTICAL);
    		
    		LinearLayout.LayoutParams topParams = 
    			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.0f);
    		topWrapper.setLayoutParams(topParams);
    		
    		LinearLayout.LayoutParams bottomParams = 
    			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.0f);
    		bottomParams.setMargins(0, 0, 0, 0);
    		bottomWrapper.setLayoutParams(bottomParams);  		
    	}
    	if(orientation == Configuration.ORIENTATION_LANDSCAPE)
    	{
    		mainWrapper.setOrientation(LinearLayout.HORIZONTAL);
    		
    		LinearLayout.LayoutParams topParams = 
    			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
    		topWrapper.setLayoutParams(topParams);
    		
    		LinearLayout.LayoutParams bottomParams = 
    			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
    		bottomParams.setMargins(StaticApp.dpToPx(5), 0, 0, 0);
    		bottomWrapper.setLayoutParams(bottomParams);
    	}
    }
}
