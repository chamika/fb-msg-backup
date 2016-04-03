package com.chamika.fbmsgbackup.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.chamika.fbmsgbackup.model.FBFriend;
import com.chamika.fbmsgbackup.model.FBThread;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

public class FBUtil {

	public static final String PARAM_FIELDS = "fields";
	public static final String PARAM_AFTER = "after";
	public static final String PARAM_SINCE = "since";
	public static final String PARAM_UNTIL = "until";
	public static final String PARAM_LIMIT = "limit";
	public static final String TAGGED_PHOTOS_API_CALL = "/me/photos";
	public static final String FRIENDS_API_CALL = "me/friends";
	public static final String INBOX_API_CALL = "me/inbox";
	public static final String ME_API_CALL = "me";
	public static final String PHOTOS_FIELDS = "id,created_time,picture,source,link,place{name},likes.summary(true).limit(0),comments.summary(true).limit(0)";
	public static final String FRIENDS_FIELDS = "picture{url},name,id";
	public static final String INBOX_FIELDS = "id,comments.limit(1),to";
	public static final String ME_FIELDS = "name,picture{url},id";
	public static final String COMMENTS_FIELDS = "from,message";

	public static Response getRequest(String graphUrl) {
		return getRequest(graphUrl, (Map<String, String>) null);
	}

	public static Response getRequest(String graphUrl, String fields) {
		Map<String, String> params = new HashMap<String, String>();
		if (fields != null) {
			params.put(PARAM_FIELDS, fields);
		}
		return getRequest(graphUrl, params);
	}

	public static Response getRequest(String graphUrl, Map<String, String> parameters) {
		// Request request = new Request(getSession(), graphUrl, null,
		// HttpMethod.GET, null);

		Bundle params = new Bundle();
		if (parameters != null) {
			Iterator it = parameters.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, String> pairs = (Entry<String, String>) it.next();
				params.putString(pairs.getKey(), pairs.getValue());
				it.remove(); // avoids a ConcurrentModificationException
			}

		}
		Request request = new Request(getSession(), graphUrl, params, HttpMethod.GET, null);
		return Request.executeAndWait(request);
	}

	private static Session getSession() {
		return Session.getActiveSession();
	}

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

	/**
	 * should be called in background thread
	 */
	public static String getProfilePicUrl() {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("redirect", "false");
		params.put("type", "large");
		Response response = getRequest("/me/picture", params);

		if (response != null && response.getError() == null) {
			try {
				JSONObject jsonObject = response.getGraphObject().getInnerJSONObject();
				return jsonObject.getJSONObject("data").getString("url");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * should be called in background thred
	 * 
	 * @return
	 */
	public static String getCoverUrl() {

		Response response = getRequest("/me", "cover");

		if (response != null && response.getError() == null) {
			try {
				JSONObject jsonObject = response.getGraphObject().getInnerJSONObject();
				return jsonObject.getJSONObject("cover").getString("source");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static String getAlbumPhotosUrl(String albumId) {
		StringBuilder sb = new StringBuilder();
		sb.append("/");
		sb.append(albumId);
		sb.append("/photos");
		return sb.toString();
	}

	// public static String getNext(Response response) {
	// try {
	// final JSONObject json = response.getGraphObject().getInnerJSONObject();
	// JSONObject cursors =
	// json.getJSONObject("paging").getJSONObject("cursors");
	// if (cursors.has("after")) {
	// String nextId = cursors.getString("after");
	// return nextId;
	// }
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// return null;
	// }

	/**
	 * returns the url field of next page.
	 * @param response
	 * @return value of next url starting from ? including that character
	 */
	public static String getNextUrl(Response response) {
		try {
			final JSONObject json = response.getGraphObject().getInnerJSONObject();
			String url = json.getJSONObject("paging").getString("next");

			if (url != null) {
				int startIndex = url.indexOf('?');
				if (startIndex >= 0) {
					return url.substring(startIndex);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String getProfileLink(String profileId) {
		StringBuilder sb = new StringBuilder();
		sb.append("https://www.facebook.com/");
		sb.append(profileId);
		return sb.toString();
	}

	public static FBFriend convertFriendsJson(JSONObject json) {
		if (json == null) {
			return null;
		}

		try {
			FBFriend friend = new FBFriend();
			friend.setUserId(json.getLong("id"));
			friend.setName(json.getString("name"));

			try {
				friend.setThumbUrl(json.getJSONObject("picture").getJSONObject("data").getString("url"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return friend;
		} catch (JSONException e) {
			e.printStackTrace();

		}

		return null;
	}

	public static FBThread convertThreadJson(JSONObject json) {
		if (json == null) {
			return null;
		}

		try {
			FBThread object = new FBThread();
			object.setMessageCount(-1);
			object.setThreadId(json.getString("id"));

			try {
				// add recipients
				JSONArray recipientsArr = json.getJSONObject("to").getJSONArray("data");

				if (recipientsArr != null) {
					List<FBFriend> recipients = new ArrayList<FBFriend>();
					for (int i = 0; i < recipientsArr.length(); i++) {
						JSONObject recipientJson = recipientsArr.getJSONObject(i);
						recipients
								.add(new FBFriend(recipientJson.getString("name"), recipientJson.getLong("id"), null));
					}
					object.setRecipients(recipients);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				// add snippet
				JSONArray messages = json.getJSONObject("comments").getJSONArray("data");
				if (messages != null && messages.length() > 0) {
					JSONObject firstMessage = messages.getJSONObject(0);
					object.setSnippet(firstMessage.getString("message"));
					JSONObject senderJson = firstMessage.getJSONObject("from");
					object.setSnippetAuthor(new FBFriend(senderJson.getString("name"), senderJson.getLong("id"), null));

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return object;
		} catch (JSONException e) {
			e.printStackTrace();

		}

		return null;
	}
}
