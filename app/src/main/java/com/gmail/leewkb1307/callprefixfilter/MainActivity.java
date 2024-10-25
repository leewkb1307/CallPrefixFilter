package com.gmail.leewkb1307.callprefixfilter;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import static android.os.AsyncTask.Status.RUNNING;

public class MainActivity extends AppCompatActivity {
    private ActionDbHelper mDbHelper;
    private PrefixActionAdapter mAdapter;
    private PrefixActionDialog mDialog;
    private AlertDialog mDialogNow;
    private GestureDetectorCompat mDetector;
    private ChangedReceiver mChangedReceiver;
    private AsyncTaskReceiver mAsyncTaskReceiver;
    private RequesterHelper mRequester;
    private boolean mIsInited;
    private boolean mDbRefresh;
    private int mSortType;
    private boolean mSortChange;
    private loading_rules mLoadTask;
    private exporting_CSV mExportTask;
    private reading_CSV mFreadTask;
    private checking_CSV mCheckTask;
    private importing_CSV mImportTask;
    private clearing_Rules mClearTask;
    private boolean mIsExited;
    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;
    private final int MY_PERMISSIONS_REQUEST_WRITE_FILE = 4;
    private final int MY_PERMISSIONS_REQUEST_READ_FILE = 5;

    private final String mLogTAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_CallPrefixFilter);
        super.onCreate(savedInstanceState);

        mIsInited = false;
        mIsExited = false;

        mRequester = new RequesterHelper(this);
        mRequester.registerOnResultCallback(onResultCallback);

        InitMainList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(mLogTAG, "onResume()");

        if (mDbRefresh) {
            mDbRefresh = false;

            updateMainList();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(mLogTAG, "onDestroy()");

        mIsExited = true;

        if (mChangedReceiver != null) {
            unregisterReceiver(mChangedReceiver);
        }

        if (mAsyncTaskReceiver != null) {
            unregisterReceiver(mAsyncTaskReceiver);
        }

        cancelAsyncTask();

        if (mDialogNow != null && mDialogNow.isShowing()) {
            mDialogNow.dismiss();
        }
    }

    private boolean isReadContactsPermitted() {
        Context context = getApplicationContext();

        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isWriteFilePermitted() {
        Context context = getApplicationContext();

        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isReadFilePermitted() {
        Context context = getApplicationContext();

        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadFilePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_READ_FILE);
    }

    private void InitMainList() {
        setContentView(R.layout.activity_main);

        mDbRefresh = false;

        mDialog = new PrefixActionDialog(this);
        mDialog.registerCallback(onPrefixModified);

        mDbHelper = new ActionDbHelper(this);

        mSortChange = false;

        mChangedReceiver = new ChangedReceiver();
        mChangedReceiver.setChangedListener(onFilterChanged);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ChangedReceiver.ACTION_FILTER_CHANGED);
        registerReceiver(mChangedReceiver, intentFilter);

        mAsyncTaskReceiver = new AsyncTaskReceiver();
        mAsyncTaskReceiver.setAsyncTaskListener(onAsyncTaskChanged);

        intentFilter = new IntentFilter();
        intentFilter.addAction(AsyncTaskReceiver.ACTION_ASYNC_ENQUIRY);
        registerReceiver(mAsyncTaskReceiver, intentFilter);

        Context context = getApplicationContext();
        Resources res = context.getResources();
        SharedPreferences sharedPref = context.getSharedPreferences(
                res.getString(R.string.option_file_name), Context.MODE_PRIVATE);
        String sharedPref_key_id = res.getString(R.string.option_file_key_type);
        mSortType = sharedPref.getInt(sharedPref_key_id, PrefixActionAdapter.SORT_BY_ID);

        // load filter rules
        updateMainList();

        mDetector = new GestureDetectorCompat(this, new SwipeGestureListener(this).setSwipeListener(onSwipe));
    }

    private void InitMainList2() {
        if (!mIsInited) {
            mIsInited = true;

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            if (fab == null) {
                Toast.makeText(getApplicationContext(), "Cannot find FAB...", Toast.LENGTH_SHORT).show();
            }
            else {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showAddDialog();
                    }
                });
                fab.setVisibility(View.VISIBLE);
            }

            mRequester.requestRole();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                boolean granted_contacts = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                onContactsPermissionsResult(granted_contacts);
                break;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_FILE: {
                boolean granted_contacts = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                if (granted_contacts) {
                    export_CSV();
                } else {
                    String[] message = {"Not permitted to write file!", null};
                    export_CSV_Dialog(message);
                }
                break;
            }
            case MY_PERMISSIONS_REQUEST_READ_FILE: {
                boolean granted_contacts = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                if (granted_contacts) {
                    import_CSV();
                } else {
                    String[] message = {"Not permitted to read file!", null};
                    import_CSV_Dialog(message);
                }
                break;
            }
        }
    }

    private RequesterHelper.Callback onResultCallback = new RequesterHelper.Callback() {
        @Override
        public void onResult(boolean isSuccess) {
            if (isSuccess) {
                // check that we can read contact list
                if (!isReadContactsPermitted()) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                }
            }
        }
    };

    private interface foo {
        void bar();
    }

    private void onContactsPermissionsResult(boolean granted_contacts) {
        final Context context = getApplicationContext();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPref.edit();
        final boolean allow_contacts = sharedPref.getBoolean("prefAllowContacts", true);
        if  (allow_contacts != granted_contacts) {
            final foo fooFunc = new foo() {
                @Override
                public void bar() {
                    Toast.makeText(context, "Not reading contacts...", Toast.LENGTH_SHORT).show();
                    if (allow_contacts) {
                        editor.putBoolean("prefAllowContacts", false);
                        editor.apply();
                    }
                    showSettingsPage();
                }
            };
            DialogInterface.OnClickListener nayListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    fooFunc.bar();
                }
            };
            DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    fooFunc.bar();
                }
            };
            if (granted_contacts) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Always allow contacts to pass through filter?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(context, "Always allow contacts...", Toast.LENGTH_SHORT).show();

                                editor.putBoolean("prefAllowContacts", true);
                                editor.apply();

                                showSettingsPage();
                            }
                        })
                        .setNegativeButton("Cancel", nayListener)
                        .setOnCancelListener(cancelListener)
                        .create();
                dialog.show();
                mDialogNow = dialog;
            } else {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Not reading contacts...")
                        .setPositiveButton("OK", nayListener)
                        .create();
                dialog.show();
                mDialogNow = dialog;
            }
        }
    }

    private boolean isAsyncTaskNone() {
        return mAsyncTaskReceiver != null && mAsyncTaskReceiver.mAsyncTask == AsyncTaskReceiver.ASYNC_TASK_NONE;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        int itemId;
        if (mSortType == PrefixActionAdapter.SORT_BY_ID)
            itemId = R.id.action_sort_not;
        else if (mSortType == PrefixActionAdapter.SORT_BY_PFX_ASC)
            itemId = R.id.action_sort_pfx;
        else if (mSortType == PrefixActionAdapter.SORT_BY_CC_PFX_ASC)
            itemId = R.id.action_sort_cc_pfx;
        else
            itemId = 0;
        MenuItem sortMenuItem = menu.findItem(itemId);
        sortMenuItem.setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        boolean is_busy = false;

        int id = item.getItemId();

        if (id == R.id.action_add_butt) {
            if (isAsyncTaskNone()) {
                toggleAddButton(item);
                return true;
            }
            else {
                is_busy = true;
            }
        }

        if (id == R.id.action_sort_not) {
            if (isAsyncTaskNone()) {
                item.setChecked(true);
                updateSortType(PrefixActionAdapter.SORT_BY_ID);
                refreshMainList();
                return true;
            }
            else {
                is_busy = true;
            }
        }

        if (id == R.id.action_sort_pfx) {
            if (isAsyncTaskNone()) {
                item.setChecked(true);
                updateSortType(PrefixActionAdapter.SORT_BY_PFX_ASC);
                refreshMainList();
                return true;
            }
            else {
                is_busy = true;
            }
        }

        if (id == R.id.action_sort_cc_pfx) {
            if (isAsyncTaskNone()) {
                item.setChecked(true);
                updateSortType(PrefixActionAdapter.SORT_BY_CC_PFX_ASC);
                refreshMainList();
                return true;
            }
            else {
                is_busy = true;
            }
        }

        if (id == R.id.action_export_csv) {
            if (isAsyncTaskNone()) {
                if (isWriteFilePermitted()) {
                    export_CSV();
                }
                else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_FILE);
                }
                return true;
            }
            else {
                is_busy = true;
            }
        }

        if (id == R.id.action_import_csv) {
            if (isAsyncTaskNone()) {
                if (isReadFilePermitted()) {
                    import_CSV();
                }
                else {
                    requestReadFilePermission();
                }
                return true;
            }
            else {
                is_busy = true;
            }
        }

        if (id == R.id.action_clear_rules) {
            if (isAsyncTaskNone()) {
                clear_Rules();
                return true;
            }
            else {
                is_busy = true;
            }
        }

        if (id == R.id.action_call_log) {
            showCallLogPage();
            return true;
        }

        if (id == R.id.action_settings) {
            showSettingsPage();
            return true;
        }

        if (id == R.id.action_help) {
            goToWebHelpPage();
            return true;
        }

        if (is_busy) {
            Toast.makeText(this, R.string.mesg_async_busy, Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleAddButton(MenuItem item) {
        boolean toActive = !item.isChecked();

        item.setChecked(toActive);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab == null) {
            Toast.makeText(getApplicationContext(), "Cannot find add button!", Toast.LENGTH_SHORT).show();
        }
        else if (toActive) {
            fab.setVisibility(View.VISIBLE);
        }
        else {
            fab.setVisibility(View.GONE);
        }
    }

    private void updateSortType(int sort_type) {
        if (mSortType != sort_type) {
            Context context = getApplicationContext();
            Resources res = context.getResources();
            SharedPreferences sharedPref = context.getSharedPreferences(
                    res.getString(R.string.option_file_name), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            String sharedPref_key_id = res.getString(R.string.option_file_key_type);
            editor.putInt(sharedPref_key_id, sort_type);
            editor.apply();

            mSortType = sort_type;
            mSortChange = true;
        }
    }

    private void showCallLogPage() {
        Intent intent = new Intent(this, CallLogActivity.class);
        startActivity(intent);
    }

    private void showSettingsPage() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void goToWebHelpPage() {
        Uri help_url = Uri.parse("http://sites.google.com/view/callprefixfilter/home/user-manual");
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, help_url);
        startActivity(browserIntent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mDetector != null) {
            mDetector.onTouchEvent(event);
        }

        return super.dispatchTouchEvent(event);
    }

    private SwipeGestureListener.SwipeListener onSwipe = new SwipeGestureListener.SwipeListener() {
        @Override
        public void onSwipeRight() {
            if (!isExited() && isSwipeSidewaysAction()) {
                showCallLogPage();
            }
        }

        @Override
        public void onSwipeLeft() {
            if (!isExited() && isSwipeSidewaysAction()) {
                showCallLogPage();
            }
        }
    };

    private boolean isSwipeSidewaysAction() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("prefSwipeSideways", true);
    }

    private void showAddDialog() {
        if (isAsyncTaskNone()) {
            PrefixAction prefixAction = new PrefixAction();
            mDialogNow = mDialog.showAddDialog(prefixAction);
        }
        else {
            Toast.makeText(this, R.string.mesg_async_busy, Toast.LENGTH_SHORT).show();
        }
    }

    public void editPrefix(View view) {
        if (isAsyncTaskNone()) {
            // find number prefix to edit
            View parent = (View) view.getParent();
            TextView prefixTextView = (TextView) parent.findViewById(R.id.text_prefix);

            PrefixAction prefixAction = (PrefixAction)prefixTextView.getTag();
            mDialogNow = mDialog.showEditDialog(prefixAction);
        }
        else {
            Toast.makeText(this, R.string.mesg_async_busy, Toast.LENGTH_SHORT).show();
        }
    }

    public void deletePrefix(View view) {
        if (isAsyncTaskNone()) {
            // find number prefix to remove
            View parent = (View) view.getParent();
            TextView prefixTextView = (TextView) parent.findViewById(R.id.text_prefix);

            PrefixAction prefixAction = (PrefixAction)prefixTextView.getTag();
            mDialogNow = mDialog.showDeleteDialog(prefixAction);
        }
        else {
            Toast.makeText(this, R.string.mesg_async_busy, Toast.LENGTH_SHORT).show();
        }
    }

    private PrefixActionDialog.onPrefixDialogDoneListener onPrefixModified = new PrefixActionDialog.onPrefixDialogDoneListener() {
        @Override
        public void onPrefixDialogDone(PrefixAction prefixAction, int done_type) {
            mDialogNow = null;
            boolean refreshList = true;
            if (done_type == PrefixActionDialog.DONE_ADD) {
                refreshList = mAdapter.add(prefixAction);
            }
            else if (done_type == PrefixActionDialog.DONE_UPDATE) {
                refreshList = mAdapter.update(prefixAction);
            }
            else if (done_type == PrefixActionDialog.DONE_REMOVE) {
                refreshList = mAdapter.remove(prefixAction);
            }
            if (refreshList)
                refreshMainList();
        }
    };

    private ChangedReceiver.onFilterChangeListener onFilterChanged = new ChangedReceiver.onFilterChangeListener() {
        @Override
        public void onFilterChanged() {
            Log.d(mLogTAG, "OnFilterChanged() called");

            mDbRefresh = true;
        }
    };

    private void setMainListAdapter() {
        ListView lv = (ListView) findViewById(R.id.list_prefix_action);
        if (lv != null) {
            lv.setAdapter(mAdapter);
        }
        else {
            Toast.makeText(this, "Filter display error!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMainList() {
        mAsyncTaskReceiver.mAsyncTask = AsyncTaskReceiver.ASYNC_TASK_LOAD;

        mLoadTask = new loading_rules();
        mLoadTask.execute();
    }

    private void refreshMainList() {
        if (mSortChange || mSortType != PrefixActionAdapter.SORT_BY_ID) {
            mSortChange = false;

            mAdapter.sort(mSortType);
        }
        mAdapter.notifyDataSetChanged();
    }

    private ArrayList<PrefixAction> getPrefixActions() {
        Log.d(mLogTAG, "getPrefixActions() Start");
        ArrayList<PrefixAction> prefixActions = mDbHelper.getPrefixActions();
        Log.d(mLogTAG, "getPrefixActions() End");

        return prefixActions;
    }

    private boolean isExited() {
        return mIsExited || isDestroyed() || isFinishing();
    }

    private void broadcastAsyncState(int state) {
        Intent intent = new Intent();
        intent.setAction(AsyncTaskReceiver.ACTION_ASYNC_STATE);
        intent.putExtra(AsyncTaskReceiver.EXTRA_ASYNC_STATE, state);
        sendBroadcast(intent);
    }

    private void broadcastAsyncProgress(int percent) {
        Intent intent = new Intent();
        intent.setAction(AsyncTaskReceiver.ACTION_ASYNC_PROGRESS);
        intent.putExtra(AsyncTaskReceiver.EXTRA_ASYNC_PROGRESS, percent);
        sendBroadcast(intent);
    }

    private void broadcastAsyncDone() {
        mAsyncTaskReceiver.mAsyncTask = AsyncTaskReceiver.ASYNC_TASK_NONE;

        Intent intent = new Intent();
        intent.setAction(AsyncTaskReceiver.ACTION_ASYNC_DONE);
        sendBroadcast(intent);
    }

    private AsyncTaskReceiver.onAsyncTaskListener onAsyncTaskChanged = new AsyncTaskReceiver.onAsyncTaskListener() {
        @Override
        public void onAsyncEnquiry() {
            broadcastAsyncState(mAsyncTaskReceiver.mAsyncTask);
        }

        @Override
        public void onAsyncState(int state) {
        }

        @Override
        public void onAsyncProgress(int percent) {
        }

        @Override
        public void onAsyncDone() {
        }
    };

    private void cancelAsyncTask() {
        if (mAsyncTaskReceiver != null) {
            switch(mAsyncTaskReceiver.mAsyncTask) {
                case AsyncTaskReceiver.ASYNC_TASK_LOAD:
                    if (mLoadTask != null && mLoadTask.getStatus() == RUNNING) {
                        mLoadTask.cancel(false);
                    }
                    break;
                case AsyncTaskReceiver.ASYNC_TASK_EXPORT:
                    if (mExportTask != null && mExportTask.getStatus() == RUNNING) {
                        mExportTask.cancel(false);
                    }
                    break;
                case AsyncTaskReceiver.ASYNC_TASK_IMPORT:
                    if (mFreadTask != null && mFreadTask.getStatus() == RUNNING) {
                        mFreadTask.cancel(false);
                    }
                    if (mCheckTask != null && mCheckTask.getStatus() == RUNNING) {
                        mCheckTask.cancel(false);
                    }
                    if (mImportTask != null && mImportTask.getStatus() == RUNNING) {
                        mImportTask.cancel(false);
                    }
                    break;
                case AsyncTaskReceiver.ASYNC_TASK_CLEAR:
                    if (mClearTask != null && mClearTask.getStatus() == RUNNING) {
                        mClearTask.cancel(false);
                    }
                    break;
            }
        }
    }

    private void keepWindowScreenOn(boolean enable) {
        int flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        Window mainWindow = getWindow();

        if (enable) {
            mainWindow.addFlags(flags);
        } else {
            mainWindow.clearFlags(flags);
        }
    }

    private class loading_rules extends AsyncTask<Void, Void, ArrayList<PrefixAction>> {
        private MainProgress mProgress;

        public loading_rules() {
            mProgress = new MainProgress(MainActivity.this);
        }

        @Override
        protected void onPreExecute() {
            Log.d(mLogTAG, "loading_rules() Start");

            mProgress.setText(R.string.mesg_prg_loading);
            mProgress.setBar(false);
            mProgress.setVisible(true);
        }

        @Override
        protected ArrayList<PrefixAction> doInBackground(Void... params) {
            return getPrefixActions();
        }

        @Override
        protected void onPostExecute(ArrayList<PrefixAction> prefixActions) {
            Log.d(mLogTAG, "loading_rules() End");

            if (!isExited()) {
                mProgress.setVisible(false);

                mAdapter = new PrefixActionAdapter(MainActivity.this, prefixActions);
                setMainListAdapter();
                refreshMainList();

                mAsyncTaskReceiver.mAsyncTask = AsyncTaskReceiver.ASYNC_TASK_NONE;

                broadcastAsyncDone();

                mLoadTask = null;

                InitMainList2();
            }
        }
    }

    private void export_CSV() {
        File pathInit = getInitPath();
        FileSelector selectDialog = new FileSelector(this, "Export CSV", pathInit);

        Calendar rightNow = Calendar.getInstance();
        Date timeNow = rightNow.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String initName = "CPF-" + dateFormat.format(timeNow) + ".csv";
        selectDialog.setEditName(initName);
        selectDialog.setEditFile(true);
        selectDialog.setFileListener(new FileSelector.FileSelectedListener() {
            @Override
            public void fileSelected(final File directory, final String filename) {
                if (new File(directory, filename).exists()) {
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Export CSV")
                            .setMessage("The file " + filename + " already exists!\n" + "Overwrite?")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    export_CSV(directory, filename);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create();
                    dialog.show();
                    mDialogNow = dialog;
                }
                else {
                    export_CSV(directory, filename);
                }
            }
        });
        mDialogNow = selectDialog.showDialog();
    }

    private void export_CSV(File directory, String filename) {
        mAsyncTaskReceiver.mAsyncTask = AsyncTaskReceiver.ASYNC_TASK_EXPORT;

        mExportTask = new exporting_CSV(directory, filename);
        mExportTask.execute();
    }

    private class exporting_CSV extends AsyncTask<Void, Integer, String[]> {
        private File mDirectory;
        private String mFilename;
        private MainProgress mProgress;

        public exporting_CSV(@NonNull File directory, @NonNull String filename) {
            mDirectory = directory;
            mFilename = filename;
            mProgress = new MainProgress(MainActivity.this);
        }

        @Override
        protected void onPreExecute() {
            keepWindowScreenOn(true);

            mProgress.setText(R.string.mesg_prg_exporting);
            mProgress.setBar(true);
            mProgress.setVisible(true);
        }

        @Override
        protected String[] doInBackground(Void... params) {
            String[] result = {"Success", null};

            try {
                File saveFilePath = new File (mDirectory, mFilename);
                FileWriter outputWriter=new FileWriter(saveFilePath);

                outputWriter.write("action,c_code,prefix,exact\n");

                publishProgress(1);

                int n, max = mAdapter.getCount();
                PercentCounter progressCnt = new PercentCounter(max, 99, 1);
                for (n = 0; n < max; n++) {
                    if (isCancelled()) {
                        break;
                    }
                    PrefixAction prefixAction = (PrefixAction) mAdapter.getItem(n);
                    String csv_line = Convert.prefixAction2TextLine(prefixAction);
                    outputWriter.write(csv_line);

                    if (progressCnt.increment()) {
                        publishProgress(progressCnt.getPercent());
                    }
                }
                outputWriter.close();

                MediaScannerConnection.scanFile(MainActivity.this,
                        new String[]{saveFilePath.toString()},
                        null,
                        null);

            } catch (IOException ioe) {
                ioe.printStackTrace();

                result[0] = "File write error!";

                String message = ioe.getMessage();
                if (message != null && !message.isEmpty()) {
                    result[1] = message;
                }
            } catch (Exception e) {
                e.printStackTrace();

                result[0] = "Error";

                String message = e.getMessage();
                if (message != null && !message.isEmpty()) {
                    result[1] = message;
                }
            }

            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            broadcastAsyncProgress(values[0]);
            if (!isCancelled()) {
                mProgress.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (!isExited()) {
                mProgress.setVisible(false);

                keepWindowScreenOn(false);

                export_CSV_Dialog(result);

                mExportTask = null;
            }
        }
    }

    private void export_CSV_Dialog(String[] message) {
        common_Result_Dialog("Export CSV", message);
    }

    private File getInitPath() {
        return getExternalFilesDir(null);
    }

    private void import_CSV() {
        File pathInit = getInitPath();
        FileSelector selectDialog = new FileSelector(this, "Import CSV", pathInit);
        selectDialog.setFileListener(new FileSelector.FileSelectedListener() {
            @Override
            public void fileSelected(File directory, String filename) {
                read_CSV(directory, filename);
            }
        });
        mDialogNow = selectDialog.showDialog();
    }

    private void read_CSV(File directory, String filename) {
        mAsyncTaskReceiver.mAsyncTask = AsyncTaskReceiver.ASYNC_TASK_IMPORT;

        mFreadTask = new reading_CSV(directory, filename);
        mFreadTask.execute();
    }

    private class reading_CSV extends AsyncTask<Void, Void, String[]> {
        private File mDirectory;
        private String mFilename;
        private ArrayList<PrefixAction> mPrefixActions;
        private MainProgress mProgress;

        public reading_CSV(@NonNull File directory, @NonNull String filename) {
            mDirectory = directory;
            mFilename = filename;
            mPrefixActions = new ArrayList<>();
            mProgress = new MainProgress(MainActivity.this);
        }

        @Override
        protected void onPreExecute() {
            keepWindowScreenOn(true);

            mProgress.setText(R.string.mesg_prg_reading);
            mProgress.setBar(false);
            mProgress.setVisible(true);
        }

        @Override
        protected String[] doInBackground(Void... params) {
            String[] result = {"Success", null};

            try {
                File loadFilePath = new File (mDirectory, mFilename);

                FileReader inputReader=new FileReader(loadFilePath);
                BufferedReader bufferedReader = new BufferedReader(inputReader);

                Convert convertor = new Convert();

                long line_num = 0L;
                String text_line;

                while (!isCancelled() && (text_line = bufferedReader.readLine()) != null) {
                    line_num++;

                    int errorCode;

                    if (line_num > 1) {
                        PrefixAction prefixAction = convertor.textLine2PrefixAction(text_line);
                        if (prefixAction != null) {
                            mPrefixActions.add(prefixAction);
                        }
                        errorCode = convertor.getErrorCode();
                    } else {
                        errorCode = Convert.checkCSVheader(text_line);
                    }

                    if (errorCode != Convert.ERROR_NONE) {
                        result[0] = "Line " + line_num + ": " + Convert.getErrorMessage(errorCode);
                        break;
                    }
                }
                inputReader.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();

                result[0] = "File read error!";

                String message = ioe.getMessage();
                if (message != null && !message.isEmpty()) {
                    result[1] = message;
                }
            } catch (Exception e) {
                e.printStackTrace();

                result[0] = "Error";

                String message = e.getMessage();
                if (message != null && !message.isEmpty()) {
                    result[1] = message;
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (!isExited()) {
                mProgress.setVisible(false);

                keepWindowScreenOn(false);

                if (result[0].equals("Success")) {
                    check_CSV(mPrefixActions);
                }
                else {
                    import_CSV_Dialog(result);
                }

                mFreadTask = null;
            }
        }
    }

    private void check_CSV(ArrayList<PrefixAction> prefixActions) {
        mCheckTask = new checking_CSV(prefixActions);
        mCheckTask.execute();
    }

    private class checking_CSV extends AsyncTask<Void, Integer, Integer[]> {
        private MainProgress mProgress;
        private ArrayList<PrefixAction> mPrefixActions;
        private ArrayList<PrefixAction> mPrefixDelta;

        public checking_CSV(@NonNull ArrayList<PrefixAction> prefixActions) {
            mProgress = new MainProgress(MainActivity.this);
            mPrefixActions = prefixActions;
        }

        @Override
        protected void onPreExecute() {
            keepWindowScreenOn(true);

            mProgress.setText(R.string.mesg_prg_checking);
            mProgress.setBar(true);
            mProgress.setVisible(true);
        }

        @Override
        protected Integer[] doInBackground(Void... params) {
            // make a hash map out of current filter rules
            int sizeCurr = mAdapter.getCount();
            int indexCurr;
            HashMap<Prefix, PrefixAction> hashCurr = new HashMap<>(Math.max(sizeCurr, 16));
            for (indexCurr = 0; indexCurr < sizeCurr; indexCurr++) {
                PrefixAction prefixAction = (PrefixAction) mAdapter.getItem(indexCurr);
                hashCurr.put(prefixAction, prefixAction);
            }

            mPrefixDelta = new ArrayList<>();
            int countNew = 0;
            int countChange = 0;
            int sizeData = mPrefixActions.size();
            HashMap<Prefix, PrefixAction> hashMap = new HashMap<>(Math.max(sizeData, 16));
            PercentCounter progressCnt = new PercentCounter(sizeData, 99);

            for (PrefixAction prefixAction : mPrefixActions) {
                boolean isAdded = false;

                if (hashMap.containsKey(prefixAction)) {
                    isAdded = true;

                    PrefixAction prefixAction1 = hashMap.get(prefixAction);
                    if (!prefixAction.isSameACPE(prefixAction1)) {
                        prefixAction1.setAction(prefixAction.getAction());
                        prefixAction1.setExact(prefixAction.getExact());
                        hashMap.put(prefixAction, prefixAction1);
                    }
                }

                if (!isAdded) {
                    if (hashCurr.containsKey(prefixAction)) {
                        PrefixAction prefixAction0 = hashCurr.get(prefixAction);
                        if (!prefixAction.isSameACPE(prefixAction0)) {
                            prefixAction.setRow_ID(prefixAction0.getRow_ID());
                            mPrefixDelta.add(prefixAction);
                            hashMap.put(prefixAction, prefixAction);
                            countChange++;
                        }
                    }
                    else {
                        prefixAction.setRow_ID(-1L);
                        mPrefixDelta.add(prefixAction);
                        hashMap.put(prefixAction, prefixAction);
                        countNew++;
                    }
                }

                if (progressCnt.increment()) {
                    if (isCancelled()) {
                        break;
                    }
                    publishProgress(progressCnt.getPercent());
                }
            }

            int sizeDelta = mPrefixDelta.size();
            int index;
            for (index = 0; index < sizeDelta; index++) {
                PrefixAction prefixAction = mPrefixDelta.get(index);
                PrefixAction prefixAction1 = hashMap.get(prefixAction);
                mPrefixDelta.set(index, prefixAction1);
            }

            Integer[] result = new Integer[2];
            result[0] = countNew;
            result[1] = countChange;
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            broadcastAsyncProgress(values[0]);
            if (!isCancelled()) {
                mProgress.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Integer[] result) {
            if (!isExited()) {
                mProgress.setVisible(false);

                keepWindowScreenOn(false);

                int countNew = result[0];
                int countChange = result[1];

                if (countNew > 0 || countChange > 0) {
                    String message = "";
                    if (countNew > 0) {
                        message = "New = " + countNew + "\n";
                    }
                    if (countChange > 0) {
                        message = message + "Change = " + countChange + "\n";
                    }
                    message = message + "Proceed?";
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Import CSV")
                            .setMessage(message)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    import_CSV(mPrefixDelta);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    broadcastAsyncDone();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    broadcastAsyncDone();
                                }
                            })
                            .create();
                    dialog.show();
                    mDialogNow = dialog;
                }
                else {
                    String[] message = {"No change", null};
                    import_CSV_Dialog(message);
                }

                mCheckTask = null;
            }
        }
    }

    private void import_CSV(ArrayList<PrefixAction> prefixActions) {
        mImportTask = new importing_CSV(prefixActions);
        mImportTask.execute();
    }

    private class importing_CSV extends AsyncTask<Void, Integer, String[]> {
        private ArrayList<PrefixAction> mPrefixActions;
        private ArrayList<PrefixAction> mPrefixNew;
        private ArrayList<PrefixAction> mPrefixChange;
        private MainProgress mProgress;

        public importing_CSV(@NonNull ArrayList<PrefixAction> prefixActions) {
            mPrefixActions = prefixActions;
            mPrefixNew = new ArrayList<>();
            mPrefixChange = new ArrayList<>();
            mProgress = new MainProgress(MainActivity.this);
        }

        @Override
        protected void onPreExecute() {
            keepWindowScreenOn(true);

            mProgress.setText(R.string.mesg_prg_importing);
            mProgress.setBar(true);
            mProgress.setVisible(true);
        }

        @Override
        protected String[] doInBackground(Void... params) {
            String[] result = {"Success", null};
            boolean isSuccess = true;
            PercentCounter progressCnt = new PercentCounter(mPrefixActions.size());
            long row_id;

            mDbHelper.beginWriteBatch();

            for (PrefixAction prefixAction : mPrefixActions) {
                if (isCancelled()) {
                    isSuccess = false;
                    break;
                }

                if (prefixAction.getRow_ID() < 0L) {
                    row_id = mDbHelper.addNewBatch(prefixAction);

                    if (row_id >= 0L) {
                        prefixAction.setRow_ID(row_id);
                        mPrefixNew.add(prefixAction);
                    }
                }
                else {
                    row_id = mDbHelper.updateExistBatch(prefixAction);

                    if (row_id >= 0L) {
                        mPrefixChange.add(prefixAction);
                    }
                }

                if (row_id < 0L) {
                    result[0] = "Update error";
                    isSuccess = false;
                    mPrefixNew.clear();
                    mPrefixChange.clear();
                    break;
                }

                if (progressCnt.increment()) {
                    publishProgress(progressCnt.getPercent());
                }
            }

            mDbHelper.endWriteBatch(isSuccess);

            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            broadcastAsyncProgress(values[0]);
            if (!isCancelled()) {
                mProgress.setProgress(values[0]);
                if (values[0] >= 100) {
                    mProgress.setText(R.string.mesg_prg_writing);
                }
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (!isExited()) {
                int sizeCurr = mAdapter.getCount();
                int indexCurr;
                HashMap<Long, Integer> hashIdx = new HashMap<>(Math.max(sizeCurr, 16));
                for (indexCurr = 0; indexCurr < sizeCurr; indexCurr++) {
                    PrefixAction prefixAction = (PrefixAction) mAdapter.getItem(indexCurr);
                    hashIdx.put(prefixAction.getRow_ID(), indexCurr);
                }
                mAdapter.ensureCapacity(sizeCurr + mPrefixNew.size());
                for (PrefixAction prefixAction : mPrefixNew) {
                    mAdapter.add(prefixAction);
                }
                for (PrefixAction prefixAction : mPrefixChange) {
                    if (hashIdx.containsKey(prefixAction.getRow_ID())) {
                        mAdapter.set(hashIdx.get(prefixAction.getRow_ID()), prefixAction);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Oops. Filter rule may be corrupted!", Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                refreshMainList();

                mProgress.setVisible(false);

                keepWindowScreenOn(false);

                import_CSV_Dialog(result);

                mImportTask = null;
            }
        }
    }

    private void import_CSV_Dialog(String[] message) {
        common_Result_Dialog("Import CSV", message);
    }

    private void clear_Rules() {
        int numRules = mAdapter.getCount();
        if (numRules > 0) {
            String message = "Removing ALL " + numRules + " filter rules!\n" + "Proceed?";
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Clear rules")
                    .setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAsyncTaskReceiver.mAsyncTask = AsyncTaskReceiver.ASYNC_TASK_CLEAR;
                            mClearTask = new clearing_Rules();
                            mClearTask.execute();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
            mDialogNow = dialog;
        }
        else {
            String[] message = {"No filter rule to remove.", null};
            clear_Rules_Dialog(message);
        }
    }

    private class clearing_Rules extends AsyncTask<Void, Integer, String[]> {
        private int mRemoveCnt;
        private MainProgress mProgress;

        public clearing_Rules() {
            mRemoveCnt = 0;
            mProgress = new MainProgress(MainActivity.this);
        }

        @Override
        protected void onPreExecute() {
            keepWindowScreenOn(true);

            mProgress.setText(R.string.mesg_prg_clearing);
            mProgress.setBar(true);
            mProgress.setVisible(true);
        }

        @Override
        protected String[] doInBackground(Void... params) {
            String[] result = {"Success", null};
            boolean isSuccess = true;

            mDbHelper.beginWriteBatch();

            int n, max = mAdapter.getCount();
            PercentCounter progressCnt = new PercentCounter(max);
            for (n = 0; n < max; n++) {
                if (isCancelled()) {
                    isSuccess = false;
                    break;
                }

                PrefixAction prefixAction = (PrefixAction) mAdapter.getItem(n);

                int row_cnt = mDbHelper.removeExistBatch(prefixAction);

                if (row_cnt == 1) {
                    mRemoveCnt++;
                }
                else {
                    result[0] = "Remove error!";
                    isSuccess = false;
                    mRemoveCnt = 0;
                    break;
                }

                if (progressCnt.increment()) {
                    publishProgress(progressCnt.getPercent());
                }
            }

            mDbHelper.endWriteBatch(isSuccess);

            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            broadcastAsyncProgress(values[0]);
            if (!isCancelled()) {
                mProgress.setProgress(values[0]);
                if (values[0] >= 100) {
                    mProgress.setText(R.string.mesg_prg_writing);
                }
            }
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (!isExited()) {
                while (mRemoveCnt > 0) {
                    mAdapter.remove(mRemoveCnt - 1);
                    mRemoveCnt--;
                }

                mProgress.setVisible(false);

                keepWindowScreenOn(false);

                clear_Rules_Dialog(result);

                mClearTask = null;
            }
        }
    }

    private void clear_Rules_Dialog(String[] message) {
        common_Result_Dialog("Clear rules", message);
    }

    private void common_Result_Dialog(String title, final String[] message) {
        broadcastAsyncDone();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message[0])
                .setPositiveButton("OK", null)
                .create();
        if (dialog != null) {
            dialog.show();

            mDialogNow = dialog;

            if (message[1] != null) {
                Toast.makeText(this, message[1], Toast.LENGTH_SHORT).show();

                TextView mesgView = (TextView) dialog.findViewById(android.R.id.message);
                if (mesgView != null) {
                    mesgView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(MainActivity.this, message[1], Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }
}
