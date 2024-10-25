package com.gmail.leewkb1307.callprefixfilter;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

class MainProgress {
    private ProgressBar mProgressBar;
    private TextView mProgressText;

    MainProgress(AppCompatActivity activity) {
        mProgressText = (TextView) activity.findViewById(R.id.text_progress_title);
        mProgressBar = (ProgressBar) activity.findViewById(R.id.progressBar_main);
    }

    public void setText(int resid) {
        if (mProgressText != null) {
            mProgressText.setText(resid);
        }
    }

    public void setBar(boolean isPercent) {
        if (mProgressBar != null) {
            if (isPercent) {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(0);
            } else {
                mProgressBar.setIndeterminate(true);
            }
        }
    }

    public void setVisible(boolean isVisible) {
        int visibility = (isVisible) ? View.VISIBLE : View.GONE;
        if (mProgressText != null) {
            mProgressText.setVisibility(visibility);
        }
        if (mProgressBar != null) {
            mProgressBar.setVisibility(visibility);
        }
    }

    public void setProgress(int percent) {
        if (mProgressBar != null) {
            mProgressBar.setProgress(percent);
        }
    }
}
