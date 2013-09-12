package com.tharindu.securespace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import android.app.Activity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.paranoiaworks.unicus.android.sse.R;

/**
 * Provides "File Row" appearance (for the File Selector)
 * @author Tharindu Wijewardane
 */
public class FileSelectArrayAdapter extends ArrayAdapter<CryptFile> {
	private final Activity context;
	private final List<CryptFile> files;
	private final Map<String, Long> dirSizeMap = new HashMap<String, Long>(); // dirs size cache

	public FileSelectArrayAdapter(Activity context, List<CryptFile> files) 
	{
		super(context, R.layout.lc_fileenc_listrow, files);
		this.context = context;
		this.files = files;	
	}
	
	static class ViewHolder {
		public ImageView fileIcon;
		public ImageView selectIcon;
		public TextView fileName;
		public TextView fileSize;
		public TextView fileDate;
		public double originalTextSize = -1;
		public int originalColor;
		public int goldColor;
		
		public boolean lastRenderSpecial = false;
	}

	@Override
	public synchronized View getView(int position, View convertView, ViewGroup parent) 
	{
		ViewHolder holder;
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.lc_fileenc_listrow, null, true);
			holder = new ViewHolder();
			holder.fileName = (TextView) rowView.findViewById(R.id.FE_fileName);
			holder.fileSize = (TextView) rowView.findViewById(R.id.FE_fileSize);
			holder.fileDate = (TextView) rowView.findViewById(R.id.FE_fileDate);
			holder.fileIcon = (ImageView) rowView.findViewById(R.id.FE_fileIcon);
			holder.selectIcon = (ImageView) rowView.findViewById(R.id.FE_selectedIcon);
			
			if (holder.originalTextSize < 0)
			{
				holder.originalTextSize = holder.fileName.getTextSize();
				holder.fileName.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)(holder.originalTextSize * 1.2));
				holder.originalColor = rowView.getResources().getColor(R.color.white_file);
				holder.goldColor = rowView.getResources().getColor(R.color.gold);
				
				resetView(holder, rowView);
			}
			
			rowView.setTag(holder);
			
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
			
		CryptFile tempFile = files.get(position);		
		boolean writable = tempFile.canWrite();
		boolean encrypted = tempFile.isEncrypted();
		
		if (holder.lastRenderSpecial)resetView(holder, rowView);

		holder.fileName.setText(tempFile.getName());	
		
		if (tempFile.isFile()) holder.fileSize.setText(Helpers.getFormatedFileSize(tempFile.length()));
			else holder.fileSize.setText("");
		
		if (!tempFile.isBackDir()) holder.fileDate.setText(Helpers.getFormatedDate(tempFile.lastModified()));
		else 
		{
			holder.fileName.setText(".../" + tempFile.getName());
			holder.fileSize.setText("");
			holder.fileDate.setText("");
			holder.fileIcon.setImageResource(R.drawable.back_file);
			return rowView;
		}
		
		if(tempFile.isSelected())
		{
//			holder.selectIcon.setImageResource(R.drawable.selected);
//			rowView.setBackgroundResource(R.drawable.d_filerow_selected);
//			holder.lastRenderSpecial = true;
		}

		//change background of the files in the list (of selected files for automatic services)
		String temFilePath = tempFile.getAbsolutePath();		
		if(FileSelectorActivity.getSelectedFileList().contains(temFilePath)){
			holder.selectIcon.setImageResource(R.drawable.selected);
			rowView.setBackgroundResource(R.drawable.d_filerow_selected);
			holder.lastRenderSpecial = true;
		}
		
		if (tempFile.isDirectory()) 
		{
			if(writable)holder.fileIcon.setImageResource(R.drawable.directory);
				else holder.fileIcon.setImageResource(R.drawable.directory_readonly);
			Long size = dirSizeMap.get(tempFile.getAbsolutePath());
			if(tempFile.isSelected())
			{
				if(size == null) holder.fileSize.setText("...");
				else holder.fileSize.setText("");
			}
			
			if(size != null && size > -1)
				holder.fileSize.setText(Helpers.getFormatedFileSize(size));
		} 
		else if(tempFile.isFile()) 
		{
			if(encrypted)
			{
				if(writable) holder.fileIcon.setImageResource(R.drawable.file_enc);
					else holder.fileIcon.setImageResource(R.drawable.file_enc_readonly);
				if(!tempFile.isSelected())rowView.setBackgroundResource(R.drawable.d_filerow_encrypted);
				holder.fileName.setTextColor(holder.goldColor);
				holder.fileSize.setTextColor(holder.goldColor);
				holder.fileDate.setTextColor(holder.goldColor);
				holder.lastRenderSpecial = true;
			}
			else
			{
				if(writable) holder.fileIcon.setImageResource(R.drawable.file);
					else holder.fileIcon.setImageResource(R.drawable.file_readonly);
			}
		} 
		else 
		{
			holder.fileIcon.setImageResource(R.drawable.null_image); // others - no image
		}
		
		return rowView;
	}
	
	private void resetView(ViewHolder holder, View rowView)
	{
		holder.selectIcon.setImageResource(R.drawable.null_image); 
		rowView.setBackgroundResource(R.drawable.d_filerow);
		holder.fileName.setTextColor(holder.originalColor);
		holder.fileSize.setTextColor(holder.originalColor);
		holder.fileDate.setTextColor(holder.originalColor);
		holder.lastRenderSpecial = false;
		
	}
	
	public void setDirSize(String dirPath, Long size)
	{
		dirSizeMap.put(dirPath, size);
	}
	
	public void removeDirSize(String dirPath)
	{
		dirSizeMap.remove(dirPath);
	}
	
	public void clearDirSizeMap()
	{
		dirSizeMap.clear();
	}
}
