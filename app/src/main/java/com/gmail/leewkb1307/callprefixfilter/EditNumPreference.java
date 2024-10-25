package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

class EditNumPreference extends EditTextPreference {
    public EditNumPreference(Context aContext, AttributeSet attrs) {
        super(aContext, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        setSummaryText();
        return super.onCreateView(parent);
    }

    @Override
    protected boolean persistString(String aNewValue) {
        if (super.persistString(aNewValue)) {
            setSummaryText();
            notifyChanged();
            return true;
        } else {
            return false;
        }
    }

    private void setSummaryText() {
        String numText = getText();

        if (numText.isEmpty()) {
            setSummary(R.string.text_n_a);
        } else {
            setSummary(numText);
        }
    }
}
