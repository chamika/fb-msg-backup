package com.chamika.fbmsgbackup.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.chamika.fbmsgbackup.model.FBFriend;
import com.chamika.fbmsgbackup.model.FBMessage;
import com.chamika.fbmsgbackup.model.FBThread;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

public class DataLoader {

	private static final String TAG = "DataLoader";

	private static final String JSON_DATA = "data";
	private static final String JSON_UID = "uid";
	private static final String JSON_NAME = "name";
	private static final String JSON_PICTURE = "pic_square";
	private static final String JSON_MESSAGE_ID = "message_id";
	private static final String JSON_BODY = "body";
	private static final String JSON_CREATED_TIME = "created_time";
	private static final String JSON_AUTHOR_ID = "author_id";
	private static final String JSON_THREAD_ID = "thread_id";
	private static final String JSON_SNIPPET = "snippet";
	private static final String JSON_SNIPPET_AUTHOR = "snippet_author";
	private static final String JSON_MESSAGE_COUNT = "message_count";
	private static final String JSON_RECEIPIENTS = "recipients";

	private static final String FILE_FRIENDS = "friends";
	private static int errorCode = -1;

	private static Hashtable<Integer, String> threadsNextIds = new Hashtable<Integer, String>();

	public static List<FBThread> loadMessageThreads(int lower, int size) {
		List<FBThread> result = new ArrayList<FBThread>();

		String url = threadsNextIds.get(lower);

		Response firstResponse = null;

		if (url != null) {
			firstResponse = FBUtil.getRequest(FBUtil.INBOX_API_CALL + url);
		} else {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(FBUtil.PARAM_FIELDS, FBUtil.INBOX_FIELDS);
			params.put(FBUtil.PARAM_LIMIT, String.valueOf(size));
			firstResponse = FBUtil.getRequest(FBUtil.INBOX_API_CALL, params);

			threadsNextIds.clear();
		}

		if (firstResponse != null && firstResponse.getError() == null) {
			List<FBThread> listResponse = handleThreadsRespnose(firstResponse);

			// remove first element if comes from next
			if (url != null && listResponse.size() > 0) {
				listResponse.remove(0);
			}

			result.addAll(listResponse);

			String nextUrl = FBUtil.getNextUrl(firstResponse);
			if (nextUrl != null) {
				threadsNextIds.put(lower + result.size(), nextUrl);
			}
		}

		// get count of each thread
		if (result.size() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("select thread_id,message_count,recipients,snippet,snippet_author from thread where viewer_id = me() and folder_id = 0 limit ");
			sb.append(lower);
			sb.append(",");
			sb.append(size);
			String fqlQuery = sb.toString();

			List<Response> responses = getFacebookFQL(fqlQuery);

			if (responses == null || responses.size() == 0) {
				return null;
			} else {
				Response response = responses.get(0);

				if (response.getError() != null) {
					return null;
				}

				try {
					JSONObject json = response.getGraphObject().getInnerJSONObject();

					if (json != null) {
						HashMap<String, Long> threadsMsgCount = new HashMap<String, Long>();
						JSONArray data = json.optJSONArray(JSON_DATA);

						for (int i = 0; i < data.length(); i++) {
							try {
								JSONObject threadJson = data.getJSONObject(i);
								threadsMsgCount.put(threadJson.getString(JSON_THREAD_ID),
										threadJson.getLong(JSON_MESSAGE_COUNT));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}

						for (FBThread thread : result) {
							Long count = threadsMsgCount.get(thread.getThreadId());
							if (count != null) {
								thread.setMessageCount(count);
							}
						}

					} else {
						return null;
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		DataStorage.getAllThreads().clear();
		DataStorage.getAllThreads().addAll(result);

		return result;
	}

	/**
	 * loads messages for the given criteria
	 * 
	 * @param threadId
	 * @param pageMin
	 * @param size
	 * @return list of loaded messages. null if error occured
	 */
	public static List<FBMessage> loadMessages(String threadId, long pageMin, long size) {
		// SELECT message_id,body,viewer_id,created_time, thread_id FROM message
		// WHERE thread_id = '1343351879719'
		// AND viewer_id = me() LIMIT 30,60

		List<FBMessage> result = new ArrayList<FBMessage>();

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT message_id,body,created_time,author_id FROM message WHERE thread_id = '");
		sb.append(threadId);
		sb.append("' AND viewer_id = me() LIMIT ");
		sb.append(pageMin);
		sb.append(",");
		sb.append(size);

		List<Response> responses = getFacebookFQL(sb.toString());

		if (responses == null || responses.size() == 0) {
			return null;
		} else {
			Response response = responses.get(0);

			try {
				JSONObject json = response.getGraphObject().getInnerJSONObject();

				if (json != null) {
					JSONArray data = json.optJSONArray(JSON_DATA);

					for (int i = 0; i < data.length(); i++) {
						JSONObject messageJSON = data.getJSONObject(i);
						try {

							FBMessage message = new FBMessage(messageJSON.getString(JSON_BODY),
									messageJSON.getString(JSON_MESSAGE_ID), messageJSON.getLong(JSON_CREATED_TIME),
									messageJSON.getString(JSON_AUTHOR_ID));

							result.add(message);
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
					errorCode = -1;

				} else {
					setMsgError(response);
					return null;
				}

			} catch (JSONException e) {
				e.printStackTrace();
				setMsgError(response);
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				setMsgError(response);
				return null;
			}
		}

		return result;
	}

	private static void setMsgError(Response response) {
		FacebookRequestError error = response.getError();
		if (error != null) {
			errorCode = error.getErrorCode();
		} else {
			errorCode = -1;
		}
	}

	public static List<FBFriend> loadFriends() {

		// get logged user profile
		try {
			Response meResponse = FBUtil.getRequest(FBUtil.ME_API_CALL, FBUtil.ME_FIELDS);
			final JSONObject json = meResponse.getGraphObject().getInnerJSONObject();

			if (json != null) {
				FBFriend user = new FBFriend();
				user.setName(json.getString("name"));
				user.setUserId(json.getLong("id"));
				try {
					user.setThumbUrl(json.getJSONObject("picture").getJSONObject("data").getString("url"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				DataStorage.setLoggedUser(user);
				DataStorage.getAllFriends().put(user.getUserId(), user);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// List<FBFriend> results = new ArrayList<FBFriend>();
		// try {
		//
		// final Response firstResponse =
		// FBUtil.getRequest(FBUtil.FRIENDS_API_CALL, FBUtil.FRIENDS_FIELDS);
		//
		// if (firstResponse != null && firstResponse.getError() == null) {
		// List<FBFriend> firstRespnose = handleFriendsRespnose(firstResponse);
		// results.addAll(firstRespnose);
		//
		// String afterId = FBUtil.getNext(firstResponse);
		// do {
		// Map<String, String> params = new HashMap<String, String>();
		// params.put(FBUtil.PARAM_FIELDS, FBUtil.FRIENDS_FIELDS);
		// params.put(FBUtil.PARAM_AFTER, afterId);
		// final Response response = FBUtil.getRequest(FBUtil.FRIENDS_API_CALL,
		// params);
		//
		// List<FBFriend> otherResponse = handleFriendsRespnose(response);
		// results.addAll(otherResponse);
		//
		// afterId = FBUtil.getNext(response);
		// } while (afterId != null);
		//
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		// String fqlQuery =
		// "SELECT uid, name, pic_square FROM user WHERE uid = me() OR uid IN (SELECT uid2 FROM friend WHERE uid1 = me())";
		// List<Response> responses = getFacebookFQL(fqlQuery);
		//
		// if (responses == null || responses.size() == 0) {
		// return null;
		// } else {
		// Response response = responses.get(0);
		//
		// try {
		// JSONObject json = response.getGraphObject().getInnerJSONObject();
		//
		// if (json != null) {
		// JSONArray data = json.optJSONArray(JSON_DATA);
		//
		// for (int i = 0; i < data.length(); i++) {
		// JSONObject friendJson = data.getJSONObject(i);
		//
		// long uid = friendJson.getLong(JSON_UID);
		//
		// String imageUrl = friendJson.getString(JSON_PICTURE);
		// imageUrl = imageUrl.replace("\\", "");
		//
		// FBFriend friend = new FBFriend(friendJson.getString(JSON_NAME), uid,
		// imageUrl);
		//
		// result.add(friend);
		// DataStorage.getAllFriends().put(uid, friend);
		//
		// // identify logged user
		// if (i == 0) {
		// DataStorage.setLoggedUser(friend);
		// }
		// }
		// } else {
		// return null;
		// }
		//
		// } catch (JSONException e) {
		// e.printStackTrace();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }

		// return results;
		return null;
	}

	private static List<FBFriend> handleFriendsRespnose(final Response response) {
		List<FBFriend> result = new ArrayList<FBFriend>();
		try {
			final JSONObject json = response.getGraphObject().getInnerJSONObject();
			final JSONArray friendsJson = json.getJSONArray("data");

			for (int i = 0; i < friendsJson.length(); i++) {
				final JSONObject photoJson = friendsJson.getJSONObject(i);
				FBFriend friend = FBUtil.convertFriendsJson(photoJson);

				if (friend != null) {
					result.add(friend);
					DataStorage.getAllFriends().put(friend.getUserId(), friend);
				}

			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static List<FBThread> handleThreadsRespnose(final Response response) {
		List<FBThread> result = new ArrayList<FBThread>();
		try {
			final JSONObject json = response.getGraphObject().getInnerJSONObject();
			final JSONArray dataJson = json.getJSONArray("data");

			for (int i = 0; i < dataJson.length(); i++) {
				final JSONObject jsonItem = dataJson.getJSONObject(i);
				FBThread convertedObj = FBUtil.convertThreadJson(jsonItem);

				if (convertedObj != null) {
					result.add(convertedObj);
				}

			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static List<Response> getFacebookFQL(String fqlQuery) {
		Bundle params = new Bundle();
		params.putString("q", fqlQuery);

		Session session = Session.getActiveSession();
		Request request = new Request(session, "/v2.0/fql", params, HttpMethod.GET, null);
		List<Response> responses = Request.executeBatchAndWait(request);

		if (responses != null && responses.size() > 0) {
			AppLogger.log(TAG, "FQL results[0]: " + responses.get(0).toString());
		}

		return responses;
	}

	private static Response getFacebookGraph(String graphUrl) {
		return FBUtil.getRequest(graphUrl, (Map<String, String>) null);
	}

	private static Response getFacebookGraph(String graphUrl, String fields) {
		return FBUtil.getRequest(graphUrl, fields);
	}

	private static Response getFacebookGraph(String graphUrl, Map<String, String> parameters) {
		return FBUtil.getRequest(graphUrl, parameters);
	}

	public static int getErrorCode() {
		return errorCode;
	}

	public static void setErrorCode(int errorCode) {
		DataLoader.errorCode = errorCode;
	}

}
