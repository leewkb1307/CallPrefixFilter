package com.gmail.leewkb1307.callprefixfilter;

import android.provider.BaseColumns;

class ActionContract {
    public static final String DB_NAME = "com.gmail.leewkb1307.callprefixfilter.db";
    public static final int DB_VERSION = 2;

    public static class ActionEntry implements BaseColumns {
        public static final String TABLE_NAME = "actions";
        public static final String COL_C_CODE = "c_code";
        public static final String COL_PREFIX = "prefix";
        public static final String COL_ACTION = "action";
    }
}
