package com.chamika.fbmsgbackup.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.chamika.fbmsgbackup.FBMsgBackupApplication;
import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.model.FBThread;
import com.chamika.fbmsgbackup.utils.AdsLoader;
import com.chamika.fbmsgbackup.utils.FileLoader;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class StorageOptionFragment extends Fragment {

	View fragmentView;

	private FBThread thread;

	EditText editTextFolderName;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		if (fragmentView == null) {
			fragmentView = inflater.inflate(R.layout.fragment_storage_options, null);
		}

		editTextFolderName = (EditText) fragmentView.findViewById(R.id.editText_fiiename);

		TextView textPrefix = (TextView) fragmentView.findViewById(R.id.text_storage_prefix);
		String prefix = FileLoader.getFilePrefix();

		textPrefix.setText(prefix);

		// load ads
		AdsLoader.loadAdMobAds(fragmentView);

		hitAnalytics();

		return fragmentView;
	}

	public String getFolderName() {
		if (editTextFolderName != null && editTextFolderName.getText().toString().trim().length() > 0) {
			return editTextFolderName.getText().toString().trim();
		} else {
			// return "fb_backup_" + thread.getThreadId();
			editTextFolderName.setError("Enter folder name");
			return null;
		}
	}

	public void setThread(FBThread thread) {
		this.thread = thread;
	}

	private void hitAnalytics() {
		Tracker t = ((FBMsgBackupApplication) getActivity().getApplication()).getTracker();
		t.setScreenName(this.getClass().getSimpleName());
		t.send(new HitBuilders.AppViewBuilder().build());
	}

}
