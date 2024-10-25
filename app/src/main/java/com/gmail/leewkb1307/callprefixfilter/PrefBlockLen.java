package com.gmail.leewkb1307.callprefixfilter;

class PrefBlockLen {
    private Integer mLen;

    PrefBlockLen() {
        mLen = -1;
    }

    PrefBlockLen(String strNum) {
        mLen = (strNum.isEmpty()) ? -1 : Integer.valueOf(strNum);
    }

    public boolean isEnabled() {
        return (mLen >= 0);
    }

    public Integer getBlockLen() {
        return mLen;
    }
}
