package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

class EnumPreference extends ListPreference {
    public EnumPreference(Context aContext, AttributeSet attrs) {
        super(aContext, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        setSummary(getEntry());
        return super.onCreateView(parent);
    }

    @Override
    protected boolean persistString(String aNewValue) {
        if (super.persistString(aNewValue)) {
            setSummary(getEntry());
            notifyChanged();
            return true;
        } else {
            return false;
        }
    }
}
