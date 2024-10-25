package com.gmail.leewkb1307.callprefixfilter;

import android.support.annotation.NonNull;

import java.util.Comparator;

class Prefix {
    private String c_code;
    private String prefix;

    public Prefix() {
        this("", "");
    }

    public Prefix(@NonNull String prefix, @NonNull String c_code) {
        setPrefix(prefix);
        setC_Code(c_code);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setC_Code(String c_code) {
        this.c_code = c_code;
    }

    public String getC_Code() {
        return c_code;
    }

    public String getC_CodePrefix() {
        if (c_code.isEmpty())
            return prefix;
        else
            return "+" + c_code + prefix;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        return isSameCP((Prefix) obj);
    }

    public boolean isSameCP(Prefix c) {
        return c_code.equals(c.getC_Code()) && prefix.equals(c.getPrefix());
    }

    static class CompPA implements Comparator<Prefix> {
        @Override
        public int compare(Prefix o1, Prefix o2) {
            return o1.getPrefix().compareTo(o2.getPrefix());
        }
    }

    static class CompCPA implements Comparator<Prefix> {
        @Override
        public int compare(Prefix o1, Prefix o2) {
            int result = o1.getC_Code().compareTo(o2.getC_Code());
            if (result == 0)
                result = o1.getPrefix().compareTo(o2.getPrefix());
            return result;
        }
    }

    public int hashCode() {
        String strCode;

        if (c_code.isEmpty()) {
            strCode = "!";
        }
        else {
            strCode = c_code;
        }

        if (prefix.isEmpty()) {
            strCode += "!";
        }
        else {
            strCode += prefix;
        }

        return strCode.hashCode();
    }
}
