package com.tharindu.securespace;

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

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.PasswordVaultActivity;
import com.paranoiaworks.unicus.android.sse.R;
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

/**
 * File Selector activity class
 * @author Tharindu Wijewardane
 */
public class FileSelectorActivity extends CryptActivity {

	private boolean askOnLeave;
	private boolean showRoot = false;
	private boolean startFromFileSystem = false;
	private Map<String, Integer> scrollPositionMap = new HashMap<String, Integer>();
	private File currentDir;
	private List<File> availableVolumesList;
	private ArrayAdapter fileSelectArrayAdapter;
	private List<CryptFile> currentFiles = new ArrayList<CryptFile>();
	private ListView filesListView;
	private CryptFile selectedItem;
	private List<String> tips = new ArrayList<String>();
	static ArrayList<String> selectedFileList;

	private TextView topTextView;
	private TextView bottomTextView;
	private Button saveFileListButton;
	private Button clearSelectedFilesButton;
	private Button toMainPageButton;
	private Dialog waitDialog;
	private int renderPhase = 0;

	private Thread dirSizeThread;
	private Thread volumeSizeThread;
	private PreferenceHelp prefHelp;


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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.th_la_fileselect);

		askOnLeave = settingDataHolder.getItemAsBoolean("SC_Common", "SI_AskIfReturnToMainPage");
		
		prefHelp = new PreferenceHelp(getApplicationContext()); //instantiating Preference helper class	
				
		//if the list has been stored in shared preferences
		if(prefHelp.getPrefList(ConstVals.PREF_KEY_SELECTED_FILES_LIST) != null){
			selectedFileList = (ArrayList<String>) prefHelp.getPrefList(ConstVals.PREF_KEY_SELECTED_FILES_LIST);
		}else{
			selectedFileList = new ArrayList<String>();	//else creates a new list
		}

		// Intent - External File Path
		final android.content.Intent intent = getIntent();
		String externalFilePath = null;
		if (intent != null) {
			android.net.Uri data = intent.getData();
			if (data != null) {
				CryptFile tempFile = new CryptFile(data.getPath());
				if (tempFile.exists() && tempFile.isFile()
						&& tempFile.isEncrypted()) {
					externalFilePath = tempFile.getAbsolutePath();
				} else {
					ComponentProvider.getShowMessageDialog(
							this,
							null,
							getResources().getString(
									R.string.common_incorrectFile_text)
									+ ": " + tempFile.getName(),
							ComponentProvider.DRAWABLE_ICON_INFO_RED).show();
				}
				startFromFileSystem = true;
			}
		}

		// GUI parameters
		topTextView = (TextView) findViewById(R.id.FE_topTextView_th);
		bottomTextView = (TextView) findViewById(R.id.FE_bottomTextView_th);
		filesListView = (ListView) findViewById(R.id.FE_list_th);
		setTitle(getResources().getString(R.string.common_app_fileEncryptor_name));

		// Available directories
		availableVolumesList = Helpers.getExtDirectories(getApplicationContext());

		// save button
		saveFileListButton = (Button) findViewById(R.id.bSaveFileList);
		saveFileListButton.setEnabled(false);
		saveFileListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				showPassworDialog();
				saveSelectedList();
				saveFileListButton.setEnabled(false);
			}
		});
		
		// clear button
		clearSelectedFilesButton = (Button) findViewById(R.id.bClearSelectedList);
		clearSelectedFilesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clearSelectedList();
				saveFileListButton.setEnabled(true);
				fileSelectArrayAdapter.notifyDataSetChanged();
			}
		});


		// To Main Menu Button
		toMainPageButton = (Button) findViewById(R.id.FE_toMainPageButton_th);
		toMainPageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setRunningCode(0);
				finish();
			}
		});


		// + Create Top Buttons Line (shortcuts to available volumes)
		LinearLayout rl = (LinearLayout) findViewById(R.id.FE_topLinearLayout_th);
		Button[] rootDirButtons = new Button[availableVolumesList.size()];
		final String deviceRootTag = "ROOT";
		for (int i = 0; i < availableVolumesList.size() && i < 3; ++i) {
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			rootDirButtons[i] = (Button) getLayoutInflater().inflate(
					R.layout.lc_smallbutton_template, null);

			if (!(availableVolumesList.get(i).getAbsolutePath().length() < 2)) // no
																				// device
																				// ROOT
			{
				rootDirButtons[i].setText(File.separator
						+ availableVolumesList.get(i).getName());

			} else {
				if (!settingDataHolder.getItemAsBoolean("SC_FileEnc",
						"SI_ShowRoot") && availableVolumesList.size() > 1)
					continue;
				rootDirButtons[i].setText(deviceRootTag);
				rootDirButtons[i].setTag(deviceRootTag);
			}
			rootDirButtons[i].setId(i);
			rl.addView((View) rootDirButtons[i]);

			rootDirButtons[i].setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Button button = ((Button) v);
					if (!deviceRootTag.equals((String) button.getTag())) {
						File tempRootDir = availableVolumesList.get(button
								.getId());
						if (tempRootDir == null
								|| currentDir.getAbsolutePath().equals(
										tempRootDir.getAbsolutePath()))
							return;
						currentDir = tempRootDir;
						showRoot = false;
					} else {
						currentDir = new File(File.separator); // device ROOT
						showRoot = true;
					}
					updateCurrentFiles();
					fileSelectArrayAdapter.notifyDataSetChanged();
					filesListView.setSelectionAfterHeaderView();
				}
			});
		}
		// - Create Top Buttons Line (shortcuts to available volumes)

		if (externalFilePath != null)
			currentDir = new File(new File(externalFilePath).getParent());
		else if (availableVolumesList.size() > 1)
			currentDir = availableVolumesList.get(1); // second dir in the list
														// use as start dir
		else
			currentDir = availableVolumesList.get(0); // ROOT
		updateCurrentFiles();

		// + Create File List View
		{
			fileSelectArrayAdapter = (new FileSelectArrayAdapter(this, currentFiles));
			RelativeLayout emptyView = (RelativeLayout) findViewById(R.id.FE_list_empty_th);
			((TextView) emptyView.getChildAt(0)).setText(getResources()
					.getString(R.string.fe_emptyDir_text));
			filesListView.setEmptyView(emptyView);
			filesListView.setAdapter(fileSelectArrayAdapter);

			// click on item (file)
			filesListView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					CryptFile clickedFile = currentFiles.get(position);
					File parentFile = clickedFile.getParentFile();

					// if Directory
					if (clickedFile.isDirectory()) {
						scrollPositionMap.put(currentDir.getAbsolutePath(),
								filesListView.getFirstVisiblePosition());
						currentDir = clickedFile;
						updateCurrentFiles();
						fileSelectArrayAdapter.notifyDataSetChanged();
						if (clickedFile.isBackDir())
							setHistoricScrollPosition(clickedFile);
						else
							filesListView.setSelectionAfterHeaderView();
					}
					// if File
					else if (clickedFile.isFile()) {
						if (parentFile == null || !parentFile.canWrite()) {
							ComponentProvider
									.getShowMessageDialog(
											view,
											null,
											getResources()
													.getString(
															R.string.fe_parentDirectoryReadOnly),
											ComponentProvider.DRAWABLE_ICON_INFO_RED)
									.show();
							return;
						}

						setSelectedItem(clickedFile);
						addToSelectedList(clickedFile);
						saveFileListButton.setEnabled(true);
						fileSelectArrayAdapter.notifyDataSetChanged();					

					}
				}
			});
			
			//on long click item
			filesListView.setOnItemLongClickListener(new OnItemLongClickListener() {
						public boolean onItemLongClick(AdapterView<?> parent,
								View view, int position, long id) {
							CryptFile clickedFile = currentFiles.get(position);
							File parentFile = clickedFile.getParentFile();

							if (clickedFile.isBackDir())
								return false;

							// Directory
							if (clickedFile.isDirectory()) {
								if (parentFile == null
										|| !parentFile.canWrite()) {
									ComponentProvider
											.getShowMessageDialog(
													view,
													null,
													getResources()
															.getString(
																	R.string.fe_parentDirectoryReadOnly),
													ComponentProvider.DRAWABLE_ICON_INFO_RED)
											.show();
									return false;
								}

								if (clickedFile.listFiles() == null) {
									ComponentProvider
											.getShowMessageDialog(
													view,
													null,
													getResources()
															.getString(
																	R.string.fe_directoryCannotBeSelected),
													ComponentProvider.DRAWABLE_ICON_INFO_RED)
											.show();
									return false;
								}

								setSelectedItem(clickedFile);
								fileSelectArrayAdapter.notifyDataSetChanged();

//								moreButton.setEnabled(true);
								saveFileListButton.setEnabled(true);
								saveFileListButton.setText(getResources()
										.getString(R.string.fe_goButtonEncDir));

								final String dirPath = clickedFile
										.getAbsolutePath();
								((FileSelectArrayAdapter) fileSelectArrayAdapter)
										.removeDirSize(dirPath);
								if (dirSizeThread != null)
									dirSizeThread.interrupt();
								dirSizeThread = new Thread(new Runnable() {
									public void run() {
										Long dirSize = null;
										try {
											dirSize = Helpers
													.getDirectorySizeWithInterruptionCheck(new File(
															dirPath));
										} catch (InterruptedException e) {
											dirSize = -1l;
											// SSElog.d("DirSizeThread: ",
											// e.getMessage());
										}
										List message = new ArrayList();
										message.add(dirPath);
										message.add(dirSize);
										universalHandler.sendMessage(Message
												.obtain(universalHandler,
														FEA_UNIVERSALHANDLER_SHOW_DIRSIZE,
														message));
									}
								});
								dirSizeThread.setPriority(Thread.MIN_PRIORITY);
								dirSizeThread.start();
							}
							// File
							else if (clickedFile.isFile()) {
								if (!clickedFile.canWrite()) {
									ComponentProvider
											.getShowMessageDialog(
													view,
													null,
													getResources()
															.getString(
																	R.string.fe_fileReadOnly),
													ComponentProvider.DRAWABLE_ICON_INFO_RED)
											.show();
									return false;
								}

								removeFromSelectedList(clickedFile);
								saveFileListButton.setEnabled(true);
								fileSelectArrayAdapter.notifyDataSetChanged();
								
							}

							return true;
						}
					});
		}
		// - Create File List View

		
		// External File Path
//		if (externalFilePath != null) {
//			int externalFileIndex = getFileIndex(new File(externalFilePath));
//			if (externalFileIndex < 0)
//				return;
//			CryptFile externalFile = currentFiles.get(externalFileIndex);
//			setSelectedItem(externalFile);
//			fileSelectArrayAdapter.notifyDataSetChanged();
//			filesListView.setSelectionFromTop(externalFileIndex, 0);
//
//			moreButton.setEnabled(true);
//			startEncDecButton.setEnabled(true);
//			if (externalFile.isEncrypted())
//				startEncDecButton.setText(getResources().getString(
//						R.string.fe_goButtonDecFile));
//			else
//				startEncDecButton.setText(getResources().getString(
//						R.string.fe_goButtonEncFile));
//			showPassworDialog();
//		}
	}

	/** Handle Message */
	protected void processMessage() // made protected by th
	{
		ActivityMessage am = getMessage();
		if (am == null)
			return;

		int messageCode = am.getMessageCode();
		// SSElog.d("Activity Message: ", "" + messageCode);
		switch (messageCode) {
		
		case COMMON_MESSAGE_CONFIRM_EXIT:
			if (am.getAttachement() == null
					|| am.getAttachement().equals(new Integer(1))) {
				setRunningCode(0);
				finish();
			}
			break;

		default:
			break;
		}
	}

	//adding to the selected file list
	private void addToSelectedList(CryptFile file){
		String path = file.getAbsolutePath();
		if(!selectedFileList.contains(path)){ //if not contained already
			selectedFileList.add(path);
		}		
	}
	
	//removing from selected file list
	private void removeFromSelectedList(CryptFile file){
		String path = file.getAbsolutePath();
		if(selectedFileList.contains(path)){
			selectedFileList.remove(path);
		}
	}
	
	private void clearSelectedList(){
		if(!selectedFileList.isEmpty()){ //if not empty
			selectedFileList.clear();
		}
	}
	
	//to get the selected file list from FileSelectArrayAdapter class 
	public static List<String> getSelectedFileList() {
		return selectedFileList;
	}
	
	//save selected list in shared preferences
	private void saveSelectedList(){
		prefHelp.savePref(ConstVals.PREF_KEY_SELECTED_FILES_LIST, selectedFileList);
	}
	

	/** Update File List and other "current files related" variables */
	private void updateCurrentFiles() {
		currentFiles.clear();
		selectedItem = null;
//		moreButton.setEnabled(false);
		saveFileListButton.setEnabled(false);
		saveFileListButton.setText("Save");
		if (fileSelectArrayAdapter != null)
			((FileSelectArrayAdapter) fileSelectArrayAdapter).clearDirSizeMap();
		if (dirSizeThread != null)
			dirSizeThread.interrupt();

		if (currentDir.getParent() != null
				&& (currentDir.getParent().length() > 1 || showRoot)) // restrict
																		// ROOT
		{
			CryptFile backDir = new CryptFile(currentDir.getParent());
			backDir.setBackDir(true);
			currentFiles.add(backDir);
		}

		if (currentDir.listFiles() != null) {
			File[] tempList = currentDir.listFiles();
			for (int j = 0; j < tempList.length; ++j) {
				currentFiles.add(new CryptFile(tempList[j]));
			}
		}

		topTextView.setText(getResources().getString(
				R.string.fe_currentDir_text)
				+ " " + currentDir.getAbsolutePath());

		Collections.sort(currentFiles);

		bottomTextView.setEllipsize(TruncateAt.END);
		if (currentFiles != null && currentFiles.size() > 0
				&& currentFiles.get(0) != null
				&& currentFiles.get(0).isBackDir()) // is Top Dir then first
													// tip?
			bottomTextView.setText(getTip(1)); // 0 for random
		else
			bottomTextView.setText(getTip(1));

		final String absulutePath = currentDir.getAbsolutePath();
		if (!Helpers.getFirstDirFromFilepathWithLFS(absulutePath).equals(
				(String) getTitleRightTag()))
			setTitleRight("");
		if (volumeSizeThread != null)
			return;
		volumeSizeThread = new Thread(new Runnable() {
			public void run() {
				String titleRight = "";
				String titleRightTag = Helpers
						.getFirstDirFromFilepathWithLFS(absulutePath);
				List message = new ArrayList();
				try {
					StatFs stat = new StatFs(absulutePath);
					long blockSize = (long) stat.getBlockSize();
					long sdAvailSize = (long) stat.getAvailableBlocks()
							* blockSize;
					long sdSize = (long) stat.getBlockCount() * blockSize;
					if (sdSize < 1)
						throw new Exception();
					titleRight = (Helpers.getFormatedFileSize(sdAvailSize)
							+ File.separator + Helpers
							.getFormatedFileSize(sdSize));
				} catch (Exception e) {
					// e.printStackTrace();
				}
				message.add(titleRight);
				message.add(titleRightTag);
				universalHandler.sendMessage(Message.obtain(universalHandler,
						FEA_UNIVERSALHANDLER_SHOW_VOLUMESIZE, message));
			}
		});
		volumeSizeThread.setPriority(Thread.MIN_PRIORITY);
		volumeSizeThread.start();
	}

	/** Restore "historic" ListView position using scrollPositionMap */
	private void setHistoricScrollPosition(CryptFile file) {
		if (file.isBackDir()) {
			Integer index = scrollPositionMap.get(file.getAbsolutePath());
			if (index != null)
				filesListView.setSelectionFromTop(index, 0);
		} else
			filesListView.setSelectionAfterHeaderView();
	}

	/** Get File position in parent directory */
	private int getFileIndex(File file) {
		int fileIndex = -1;
		for (int i = 0; i < currentFiles.size(); ++i) {
			if (currentFiles.get(i).getAbsolutePath()
					.equals(file.getAbsolutePath())) {
				fileIndex = i;
				break;
			}
		}
		return fileIndex;
	}

	/** Set this file as the Selected One */
	private void setSelectedItem(CryptFile file) {
		if (selectedItem != null)
			selectedItem.setSelected(false);
		selectedItem = file;
		selectedItem.setSelected(true);
		bottomTextView.setEllipsize(TruncateAt.START);
		bottomTextView.setText(getResources().getString(
				R.string.fe_selected_text)
				+ " " + selectedItem.getName());
	}



	/** Get Tip Text (fe_tip_X) - code == 0 for random */
	private String getTip(int tipCode) {
		if (tips.size() == 0) {
			int tipCounter = 0;
			while (true) {
				++tipCounter;
				String resourceName = "fe_tip_" + tipCounter;
				String tempTip = getStringResource(resourceName);
				if (!tempTip.equals(resourceName))
					tips.add(tempTip);
				else
					break;
			}
		}

		String tip = "NULL";
		int tipIndex;

		if (tipCode > 0)
			tipIndex = tipCode - 1;
		else {
			Random rand = new Random(System.currentTimeMillis());
			tipIndex = rand.nextInt(tips.size());
		}
		try {
			tip = tips.get(tipIndex);
		} catch (Exception e) {
		}

		return tip;
	}

	/**
	 * Back Button - if current dir has a parent show the parent - else go to
	 * the main menu
	 */
	@Override
	public void onBackPressed() {
		CryptFile cf = null;
		if (currentFiles != null && currentFiles.size() > 0)
			cf = currentFiles.get(0);
		if (cf != null && cf.isBackDir()) {
			currentDir = cf;
			updateCurrentFiles();
			fileSelectArrayAdapter.notifyDataSetChanged();
			setHistoricScrollPosition(cf);
		} else {
			if (askOnLeave && !startFromFileSystem) {
				ComponentProvider
						.getBaseQuestionDialog(
								this,
								getResources().getString(
										R.string.common_returnToMainMenuTitle),
								getResources()
										.getString(
												R.string.common_question_leave)
										.replaceAll(
												"<1>",
												getResources()
														.getString(
																R.string.common_app_fileEncryptor_name)),
								null, COMMON_MESSAGE_CONFIRM_EXIT).show();
			} else
				setMessage(new ActivityMessage(COMMON_MESSAGE_CONFIRM_EXIT,
						null));
		}
		
		saveSelectedList(); //save selected file list on exit
	}

	@Override
	protected void onStart() {
		setRunningCode(RUNNING_FILEENCACTIVITY);
		super.onStart();
	}

	@Override
	public void onDestroy() {
		if (dirSizeThread != null)
			dirSizeThread.interrupt();
		super.onDestroy();
	}


	// Handler for miscellaneous background activities
	Handler universalHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == FEA_UNIVERSALHANDLER_SHOW_WAITDIALOG) {
				if (waitDialog != null)
					waitDialog.show();
				return;
			}
			if (msg.what == FEA_UNIVERSALHANDLER_HIDE_WAITDIALOG) {
				if (waitDialog != null)
					waitDialog.cancel();
				return;
			}
			if (msg.what == FEA_UNIVERSALHANDLER_REFRESH_FILELIST) {
				updateCurrentFiles();
				fileSelectArrayAdapter.notifyDataSetChanged();
				return;
			}
			if (msg.what == FEA_UNIVERSALHANDLER_SHOW_DIRSIZE) {
				List message = (List) msg.obj;
				String path = (String) message.get(0);
				Long size = (Long) message.get(1);
				((FileSelectArrayAdapter) fileSelectArrayAdapter).setDirSize(path, size);
				fileSelectArrayAdapter.notifyDataSetChanged();
				return;
			}
			if (msg.what == FEA_UNIVERSALHANDLER_SHOW_VOLUMESIZE) {
				List message = (List) msg.obj;
				String titleRight = (String) message.get(0);
				String titleRightTag = (String) message.get(1);
				setTitleRight(titleRight);
				setTitleRightTag(titleRightTag);
				volumeSizeThread = null;
				return;
			}
		}
	};
	
}
