package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import static android.R.drawable.ic_delete;
import static android.R.drawable.ic_menu_call;

class PrefixActionAdapter extends BaseAdapter {
    private final ArrayList<PrefixAction> actionArrayList;

    private final LayoutInflater mInflater;

    public PrefixActionAdapter(Context context, ArrayList<PrefixAction> actions) {
        actionArrayList = actions;
        mInflater = LayoutInflater.from(context);
    }

    public void ensureCapacity(int minimumCapacity) {
        actionArrayList.ensureCapacity(minimumCapacity);
    }

    public ArrayList<PrefixAction> getItemAll() {
        return actionArrayList;
    }

    public int getCount() {
        return actionArrayList.size();
    }

    public Object getItem(int position) {
        return actionArrayList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        actionArrayList.clear();
    }

    public boolean add(PrefixAction c) {
        return actionArrayList.add(c);
    }

    public boolean addAll(ArrayList<PrefixAction> c) {
        return actionArrayList.addAll(c);
    }

    public boolean update(PrefixAction c) {
        boolean result;
        int index = indexOf_by_row_id(c.getRow_ID());
        if (index >= 0) {
            actionArrayList.set(index, c);
            result = true;
        }
        else
            result = false;
        return result;
    }

    public PrefixAction set(int index, PrefixAction c) {
        return actionArrayList.set(index, c);
    }

    public boolean remove(PrefixAction c) {
        boolean result;
        int index = indexOf_by_row_id(c.getRow_ID());
        if (index >= 0) {
            actionArrayList.remove(index);
            result = true;
        }
        else
            result = false;
        return result;
    }

    public PrefixAction remove(int index) {
        return actionArrayList.remove(index);
    }

    public int searchPrefix(Prefix c) {
        int index, size = actionArrayList.size();
        for (index = 0; index < size; index++) {
            Prefix prefix = actionArrayList.get(index);
            if (prefix.equals(c)) {
                return index;
            }
        }
        return -1;
    }

    private int indexOf_by_row_id(long row_id) {
        int index, size = actionArrayList.size();
        for (index = 0; index < size; index++) {
            PrefixAction prefixAction = actionArrayList.get(index);
            if (prefixAction.getRow_ID() == row_id) {
                return index;
            }
        }
        return -1;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_prefix_action, null);
            holder = new ViewHolder();
            holder.txtPrefix = (TextView) convertView.findViewById(R.id.text_prefix);
            holder.txtC_Code = (TextView) convertView.findViewById(R.id.text_c_code);
            holder.imgAction = (ImageView) convertView.findViewById(R.id.icon_action);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PrefixAction prefixAction = actionArrayList.get(position);

        String textPrefix = prefixAction.getPrefix();
        if (!prefixAction.getExact())
            textPrefix = textPrefix.concat("*");
        holder.txtPrefix.setText(textPrefix);

        String textC_Code = prefixAction.getC_Code();
        if (textC_Code.isEmpty())
            textC_Code = "*";
        holder.txtC_Code.setText(textC_Code);

        String textAction = prefixAction.getAction();
        if (textAction.equals(PrefixAction.ACTION_ALLOW)) {
            holder.imgAction.setImageResource(ic_menu_call);
        } else {
            holder.imgAction.setImageResource(ic_delete);
        }
        holder.txtPrefix.setTag(prefixAction);

        return convertView;
    }

    public static final int SORT_BY_ID         = 0;
    public static final int SORT_BY_PFX_ASC    = 1;
    public static final int SORT_BY_CC_PFX_ASC = 2;

    public void sort(int sort_type) {
        switch(sort_type) {
            case SORT_BY_ID:
                Collections.sort(actionArrayList, new PrefixAction.CompIDA());
                break;
            case SORT_BY_PFX_ASC:
                Collections.sort(actionArrayList, new PrefixAction.CompPA());
                break;
            case SORT_BY_CC_PFX_ASC:
                Collections.sort(actionArrayList, new PrefixAction.CompCPA());
                break;
            default:
                break;
        }
    }

    static class ViewHolder {
        TextView txtPrefix;
        TextView txtC_Code;
        ImageView imgAction;
    }
}
