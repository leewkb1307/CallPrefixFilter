package com.gmail.leewkb1307.callprefixfilter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;

class ChangedReceiver extends BroadcastReceiver {
    public static final String ACTION_FILTER_CHANGED = "com.gmail.leewkb1307.callprefixfilter.FILTER_CHANGED";

    interface onFilterChangeListener {
        void onFilterChanged();
    }

    private onFilterChangeListener mChangeListener = null;

    public ChangedReceiver() {
    }

    public void setChangedListener(@Nullable onFilterChangeListener changeListener) {
        mChangeListener = changeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(ACTION_FILTER_CHANGED)) {
            if (mChangeListener != null) {
                mChangeListener.onFilterChanged();
            }
        }
    }
}
