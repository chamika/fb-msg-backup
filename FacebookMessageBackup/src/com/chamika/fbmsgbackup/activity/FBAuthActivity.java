package com.chamika.fbmsgbackup.activity;

import java.util.Arrays;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.R.id;
import com.chamika.fbmsgbackup.R.layout;
import com.chamika.fbmsgbackup.utils.AppLogger;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.google.android.gms.analytics.GoogleAnalytics;

public class FBAuthActivity extends ActionBarActivity {

	private static final String TAG = "FBAuthActivity";

	private static final String[] PERMISSIONS = { "read_mailbox" };

	private UiLifecycleHelper uiHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fbauth);

		if (savedInstanceState == null) {

		}

		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		LoginButton authButton = (LoginButton) findViewById(R.id.authButton);
		authButton.setReadPermissions(Arrays.asList(PERMISSIONS));

	}

	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		if (state.isOpened()) {
			AppLogger.log(TAG, "Logged in...");
			setResult(RESULT_OK);
			finish();
		} else if (state.isClosed()) {
			AppLogger.log(TAG, "Logged out...");
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// For scenarios where the main activity is launched and user
		// session is not null, the session state change notification
		// may not be triggered. Trigger it if it's open/closed.
		// Session session = Session.getActiveSession();
		// if (session != null && (session.isOpened() || session.isClosed())) {
		// onSessionStateChange(session, session.getState(), null);
		// }

		uiHelper.onResume();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		GoogleAnalytics.getInstance(this).reportActivityStart(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		GoogleAnalytics.getInstance(this).reportActivityStop(this);
	}
	

}
