package com.paranoiaworks.unicus.android.sse.dao;

import java.io.Serializable;
import java.text.SimpleDateFormat;


/**
 * Keeps application status (history record)
 * TODO needs to be extended in next versions
 * 
 * @author Unicus (unicus<atmark>paranoiaworks.com) for Paranoia Works
 * @version 1.0.1
 * @related DB table APP_STATUS
 */ 
public class ApplicationStatusBean implements Serializable {
	
	private static final long serialVersionUID = 10L;

	private final String DATE_FORMAT = "dd.MM.yyyy HH:mm";
	private long presentRun = 0;
	private long lastRun = 0;
	private int numberOfRuns = 1;
	private long firstRun = 0;
	private String field1 = null;
	private String checksum = "";
	private boolean checksumOk = false;
	
	
	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public boolean isChecksumOk() {
		return checksumOk;
	}

	public String isChecksumOkText() {
		String text = checksumOk ? "OK" : "KO";
		return text;
	}
	
	public void setChecksumOk(boolean checksumOk) {
		this.checksumOk = checksumOk;
	}

	public long getFirstRun() {
		return firstRun;
	}
	
	public String getFirstRunString() {
		return formatDate(firstRun);
	}

	public void setFirstRun(long firstRun) {
		this.firstRun = firstRun;
	}

	public long getPresentRun() {
		return this.presentRun;
	}
	
	public String getPresentRunString() {
		return formatDate(presentRun);
	}
	
	public void setPresentRun(long presentRun) {
		this.presentRun = presentRun;
	}
	
	public long getLastRun() {
		return lastRun;
	}

	public void setLastRun(long lastRun) {
		this.lastRun = lastRun;
	}
	
	public String getLastRunString() {
		return formatDate(lastRun);
	}

	public int getNumberOfRuns() {
		return numberOfRuns;
	}
	
	public void setNumberOfRuns(int numberOfRuns) {
		this.numberOfRuns = numberOfRuns;
	}
	
	private String formatDate(long time)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		return sdf.format(time);
	}

	public String getField1() {
		return field1;
	}

	public void setField1(String field1) {
		this.field1 = field1;
	}
	
}

