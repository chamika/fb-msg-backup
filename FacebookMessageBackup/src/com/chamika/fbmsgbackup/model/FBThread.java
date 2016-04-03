package com.chamika.fbmsgbackup.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FBThread implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4647839576388750137L;

	private String threadId;
	private long messageCount;
	private List<FBFriend> recipients;
	private String snippet;
	private FBFriend snippetAuthor;

	private List<FBFriend> friends;

	private String cachedSnippet = null;

	public FBThread() {
		super();
	}

	public FBThread(String threadId, long messageCount, List<FBFriend> recipients) {
		super();
		this.threadId = threadId;
		this.messageCount = messageCount;
		this.recipients = recipients;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public long getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(long messageCount) {
		this.messageCount = messageCount;
	}

	public List<FBFriend> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<FBFriend> recipients) {
		this.recipients = recipients;
	}

	public List<FBFriend> getFriends(FBFriend loggedUser) {
		if (friends == null) {
			friends = new ArrayList<FBFriend>();
			if (this.recipients != null) {
				for (FBFriend friend : this.recipients) {
					if (loggedUser == null) {
						friends.add(friend);
					} else if (loggedUser.getUserId() != friend.getUserId()) {
						friends.add(friend);
					}
				}
			}
		}

		return friends;
	}

	public String getSnippet() {
		return snippet;
	}

	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}

	public FBFriend getSnippetAuthor() {
		return snippetAuthor;
	}

	public void setSnippetAuthor(FBFriend snippetAuthor) {
		this.snippetAuthor = snippetAuthor;
	}

	public String getFriendsNames(FBFriend loggedUser) {

		StringBuilder sb = new StringBuilder();

		List<FBFriend> friends = getFriends(loggedUser);

		int size = friends.size();

		if (size == 0) {
			sb.append("Unknown");
		} else {
			for (int i = 0; i < size; i++) {
				if (i != 0) {
					sb.append(", ");
				}

				if (size == 1) {
					sb.append(friends.get(i).getName());
				} else {
					String name = friends.get(i).getFirstName();
					if (name != null)
						sb.append(name.split(" ")[0]);
				}
			}
		}

		return sb.toString();
	}

	/**
	 * @return snippet with author of the snippet seperated by semicolan
	 */
	public String getSnippetWithFriend() {
		if (cachedSnippet == null) {
			if (getSnippet() == null) {
				cachedSnippet = null;
			} else {
				if (getSnippetAuthor() != null) {
					StringBuilder sb = new StringBuilder();
					sb.append(getSnippetAuthor().getFirstName());
					sb.append(": ");
					sb.append(getSnippet());
					cachedSnippet = sb.toString();
				} else {
					cachedSnippet = getSnippet();
				}
			}
		}

		return cachedSnippet;
	}
}
