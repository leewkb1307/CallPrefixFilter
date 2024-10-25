package com.gmail.leewkb1307.callprefixfilter;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Activity mActivity;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        mActivity = getActivity();

        Preference pref = findPreference("prefAllowContacts");
        Objects.requireNonNull(pref).setOnPreferenceChangeListener(onAllowContactsChange);

        Preference pref2 = findPreference("prefBlockShorter");
        Objects.requireNonNull(pref2).setOnPreferenceChangeListener(onBlockShorterChange);

        Preference pref3 = findPreference("prefBlockLonger");
        Objects.requireNonNull(pref3).setOnPreferenceChangeListener(onBlockLongerChange);
    }

    private Preference.OnPreferenceChangeListener onAllowContactsChange = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(@NonNull Preference p,
                                          Object newValue) {
            if (newValue == Boolean.TRUE && !isReadContactsPermitted()) {
                Toast.makeText(mActivity, "Not permitted to read contacts...", Toast.LENGTH_SHORT).show();

                return false;
            }
            return true;
        }
    };

    private boolean isReadContactsPermitted() {
        return ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private Preference.OnPreferenceChangeListener onBlockShorterChange = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(@NonNull Preference p,
                                          Object newValue) {
            boolean isValid = true;
            String errMesg = "";

            PrefShorterThan blockShorter = new PrefShorterThan();

            try {
                blockShorter = new PrefShorterThan((String) newValue);
            } catch (Exception e) {
                isValid = false;
                errMesg = "Invalid number entered...";
            }

            if (isValid && blockShorter.isEnabled()) {
                PrefLongerThan blockLonger = new PrefLongerThan(mActivity);

                if (blockLonger.isEnabled() && blockShorter.isLenShorter(blockLonger.getBlockLen())) {
                    isValid = false;
                    errMesg = "Number entered is too big... (" + newValue + " > " + blockLonger.getBlockLen() + ")";
                }
            }

            if (!isValid) {
                Toast.makeText(mActivity, errMesg, Toast.LENGTH_SHORT).show();
            }

            return isValid;
        }
    };

    private Preference.OnPreferenceChangeListener onBlockLongerChange = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(@NonNull Preference p,
                                          Object newValue) {
            boolean isValid = true;
            String errMesg = "";

            PrefLongerThan blockLonger = new PrefLongerThan();

            try {
                blockLonger = new PrefLongerThan((String) newValue);
            } catch (Exception e) {
                isValid = false;
                errMesg = "Invalid number entered...";
            }

            if (isValid && blockLonger.isEnabled()) {
                PrefShorterThan blockShorter = new PrefShorterThan(mActivity);

                if (blockShorter.isEnabled() && blockLonger.isLenLonger(blockShorter.getBlockLen())) {
                    isValid = false;
                    errMesg = "Number entered is too small... (" + newValue + " < " + blockShorter.getBlockLen() + ")";
                }
            }

            if (!isValid) {
                Toast.makeText(mActivity, errMesg, Toast.LENGTH_SHORT).show();
            }

            return isValid;
        }
    };
}
