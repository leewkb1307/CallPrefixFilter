package com.gmail.leewkb1307.callprefixfilter;

import static android.content.Context.ROLE_SERVICE;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.widget.Toast;

class RequesterHelper {
    interface Callback {
        void onResult(boolean isSuccess);
    }

    private Callback mCallback;

    private AppCompatActivity mActivity;
    private ActivityResultLauncher<Intent> mRequestRoleLauncher;

    public RequesterHelper(AppCompatActivity activity) {
        mActivity = activity;
        mRequestRoleLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                requestRoleCallback);
    }

    public void requestRole() {
        RoleManager roleManager = (RoleManager) mActivity.getSystemService(ROLE_SERVICE);
        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
        mRequestRoleLauncher.launch(intent);
    }

    private ActivityResultCallback<ActivityResult> requestRoleCallback = new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            boolean isSuccess = result.getResultCode() == Activity.RESULT_OK;
            if (!isSuccess) {
                Toast.makeText(mActivity,"Call screener setup not good!", Toast.LENGTH_SHORT).show();
            }

            if (mCallback != null) {
                mCallback.onResult(isSuccess);
            }
        }
    };

    public void registerOnResultCallback(Callback callback)
    {
        mCallback = callback;
    }
}
