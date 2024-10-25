package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import static android.R.drawable.ic_delete;
import static android.R.drawable.sym_call_incoming;
import static android.R.drawable.sym_call_missed;
import static android.R.drawable.sym_call_outgoing;

class PhoneCallLogAdapter extends BaseAdapter {
    private final ArrayList<PhoneCallLog> calllogArrayList;

    private final LayoutInflater mInflater;

    public PhoneCallLogAdapter(Context context, ArrayList<PhoneCallLog> calllog) {
        calllogArrayList = calllog;
        mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return calllogArrayList.size();
    }

    public Object getItem(int position) {
        return calllogArrayList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        calllogArrayList.clear();
    }

    public boolean addAll(ArrayList<PhoneCallLog> c) {
        return calllogArrayList.addAll(c);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_call_log, null);
            holder = new ViewHolder();
            holder.txtNumber = (TextView) convertView.findViewById(R.id.text_number);
            holder.txtDate = (TextView) convertView.findViewById(R.id.text_date);
            holder.imgCallType = (ImageView) convertView.findViewById(R.id.icon_type);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.txtNumber.setText(calllogArrayList.get(position).getPhoneNumber());
        holder.txtDate.setText(calllogArrayList.get(position).getCallDate());
        int intCallType = calllogArrayList.get(position).getCallType();
        switch(intCallType) {
            case CallLog.Calls.INCOMING_TYPE:
                holder.imgCallType.setImageResource(sym_call_incoming);
                break;
            case CallLog.Calls.OUTGOING_TYPE:
                holder.imgCallType.setImageResource(sym_call_outgoing);
                break;
            case CallLog.Calls.MISSED_TYPE:
            case CallLog.Calls.VOICEMAIL_TYPE:
                holder.imgCallType.setImageResource(sym_call_missed);
                break;
            default:
                holder.imgCallType.setImageResource(ic_delete);
                break;
        }

        return convertView;
    }

    static class ViewHolder {
        TextView txtNumber;
        TextView txtDate;
        ImageView imgCallType;
    }

    public static final int SORT_BY_TIME = 0;
    public static final int SORT_BY_NUM  = 1;

    public void sort(int sort_type) {
        switch(sort_type) {
            case SORT_BY_TIME:
                Collections.sort(calllogArrayList, new PhoneCallLog.CompT());
                break;
            case SORT_BY_NUM:
                Collections.sort(calllogArrayList, new PhoneCallLog.CompNT());
                break;
            default:
                break;
        }
    }
}
