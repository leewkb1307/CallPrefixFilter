package com.gmail.leewkb1307.callprefixfilter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;

import static com.gmail.leewkb1307.callprefixfilter.ActionContract.ActionEntry.COL_ACTION;
import static com.gmail.leewkb1307.callprefixfilter.ActionContract.ActionEntry.COL_C_CODE;
import static com.gmail.leewkb1307.callprefixfilter.ActionContract.ActionEntry.COL_PREFIX;
import static com.gmail.leewkb1307.callprefixfilter.ActionContract.ActionEntry.TABLE_NAME;
import static com.gmail.leewkb1307.callprefixfilter.ActionContract.ActionEntry._ID;

class ActionDbHelper extends SQLiteOpenHelper {
    private static final String ACTION_BLOCK       = "0";
    private static final String ACTION_ALLOW       = "1";
    private static final String ACTION_BLOCK_EXACT = "2";
    private static final String ACTION_ALLOW_EXACT = "3";

    private SQLiteDatabase mDB;

    public ActionDbHelper(Context context) {
        super(context, ActionContract.DB_NAME, null, ActionContract.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " ( " +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_C_CODE + " TEXT NOT NULL, " +
                COL_PREFIX + " TEXT NOT NULL, " +
                COL_ACTION + " TEXT NOT NULL);";

        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1)
        {
            String upgradeTable1 = "ALTER TABLE " + TABLE_NAME +
                    " ADD COLUMN " + COL_C_CODE + " TEXT NOT NULL DEFAULT \'\'";

            db.execSQL(upgradeTable1);
        }
        else
        {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    public ArrayList<PrefixAction> getPrefixActions() {
        return getPrefixActions(null, false);
    }

    public ArrayList<PrefixAction> getPrefixActions(@Nullable String phoneNumber, boolean use_c_code) {
        String c_code_prefix;

        LinkedList<PrefixAction> actions = new LinkedList<>();

        PrefixAction pa;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{_ID, COL_PREFIX, COL_ACTION, COL_C_CODE},
                null, null, null, null, null);
        while (cursor.moveToNext()) {
            int idx1 = cursor.getColumnIndex(COL_PREFIX);
            int idx2 = cursor.getColumnIndex(COL_ACTION);
            int idx3 = cursor.getColumnIndex(COL_C_CODE);

            String prefix = cursor.getString(idx1);
            String action = cursor.getString(idx2);
            String c_code = cursor.getString(idx3);
            boolean is_exact = false;

            if (action.equals(ACTION_BLOCK_EXACT)) {
                action = ACTION_BLOCK;
                is_exact = true;
            }
            else if (action.equals(ACTION_ALLOW_EXACT)) {
                action = ACTION_ALLOW;
                is_exact = true;
            }

            if (phoneNumber != null) {
                if (use_c_code && !c_code.isEmpty())
                    c_code_prefix = "+" + c_code + prefix;
                else
                    c_code_prefix = prefix;

                if (!c_code_prefix.isEmpty()) {
                    if (!is_exact && phoneNumber.startsWith(c_code_prefix)) {
                        c_code_prefix = null;
                    }
                    else if (is_exact && phoneNumber.equals(c_code_prefix)) {
                        c_code_prefix = null;
                    }
                }
            }
            else
                c_code_prefix = null;

            if (c_code_prefix == null) {
                pa = new PrefixAction(prefix, action, c_code, is_exact);
                int idx0 = cursor.getColumnIndex(_ID);
                long row_id = cursor.getLong(idx0);
                pa.setRow_ID(row_id);
                actions.add(pa);
            }
        }

        cursor.close();
        db.close();

        return new ArrayList<>(actions);
    }

    public boolean isExist(PrefixAction prefixAction) {
        boolean is_exist = false;

        String c_code = prefixAction.getC_Code();
        String prefix = prefixAction.getPrefix();

        SQLiteDatabase db = getReadableDatabase();
        String whereClause = COL_C_CODE + " = ? AND " + COL_PREFIX + " = ?";
        String[] whereArgs = new String[] {c_code, prefix};
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{_ID},
                whereClause, whereArgs, null, null, null);
        if (cursor != null) {
            is_exist = (cursor.getCount() > 0);

            cursor.close();
        }
        db.close();

        return is_exist;
    }

    private long addNewCommon(PrefixAction prefixAction, SQLiteDatabase db) {
        boolean is_exact = prefixAction.getExact();
        String action = prefixAction.getAction();
        String c_code = prefixAction.getC_Code();
        String prefix = prefixAction.getPrefix();

        if (action.equals(PrefixAction.ACTION_ALLOW)) {
            action = (is_exact) ? ACTION_ALLOW_EXACT : ACTION_ALLOW;
        }
        else {
            action = (is_exact) ? ACTION_BLOCK_EXACT : ACTION_BLOCK;
        }

        long new_row_id;

        ContentValues values = new ContentValues();
        values.put(COL_C_CODE, c_code);
        values.put(COL_PREFIX, prefix);
        values.put(COL_ACTION, action);
        new_row_id = db.insertWithOnConflict(TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_ABORT);

        return new_row_id;
    }

    public long addNew(PrefixAction prefixAction) {

        SQLiteDatabase db = getWritableDatabase();
        long new_row_id = addNewCommon(prefixAction, db);
        db.close();

        return new_row_id;
    }

    public long addNewBatch(PrefixAction prefixAction) {
        return addNewCommon(prefixAction, mDB);
    }

    private long updateExistCommon(PrefixAction prefixAction, SQLiteDatabase db) {
        boolean is_exact = prefixAction.getExact();
        String action = prefixAction.getAction();
        String c_code = prefixAction.getC_Code();
        String prefix = prefixAction.getPrefix();
        String str_row_id = String.valueOf(prefixAction.getRow_ID());

        if (action.equals(PrefixAction.ACTION_ALLOW)) {
            action = (is_exact) ? ACTION_ALLOW_EXACT : ACTION_ALLOW;
        }
        else {
            action = (is_exact) ? ACTION_BLOCK_EXACT : ACTION_BLOCK;
        }

        long new_row_id;

        ContentValues values = new ContentValues();
        values.put(_ID, str_row_id);
        values.put(COL_C_CODE, c_code);
        values.put(COL_PREFIX, prefix);
        values.put(COL_ACTION, action);
        new_row_id = db.replace(TABLE_NAME,
                null, values);

        return new_row_id;
    }

    public long updateExist(PrefixAction prefixAction) {
        SQLiteDatabase db = getWritableDatabase();
        long new_row_id = updateExistCommon(prefixAction, db);
        db.close();

        return new_row_id;
    }

    public long updateExistBatch(PrefixAction prefixAction) {
        return updateExistCommon(prefixAction, mDB);
    }

    private int removeExistCommon(PrefixAction prefixAction, SQLiteDatabase db) {
        String str_row_id = String.valueOf(prefixAction.getRow_ID());

        int row_cnt = db.delete(TABLE_NAME,
                _ID + " = ?",
                new String[]{str_row_id});

        return row_cnt;
    }

    public int removeExist(PrefixAction prefixAction) {
        SQLiteDatabase db = getWritableDatabase();
        int row_cnt = removeExistCommon(prefixAction, db);
        db.close();

        return row_cnt;
    }

    public int removeExistBatch(PrefixAction prefixAction) {
        return removeExistCommon(prefixAction, mDB);
    }

    public void beginWriteBatch() {
        mDB = getWritableDatabase();
        mDB.beginTransactionNonExclusive();
    }

    public void endWriteBatch(boolean isSuccess) {
        if (isSuccess) {
            mDB.setTransactionSuccessful();
        }
        mDB.endTransaction();
        mDB.close();
    }
}
