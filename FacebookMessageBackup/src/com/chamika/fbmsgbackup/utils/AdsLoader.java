package com.chamika.fbmsgbackup.utils;

import android.app.Activity;
import android.view.View;

import com.chamika.fbmsgbackup.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class AdsLoader {

	public static void loadAdMobAds(Activity activity) {
		AdView adView = (AdView) activity.findViewById(R.id.adView);
		startLoadingAds(adView);
	}

	public static void loadAdMobAds(View view) {
		AdView adView = (AdView) view.findViewById(R.id.adView);
		startLoadingAds(adView);
	}

	private static void startLoadingAds(AdView adView) {
		AdRequest adRequest = new AdRequest.Builder()
		// test devices
		// .addTestDevice("07BC280E5666CC02442A1A556D810651")
		// test devices end
				.build();

		adView.loadAd(adRequest);
	}
}
