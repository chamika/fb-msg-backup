package com.chamika.fbmsgbackup.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.R.id;
import com.chamika.fbmsgbackup.R.layout;
import com.chamika.fbmsgbackup.utils.ToastFactory;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		String versionString = "";

		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionString = "v" + pInfo.versionName;

		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		TextView versionText = (TextView) findViewById(R.id.text_app_version);
		if (versionText != null) {
			versionText.setText(versionString);
		}

	}

	public void clickRate(View v) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://details?id=com.chamika.fbmsgbackup"));
		if (!startAct(intent)) {
			intent.setData(Uri.parse("https://play.google.com/store/apps/details?com.chamika.fbmsgbackup"));
			if (!startAct(intent)) {
				ToastFactory.showToast(this, "Could not open Android market, please install it");
			}
		}
	}

	private boolean startAct(Intent intent) {
		try {
			startActivity(intent);
			return true;
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}

}
