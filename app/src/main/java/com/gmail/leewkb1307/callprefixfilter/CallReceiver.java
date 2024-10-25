package com.gmail.leewkb1307.callprefixfilter;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static android.R.drawable.sym_call_incoming;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED;


public class CallReceiver extends BroadcastReceiver {
    private final String mLogTAG = "CPF CallReceiver";

    public CallReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (action.equals(ACTION_PHONE_STATE_CHANGED) && state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            // null ==> unknown number
            if (incomingNumber == null) {
                Log.d(mLogTAG, "Incoming number is NULL!");
            }
            else {
                Log.d(mLogTAG, "Incoming number --> " + incomingNumber);
            }

            boolean isEndCall;

            SharedPreferences sharedPref0 = PreferenceManager.getDefaultSharedPreferences(context);
            boolean enable_filter = sharedPref0.getBoolean("prefEnableFilter", true);
            boolean allow_contacts = sharedPref0.getBoolean("prefAllowContacts", true);
            boolean block_numless = sharedPref0.getBoolean("prefBlockNumLess", false);
            String str_ccode_type = sharedPref0.getString("prefCcodeType", "2");

            boolean isNumLess = (incomingNumber == null || incomingNumber.isEmpty());
            if (isNumLess)
                incomingNumber = "Unknown Number";

            // is incoming number blocked?
            if (!enable_filter)
                isEndCall = false;
            else if (isNumLess)
                isEndCall = block_numless;
            else if (allow_contacts && isInContactList(context, incomingNumber))
                isEndCall = false;
            else {
                PrefShorterThan blockShorter;
                try {
                    blockShorter = new PrefShorterThan(context);
                } catch (Exception e) {
                    blockShorter = new PrefShorterThan();
                }

                PrefLongerThan blockLonger;
                try {
                    blockLonger = new PrefLongerThan(context);
                } catch (Exception e) {
                    blockLonger = new PrefLongerThan();
                }

                isEndCall = blockShorter.isEnabled() && blockShorter.isLenShorter(incomingNumber.length());

                if (!isEndCall) {
                    isEndCall = blockLonger.isEnabled() && blockLonger.isLenLonger(incomingNumber.length());
                }

                if (!isEndCall) {
                    int set_ccode_type = Integer.parseInt(str_ccode_type);
                    boolean use_c_code = (set_ccode_type == 1 || set_ccode_type == 2 && incomingNumber.startsWith("+"));
                    isEndCall = isNumberBlocked(context, incomingNumber, use_c_code);
                }
            }

            if (isEndCall) {
                boolean isTeleErr = false;

                ITelephony telephonyService;
                TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                // hang up
                try {
                    Class<?> c = Class.forName(telephony.getClass().getName());
                    Method m = c.getDeclaredMethod("getITelephony");
                    m.setAccessible(true);
                    telephonyService = (ITelephony)m.invoke(telephony);
                    telephonyService.endCall();
                }
                catch (NoSuchMethodError e) {
                    isTeleErr = true;
                }
                catch (Exception e) {
                    // something goes wrong
                    e.printStackTrace();

                    isTeleErr = true;
                }

                if (isTeleErr) {
                    Toast.makeText(context, "Error blocking " + incomingNumber + "...", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(context, "Block call " + incomingNumber + "...", Toast.LENGTH_SHORT).show();
                }
            }

            if (enable_filter) {
                Resources res = context.getResources();
                SharedPreferences sharedPref = context.getSharedPreferences(
                        res.getString(R.string.noti_file_name), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                String sharedPref_key_id = res.getString(R.string.noti_file_key_id);
                String sharedPref_key_prefix = res.getString(R.string.noti_file_key_pfx);
                String sharedPref_key_number = res.getString(R.string.noti_file_key_num);
                int notiId = sharedPref.getInt(sharedPref_key_id, 0);
                String notiPrefix = sharedPref.getString(sharedPref_key_prefix, "");
                int notiNum = sharedPref.getInt(sharedPref_key_number, 1);

                if (notiPrefix.equals(incomingNumber)) {
                    notiNum++;
                } else {
                    notiPrefix = incomingNumber;
                    notiNum = 1;

                    editor.putString(sharedPref_key_prefix, notiPrefix);
                }
                editor.putInt(sharedPref_key_number, notiNum);
                editor.apply();

                String str_noti_type = sharedPref0.getString("prefNotiType", "2");
                int set_noti_type = Integer.parseInt(str_noti_type);

                boolean showNoti = ((isEndCall && set_noti_type > 0) || (set_noti_type == 2));

                if (showNoti) {
                    // show call log when notification is clicked
                    Intent showCallLog = new Intent(context, CallLogActivity.class);
//                  showCallLog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    showCallLog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("HideBackButton", true);
                    showCallLog.putExtras(bundle);
                    PendingIntent notiIntent = PendingIntent.getActivity(context, 0, showCallLog, PendingIntent.FLAG_UPDATE_CURRENT);

                    String notiMesg = (isEndCall) ? "Block" : "Allow";
                    notiMesg += " call " + incomingNumber;

                    Notification notify = newNotification(context, notiIntent, sym_call_incoming, "Call Prefix Filter", notiMesg, notiNum);
                    if (notify != null) {
                        NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        notifyManager.notify(notiId, notify);
                    }
                }
            }
        }
    }

    // check if a phone number is blocked in filter rules
    private boolean isNumberBlocked(Context context, String phoneNumber, boolean use_c_code) {
        ActionDbHelper mDbHelper = new ActionDbHelper(context);
        ArrayList<PrefixAction> prefixActions = mDbHelper.getPrefixActions(phoneNumber, use_c_code);

        boolean isBlocked = false;

        for (PrefixAction prefixAction : prefixActions) {
            String action = prefixAction.getAction();
            if (action.equals(PrefixAction.ACTION_BLOCK)) {
                isBlocked = true;
            }
            else if (action.equals(PrefixAction.ACTION_ALLOW)) {
                isBlocked = false;
                break;
            }
        }

        return isBlocked;
    }

    // check if a phone number is stored in contact list
    private boolean isInContactList(Context context, String phoneNumber) {
        boolean isContact;

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            // lookup phone number
            ContentResolver resolver = context.getContentResolver();

            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = resolver.query(uri, null, null, null, null);

            if (cursor == null) {
                // the search fails or what...
                isContact = false;
            }
            else {
                isContact = cursor.getCount() > 0;

                cursor.close();
            }

        } else {
            // we cannot read the contact list...
            isContact = false;
        }

        return isContact;
    }

    private Notification newNotification(Context context, PendingIntent pi, int ico, String title, String msg, int num) {
        if (Build.VERSION.SDK_INT >= HONEYCOMB) {
            return  NotificationGE11(context, pi, ico, title, msg, num);
        } else {
            return  NotificationLT11(context, pi, ico, title, msg, num);
        }
    }

    private Notification NotificationGE11(Context context, PendingIntent pi, int ico, String title, String msg, int num) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(ico)
                .setNumber(num)
                .setContentIntent(pi)
                .setContentTitle(title)
                .setContentText(msg);
        return builder.build();
    }

    @SuppressWarnings("deprecation")
    private Notification NotificationLT11(Context context, PendingIntent pi, int ico, String title, String msg, int num) {
        Notification notify = new Notification(ico, title, 0);
        notify.number = num;
        try {
            Method deprecatedMethod = notify.getClass().getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
            deprecatedMethod.invoke(notify, context, title, msg, pi);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();

            notify = null;
        }

        return notify;
    }
}
