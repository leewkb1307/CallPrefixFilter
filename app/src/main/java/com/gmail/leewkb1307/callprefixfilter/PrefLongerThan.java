package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import androidx.preference.PreferenceManager;

class PrefLongerThan extends PrefBlockLen {
    public PrefLongerThan(Context context) {
        super(PreferenceManager.getDefaultSharedPreferences(context).getString("prefBlockLonger", ""));
    }

    public PrefLongerThan(String strNum) {
        super(strNum);
    }

    public PrefLongerThan() {
        super();
    }

    public boolean isLenLonger(int numLen) {
        return isEnabled() && numLen > getBlockLen();
    }
}
