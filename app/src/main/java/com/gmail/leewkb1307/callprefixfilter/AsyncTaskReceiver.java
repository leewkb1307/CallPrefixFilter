package com.gmail.leewkb1307.callprefixfilter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

class AsyncTaskReceiver extends BroadcastReceiver {
    public static final String ACTION_ASYNC_ENQUIRY  = "com.gmail.leewkb1307.callprefixfilter.ASYNC_ENQUIRY";
    public static final String ACTION_ASYNC_STATE    = "com.gmail.leewkb1307.callprefixfilter.ASYNC_STATE";
    public static final String ACTION_ASYNC_PROGRESS = "com.gmail.leewkb1307.callprefixfilter.ASYNC_PROGRESS";
    public static final String ACTION_ASYNC_DONE     = "com.gmail.leewkb1307.callprefixfilter.ASYNC_DONE";

    public static final String EXTRA_ASYNC_STATE     = "EXTRA_STATE";
    public static final String EXTRA_ASYNC_PROGRESS  = "EXTRA_PERCENT";

    public static final int ASYNC_TASK_NONE   = 0;
    public static final int ASYNC_TASK_LOAD   = 1;
    public static final int ASYNC_TASK_EXPORT = 2;
    public static final int ASYNC_TASK_IMPORT = 3;
    public static final int ASYNC_TASK_CLEAR  = 4;

    public int mAsyncTask = ASYNC_TASK_NONE;

    interface onAsyncTaskListener {
        void onAsyncEnquiry();
        void onAsyncState(int state);
        void onAsyncProgress(int percent);
        void onAsyncDone();
    }

    private onAsyncTaskListener mAsyncTaskListener = null;

    public AsyncTaskReceiver() {
    }

    public void setAsyncTaskListener(@Nullable onAsyncTaskListener asyncStateListener) {
        mAsyncTaskListener = asyncStateListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mAsyncTaskListener != null) {
            String action = intent.getAction();

            if (action.equals(ACTION_ASYNC_ENQUIRY)) {
                mAsyncTaskListener.onAsyncEnquiry();
            }
            else if (action.equals(ACTION_ASYNC_STATE)) {
                int state = intent.getIntExtra(EXTRA_ASYNC_STATE, ASYNC_TASK_NONE);
                mAsyncTaskListener.onAsyncState(state);
            }
            else if (action.equals(ACTION_ASYNC_PROGRESS)) {
                int percent = intent.getIntExtra(EXTRA_ASYNC_PROGRESS, 0);
                mAsyncTaskListener.onAsyncProgress(percent);
            }
            else if (action.equals(ACTION_ASYNC_DONE)) {
                mAsyncTaskListener.onAsyncDone();
            }
        }
    }
}
