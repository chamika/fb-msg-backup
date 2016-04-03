package com.chamika.fbmsgbackup.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FBMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3934189422116650669L;

	private String body;
	private String messageId;
	private long time;
	private String author;

	public FBMessage() {
		super();
	}

	public FBMessage(String body, String messageId, long time, String author) {
		super();
		this.body = body;
		this.messageId = messageId;
		this.time = time;
		this.author = author;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public Date getUtilDate() {
		return new Date(time * 1000L); // *1000 is to convert seconds to
										// milliseconds
	}

	public String getFormattedTime() {
		SimpleDateFormat formater = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z", Locale.US);
		Date date = getUtilDate();
		String result = null;
		try {
			result = formater.format(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (result == null) {
			result = String.valueOf(date.getTime());
		}

		return result;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

}
