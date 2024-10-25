package com.gmail.leewkb1307.callprefixfilter;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;

public class CallLogActivity extends AppCompatActivity {
    private PhoneCallLogAdapter mAdapter;
    private ActionDbHelper mDbHelper;
    private int mSortType;
    private PrefixActionDialog mDialog;
    private AlertDialog mDialogNow;
    private GestureDetectorCompat mDetector;
    private ArrayList<PrefixAction> mPrefixActions;
    private AsyncTaskReceiver mAsyncTaskReceiver;
    private boolean mAsyncBlock = false;
    private final int MY_PERMISSIONS_REQUEST_READ_CALL_LOG = 3;
    private final int CONTEXT_MENU_CONTACT = 0;
    private final int CONTEXT_MENU_EDIT_PREFIX = 1;
    private final int CONTEXT_MENU_ADD_PREFIX = 2;
    private final int CONTEXT_MENU_SEARCH_WEB = 3;

    private final String mLogTAG = "CPF CallLogActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSortType = PhoneCallLogAdapter.SORT_BY_TIME;

        // check that we can read call log
        if (isReadCallLogPermitted()) {
            InitCallLogList();
        } else {
            requestCallLogPermission();
        }
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

    private boolean isReadCallLogPermitted() {
        if (Build.VERSION.SDK_INT < JELLY_BEAN)
            return true;

        Context context = getApplicationContext();
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void requestCallLogPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CALL_LOG},
                MY_PERMISSIONS_REQUEST_READ_CALL_LOG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CALL_LOG: {
                InitCallLogList();
                break;
            }
        }
    }

    private void InitCallLogList() {
        Bundle bundle = getIntent().getExtras();
        boolean hideBack = false;
        if (bundle != null)
            hideBack = bundle.getBoolean("HideBackButton");

        setContentView(R.layout.activity_call_log);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(!hideBack);
        }

        boolean isLogReady = isReadCallLogPermitted();

        if (isLogReady) {
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

            mDbHelper = new ActionDbHelper(this);

            ArrayList<PhoneCallLog> phoneCallLog = getPhoneCallLog();
            mAdapter = new PhoneCallLogAdapter(this, phoneCallLog);

            final ListView lv = (ListView) findViewById(R.id.list_call_log);
            if (lv != null) {
                lv.setAdapter(mAdapter);

                registerForContextMenu(lv);
            }

            mDetector = new GestureDetectorCompat(this, new SwipeGestureListener(this).setSwipeListener(onSwipe));
        }
        else {
            TextView tview = (TextView) findViewById(R.id.text_call_log_error);
            if (tview != null)
                tview.setVisibility(View.VISIBLE);
        }
    }

    private ArrayList<PhoneCallLog> getPhoneCallLog() {
        ArrayList<PhoneCallLog> calllogs = new ArrayList<>();

        PhoneCallLog pcl;

        DateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm");
        String[] CallDetails;
        if (Build.VERSION.SDK_INT >= KITKAT) {
            CallDetails = new String[] {
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.NUMBER_PRESENTATION
            };
        }
        else {
            CallDetails = new String[] {
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE
            };
        }

        String sort_order = CallLog.Calls.DEFAULT_SORT_ORDER;
        int disp_seq = 0;
        try {
            Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, CallDetails, null, null, sort_order);
            if (cursor != null) {
                calllogs.ensureCapacity(cursor.getCount());
                while (cursor.moveToNext()) {
                    boolean isNum = true;
                    int idx_num = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    int idx_date = cursor.getColumnIndex(CallLog.Calls.DATE);
                    int idx_type = cursor.getColumnIndex(CallLog.Calls.TYPE);

                    String str_number = cursor.getString(idx_num);
                    if (str_number == null)
                        continue;

                    if (Build.VERSION.SDK_INT >= KITKAT) {
                        int idx_psnt = cursor.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION);
                        int psnt_type;

                        try {
                            psnt_type = Integer.parseInt(cursor.getString(idx_psnt));

                            switch (psnt_type) {
                                case CallLog.Calls.PRESENTATION_RESTRICTED:
                                    if (str_number.isEmpty() || str_number.equals("-2")) {
                                        str_number = "Private number";
                                        isNum = false;
                                    }
                                    break;
                                case CallLog.Calls.PRESENTATION_UNKNOWN:
                                    if (str_number.isEmpty() || str_number.equals("-3")) {
                                        str_number = "Unknown number";
                                        isNum = false;
                                    }
                                    break;
                                case CallLog.Calls.PRESENTATION_PAYPHONE:
                                    if (str_number.isEmpty() || str_number.equals("-4")) {
                                        str_number = "Pay phone";
                                        isNum = false;
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        if (str_number.isEmpty()) {
                            str_number = "Unknown number";
                            isNum = false;
                        }
                    }

                    if (!str_number.isEmpty()) {
                        Date call_date = new Date(cursor.getLong(idx_date));
                        int call_type = Integer.parseInt(cursor.getString(idx_type));

                        pcl = new PhoneCallLog();
                        pcl.setPhoneNumber(str_number);
                        pcl.setCallDate(dateFormat.format(call_date));
                        pcl.setCallType(call_type);
                        pcl.setIsNumber(isNum);
                        pcl.setDispSeq(disp_seq);
                        calllogs.add(pcl);

                        disp_seq++;
                    }
                }

                cursor.close();
            }
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }

        return calllogs;
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
        if (isReadCallLogPermitted()) {
            getMenuInflater().inflate(R.menu.menu_call_log, menu);

            int itemId;
            if (mSortType == PhoneCallLogAdapter.SORT_BY_NUM)
                itemId = R.id.action_sort_num;
            else
                itemId = R.id.action_sort_time;
            MenuItem sortMenuItem = menu.findItem(itemId);
            sortMenuItem.setChecked(true);
        }

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
            updateSortType(PhoneCallLogAdapter.SORT_BY_TIME);
            return true;
        }

        if (id == R.id.action_sort_num) {
            item.setChecked(true);
            updateSortType(PhoneCallLogAdapter.SORT_BY_NUM);
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
            PhoneCallLog pcl = (PhoneCallLog) lv.getItemAtPosition(acmi.position);
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
                mPrefixActions = mDbHelper.getPrefixActions(phoneNumber, use_c_code);

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
        PhoneCallLog pcl = (PhoneCallLog) mAdapter.getItem(acmi.position);
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
        Log.d(mLogTAG, "AsyncBlock = " + String.valueOf(mAsyncBlock));
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
}
