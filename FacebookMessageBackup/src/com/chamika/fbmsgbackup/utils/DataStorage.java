package com.chamika.fbmsgbackup.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chamika.fbmsgbackup.model.FBFriend;
import com.chamika.fbmsgbackup.model.FBThread;

public class DataStorage {

	public static Map<Long, FBFriend> allFriends;
	public static List<FBThread> allThreads;
	public static FBFriend loggedUser;

	public static Map<Long, FBFriend> getAllFriends() {
		if (allFriends == null) {
			allFriends = new HashMap<Long, FBFriend>();
		}
		return allFriends;
	}

	public static List<FBThread> getAllThreads() {
		if (allThreads == null) {
			allThreads = new ArrayList<FBThread>();
		}
		return allThreads;
	}

	public static FBFriend getLoggedUser() {
		return loggedUser;
	}

	public static void setLoggedUser(FBFriend loggedUser) {
		DataStorage.loggedUser = loggedUser;
	}
	
	

}
