package com.gmail.leewkb1307.callprefixfilter;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;

class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
    private SwipeListener mSwipeListener;
    private float mMinDeltaX;
    private float mMaxDeltaY;
    private float mMinVelocityX;
    private MotionEvent mLastOnDownEvent = null;

    public SwipeGestureListener(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float min_x_thres = Math.abs((float)metrics.widthPixels * (2.0f / 5.0f));  // two-fifth screen
        float min_y_thres = Math.abs((float)metrics.heightPixels * (2.0f / 5.0f));  // two-fifth screen
        float max_x_thres = Math.abs(metrics.xdpi * 2);  // within 2 inch
        float max_y_thres = Math.abs(metrics.ydpi * 2);  // within 2 inch

        mMinDeltaX = Math.min(min_x_thres, max_x_thres);
        mMaxDeltaY = Math.min(min_y_thres, max_y_thres);
        mMinVelocityX = mMinDeltaX * (5.0f / 4.0f);  // in 0.8 seconds
    }

    public interface SwipeListener {
        void onSwipeRight();

        void onSwipeLeft();
    }

    public SwipeGestureListener setSwipeListener(SwipeListener swipeListener) {
        mSwipeListener = swipeListener;
        return this;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        mLastOnDownEvent = event;
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        boolean isHandled = false;

        if (event1 == null) {
            event1 = mLastOnDownEvent;
        }

        if (event1 != null && event2 != null) {
            float deltaX = event2.getX() - event1.getX();
            float deltaY = event2.getY() - event1.getY();

            if (Math.abs(deltaX) >= mMinDeltaX && Math.abs(deltaY) <= mMaxDeltaY && Math.abs(velocityX) >= mMinVelocityX) {
                if (deltaX > 0) {
                    if (mSwipeListener != null) {
                        mSwipeListener.onSwipeRight();
                    }
                    isHandled = true;
                }
                else if (deltaX < 0) {
                    if (mSwipeListener != null) {
                        mSwipeListener.onSwipeLeft();
                    }
                    isHandled = true;
                }
            }
        }

        return isHandled;
    }
}
