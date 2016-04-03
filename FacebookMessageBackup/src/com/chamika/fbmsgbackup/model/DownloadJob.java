package com.chamika.fbmsgbackup.model;

import java.io.Serializable;

public class DownloadJob implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3799961927743210789L;
	FBThread thread;
	long minIndex;
	long maxIndex;

	long downloadIndex;
	boolean stop; // setting true will stop the download job
	boolean working; // whether downloading or not

	String jobId;

	// used to store filename and maximum row count of a file to be saved
	private String folderName;
	long rowCount = 5000L;

	public DownloadJob() {
		super();
		folderName = "fb_backup";
		rowCount = 5000;
	}

	public DownloadJob(FBThread thread, long minIndex, long maxIndex) {
		this();
		this.thread = thread;
		this.minIndex = minIndex;
		this.maxIndex = maxIndex;

	}

	public FBThread getThread() {
		return thread;
	}

	public void setThread(FBThread thread) {
		this.thread = thread;
		this.jobId = null;
	}

	public long getMinIndex() {
		return minIndex;
	}

	public void setMinIndex(long minIndex) {
		this.minIndex = minIndex;
		this.jobId = null;
	}

	public long getMaxIndex() {
		return maxIndex;
	}

	public void setMaxIndex(long maxIndex) {
		this.maxIndex = maxIndex;
		this.jobId = null;
	}

	public long getRowCount() {
		return rowCount;
	}

	public void setRowCount(long rowCount) {
		this.rowCount = rowCount;
	}

	public long getDownloadIndex() {
		return downloadIndex;
	}

	public void setDownloadIndex(long downloadIndex) {
		this.downloadIndex = downloadIndex;
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
		if (stop == true) {
			this.working = false;
		}
	}

	public boolean isWorking() {
		return working;
	}

	public void setWorking(boolean working) {
		this.working = working;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	/**
	 * generate unique id for the download job
	 * 
	 * @return threadId_min_max_filename
	 */
	private String generateJobId() {
		StringBuilder sb = new StringBuilder();

		sb.append(this.getThread().getThreadId());
		sb.append("_");
		sb.append(this.getMinIndex());
		sb.append("_");
		sb.append(this.getMaxIndex());
		sb.append("_");
		sb.append(this.getFolderName());

		this.jobId = sb.toString();
		return this.jobId;
	}

	public String getJobId() {
		if (this.jobId == null) {
			this.jobId = generateJobId();
		}
		return jobId;
	}
}
