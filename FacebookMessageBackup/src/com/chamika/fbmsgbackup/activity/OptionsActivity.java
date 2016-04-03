package com.chamika.fbmsgbackup.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.R.id;
import com.chamika.fbmsgbackup.R.layout;
import com.chamika.fbmsgbackup.adapters.OptionsFragmentAdapter;
import com.chamika.fbmsgbackup.fragments.MessageOptionFragment;
import com.chamika.fbmsgbackup.fragments.StorageOptionFragment;
import com.chamika.fbmsgbackup.model.DownloadJob;
import com.chamika.fbmsgbackup.model.FBThread;
import com.chamika.fbmsgbackup.utils.AppLogger;
import com.chamika.fbmsgbackup.utils.ToastFactory;
import com.chamika.fbmsgbackup.views.CustomViewPager;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.viewpagerindicator.CirclePageIndicator;

public class OptionsActivity extends ActionBarActivity {

	public static final String TAG = "OptionsActivity";

	public static final String INTENT_EXTRA_THREAD = "THREAD";

	private static final int REQUEST_CODE_STATUS_ACTIVITY = 100;

	private FBThread thread;

	private MessageOptionFragment messageOptionsFragment;
	private StorageOptionFragment storageOptionFragment;

	private CustomViewPager pager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_options);

		// if (savedInstanceState == null) {
		// getSupportFragmentManager().beginTransaction().add(R.id.container,
		// new PlaceholderFragment()).commit();
		// }

		if (getIntent().hasExtra(INTENT_EXTRA_THREAD)) {
			thread = (FBThread) getIntent().getSerializableExtra(INTENT_EXTRA_THREAD);
		} else {
			finish();
		}

		initComponents();
	}

	@Override
	public void onBackPressed() {
		if (pager.getCurrentItem() == 0) {
			super.onBackPressed();
		} else {
			pager.setCurrentItem(pager.getCurrentItem() - 1, true);
		}

	}

	private void initComponents() {

		OptionsFragmentAdapter adapter = new OptionsFragmentAdapter(getSupportFragmentManager());

		pager = (CustomViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);

		pager.setPagingEnabled(false);

		CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);

		for (int i = 0; i < adapter.getCount(); i++) {
			if (adapter.getItem(i) instanceof MessageOptionFragment) {
				messageOptionsFragment = (MessageOptionFragment) adapter.getItem(i);
			} else if (adapter.getItem(i) instanceof StorageOptionFragment) {
				storageOptionFragment = (StorageOptionFragment) adapter.getItem(i);
			}
		}

		if (messageOptionsFragment != null) {
			messageOptionsFragment.setRange(thread.getMessageCount());
			messageOptionsFragment.setThread(thread);
		}

		if (storageOptionFragment != null) {
			storageOptionFragment.setThread(thread);
		}
	}

	public void option1Next(View v) {
		if (pager.getCurrentItem() + 1 < pager.getAdapter().getCount()) {
			pager.setCurrentItem(pager.getCurrentItem() + 1);
		}
	}

	public void option2Back(View v) {
		onBackPressed();
	}

	public void submit(View v) {
		AppLogger.log(TAG, "Submit");

		if (messageOptionsFragment != null && storageOptionFragment != null) {
			DownloadJob job = new DownloadJob();
			job.setThread(thread);
			job.setMinIndex(messageOptionsFragment.getLowIndex());
			job.setMaxIndex(messageOptionsFragment.getHighIndex());

			// storage options show
			String folderName = storageOptionFragment.getFolderName();
			if(folderName == null){
				return;
			}
			job.setFolderName(folderName);

			Intent intent = new Intent(getApplicationContext(), StatusActivity.class);
			intent.putExtra(StatusActivity.INTENT_EXTRA_JOB, job);
			intent.putExtra(StatusActivity.INTENT_EXTRA_STATUS, 0);

			startActivityForResult(intent, REQUEST_CODE_STATUS_ACTIVITY);

		} else {
			ToastFactory.showToast(this, "Something went wrong. Can't continue");
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_STATUS_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				finish();
			}
		}

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
	

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// // Handle action bar item clicks here. The action bar will
	// // automatically handle clicks on the Home/Up button, so long
	// // as you specify a parent activity in AndroidManifest.xml.
	// int id = item.getItemId();
	// if (id == R.id.action_settings) {
	// return true;
	// }
	// return super.onOptionsItemSelected(item);
	// }

}
