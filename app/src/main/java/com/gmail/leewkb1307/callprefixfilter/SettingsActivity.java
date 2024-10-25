package com.gmail.leewkb1307.callprefixfilter;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {
    RequesterHelper mRequester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_settings, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mRequester = new RequesterHelper(this);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(onPrefChangeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.unregisterOnSharedPreferenceChangeListener(onPrefChangeListener);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private SharedPreferences.OnSharedPreferenceChangeListener onPrefChangeListener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals("prefEnableFilter")) {
                    boolean isEnabled = prefs.getBoolean(key, false);

                    if (isEnabled) {
                        mRequester.requestRole();
                    }
                }
            }
        };
}
