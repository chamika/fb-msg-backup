package com.chamika.fbmsgbackup.utils;

import android.util.Log;

public class AppLogger {

	public static boolean DEBUG = true;

	public static void log(String tag, String message) {
		if (DEBUG) {
			Log.d(tag, message);
		}
	}

}
