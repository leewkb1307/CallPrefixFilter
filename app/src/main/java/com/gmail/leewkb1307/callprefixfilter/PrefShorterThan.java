package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import android.preference.PreferenceManager;

class PrefShorterThan extends PrefBlockLen {
    public PrefShorterThan(Context context) {
        super(PreferenceManager.getDefaultSharedPreferences(context).getString("prefBlockShorter", ""));
    }

    public PrefShorterThan(String strNum) {
        super(strNum);
    }

    public PrefShorterThan() {
        super();
    }

    public boolean isLenShorter(int numLen) {
        return isEnabled() && getBlockLen() > numLen;
    }
}
