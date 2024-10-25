package com.gmail.leewkb1307.callprefixfilter;

import android.provider.BaseColumns;

class CallLogContract {
    public static final String DB_NAME = "com.gmail.leewkb1307.calllog.db";
    public static final int DB_VERSION = 1;

    public static class CallEntry implements BaseColumns {
        public static final String TABLE_NAME = "calls";
        public static final String COL_TIME = "time";
        public static final String COL_ACTION = "action";
        public static final String COL_NUMBER = "number";
    }
}
