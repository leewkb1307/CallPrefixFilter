package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import static android.R.drawable.ic_delete;
import static android.R.drawable.ic_menu_call;

class CallLogAdapter extends BaseAdapter {
    private final ArrayList<CallLog> calllogArrayList;

    private final LayoutInflater mInflater;

    public CallLogAdapter(Context context, ArrayList<CallLog> calllog) {
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

    public boolean addAll(ArrayList<CallLog> c) {
        return calllogArrayList.addAll(c);
    }

    private String callTimeToText(long epochTime) {
        Date date = new Date(epochTime);
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");
        return format.format(date);
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
        holder.txtDate.setText(callTimeToText(calllogArrayList.get(position).getCallTime()));
        int intActionType = calllogArrayList.get(position).getActionType();
        if (intActionType == CallLog.ACTION_ALLOW) {
            holder.imgCallType.setImageResource(ic_menu_call);
        } else {
            holder.imgCallType.setImageResource(ic_delete);
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
                Collections.sort(calllogArrayList, new CallLog.CompT());
                break;
            case SORT_BY_NUM:
                Collections.sort(calllogArrayList, new CallLog.CompNT());
                break;
            default:
                break;
        }
    }
}
