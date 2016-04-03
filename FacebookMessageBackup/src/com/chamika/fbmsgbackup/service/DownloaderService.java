package com.chamika.fbmsgbackup.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.chamika.fbmsgbackup.Constants;
import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.activity.StatusActivity;
import com.chamika.fbmsgbackup.database.FBMessageTable;
import com.chamika.fbmsgbackup.model.DownloadJob;
import com.chamika.fbmsgbackup.model.FBFriend;
import com.chamika.fbmsgbackup.model.FBMessage;
import com.chamika.fbmsgbackup.utils.AppLogger;
import com.chamika.fbmsgbackup.utils.DataLoader;
import com.chamika.fbmsgbackup.utils.DataStorage;
import com.chamika.fbmsgbackup.utils.FileLoader;
import com.facebook.Session;

public class DownloaderService extends Service {

	private static final int MAX_LINE_COUNT = 5000;

	// private String ENCODING = "UTF8";
	private int BUFFER_SIZE = 8192;

	public static final String INTENT_EXTRA_ACTION = "action";
	public static final String ACTION_START = "start";
	public static final String ACTION_STOP = "stop";

	private static final String TAG = "DownloaderService";

	private List<DownloadJob> jobs;
	private List<Thread> threads;

	public static final String INTENT_EXTRA_JOB = "JOB";
	public static final String INTENT_EXTRA_PROGRESS = "PROGRESS";

	private static final String FILE_NAME_JOBS = "JOBS";

	private static final long FB_MAX_MESSAGE_LIMIT = 30;

	private static int NOTIFICATION_ID = 100;

	public static final String RECEIVER = "com.chamika.fbmsgbackup.service.receiver";

	private static Map<String, String> cacheNames;
	private boolean friendsLoaded = false;
	private boolean downloading = false;

	@Override
	public void onCreate() {
		super.onCreate();

		jobs = loadFromFile();

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (cacheNames == null) {
			cacheNames = new HashMap<String, String>();
		}

		if (jobs == null) {
			jobs = loadFromFile();

			if (jobs == null) {
				jobs = new ArrayList<DownloadJob>();
			}
		}

		if (intent == null) {
			// start after force close
			boolean successSession = validateFBLogin();
			AppLogger.log(TAG, "fb session=" + successSession);
			startAllDownloads();
		} else {
			Serializable serializable = intent.getSerializableExtra(INTENT_EXTRA_JOB);
			String action = intent.getStringExtra(INTENT_EXTRA_ACTION);
			if (serializable != null && action != null && serializable instanceof DownloadJob) {
				DownloadJob downloadJob = (DownloadJob) serializable;

				if (action.equals(ACTION_START)) {
					boolean isAdded = addJob(downloadJob);
					startAllDownloads();
				} else if (action.equals(ACTION_STOP)) {
					stopDownloadJob(downloadJob);
				}
			}
		}

		return Service.START_STICKY;
	}

	/**
	 * start downloading given job
	 * 
	 * @param job
	 */
	private void startDownloadJob(final DownloadJob job) {

		final int notificationId = ++NOTIFICATION_ID;

		final NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		// mBuilder.setContentTitle("Message Download")
		String notificationTitle = job.getFolderName();
		FBFriend loggedUser = DataStorage.getLoggedUser();
		if (loggedUser != null) {
			notificationTitle = job.getThread().getFriendsNames(loggedUser);
		}
		mBuilder.setContentTitle(notificationTitle).setContentText("Download in progress")
				.setSmallIcon(R.drawable.ic_launcher).setAutoCancel(true);

		Intent resultIntent = new Intent(this, StatusActivity.class);
		resultIntent.putExtra(StatusActivity.INTENT_EXTRA_STATUS, 1);
		resultIntent.putExtra(StatusActivity.INTENT_EXTRA_JOB, job);
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		// resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
		// Intent.FLAG_ACTIVITY_SINGLE_TOP);

		// generate unique request id
		int n = new Random().nextInt(50) + 1;
		int requestID = n * notificationId;

		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, requestID, resultIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		// PendingIntent.FLAG_ONE_SHOT);
		mBuilder.setContentIntent(resultPendingIntent);

		Thread thread = new Thread() {

			@Override
			public void run() {
				super.run();
				downloading = true;
				String threadId = job.getThread().getThreadId();

				FBMessageTable dbTable = new FBMessageTable(getApplicationContext(), threadId);
				dbTable.createTableIfNotExist();

				// indexes starts from 0
				// start from begining or from the last index
				// long start = Math.max(job.getMinIndex() - 1,
				// job.getDownloadIndex());
				long start = job.getMinIndex() - 1;
				long end = job.getMaxIndex() - 1;

				long current = start;

				AppLogger.log(TAG, "Job started thread_id=" + threadId + " from " + start + " to " + end);

				Date startDate = new Date();

				boolean working = true;
				job.setWorking(true);
				saveJobList();
				while (working) {

					long lowerLimit = current;
					// long upperLimit = Math.min(end, current +
					// FB_MAX_MESSAGE_LIMIT);
					long upperLimit = Math.min(end, current + FB_MAX_MESSAGE_LIMIT);

					// check if already found in the database
					Cursor cursor = dbTable.getExportMessages(threadId, lowerLimit, upperLimit);

					boolean contains = containsAll(lowerLimit, upperLimit, cursor);
					dbTable.closeReadableDatabase();
					if (contains) {
						StringBuilder sb = new StringBuilder();
						sb.append("found in database from ");
						sb.append(lowerLimit);
						sb.append(" to ");
						sb.append(upperLimit);
						AppLogger.log(TAG, sb.toString());
					} else {
						// List<FBMessage> messages =
						// DataLoader.loadMessages(threadId, lowerLimit,
						// upperLimit);
						List<FBMessage> messages = DataLoader.loadMessages(threadId, lowerLimit, FB_MAX_MESSAGE_LIMIT);

						if (messages == null) {
							// error occured

							// 104 = OAuthException.
							if (DataLoader.getErrorCode() == 104) {
								// try to create session from cache
								validateFBLogin();
								// try missed one again
								upperLimit = lowerLimit;
							}
						} else {
							// TODO testing use. should remove after testing
							StringBuilder sb = new StringBuilder();
							sb.append("Messages from ");
							sb.append(lowerLimit);
							sb.append(" to ");
							sb.append(upperLimit);
							sb.append(" size=");
							sb.append(messages.size());

							AppLogger.log(TAG, sb.toString());

							// save to database
							dbTable.addAll(messages, lowerLimit);
						}
					}

					job.setDownloadIndex(current);
					// saveJobList();

					// prepare for next iteration
					current = upperLimit;

					if (current >= end) {
						working = false;
					}

					if (job.isStop()) {
						working = false;

					}

					int progress = 0;
					if (end == start) {
						progress = 0;
					} else {
						progress = (int) ((current - start) * 100 / (end - start));
					}
					mBuilder.setProgress(100, progress, false);
					mNotifyManager.notify(notificationId, mBuilder.build());
					publishStatus(job, progress);
				}

				AppLogger.log(TAG, "Job ended thread_id=" + threadId + " from " + start + " to " + end + " in "
						+ (new Date().getTime() - startDate.getTime()) / 1000 + "seconds");

				// write csv file
				mBuilder.setContentText("Download complete. Generating file").setProgress(0, 0, false);
				Cursor cursor = dbTable.getExportMessages(threadId, start, end);

				if (cursor != null) {
					generateOutFile(job, cursor);
				}

				dbTable.closeReadableDatabase();

				// //////////////
				if (job.isStop()) {
					mBuilder.setContentText("Download stopped").setProgress(0, 0, false);
				} else {
					mBuilder.setContentText("Finished").setProgress(0, 0, false);
				}
				mNotifyManager.notify(notificationId, mBuilder.build());

				// //////////////
				job.setStop(true);
				boolean removed = removeJob(job);
				if (removed) {
					saveJobList();
				}
				downloading = false;
				if (jobs != null) {
					if (jobs.size() == 0) {
						stopSelf();
					} else {
						startAllDownloads();
					}
				}

			}

		};

		if (threads == null) {
			threads = new ArrayList<Thread>();
		}
		threads.add(thread);
		thread.start();

	}

	private boolean containsAll(long lower, long upper, Cursor cursor) {
		boolean contains = true;

		if (cursor != null) {

			int count = 0;
			do {
				++count;

			} while (cursor.moveToNext());

			if (count == (1 + upper - lower)) {
				contains = true;
			} else {
				contains = false;
			}
		} else {
			return false;
		}

		return contains;
	}

	private void generateOutFile(DownloadJob job, Cursor cursor) {

		File folder = new File(Environment.getExternalStorageDirectory() + Constants.BACKUP_FOLDER);

		if (!folder.exists()) {
			folder.mkdir();
		}

		folder = new File(Environment.getExternalStorageDirectory() + Constants.BACKUP_FOLDER + File.separator
				+ job.getFolderName() + File.separator);

		if (!folder.exists()) {
			folder.mkdir();
		}

		int fileCount = 0;
		String fileNameWithPath = generateFileName(folder, fileCount);
		try {

			BufferedWriter writer = getWriter(fileNameWithPath);

			// FileWriter writer = new FileWriter(fileNameWithPath);

			try {
				// write header
				writeCsvLine(writer, "Time", "Sender", "Message");

				int lineCount = 0;
				do {
					String time = cursor.getString(0);
					String authorId = cursor.getString(1);
					String body = cursor.getString(2);

					String authorName = getFriendNameForId(authorId);

					writeCsvLine(writer, time, authorName, body);
					++lineCount;
					if (lineCount > MAX_LINE_COUNT) {
						lineCount = 0;
						++fileCount;
						fileNameWithPath = generateFileName(folder, fileCount);
						writer.flush();
						writer.close();
						// writer = new FileWriter(fileNameWithPath);
						writer = getWriter(fileNameWithPath);
					}

				} while (cursor.moveToNext());
			} catch (Exception e) {
				e.printStackTrace();
				AppLogger.log(TAG, e.getMessage());
			} finally {
				writer.flush();
				writer.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private BufferedWriter getWriter(String fileNameWithPath) throws UnsupportedEncodingException,
			FileNotFoundException {
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileNameWithPath),
				Charset.forName("UTF8")), BUFFER_SIZE);
	}

	private String generateFileName(File folder, int fileCount) {
		return folder.toString() + File.separator + fileCount + ".csv";
	}

	private void writeCsvLine(FileWriter writer, String s1, String s2, String s3) throws IOException {
		s3 = s3.replaceAll("\n", "|");
		s3 = s3.replaceAll("\r", "|");
		s3 = s3.replaceAll(",", "+");
		String line = String.format("%s,%s,%s\n", s1, s2, s3);
		// String line = String.format("\"%s\",\"%s\",\"%s\"\n", s1, s2, s3);
		writer.write(line);
	}

	private void writeCsvLine(BufferedWriter writer, String s1, String s2, String s3) throws IOException {
		s3 = s3.replaceAll("\n", "|");
		s3 = s3.replaceAll("\r", "|");
		s3 = s3.replaceAll(",", "+");
		String line = String.format("%s,%s,%s\n", s1, s2, s3);
		// String line = String.format("\"%s\",\"%s\",\"%s\"\n", s1, s2, s3);
		writer.write(line);
	}

	/**
	 * get friends for ID with cached enabled for performance
	 * 
	 * @param friendId
	 * @return
	 */
	private String getFriendNameForId(String friendId) {
		String cachedName = cacheNames.get(friendId);
		if (cachedName != null) {
			return cachedName;
		}

		Map<Long, FBFriend> allFriends = DataStorage.getAllFriends();
		if (allFriends == null || allFriends.size() == 0) {
			validateFBLogin();
			DataLoader.loadFriends();
			allFriends = DataStorage.getAllFriends();
			AppLogger.log(TAG, "Friendlist loaded");

			friendsLoaded = true;
		}

		FBFriend fbFriend = allFriends.get(Long.valueOf(friendId));
		if (fbFriend != null) {
			String name = fbFriend.getName();
			if (name != null) {
				cacheNames.put(friendId, name);
				return name;
			}
		}

		return friendId;

	}

	private void publishStatus(DownloadJob job, int progress) {
		Intent intent = new Intent(RECEIVER);
		intent.putExtra(INTENT_EXTRA_JOB, job);
		intent.putExtra(INTENT_EXTRA_PROGRESS, progress);
		sendBroadcast(intent);
	}

	// /////////
	// //////////////////////

	@Override
	public void onDestroy() {
		// stop threads
		if (threads != null) {
			for (Thread thread : threads) {
				try {
					thread.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		// save jobs List
		if (jobs != null) {
			for (DownloadJob job : jobs) {
				if (job.isWorking()) {
					job.setWorking(false);
				}
			}
		}
		saveJobList();

		super.onDestroy();
	}

	/**
	 * stops a download job
	 * 
	 * @param downloadJob
	 *            job to be stopped
	 */
	public void stopDownloadJob(final DownloadJob downloadJob) {
		if (jobs != null) {
			String id = downloadJob.getJobId();
			for (DownloadJob job : jobs) {
				if (id.equals(job.getJobId())) {
					job.setStop(true);
					break;
				}
			}
		}

	}

	/**
	 * Add a download job to as 1st element in jobs list if the provided
	 * download job not exist
	 * 
	 * @param downloadJob
	 * @return true if
	 */
	private boolean addJob(DownloadJob downloadJob) {
		boolean contain = false;
		if (jobs != null) {
			String id = downloadJob.getJobId();
			for (DownloadJob job : jobs) {
				if (id.equals(job.getJobId())) {
					contain = true;
				}
			}

			if (!contain) {
				jobs.add(0, downloadJob);
			}
		} else {
			jobs = new ArrayList<DownloadJob>();
			jobs.add(downloadJob);
			contain = false;
		}

		return !contain;
	}

	private synchronized boolean removeJob(DownloadJob downloadJob) {
		boolean contain = false;
		if (jobs != null) {
			String id = downloadJob.getJobId();
			for (DownloadJob job : jobs) {
				if (id.equals(job.getJobId())) {
					contain = true;
				}
			}

			if (contain) {
				jobs.remove(downloadJob);
			}
		}

		return contain;
	}

	/**
	 * starts all download jobs in parallel
	 */
	private void startAllDownloads() {
		if (!downloading && jobs != null && jobs.size() > 0) {
			for (DownloadJob nextJob : jobs) {
				AppLogger.log(TAG, "job " + nextJob.getJobId() + " working=" + nextJob.isWorking());
				if (!nextJob.isWorking()) {
					startDownloadJob(nextJob);
					break;
				}
			}
			// for (DownloadJob job : jobs) {
			// AppLogger.log(TAG, "job " + job.generateJobId() + " working=" +
			// job.isWorking());
			// if (!job.isWorking()) {
			// startDownloadJob(job);
			// }
			// }
		}
	}

	// TODO too many file write request. replace with db
	private void saveJobList() {
		FileLoader.saveObject(getApplicationContext(), FILE_NAME_JOBS, (Serializable) jobs);
	}

	private List<DownloadJob> loadFromFile() {
		List<DownloadJob> list = (List<DownloadJob>) FileLoader.loadObject(this, FILE_NAME_JOBS);

		// set to working false to start all after loading from file
		if (list != null) {
			for (DownloadJob job : list) {
				job.setWorking(false);
			}
		}

		return list;
	}

	// ////////////////////

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
			Session sessionNew = Session.openActiveSessionFromCache(this);

			if (sessionNew != null && sessionNew.isOpened()) {
				return true;
			}
		}

		return false;
	}

}
