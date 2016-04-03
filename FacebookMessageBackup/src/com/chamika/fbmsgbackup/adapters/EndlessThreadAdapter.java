package com.chamika.fbmsgbackup.adapters;

import java.util.List;

import android.content.Context;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;

import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.activity.MainActivity;
import com.chamika.fbmsgbackup.model.FBThread;
import com.commonsware.cwac.endless.EndlessAdapter;

class EndlessThreadAdapter<T> extends EndlessAdapter {
	private RotateAnimation rotate = null;
	private View pendingView = null;
	private Context context;
	
	private List<FBThread> nextThreads;

	EndlessThreadAdapter(Context ctxt, int resourceId, List<T> list) {
		super(new ArrayAdapter<T>(ctxt, resourceId, android.R.id.text1, list));
		rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		rotate.setDuration(600);
		rotate.setRepeatMode(Animation.RESTART);
		rotate.setRepeatCount(Animation.INFINITE);
		this.context = ctxt;
	}

	@Override
	protected View getPendingView(ViewGroup parent) {
		View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_view_thread_loading_row, null);

		pendingView = row.findViewById(R.id.throbber);
		pendingView.setVisibility(View.VISIBLE);
		startProgressAnimation();

		return (row);
	}

	@Override
	protected boolean cacheInBackground() {

		nextThreads = ((MainActivity) context).loadNextThreads();

		return (nextThreads != null && nextThreads.size() > 0);
	}

	@Override
	protected void appendCachedData() {
		if(nextThreads != null){
			((MainActivity) context).getAllThreads().addAll(nextThreads);
			nextThreads = null;
		}
	}

	void startProgressAnimation() {
		if (pendingView != null) {
			pendingView.startAnimation(rotate);
		}
	}
}