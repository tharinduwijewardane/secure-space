package com.paranoiaworks.unicus.android.sse.dao;

/**
 * Part of "Activity messaging system"
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.0
 * @related CryptActivity.java and all child classes (methods - getMessage, setMessage, resetMessage, processMessage)
 */
public class ActivityMessage {

	private int messageCode;
	private String mainMessage;
	private Object attachement;
	
	public ActivityMessage (int messageCode, String mainMessage)
	{
		this.messageCode = messageCode;
		this.mainMessage = mainMessage;
	}
	
	public ActivityMessage (int messageCode, String mainMessage, Object attachement)
	{
		this.messageCode = messageCode;
		this.mainMessage = mainMessage;
		this.attachement = attachement;
	}

	public int getMessageCode() {
		return messageCode;
	}

	public String getMainMessage() {
		return mainMessage;
	}

	public Object getAttachement() {
		return attachement;
	}
}
