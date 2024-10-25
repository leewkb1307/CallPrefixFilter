package com.gmail.leewkb1307.callprefixfilter;

class PercentCounter {
    private int mMax;
    private int mPercent;
    private int mPercent_001;
    private int mPercent_max;
    private int mPercent_dMax_R;
    private int mPercent_dMax_r;

    public PercentCounter(int max) {
        this(max, 100);
    }

    public PercentCounter(int max, int maxPercent) {
        this(max, maxPercent, 0);
    }

    public PercentCounter(int max, int maxPercent, int initPercent) {
        mMax = max;
        mPercent = initPercent;
        mPercent_001 = 0;
        mPercent_max = maxPercent;

        if (max > 0) {
            mPercent_dMax_R = maxPercent / max;
            mPercent_dMax_r = maxPercent % max;
        }
        else {
            mPercent_dMax_R = maxPercent - initPercent;
            mPercent_dMax_r = 0;
        }
    }

    public boolean increment() {
        boolean isOverflow = false;

        if (mPercent < mPercent_max) {
            if (mPercent_dMax_r < mMax - mPercent_001) {
                mPercent_001 += mPercent_dMax_r;
            }
            else {
                mPercent_001 = mPercent_001 + mPercent_dMax_r - mMax;
                mPercent++;
                isOverflow = true;
            }

            mPercent += mPercent_dMax_R;
            if (mPercent_dMax_R > 0) {
                isOverflow = true;
            }
        }
        else {
            isOverflow = true;
        }

        return isOverflow;
    }

    public int getPercent() {
        return mPercent;
    }
}
