package com.gmail.leewkb1307.callprefixfilter;

import android.Manifest;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import androidx.preference.PreferenceManager;
import android.provider.ContactsContract;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class CallLogActivity extends AppCompatActivity {
    private CallLogAdapter mAdapter;
    private CallLogDbHelper mCallLogDbHelper;
    private ActionDbHelper mActionDbHelper;
    private int mSortType;
    private PrefixActionDialog mDialog;
    private AlertDialog mDialogNow;
    private GestureDetectorCompat mDetector;
    private ArrayList<PrefixAction> mPrefixActions;
    private AsyncTaskReceiver mAsyncTaskReceiver;
    private boolean mAsyncBlock = false;
    private final int CONTEXT_MENU_CONTACT = 0;
    private final int CONTEXT_MENU_EDIT_PREFIX = 1;
    private final int CONTEXT_MENU_ADD_PREFIX = 2;
    private final int CONTEXT_MENU_SEARCH_WEB = 3;

    private final String mLogTAG = "CallLogActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSortType = CallLogAdapter.SORT_BY_TIME;

        InitCallLogList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mAsyncTaskReceiver != null) {
            unregisterReceiver(mAsyncTaskReceiver);
        }

        if (mDialogNow != null && mDialogNow.isShowing()) {
            mDialogNow.dismiss();
        }
    }

    private void InitCallLogList() {
        Bundle bundle = getIntent().getExtras();
        boolean hideBack = false;
        if (bundle != null)
            hideBack = bundle.getBoolean("HideBackButton");

        setContentView(R.layout.activity_call_log);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(!hideBack);
        }

        mDialog = new PrefixActionDialog(this);
        mDialog.registerCallback(onPrefixModified);

        mAsyncTaskReceiver = new AsyncTaskReceiver();
        mAsyncTaskReceiver.setAsyncTaskListener(onAsyncTaskChanged);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AsyncTaskReceiver.ACTION_ASYNC_STATE);
        intentFilter.addAction(AsyncTaskReceiver.ACTION_ASYNC_PROGRESS);
        intentFilter.addAction(AsyncTaskReceiver.ACTION_ASYNC_DONE);
        registerReceiver(mAsyncTaskReceiver, intentFilter);

        broadcastAsyncEnquiry();

        mActionDbHelper = new ActionDbHelper(this);

        mCallLogDbHelper = new CallLogDbHelper(this);

        ArrayList<CallLog> callLog = getPhoneCallLog();
        mAdapter = new CallLogAdapter(this, callLog);

        final ListView lv = (ListView) findViewById(R.id.list_call_log);
        if (lv != null) {
            lv.setAdapter(mAdapter);

            registerForContextMenu(lv);
        }

        mDetector = new GestureDetectorCompat(this, new SwipeGestureListener(this).setSwipeListener(onSwipe));
    }

    private ArrayList<CallLog> getPhoneCallLog() {
        return mCallLogDbHelper.getCallLog();
    }

    private void updateSortType(int sort_type) {
        if (mSortType != sort_type) {
            mSortType = sort_type;

            mAdapter.sort(sort_type);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_call_log, menu);

        int itemId;
        if (mSortType == CallLogAdapter.SORT_BY_NUM)
            itemId = R.id.action_sort_num;
        else
            itemId = R.id.action_sort_time;
        MenuItem sortMenuItem = menu.findItem(itemId);
        sortMenuItem.setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        if (id == R.id.action_sort_time) {
            item.setChecked(true);
            updateSortType(CallLogAdapter.SORT_BY_TIME);
            return true;
        }

        if (id == R.id.action_sort_num) {
            item.setChecked(true);
            updateSortType(CallLogAdapter.SORT_BY_NUM);
            return true;
        }

        if (id == R.id.action_clear_log) {
                clear_Log();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String getContactName(Context context, String phoneNumber) {
        String contactName;

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            // lookup phone number
            ContentResolver resolver = context.getContentResolver();

            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            String[] projection = {
                    ContactsContract.Contacts.DISPLAY_NAME
            };
            Cursor cursor = resolver.query(uri, projection, null, null, null);

            if (cursor == null) {
                // the search fails or what...
                contactName = null;
            }
            else {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int idx1 = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);

                    contactName = cursor.getString(idx1);
                }
                else
                    contactName = null;

                cursor.close();
            }

        } else {
            // we cannot read the contact list...
            contactName = null;
        }

        return contactName;
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, view, menuInfo);

        if (view.getId() == R.id.list_call_log) {
            ListView lv = (ListView) view;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            CallLog pcl = (CallLog) lv.getItemAtPosition(acmi.position);
            String phoneNumber = pcl.getPhoneNumber();
            boolean isNumber = pcl.getIsNumber();

            if (phoneNumber.isEmpty()) {
                menu.setHeaderTitle("Phone number empty");
                menu.add(CONTEXT_MENU_CONTACT, 0, Menu.NONE, "Unknown number");
                menu.setGroupEnabled(CONTEXT_MENU_CONTACT, false);
            }
            else if (!isNumber) {
                menu.setHeaderTitle(phoneNumber);
                menu.add(CONTEXT_MENU_CONTACT, 0, Menu.NONE, "Cannot get phone number.");
                menu.setGroupEnabled(CONTEXT_MENU_CONTACT, false);
            }
            else {
                String contactName = getContactName(this, phoneNumber);
                String contactMesg;

                if (contactName == null)
                    contactMesg = "Cannot find phone contact.";
                else
                    contactMesg = "Contact name: " + contactName;

                boolean use_c_code = isUseCcode(phoneNumber);
                mPrefixActions = mActionDbHelper.getPrefixActions(phoneNumber, use_c_code);

                menu.setHeaderTitle("Phone number " + phoneNumber);
                menu.add(CONTEXT_MENU_CONTACT, 0, Menu.NONE, contactMesg);

                if (!mAsyncBlock) {
                    int index, size = mPrefixActions.size();
                    for (index = 0; index < size; index++) {
                        PrefixAction prefixAction = mPrefixActions.get(index);
                        String titlePrefix = prefixAction.getC_CodePrefix();
                        if (!prefixAction.getExact())
                            titlePrefix = titlePrefix + "*";
                        menu.add(CONTEXT_MENU_EDIT_PREFIX, index, Menu.NONE, "Edit " + titlePrefix);
                    }
                    menu.add(CONTEXT_MENU_ADD_PREFIX, 0, Menu.NONE, "Add filter");
                }
                menu.add(CONTEXT_MENU_SEARCH_WEB, 0, Menu.NONE, "Search WEB");
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        CallLog pcl = (CallLog) mAdapter.getItem(acmi.position);
        String phoneNumber = pcl.getPhoneNumber();

        int itemId = item.getItemId();
        int groupId = item.getGroupId();

        if (groupId == CONTEXT_MENU_CONTACT) {
            // go to contacts
            Intent showContacts = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
            startActivity(showContacts);
        }
        else if (groupId == CONTEXT_MENU_EDIT_PREFIX) {
            // edit the selected filter rule
            PrefixAction prefixAction = mPrefixActions.get(itemId);
            mDialogNow = mDialog.showEditDialog(prefixAction);
        }
        else if (groupId == CONTEXT_MENU_ADD_PREFIX) {
            // add a filter rule for the number
            PrefixAction prefixAction = new PrefixAction();
            prefixAction.setPrefix(pcl.getPhoneNumber());
            prefixAction.setExact(true);
            mDialogNow = mDialog.showAddDialog(prefixAction);
        }
        else if (groupId == CONTEXT_MENU_SEARCH_WEB) {
            // search web for the number
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, phoneNumber);
            startActivity(intent);
        }
        else {
            return false;
        }

        return true;
    }

    private PrefixActionDialog.onPrefixDialogDoneListener onPrefixModified = new PrefixActionDialog.onPrefixDialogDoneListener() {
        @Override
        public void onPrefixDialogDone(PrefixAction prefixAction, int done_type) {
            mDialogNow = null;
            broadcastFilterChanged();
        }
    };

    private void broadcastFilterChanged() {
        Intent intent = new Intent();
        intent.setAction(ChangedReceiver.ACTION_FILTER_CHANGED);
        sendBroadcast(intent);
    }

    private void broadcastAsyncEnquiry() {
        Intent intent = new Intent();
        intent.setAction(AsyncTaskReceiver.ACTION_ASYNC_ENQUIRY);
        sendBroadcast(intent);
    }

    private AsyncTaskReceiver.onAsyncTaskListener onAsyncTaskChanged = new AsyncTaskReceiver.onAsyncTaskListener() {
        @Override
        public void onAsyncEnquiry() {
        }

        @Override
        public void onAsyncState(int state) {
            mAsyncBlock = state != AsyncTaskReceiver.ASYNC_TASK_NONE;
            debugAsyncBlock();
        }

        @Override
        public void onAsyncProgress(int percent) {
            mAsyncBlock = true;
            debugAsyncBlock();
        }

        @Override
        public void onAsyncDone() {
            mAsyncBlock = false;
            debugAsyncBlock();
        }
    };

    private void debugAsyncBlock() {
        Log.d(mLogTAG, "AsyncBlock = " + mAsyncBlock);
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
            if (isSwipeSidewaysAction()) {
                finish();
            }
        }

        @Override
        public void onSwipeLeft() {
            if (isSwipeSidewaysAction()) {
                finish();
            }
        }
    };

    private boolean isSwipeSidewaysAction() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("prefSwipeSideways", true);
    }

    private boolean isUseCcode(String phoneNumber) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String str_ccode_type = sharedPref.getString("prefCcodeType", "2");
        int set_ccode_type = Integer.parseInt(str_ccode_type);
        boolean use_c_code = (set_ccode_type == 1 || set_ccode_type == 2 && phoneNumber.startsWith("+"));

        return use_c_code;
    }

    private void clear_Log() {
        String message = "Clearing log!\n" + "Proceed?";
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Clear log")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mCallLogDbHelper.removeAll();
                        mAdapter.clear();
                        mAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        mDialogNow = dialog;
    }
}
