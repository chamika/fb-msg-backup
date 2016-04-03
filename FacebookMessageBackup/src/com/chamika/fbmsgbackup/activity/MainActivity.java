package com.chamika.fbmsgbackup.activity;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chamika.fbmsgbackup.Constants;
import com.chamika.fbmsgbackup.FBMsgBackupApplication;
import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.adapters.ThreadsListAdapter;
import com.chamika.fbmsgbackup.model.FBFriend;
import com.chamika.fbmsgbackup.model.FBThread;
import com.chamika.fbmsgbackup.utils.AdsLoader;
import com.chamika.fbmsgbackup.utils.AppLogger;
import com.chamika.fbmsgbackup.utils.DataLoader;
import com.chamika.fbmsgbackup.utils.DataStorage;
import com.chamika.fbmsgbackup.utils.ToastFactory;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.android.gms.analytics.GoogleAnalytics;

public class MainActivity extends ActionBarActivity {

	private static final int REQUEST_CODE_OPTIONS = 100;

	private static final String TAG = "MainActivity";

	private static final int ACTIVITY_REQUEST_CODE_FB_AUTH = 100;

	private final int PAGING_SIZE = 40;

	private Session.StatusCallback statusCallback;

	private List<FBThread> allThreads;

	private Context context;

	// use to store index of last loaded thread
	private int lastThreadIndex = 0;

	// use to identify if there a change in thread count to stop lazy thread
	// load
	private int oldThreadsCount = 0;

	// ui elements
	private ListView threadListView;
	private PullToRefreshLayout pullToRefreshLayout;
	private ProgressBar progressBar;
	private TextView noConnectionView;
	private Handler lazyThreadLoadHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {

		}

		context = this;

		initUIComponents();

		statusCallback = new SessionStatusCallback();

		if (validateFBLogin()) {
			updateList();
		} else {
			startFBLogin();
		}

		lazyThreadLoadHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (msg.what == 0) {
					notifyListDataChange();
				}
			}

		};

		// init analytics
		((FBMsgBackupApplication) getApplication()).getTracker();

		// getKeyHash();
	}

	private void initUIComponents() {
		threadListView = (ListView) findViewById(R.id.list_view_threads);

		pullToRefreshLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);
		noConnectionView = (TextView) findViewById(R.id.text_no_connection);

		ActionBarPullToRefresh.from(this).allChildrenArePullable().listener(new OnRefreshListener() {
			@Override
			public void onRefreshStarted(View view) {
				updateList();
			}
		}).setup(pullToRefreshLayout);

		progressBar = (ProgressBar) findViewById(R.id.progress_activity_main);

		noConnectionView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateList();
			}
		});

		setTitle(R.string.app_main_title);

		// init ads
		AdsLoader.loadAdMobAds(this);

	}

	/**
	 * validates if the user logged to fb using the app
	 * 
	 * @return
	 */
	private boolean validateFBLogin() {
		Session session = Session.getActiveSession();
		if (session != null && session.isOpened()) {
			return true;
		} else {
			Session sessionNew = Session.openActiveSession(this, false, statusCallback);

			if (sessionNew != null && sessionNew.isOpened()) {
				return true;
			}
		}

		return false;
	}

	private void startFBLogin() {
		startActivityForResult(new Intent(this, FBAuthActivity.class), ACTIVITY_REQUEST_CODE_FB_AUTH);
	}

	private void startDownloadFolder() {
		Intent intent = new Intent(this, FolderBrowserActivity.class);
		String path = Environment.getExternalStorageDirectory() + Constants.BACKUP_FOLDER;
		intent.putExtra(FolderBrowserActivity.INTENT_EXTRA_FOLDER_PATH, path);
		intent.putExtra(FolderBrowserActivity.INTENT_EXTRA_ENABLE_BACK, true);
		startActivity(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == ACTIVITY_REQUEST_CODE_FB_AUTH) {
			if (resultCode == Activity.RESULT_OK) {
				updateList();
			} else {
				if (!validateFBLogin()) {
					finish();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();
		if (id == R.id.action_logout) {
			startFBLogin();
		} else if (id == R.id.action_about) {
			startActivity(new Intent(this, AboutActivity.class));
		} else if (id == R.id.action_refresh) {
			updateList();
		} else if (id == R.id.action_download) {
			startDownloadFolder();
		}
		return super.onOptionsItemSelected(item);
	}

	private class SessionStatusCallback implements Session.StatusCallback {
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			AppLogger.log(TAG, "session update");
		}

	}

	private void updateList() {
		new FirstMessageThreadLoader().execute();
	}

	private void refreshList() {
		ThreadsListAdapter adapter = new ThreadsListAdapter(context, allThreads);

		threadListView.setAdapter(adapter);
		threadListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				FBThread selectedThread = allThreads.get(position);

				// Cache friends for this thread
				if (selectedThread.getFriends(null) != null) {
					for (FBFriend friend : selectedThread.getFriends(DataStorage.getLoggedUser())) {
						DataStorage.getAllFriends().put(friend.getUserId(), friend);
					}
				}

				startOptionsActivity(selectedThread);
			}
		});

	}

	private void notifyListDataChange() {
		ListAdapter adapter = threadListView.getAdapter();
		if (adapter != null && adapter instanceof ThreadsListAdapter) {
			((ThreadsListAdapter) adapter).notifyDataSetChanged();
		}
	}

	private void setProgressVisibility(boolean visible) {
		if (visible) {
			// Toast.makeText(this, "Starting..", Toast.LENGTH_LONG).show();
			progressBar.setVisibility(View.VISIBLE);
			threadListView.setVisibility(View.GONE);
		} else {
			// Toast.makeText(this, "Stopping..", Toast.LENGTH_LONG).show();
			progressBar.setVisibility(View.INVISIBLE);
			threadListView.setVisibility(View.VISIBLE);
		}

	}

	private void startOptionsActivity(FBThread thread) {
		Intent intent = new Intent(this, OptionsActivity.class);
		intent.putExtra(OptionsActivity.INTENT_EXTRA_THREAD, thread);
		startActivityForResult(intent, REQUEST_CODE_OPTIONS);
	}

	private void setNoConnectionVisibility(boolean visible) {
		if (visible) {
			noConnectionView.setVisibility(View.VISIBLE);
		} else {
			noConnectionView.setVisibility(View.GONE);
		}
	}

	private void startNextThreadsLoad() {
		Thread t = new Thread() {
			public void run() {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						new LazyMessageThreadLoader().execute(lastThreadIndex, lastThreadIndex + PAGING_SIZE);
					}
				});

			}
		};

		t.start();
	}

	// should be called in background thread
	public List<FBThread> loadNextThreads() {
		lastThreadIndex = allThreads.size();

		List<FBThread> threads = DataLoader.loadMessageThreads(lastThreadIndex, PAGING_SIZE);
		return threads;
	}

	public List<FBThread> getAllThreads() {
		if (allThreads == null) {
			allThreads = new ArrayList<FBThread>();
		}
		return allThreads;
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

	class FirstMessageThreadLoader extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setProgressVisibility(true);
			setNoConnectionVisibility(false);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			DataLoader.loadFriends();
			allThreads = DataLoader.loadMessageThreads(0, PAGING_SIZE);

			if (allThreads != null) {
				lastThreadIndex = PAGING_SIZE;
				oldThreadsCount = allThreads.size();
				return true;
			}

			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			setProgressVisibility(false);
			if (pullToRefreshLayout != null) {
				pullToRefreshLayout.setRefreshComplete();
			}
			if (result) {
				refreshList();
				// startNextThreadsLoad();
			} else {
				ToastFactory.showToast(context,
						"Unable to retrieve facebook messages. Please check your internet connection");
				setNoConnectionVisibility(true);
			}
		}

	}

	class LazyMessageThreadLoader extends AsyncTask<Integer, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pullToRefreshLayout.setRefreshing(true);
		}

		@Override
		protected Boolean doInBackground(Integer... params) {
			if (params.length == 2) {
				List<FBThread> threads = DataLoader.loadMessageThreads(params[0], PAGING_SIZE);

				if (threads != null && allThreads != null) {
					// TODO find unique threads and add
					allThreads.addAll(threads);

					lazyThreadLoadHandler.sendEmptyMessage(0);

					lastThreadIndex = params[0] + PAGING_SIZE;
					if (oldThreadsCount == allThreads.size()) {
						return false;
					} else {
						oldThreadsCount = allThreads.size();
						return true;
					}
				}

			}

			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			setProgressVisibility(false);
			if (result) {
				// startNextThreadsLoad();
			} else {
				if (pullToRefreshLayout != null) {
					pullToRefreshLayout.setRefreshComplete();

				}
			}
		}

	}

	/**
	 * use to find key hash for development
	 */
	private void getKeyHash() {
		try {

			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);

			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
			}

		} catch (NameNotFoundException e) {
			Log.e("name not found", e.toString());
		} catch (NoSuchAlgorithmException e) {
			Log.e("no such an algorithm", e.toString());
		}
	}

}
