package com.gmail.leewkb1307.callprefixfilter;

import java.util.Comparator;

class CallLog {
    private String phone_number = "";
    private long call_time = 0;
    private int action_type = ACTION_BLOCK;
    private boolean is_number = true;
    private int disp_seq = -1;

    public static final int ACTION_BLOCK = 0;
    public static final int ACTION_ALLOW = 1;

    public void setPhoneNumber(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getPhoneNumber() {
        return phone_number;
    }

    public void setCallTime(long call_time) {
        this.call_time = call_time;
    }

    public long getCallTime() {
        return call_time;
    }

    public void setActionType(int action_type) {
        this.action_type = action_type;
    }

    public int getActionType() {
        return action_type;
    }

    public void setIsNumber(boolean is_number) {
        this.is_number = is_number;
    }

    public boolean getIsNumber() {
        return is_number;
    }

    public void setDispSeq(int disp_seq) {
        this.disp_seq = disp_seq;
    }

    public int getDispSeq() {
        return disp_seq;
    }

    static class CompT implements Comparator<CallLog> {
        @Override
        public int compare(CallLog o1, CallLog o2) {
            return Integer.valueOf(o1.getDispSeq()).compareTo(o2.getDispSeq());
        }
    }

    static class CompNT implements Comparator<CallLog> {
        @Override
        public int compare(CallLog o1, CallLog o2) {
            int result = o1.getPhoneNumber().compareTo(o2.getPhoneNumber());
            if (result == 0)
                result = Integer.valueOf(o1.getDispSeq()).compareTo(o2.getDispSeq());
            return result;
        }
    }
}
