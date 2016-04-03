package com.chamika.fbmsgbackup.activity;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chamika.fbmsgbackup.Constants;
import com.chamika.fbmsgbackup.FBMsgBackupApplication;
import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.R.id;
import com.chamika.fbmsgbackup.R.layout;
import com.chamika.fbmsgbackup.R.string;
import com.chamika.fbmsgbackup.model.DownloadJob;
import com.chamika.fbmsgbackup.service.DownloaderService;
import com.chamika.fbmsgbackup.utils.AdsLoader;
import com.chamika.fbmsgbackup.utils.DataStorage;
import com.chamika.fbmsgbackup.utils.FileLoader;
import com.chamika.fbmsgbackup.utils.ToastFactory;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class StatusActivity extends ActionBarActivity {

	public static String INTENT_EXTRA_JOB = "JOB";// DownloadJob object
	public static String INTENT_EXTRA_STATUS = "TYPE"; // int 0=new 1=status

	private DownloadJob downloadJob;

	private StatusJobFragment statusFragment = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_status);

		// this.context = this;

		int status = getIntent().getIntExtra(INTENT_EXTRA_STATUS, -1);

		if (status < 0) {
			showErrorAndClose();
			return;
		} else {

			if (getIntent().hasExtra(INTENT_EXTRA_JOB)) {

				downloadJob = (DownloadJob) getIntent().getSerializableExtra(INTENT_EXTRA_JOB);

				if (downloadJob != null) {
					if (status == 0) {
						// new job request
						NewJobFragment fragment = new NewJobFragment();
						fragment.setContext(this);
						fragment.setJob(downloadJob);
						placeFragment(fragment);
					} else if (status == 1) {
						// status request
						statusFragment = new StatusJobFragment();
						statusFragment.setContext(this);
						statusFragment.setJob(downloadJob);
						placeFragment(statusFragment);
					}
				} else {
					showErrorAndClose();
					return;
				}

			} else {
				showErrorAndClose();
				return;
			}

		}

		// load Ads
		AdsLoader.loadAdMobAds(this);

	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				DownloadJob receriverJob = (DownloadJob) bundle.getSerializable(DownloaderService.INTENT_EXTRA_JOB);
				int progress = bundle.getInt(DownloaderService.INTENT_EXTRA_PROGRESS);

				if (statusFragment != null) {
					statusFragment.updateProgress(progress, receriverJob);
				}
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		if (statusFragment != null) {
			registerReceiver(receiver, new IntentFilter(DownloaderService.RECEIVER));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (statusFragment != null) {
			unregisterReceiver(receiver);
		}
	}

	private void showErrorAndClose() {
		ToastFactory.showToast(getApplicationContext(), "Something went wrong");
		finish();
	}

	private void placeFragment(Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.container, fragment);
		transaction.commit();
	}

	public static class NewJobFragment extends Fragment {

		private Context context;
		private DownloadJob job;

		public NewJobFragment() {
		}

		public void setContext(Context context) {
			this.context = context;
		}

		public void setJob(DownloadJob job) {
			this.job = job;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_new_job, container, false);

			StatusActivity.setStatusFields(this.getActivity(), rootView, job);

			Button submitButton = (Button) rootView.findViewById(R.id.button_submit);
			submitButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (context == null) {
						context = getActivity();
					}

					if (job != null || context != null) {
						Intent intent = new Intent(context.getApplicationContext(), DownloaderService.class);
						intent.putExtra(DownloaderService.INTENT_EXTRA_JOB, job);
						intent.putExtra(DownloaderService.INTENT_EXTRA_ACTION, DownloaderService.ACTION_START);
						context.startService(intent);
						hitStartEvent();
						ToastFactory.showToast(context, "Download started. Check notifications");

						getActivity().setResult(RESULT_OK);
						getActivity().finish();
					} else {
						if (context != null) {
							ToastFactory.showToast(context, "Can't continue");
						}
					}
				}
			});

			hitAnalytics();

			return rootView;
		}

		private void hitAnalytics() {
			Tracker t = ((FBMsgBackupApplication) getActivity().getApplication()).getTracker();
			t.setScreenName(this.getClass().getSimpleName());
			t.send(new HitBuilders.AppViewBuilder().build());
		}

		private void hitStartEvent() {
			Tracker t = ((FBMsgBackupApplication) getActivity().getApplication()).getTracker();
			t.send(new HitBuilders.EventBuilder().setCategory(Constants.ANALYTICS_CATEGORY)
					.setAction(Constants.ANALYTICS_ACTION_START).setLabel(Constants.ANALYTICS_ACTION_START_LABEL)
					.setValue(job.getMaxIndex() - job.getMinIndex()).build());
		}

	}

	public static class StatusJobFragment extends Fragment {
		private Context context;
		private DownloadJob job;

		private ProgressBar progressBar;
		private Button stopButton;
		private Button openFolderButton;
		private TextView statusText;

		private String textDownloading;
		private String textComplete;

		public StatusJobFragment() {
		}

		public void setContext(Context context) {
			this.context = context;
		}

		public void setJob(DownloadJob job) {
			this.job = job;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_status_job, container, false);

			StatusActivity.setStatusFields(this.getActivity(), rootView, job);

			progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar_job);
			stopButton = (Button) rootView.findViewById(R.id.button_stop);
			openFolderButton = (Button) rootView.findViewById(R.id.button_show_folder);

			progressBar.setMax(100);

			statusText = (TextView) rootView.findViewById(R.id.text_status);

			stopButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (context == null) {
						context = getActivity();
					}

					if (job != null || context != null) {
						Intent intent = new Intent(context.getApplicationContext(), DownloaderService.class);
						intent.putExtra(DownloaderService.INTENT_EXTRA_JOB, job);
						intent.putExtra(DownloaderService.INTENT_EXTRA_ACTION, DownloaderService.ACTION_STOP);
						context.startService(intent);
						ToastFactory.showToast(context, "Download Stopped");
						getActivity().finish();
					} else {
						if (context != null) {
							ToastFactory.showToast(context, "Can't continue");
						}
					}
				}
			});

			openFolderButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					openFolder();
				}
			});

			textDownloading = this.getResources().getString(R.string.downloading);
			textComplete = this.getResources().getString(R.string.download_complete);

			TextView textRecipients = (TextView) rootView.findViewById(R.id.text_recipients);
			textRecipients.setText(job.getThread().getFriendsNames(DataStorage.getLoggedUser()));

			setDownloadingView(false);

			hitAnalytics();

			return rootView;
		}

		private void hitAnalytics() {
			Tracker t = ((FBMsgBackupApplication) getActivity().getApplication()).getTracker();
			t.setScreenName(this.getClass().getSimpleName());
			t.send(new HitBuilders.AppViewBuilder().build());
		}

		private void setDownloadingView(boolean isDownloading) {
			if (isDownloading) {
				// avoid updating view always when publishing progress
				if (stopButton.getVisibility() != View.VISIBLE) {
					stopButton.setVisibility(View.VISIBLE);
					openFolderButton.setVisibility(View.GONE);
					statusText.setText(textDownloading);
				}
			} else {
				if (stopButton.getVisibility() != View.GONE) {
					stopButton.setVisibility(View.GONE);
				}
				openFolderButton.setVisibility(View.VISIBLE);
				progressBar.setProgress(100);
				statusText.setText(textComplete);
			}
		}

		public void updateProgress(int progress, DownloadJob receiverJob) {
			if (progressBar != null && receiverJob != null && receiverJob.getJobId() != null && (job.getJobId().equals(receiverJob.getJobId()))) {
				progressBar.setProgress(progress);
				statusText.setText(textDownloading + " (" + progress + "%)");
				if (progress >= 100) {
					setDownloadingView(false);
				} else {
					setDownloadingView(true);
				}
			}
		}

		public void openFolder() {
			Intent intent = new Intent(context, FolderBrowserActivity.class);
			String path = Environment.getExternalStorageDirectory() + Constants.BACKUP_FOLDER;
			if(job != null){
				path += File.separator + job.getFolderName();
			}
			intent.putExtra(FolderBrowserActivity.INTENT_EXTRA_FOLDER_PATH, path);
			startActivity(intent);
		}
	}

	public static void setStatusFields(Context context, View rootView, DownloadJob job) {
		// show download job details
		TextView textRecipients = (TextView) rootView.findViewById(R.id.text_recipients);
		TextView textMsgCount = (TextView) rootView.findViewById(R.id.text_msg_count);
		TextView textFilename = (TextView) rootView.findViewById(R.id.text_filename);

		String names = job.getThread().getFriendsNames(DataStorage.getLoggedUser());
		String threadText = context.getResources().getString(R.string.thread_between);
		String msgCount = context.getResources().getString(R.string.msg_from_to);
		String foldername = context.getResources().getString(R.string.output_file_name);

		textRecipients.setText(threadText.replaceFirst("#", names));
		textMsgCount.setText(msgCount.replaceFirst("#", String.valueOf(job.getMinIndex())).replaceFirst("@",
				String.valueOf(job.getMaxIndex())));
		textFilename.setText(foldername.replaceFirst("#", FileLoader.getFilePrefix() + job.getFolderName()) + File.separator + "##.csv");
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
