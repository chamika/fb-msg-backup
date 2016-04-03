package com.chamika.fbmsgbackup.model;

import java.io.Serializable;

public class FBFriend implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6171475469559104025L;

	private String name;
	private long userId;
	private String thumbUrl;

	public FBFriend() {
		super();
	}

	public FBFriend(String name, long userId, String thumbUrl) {
		super();
		this.name = name;
		this.userId = userId;
		this.thumbUrl = thumbUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public String getThumbUrl() {
		if (thumbUrl == null) {
			thumbUrl = "http://graph.facebook.com/" + this.userId + "/picture";
		}
		return thumbUrl;
	}

	public void setThumbUrl(String thumbUrl) {
		this.thumbUrl = thumbUrl;
	}

	public String getFirstName() {
		if (this.name == null) {
			return null;
		} else {
			return this.name.split(" ")[0];
		}
	}

	@Override
	public String toString() {
		return "FBFriend [userId=" + userId + ", name=" + name + ", thumbUrl=" + thumbUrl + "]";
	}

}
