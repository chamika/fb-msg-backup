package com.chamika.fbmsgbackup.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.chamika.fbmsgbackup.FBMsgBackupApplication;
import com.chamika.fbmsgbackup.R;
import com.chamika.fbmsgbackup.adapters.MessagesListAdapter;
import com.chamika.fbmsgbackup.model.FBMessage;
import com.chamika.fbmsgbackup.model.FBThread;
import com.chamika.fbmsgbackup.utils.AppLogger;
import com.chamika.fbmsgbackup.utils.DataLoader;
import com.chamika.fbmsgbackup.views.RangeSeekBar;
import com.chamika.fbmsgbackup.views.RangeSeekBar.OnRangeSeekBarChangeListener;
import com.chamika.fbmsgbackup.views.RotaryKnobView;
import com.chamika.fbmsgbackup.views.RotaryKnobView.OnValueChangeListener;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class MessageOptionFragment extends Fragment {

	private static final String TAG = "MessageOptionFragment";

	private static final int MESSAGE_LOAD_OFFSET = 5;

	private FBThread thread;

	private List<FBMessage> lowMessages;
	private List<FBMessage> highMessages;

	private long oldLow = 1L;
	private long lowIndex = 1L;
	private long highIndex = 2L;
	private long oldHigh = 2L;

	private View fragmentView;
	private TextView textLow;
	private TextView textHight;
	private RotaryKnobView rotaryKnob;
	private RangeSeekBar<Long> seekBar;
	private RadioButton radioMin;
	private RadioButton radioMax;
	private ListView listViewMin;
	private ListView listViewMax;

	private Timer highTimer;
	private Timer lowTimer;

	// recent user's interaction to change value using knob
	private QueryState recentSelection = QueryState.QUERY_LOW;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		if (fragmentView == null) {
			fragmentView = inflater.inflate(R.layout.fragment_message_options, null);

			// setup range text low hight
			textLow = (TextView) fragmentView.findViewById(R.id.text_range_low);
			textHight = (TextView) fragmentView.findViewById(R.id.text_range_high);
			rotaryKnob = (RotaryKnobView) fragmentView.findViewById(R.id.rotaryknob);

			lowIndex = oldLow;
			highIndex = oldHigh;

			textLow.setText(String.valueOf(lowIndex));
			textHight.setText(String.valueOf(highIndex));

			seekBar = new RangeSeekBar<Long>(oldLow, oldHigh, this.getActivity());
			seekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Long>() {
				@Override
				public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Long minValue, Long maxValue) {
					lowIndex = minValue;
					highIndex = maxValue;
					AppLogger.log(TAG, "User selected new range values: MIN=" + minValue + ", MAX=" + maxValue);

					updateLowText();
					updateHightText();

					startUpdateLowIndex(false);
					startUpdateHighIndex(false);

				}

			});
			ViewGroup rangebarParent = (ViewGroup) fragmentView.findViewById(R.id.rangebar);
			if (rangebarParent != null) {
				rangebarParent.addView(seekBar, 0);
			}

			// setup rotary knob
			rotaryKnob.setKnobImages(new int[] { R.drawable.knob_min, R.drawable.knob_max });
			rotaryKnob.setOnValueChangeListener(new OnValueChangeListener() {

				@Override
				public void onValueChange(int value) {

					AppLogger.log(TAG, "value change=" + value);

					if (recentSelection == QueryState.QUERY_LOW) {
						long newVal = lowIndex - value;
						if (newVal >= seekBar.getAbsoluteMinValue() && newVal < seekBar.getSelectedMaxValue()) {
							lowIndex = newVal;
							// startUpdateLowIndex();
							seekBar.setSelectedMinValue(lowIndex);
							updateLowText();
						}
					} else if (recentSelection == QueryState.QUERY_HIGH) {
						long newVal = highIndex - value;
						if (newVal <= seekBar.getAbsoluteMaxValue() && newVal > seekBar.getSelectedMinValue()) {
							highIndex = newVal;
							// startUpdateHighIndex();
							seekBar.setSelectedMaxValue(highIndex);
							updateHightText();
						}
					}

				}

				@Override
				public void onValueChangeEnd() {
					startUpdateLowIndex(false);
					startUpdateHighIndex(false);
				}
			});

			// setup radio group
			RadioGroup radioGroup = (RadioGroup) fragmentView.findViewById(R.id.radiogroup_selection);
			radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					if (checkedId == R.id.radio_min) {
						recentSelection = QueryState.QUERY_LOW;
						rotaryKnob.changeKnobImage(0);
					} else if (checkedId == R.id.radio_max) {
						recentSelection = QueryState.QUERY_HIGH;
						rotaryKnob.changeKnobImage(1);
					} else {
						recentSelection = QueryState.QUERY_LOW;
						rotaryKnob.changeKnobImage(1);
					}
				}
			});

			radioMin = (RadioButton) radioGroup.findViewById(R.id.radio_min);
			radioMax = (RadioButton) radioGroup.findViewById(R.id.radio_max);

			listViewMin = (ListView) fragmentView.findViewById(R.id.listViewMin);
			listViewMax = (ListView) fragmentView.findViewById(R.id.listViewMax);

			if (recentSelection == QueryState.QUERY_LOW) {
				radioMin.setChecked(true);
				rotaryKnob.changeKnobImage(0);
			}

			// high index called first because min radio should be set initially
			startUpdateHighIndex(true);
			startUpdateLowIndex(true);
		}

		hitAnalytics();

		return fragmentView;
	}

	public void setRange(long count) {
		oldHigh = count;
	}

	public long getHighIndex() {
		return highIndex;
	}

	public long getLowIndex() {
		return lowIndex;
	}

	public FBThread getThread() {
		return thread;
	}

	public void setThread(FBThread thread) {
		this.thread = thread;
	}

	private void updateHightText() {
		textHight.setText(String.valueOf(highIndex));
	}

	private void updateLowText() {
		textLow.setText(String.valueOf(lowIndex));
	}

	private void updateLowList() {
		if (listViewMin != null && lowMessages != null) {
			List<FBMessage> list = new ArrayList<FBMessage>();
			for (int i = 0; i < Math.min(MESSAGE_LOAD_OFFSET, lowMessages.size()); i++) {
				list.add(lowMessages.get(i));
			}

			if (this.getActivity() != null) {
				listViewMin.setAdapter(new MessagesListAdapter(this.getActivity(), list));
			}
		}
	}

	private void updateHighList() {
		if (listViewMax != null && highMessages != null) {
			List<FBMessage> list = new ArrayList<FBMessage>();
			int size = Math.min(MESSAGE_LOAD_OFFSET, highMessages.size());
			for (int i = 0; i < size; i++) {
				list.add(highMessages.get(highMessages.size() - size + i));
			}

			if (this.getActivity() != null) {
				listViewMax.setAdapter(new MessagesListAdapter(this.getActivity(), list));
			}
		}

	}

	private void startUpdateHighIndex(boolean force) {

		if (oldHigh != highIndex || force) {

			recentSelection = QueryState.QUERY_HIGH;
			radioMax.setChecked(true);
			rotaryKnob.changeKnobImage(1);
			listViewMax.setAdapter(new MessagesListAdapter(this.getActivity(), new ArrayList<FBMessage>()));

			if (highTimer != null) {
				highTimer.cancel();
			}

			highTimer = new Timer();
			highTimer.schedule(new TimerTask() {
				public void run() {
					AppLogger.log(TAG, "started high index update");
					oldHigh = highIndex;

					MessageLoader loader = new MessageLoader();
					loader.setState(QueryState.QUERY_HIGH);
					loader.execute();

				}
			}, 1000);

		}
	}

	private void startUpdateLowIndex(boolean force) {

		if (oldLow != lowIndex || force) {

			recentSelection = QueryState.QUERY_LOW;
			radioMin.setChecked(true);
			rotaryKnob.changeKnobImage(0);
			listViewMin.setAdapter(new MessagesListAdapter(this.getActivity(), new ArrayList<FBMessage>()));

			if (lowTimer != null) {
				lowTimer.cancel();
			}

			lowTimer = new Timer();
			lowTimer.schedule(new TimerTask() {
				public void run() {
					AppLogger.log(TAG, "started low index update");
					oldLow = lowIndex;

					MessageLoader loader = new MessageLoader();
					loader.setState(QueryState.QUERY_LOW);
					loader.execute();
				}
			}, 1000);
		}
	}

	public enum QueryState {
		QUERY_LOW, QUERY_HIGH, QUERY_NULL;
	}

	private class MessageLoader extends AsyncTask<Void, Void, QueryState> {

		QueryState state;

		public void setState(final QueryState state) {
			this.state = state;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected QueryState doInBackground(Void... params) {
//			DataLoader.loadFriends();
			if (state == QueryState.QUERY_LOW) {
				try {
					if (thread != null) {
						lowMessages = DataLoader.loadMessages(thread.getThreadId(), lowIndex,
								Math.min(lowIndex + MESSAGE_LOAD_OFFSET, highIndex));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return QueryState.QUERY_LOW;

			} else if (state == QueryState.QUERY_HIGH) {
				try {
					if (thread != null) {
						highMessages = DataLoader.loadMessages(thread.getThreadId(),
								Math.max(lowIndex, highIndex - MESSAGE_LOAD_OFFSET), highIndex);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return QueryState.QUERY_HIGH;
			}

			return QueryState.QUERY_NULL;
		}

		@Override
		protected void onPostExecute(QueryState result) {
			if (result == QueryState.QUERY_HIGH) {
				if (highMessages != null) {
//					AppLogger.log(TAG, "high=" + highMessages.size());
					updateHighList();
				}
			} else if (result == QueryState.QUERY_LOW) {
				if (lowMessages != null) {
					updateLowList();
//					AppLogger.log(TAG, "low=" + lowMessages.size());
				}
			}

		}

	}

	private void hitAnalytics() {
		Tracker t = ((FBMsgBackupApplication) getActivity().getApplication()).getTracker();
		t.setScreenName(this.getClass().getSimpleName());
		t.send(new HitBuilders.AppViewBuilder().build());
	}

}
