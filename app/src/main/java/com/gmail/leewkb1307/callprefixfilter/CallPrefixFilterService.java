package com.gmail.leewkb1307.callprefixfilter;

import static android.telecom.Call.Details.DIRECTION_INCOMING;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import static android.R.drawable.sym_call_incoming;

public class CallPrefixFilterService extends CallScreeningService {
    private static final String mLogTAG = "CallScreener";
    private static final String NotiChlId = "CallScreener";

    @Override
    public void onScreenCall(@NonNull Call.Details details) {
        Context context = getApplicationContext();
        if (details.getCallDirection() == DIRECTION_INCOMING) {
            String incomingNumber;
            Uri handle = details.getHandle();
            if (handle != null) {
                incomingNumber = handle.toString().replace("tel:", "").replace("%2B", "+");
            } else {
                incomingNumber = null;
            }

            // null ==> unknown number
            if (incomingNumber == null) {
                Log.d(mLogTAG, "Incoming number is NULL!");
            }
            else {
                Log.d(mLogTAG, "Incoming number --> " + incomingNumber);
            }

            boolean isNumLess = (incomingNumber == null || incomingNumber.isEmpty());

            boolean isEndCall;

            SharedPreferences sharedPref0 = PreferenceManager.getDefaultSharedPreferences(context);
            boolean enable_filter = sharedPref0.getBoolean("prefEnableFilter", true);
            boolean allow_contacts = sharedPref0.getBoolean("prefAllowContacts", true);
            boolean block_numless = sharedPref0.getBoolean("prefBlockNumLess", false);
            String str_ccode_type = sharedPref0.getString("prefCcodeType", "2");

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

            CallResponse response = !isEndCall ? allowCallResponse() : rejectCallResponse();
            respondToCall(details, response);

            CallLog callLog = new CallLog();
            callLog.setCallTime(details.getCreationTimeMillis());
            int callAction = (isEndCall) ? CallLog.ACTION_BLOCK : CallLog.ACTION_ALLOW;
            callLog.setActionType(callAction);
            String callNumber = (incomingNumber == null) ? "" : incomingNumber;
            callLog.setPhoneNumber(callNumber);
            if (saveCallLog(context, callLog) < 0) {
                Toast.makeText(context, "Cannot save call log...", Toast.LENGTH_SHORT).show();
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

                String notiNumber = (isNumLess) ? "Unknown Number" : incomingNumber;

                if (notiPrefix.equals(notiNumber)) {
                    notiNum++;
                } else {
                    notiPrefix = notiNumber;
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
                    showCallLog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("HideBackButton", true);
                    showCallLog.putExtras(bundle);
                    PendingIntent notiIntent = PendingIntent.getActivity(context, 0, showCallLog, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    String notiMesg = (isEndCall) ? "Block" : "Allow";
                    notiMesg += " call " + notiNumber;

                    createNotificationChannel();

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotiChlId)
                            .setSmallIcon(sym_call_incoming)
                            .setContentTitle(getString(R.string.app_service))
                            .setNumber(notiNum)
                            .setContentText(notiMesg)
                            .setContentIntent(notiIntent)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    notificationManager.notify(notiId, builder.build());
                }
            }
        }
    }

    private CallResponse allowCallResponse() {
        return new CallScreeningService.CallResponse.Builder().build();

    }

    private CallResponse disallowCallResponse() {
        return new CallScreeningService.CallResponse.Builder().setDisallowCall(true).build();

    }

    private CallResponse rejectCallResponse() {
        return new CallScreeningService.CallResponse.Builder().setDisallowCall(true).setRejectCall(true).build();
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

    private boolean isInContactList(Context context, String phoneNumber) {
        boolean isContact;

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

        return isContact;
    }

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.app_service);
        String description = "Call screener log";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(NotiChlId, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private long saveCallLog(Context context, CallLog callLog) {
        CallLogDbHelper mDbHelper = new CallLogDbHelper(context);
        return mDbHelper.addNew(callLog);
    }
}
