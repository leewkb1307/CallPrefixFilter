package com.gmail.leewkb1307.callprefixfilter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedList;

import static com.gmail.leewkb1307.callprefixfilter.CallLogContract.CallEntry.COL_ACTION;
import static com.gmail.leewkb1307.callprefixfilter.CallLogContract.CallEntry.COL_NUMBER;
import static com.gmail.leewkb1307.callprefixfilter.CallLogContract.CallEntry.COL_TIME;
import static com.gmail.leewkb1307.callprefixfilter.CallLogContract.CallEntry.TABLE_NAME;
import static com.gmail.leewkb1307.callprefixfilter.CallLogContract.CallEntry._ID;

class CallLogDbHelper extends SQLiteOpenHelper {
    public CallLogDbHelper(Context context) {
        super(context, CallLogContract.DB_NAME, null, CallLogContract.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " ( " +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TIME + " INTEGER, " +
                COL_NUMBER + " TEXT NOT NULL, " +
                COL_ACTION + " INTEGER);";

        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public ArrayList<CallLog> getCallLog() {
        LinkedList<CallLog> calls = new LinkedList<>();

        CallLog cl;
        int disp_seq = 0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{_ID, COL_TIME, COL_ACTION, COL_NUMBER},
                null, null, null, null, "_ID DESC");
        while (cursor.moveToNext()) {
            int idx1 = cursor.getColumnIndex(COL_TIME);
            int idx2 = cursor.getColumnIndex(COL_ACTION);
            int idx3 = cursor.getColumnIndex(COL_NUMBER);

            long epoch = cursor.getLong(idx1);
            int action = cursor.getInt(idx2);
            String number = cursor.getString(idx3);
            boolean isNum;

            if (number == null || number.isEmpty()) {
                number = "Unknown number";
                isNum = false;
            } else {
                isNum = true;
            }

            cl = new CallLog();
            cl.setPhoneNumber(number);
            cl.setCallTime(epoch);
            cl.setActionType(action);
            cl.setIsNumber(isNum);
            cl.setDispSeq(disp_seq);
            calls.add(cl);

            disp_seq++;
        }

        cursor.close();
        db.close();

        return new ArrayList<>(calls);
    }

    public long addNew(CallLog callLog) {
        long new_row_id;

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_TIME, callLog.getCallTime());
        values.put(COL_ACTION, callLog.getActionType());
        values.put(COL_NUMBER, callLog.getPhoneNumber());
        new_row_id = db.insertWithOnConflict(TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_ABORT);

        db.close();

        return new_row_id;
    }

    public void removeAll() {
        SQLiteDatabase db = getWritableDatabase();

        db.delete(TABLE_NAME, null, null);

        db.close();
    }
}
