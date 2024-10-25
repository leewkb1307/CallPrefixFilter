package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

import java.util.Objects;

class EditNumPreference extends EditTextPreference {
    public EditNumPreference(Context aContext, AttributeSet attrs) {
        super(aContext, attrs);

        setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
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

        if (Objects.requireNonNull(numText).isEmpty()) {
            setSummary(R.string.text_n_a);
        } else {
            setSummary(numText);
        }
    }
}
