package com.chamika.fbmsgbackup.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.chamika.fbmsgbackup.R;

public class RotaryKnobView extends View {

	private final static int VALUE_FACTOR = 2;
	private static final int FACTOR_LIMIT = (360 / VALUE_FACTOR) - 30;

	// private final static String TAG = "RotaryKnobView";

	private Scroller scroller;
	private Bitmap[] knobImages;
	private Bitmap imageBitmap;

	// currently showing image index
	private int showingIndex = -1;

	private int rotation = 0;

	private GestureDetector detector;

	private int centerX = 0;
	private int centerY = 0;
	private int lastY = 0;

	private OnValueChangeListener listener;
	private int oldValue;

	private boolean scrolling = false;
	private boolean touching = false;

	private boolean scaled = false;

	public RotaryKnobView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public RotaryKnobView(Context context) {
		super(context);
		init();
	}

	public RotaryKnobView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		scroller = new Scroller(getContext());

		imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.knob_min);

		detector = new GestureDetector(RotaryKnobView.this.getContext(), new GestureListener());
		rotation = 0;
		oldValue = 0;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		centerX = getWidth() / 2;
		centerY = getHeight() / 2;

		if (!scaled) {

			int width = getWidth();
			int height = getHeight();

			double scale = 1.0f;

			if (width > height) {
				scale = (height * 1.0) / imageBitmap.getHeight();
			} else {
				scale = (width * 1.0) / imageBitmap.getWidth();
			}

			imageBitmap = Bitmap.createScaledBitmap(imageBitmap, (int) (imageBitmap.getWidth() * scale),
					(int) (imageBitmap.getHeight() * scale), false);
			scaled = true;
		}

		rotation = prepareRotationValue(rotation);

		int newValue = rotation / VALUE_FACTOR;
		if (oldValue != newValue) {
			int diff = newValue - oldValue;
			oldValue = newValue;
			notifyListener(diff);
		}

		canvas.rotate(-rotation, getWidth() / 2, getHeight() / 2);

		canvas.drawColor(Color.TRANSPARENT);
		canvas.drawBitmap(imageBitmap, centerX - (imageBitmap.getWidth() / 2), centerY - (imageBitmap.getHeight() / 2),
				null);

		if (scroller.computeScrollOffset()) {
			int newY = (-scroller.getCurrY()) - lastY;
			lastY = -scroller.getCurrY();
			int deltaAngle = newY % 360;
			updateRotation(deltaAngle);
			scrolling = true;
		} else {
			scrolling = false;
			// notifyEnd();
		}
	}

	private void notifyListener(int diff) {
		if (listener != null && (diff < FACTOR_LIMIT && diff > -FACTOR_LIMIT)) {
			listener.onValueChange(diff);
		}
	}

	private void notifyEnd() {
		if (listener != null && !touching && !scrolling) {
			listener.onValueChangeEnd();
		}
	}

	private int prepareRotationValue(int rotation) {
		return rotation % 360;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		detector.onTouchEvent(event);
		int action = event.getActionMasked();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			touching = true;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			touching = false;
			notifyEnd();
			break;
		default:
			break;
		}

		return true;
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			// Set the pie rotation directly.
			float scrollTheta = vectorToScalarScroll(distanceX / 50, distanceY / 50, e2.getX() - centerX, e2.getY()
					- centerY);
			updateRotation(scrollTheta);
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// // Set up the Scroller for a fling
			float scrollTheta = vectorToScalarScroll(velocityX, velocityY, e2.getX() - centerX, e2.getY() - centerY);
			int rotationDelta = (int) (180 * scrollTheta / Math.PI);
			rotation = rotation + rotationDelta;
			scroller.fling(0, 0, 0, (int) scrollTheta / 4, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
			//
			// // Start the animator and tell it to animate for the expected
			// // duration of the fling.
			// if (Build.VERSION.SDK_INT >= 11) {
			// mScrollAnimator.setDuration(mScroller.getDuration());
			// mScrollAnimator.start();
			// }
			invalidate();
			return true;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			// The user is interacting with the pie, so we want to turn on
			// acceleration
			// so that the interaction is smooth.
			// mPieView.accelerate();
			// if (isAnimationRunning()) {
			// stopScrolling();
			// }
			return true;
		}
	}

	private void updateRotation(float scrollTheta) {
		int rotationDelta = (int) (180 * scrollTheta / Math.PI);
		rotation = rotation + rotationDelta;
		invalidate();
	}

	private void updateRotation(int deltaAngle) {
		rotation = rotation + deltaAngle;
		rotation = prepareRotationValue(rotation);
		invalidate();
	}

	/**
	 * Helper method for translating (x,y) scroll vectors into scalar rotation
	 * of the pie.
	 * 
	 * @param dx
	 *            The x component of the current scroll vector.
	 * @param dy
	 *            The y component of the current scroll vector.
	 * @param x
	 *            The x position of the current touch, relative to the pie
	 *            center.
	 * @param y
	 *            The y position of the current touch, relative to the pie
	 *            center.
	 * @return The scalar representing the change in angular position for this
	 *         scroll.
	 */
	private static float vectorToScalarScroll(float dx, float dy, float x, float y) {
		// get the length of the vector
		float l = (float) Math.sqrt(dx * dx + dy * dy);

		// decide if the scalar should be negative or positive by finding
		// the dot product of the vector perpendicular to (x,y).
		float crossX = -y;
		float crossY = x;

		float dot = (crossX * dx + crossY * dy);
		float sign = Math.signum(dot);

		return l * sign;
	}

	public void setKnobImages(int[] imageResources) {
		knobImages = new Bitmap[imageResources.length];

		for (int i = 0; i < imageResources.length; i++) {
			knobImages[i] = BitmapFactory.decodeResource(getResources(), imageResources[i]);
		}
	}

	public void changeKnobImage(int index) {
		if (index < knobImages.length) {
			if (index != showingIndex) {
				showingIndex = index;
				imageBitmap = knobImages[index];
				scaled = false;// for re-init image
				invalidate();
			}
		}
	}

	public void setOnValueChangeListener(OnValueChangeListener listener) {
		this.listener = listener;
	}

	public interface OnValueChangeListener {
		public void onValueChange(int value);

		public void onValueChangeEnd();
	}
}
