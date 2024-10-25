package com.gmail.leewkb1307.callprefixfilter;

import java.util.Comparator;

class PhoneCallLog {
    private String phone_number = "";
    private String call_date = "";
    private int call_type = 0;
    private boolean is_number = true;
    private int disp_seq = -1;

    public void setPhoneNumber(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getPhoneNumber() {
        return phone_number;
    }

    public void setCallDate(String call_date) {
        this.call_date = call_date;
    }

    public String getCallDate() {
        return call_date;
    }

    public void setCallType(int call_type) {
        this.call_type = call_type;
    }

    public int getCallType() {
        return call_type;
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

    static class CompT implements Comparator<PhoneCallLog> {
        @Override
        public int compare(PhoneCallLog o1, PhoneCallLog o2) {
            return Integer.valueOf(o1.getDispSeq()).compareTo(o2.getDispSeq());
        }
    }

    static class CompNT implements Comparator<PhoneCallLog> {
        @Override
        public int compare(PhoneCallLog o1, PhoneCallLog o2) {
            int result = o1.getPhoneNumber().compareTo(o2.getPhoneNumber());
            if (result == 0)
                result = Integer.valueOf(o1.getDispSeq()).compareTo(o2.getDispSeq());
            return result;
        }
    }
}
