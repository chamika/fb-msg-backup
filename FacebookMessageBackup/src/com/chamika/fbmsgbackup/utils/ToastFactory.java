package com.chamika.fbmsgbackup.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastFactory {

	public static void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

}
