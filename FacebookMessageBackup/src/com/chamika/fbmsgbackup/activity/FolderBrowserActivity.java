package com.chamika.fbmsgbackup.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chamika.fbmsgbackup.Constants;
import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.utils.AdsLoader;

public class FolderBrowserActivity extends ActionBarActivity {

	public static String INTENT_EXTRA_FOLDER_PATH = "folderPath";
	public static String INTENT_EXTRA_ENABLE_BACK = "enableBack";

	private List<String> item = null;
	private List<String> path = null;
	private TextView textViewPath;
	private TextView textViewError;
	private ListView listViewFiles;

	private Context context;

	private boolean enableBack = false;

	private String root = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = this;

		setContentView(R.layout.activity_folder_browser);
		initUIComponents();

		// set initial path
		String initialPath = null;
		if (getIntent().getExtras() != null) {
			if (getIntent().hasExtra(INTENT_EXTRA_FOLDER_PATH)) {
				initialPath = getIntent().getStringExtra(INTENT_EXTRA_FOLDER_PATH);
			}
			if (getIntent().hasExtra(INTENT_EXTRA_ENABLE_BACK)) {
				enableBack = getIntent().getBooleanExtra(INTENT_EXTRA_ENABLE_BACK, false);
			}
		}

		if (initialPath == null || initialPath.length() == 0) {
			initialPath = Environment.getExternalStorageDirectory().getPath();
		}
		root = initialPath;
		getDir(initialPath);

		// load Ads
		AdsLoader.loadAdMobAds(this);
	}

	private void initUIComponents() {
		textViewPath = (TextView) findViewById(R.id.text_path);
		textViewError = (TextView) findViewById(R.id.text_error);

		listViewFiles = (ListView) findViewById(R.id.list_files);
		listViewFiles.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				onListItemClick(position);
			}
		});
	}

	private void getDir(final String dirPath) {

		AsyncTask<Void, Void, Boolean> fileLoader = new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				textViewPath.setText("Location: " + dirPath);
				showError("Retrieving files");
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				item = new ArrayList<String>();
				path = new ArrayList<String>();
				File f = new File(dirPath);
				File[] files = f.listFiles();

				// for back item
				if (enableBack) {
					if (!dirPath.equals(root)) {
						item.add("< Back");
						path.add(f.getParent());
					}
				}

				if (files != null && files.length > 0) {
					Arrays.sort(files);
					for (int i = 0; i < files.length; i++) {
						File file = files[i];

						if (!file.isHidden() && file.canRead()) {
							path.add(file.getPath());
							if (file.isDirectory()) {
								item.add(file.getName() + "/");
							} else {
								item.add(file.getName());
							}
						}
					}

					return true;

				} else {
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				ArrayAdapter<String> fileList = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1,
						item);
				listViewFiles.setAdapter(fileList);
				if (result) {
					hideError();
				} else {
					showError("No files found");
				}
			}

		};

		fileLoader.execute();
	}

	private void showError(String msg) {
		textViewError.setText(msg);
		textViewError.setVisibility(View.VISIBLE);
	}

	private void hideError() {
		textViewError.setVisibility(View.GONE);
	}

	private void onListItemClick(int position) {
		File file = new File(path.get(position));

		if (file.isDirectory()) {
			if (file.canRead()) {
				getDir(path.get(position));
			} else {
				Toast.makeText(context, file.getName() + " folder can't be read!", Toast.LENGTH_LONG).show();
			}
		} else {

			Uri uri_path = Uri.fromFile(file);
			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
					MimeTypeMap.getFileExtensionFromUrl(file.getPath()));

			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setType(mimeType);
			intent.setDataAndType(uri_path, mimeType);
			try {
				handleDefaultActivityRun(intent);
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
				handleNoActivity();
			}

		}
	}

	private void handleNoActivity() {
		if (context != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("Install CSV Viewer").setCancelable(false)
					.setMessage("You do not have any file viewer to open this file. Do you want to download it now?");

			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
					showViewerInstall();
				}
			});

			builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
				}
			});

			builder.show();
		}
	}

	private void handleDefaultActivityRun(final Intent intent) {
		if (context != null) {

			boolean hasRecommended = false;

			try {
				context.getPackageManager().getPackageInfo(Constants.CSV_EXTERNAL_APP_PACKAGE,
						PackageManager.GET_ACTIVITIES);
				hasRecommended = true;
			} catch (PackageManager.NameNotFoundException localNameNotFoundException) {
				hasRecommended = false;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("CSV Viewer").setCancelable(false);
			builder.setMessage("Some applications may not show native language characters properly.");

			if (hasRecommended) {
				builder.setPositiveButton("Use Recommended", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface paramDialogInterface, int paramInt) {
						intent.setPackage(Constants.CSV_EXTERNAL_APP_PACKAGE);
						startActivity(intent);
					}
				});
			} else {
				builder.setPositiveButton("Install Recommended", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface paramDialogInterface, int paramInt) {
						showViewerInstall();
					}
				});
			}

			builder.setNegativeButton("Skip anyway", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface paramDialogInterface, int paramInt) {
					startActivity(intent);
				}
			});

			builder.show();
		}
	}

	private void showViewerInstall() {
		if (context != null) {
			Toast.makeText(context, "Navigating to Google Play to install CSV Viewer", Toast.LENGTH_LONG).show();
			Intent localIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.CSV_EXTERNAL_APP));
			context.startActivity(localIntent);
		} else {
			Toast.makeText(getApplicationContext(), "Unable to launch Google Play to install CSV Viewer",
					Toast.LENGTH_LONG).show();
		}
	}
}
