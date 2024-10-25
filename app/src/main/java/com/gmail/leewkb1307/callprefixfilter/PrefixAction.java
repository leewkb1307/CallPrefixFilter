package com.gmail.leewkb1307.callprefixfilter;

import androidx.annotation.NonNull;

import java.util.Comparator;

class PrefixAction extends Prefix {
    public static final String ACTION_BLOCK = "0";
    public static final String ACTION_ALLOW = "1";

    private String action = "";
    private long row_id = -1;
    private boolean is_exact = false;

    public PrefixAction() {
    }

    public PrefixAction(@NonNull String prefix, @NonNull String action, @NonNull String c_code) {
        super(prefix, c_code);
        setAction(action);
    }

    public PrefixAction(@NonNull String prefix, @NonNull String action, @NonNull String c_code, boolean is_exact) {
        this(prefix, action, c_code);
        setExact(is_exact);
    }

    public void setExact(boolean is_exact) {
        this.is_exact = is_exact;
    }

    public boolean getExact() {
        return is_exact;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setRow_ID(long row_id) {
        this.row_id = row_id;
    }

    public long getRow_ID() {
        return row_id;
    }

    public boolean isSameACPE(PrefixAction c) {
        return isSameCP(c) && action.equals(c.getAction()) && is_exact == c.getExact();

    }

    static class CompIDA implements Comparator<PrefixAction> {
        @Override
        public int compare(PrefixAction o1, PrefixAction o2) {
            return (Long.valueOf(o1.getRow_ID())).compareTo(o2.getRow_ID());
        }
    }
}
