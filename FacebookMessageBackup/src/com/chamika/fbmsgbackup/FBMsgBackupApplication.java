package com.chamika.fbmsgbackup;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class FBMsgBackupApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Create global configuration and initialize ImageLoader with this
		// configuration
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext()).threadPoolSize(
				10).build();
		ImageLoader.getInstance().init(config);

	}

	Tracker tracker = null;

	public synchronized Tracker getTracker() {
		if (tracker == null) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			tracker = analytics.newTracker(R.xml.tracker);
		}
		return tracker;
	}
}
