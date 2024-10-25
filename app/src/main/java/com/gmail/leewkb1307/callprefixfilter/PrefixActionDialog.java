package com.gmail.leewkb1307.callprefixfilter;

import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

class PrefixActionDialog {
    private AppCompatActivity mActivity;
    private ActionDbHelper mDbHelper;
    private onPrefixDialogDoneListener mCallback;

    public static final int DONE_UNKNOWN = 0;
    public static final int DONE_ADD     = 1;
    public static final int DONE_UPDATE  = 2;
    public static final int DONE_REMOVE  = 3;

    interface onPrefixDialogDoneListener {
        void onPrefixDialogDone(PrefixAction prefixAction, int done_type);
    }

    public PrefixActionDialog(@NonNull AppCompatActivity activity) {
        mActivity = activity;
        mDbHelper = new ActionDbHelper(activity);
    }

    public void registerCallback(onPrefixDialogDoneListener callback){
        mCallback = callback;
    }

    public AlertDialog showAddDialog(PrefixAction prefixAction) {
        String prefix = prefixAction.getPrefix();
        String c_code = prefixAction.getC_Code();
        boolean is_exact = prefixAction.getExact();

        LayoutInflater factory = LayoutInflater.from(mActivity);
        final View prefixEntryView = factory.inflate(R.layout.input_prefix_action, null);
        final EditText prefixEditText = (EditText) prefixEntryView.findViewById(R.id.text_prefix);
        final EditText c_codeEditText = (EditText) prefixEntryView.findViewById(R.id.text_c_code);
        final CheckBox is_exactCheck = (CheckBox) prefixEntryView.findViewById(R.id.checkBox_exact);
        prefixEditText.setText(prefix);
        c_codeEditText.setText(c_code);
        is_exactCheck.setChecked(is_exact);
        final CharSequence[] items = {"Block the call", "Allow the call"};
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle("Add a new prefix")
                .setSingleChoiceItems(items, 0, null)
                .setView(prefixEntryView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Context context = mActivity.getApplicationContext();
                        String c_code = String.valueOf(c_codeEditText.getText());
                        String prefix = String.valueOf(prefixEditText.getText());
                        if (prefix == null || c_code == null || (prefix.isEmpty() && c_code.isEmpty())) {
                            Toast.makeText(context, "Empty prefix entered...", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean is_exact = is_exactCheck.isChecked();

                        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                        String action = (selectedPosition == 1) ? PrefixAction.ACTION_ALLOW : PrefixAction.ACTION_BLOCK;

                        // check if the prefix is present in filter rules
                        PrefixAction prefixAction = new PrefixAction(prefix, action, c_code, is_exact);
                        String c_code_prefix = prefixAction.getC_CodePrefix();
                        if (!mDbHelper.isExist(prefixAction)) {
                            // add a new filter rule
                            long row_id = mDbHelper.addNew(prefixAction);

                            if (row_id >= 0) {
                                Toast.makeText(context, "Add prefix " + c_code_prefix, Toast.LENGTH_SHORT).show();

                                prefixAction.setRow_ID(row_id);

                                // callback
                                if (mCallback != null)
                                    mCallback.onPrefixDialogDone(prefixAction, DONE_ADD);
                            }
                            else {
                                Toast.makeText(context, "Error! Prefix not added...", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(context, "Prefix " + c_code_prefix + " is a duplicate", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        return dialog;
    }

    public AlertDialog showEditDialog(PrefixAction prefixAction) {
        final String prefix = prefixAction.getPrefix();
        final String c_code = prefixAction.getC_Code();
        boolean is_exact = prefixAction.getExact();
        String action = prefixAction.getAction();
        int selectedPosition;
        if (action.equals(PrefixAction.ACTION_ALLOW))
            selectedPosition = 1;
        else
            selectedPosition = 0;
        final long row_id = prefixAction.getRow_ID();

        LayoutInflater factory = LayoutInflater.from(mActivity);
        final View prefixEntryView = factory.inflate(R.layout.input_prefix_action, null);
        final EditText prefixEditText = (EditText) prefixEntryView.findViewById(R.id.text_prefix);
        final EditText c_codeEditText = (EditText) prefixEntryView.findViewById(R.id.text_c_code);
        final CheckBox is_exactCheck = (CheckBox) prefixEntryView.findViewById(R.id.checkBox_exact);
        prefixEditText.setText(prefix);
        c_codeEditText.setText(c_code);
        is_exactCheck.setChecked(is_exact);
        final CharSequence[] items = {"Block the call", "Allow the call"};
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle("Edit a prefix")
                .setSingleChoiceItems(items, selectedPosition, null)
                .setView(prefixEntryView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Context context = mActivity.getApplicationContext();
                        String c_code2 = String.valueOf(c_codeEditText.getText());
                        String prefix2 = String.valueOf(prefixEditText.getText());
                        if (prefix2 == null || c_code2 == null || (prefix2.isEmpty() && c_code2.isEmpty())) {
                            Toast.makeText(context, "Empty prefix entered...", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean is_exact2 = is_exactCheck.isChecked();

                        int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                        String action2 = (selectedPosition == 1) ? PrefixAction.ACTION_ALLOW : PrefixAction.ACTION_BLOCK;

                        // check for prefix duplicate
                        PrefixAction prefixAction2 = new PrefixAction(prefix2, action2, c_code2, is_exact2);
                        String c_code_prefix2 = prefixAction2.getC_CodePrefix();
                        if (!prefix.equals(prefix2) || !c_code.equals(c_code2)) {
                            if (mDbHelper.isExist(prefixAction2)) {
                                Toast.makeText(context, "Prefix " + c_code_prefix2 + " is a duplicate", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        // update filter rule
                        prefixAction2.setRow_ID(row_id);
                        long row_id2 = mDbHelper.updateExist(prefixAction2);

                        if (row_id2 >= 0) {
                            if (row_id2 == row_id) {
                                Toast.makeText(context, "Set prefix " + c_code_prefix2, Toast.LENGTH_SHORT).show();

                                // callback
                                if (mCallback != null)
                                    mCallback.onPrefixDialogDone(prefixAction2, DONE_UPDATE);
                            }
                            else {
                                Toast.makeText(context, "Error! Inconsistent update!", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(context, "Error! Prefix not updated...", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        return dialog;
    }

    public AlertDialog showDeleteDialog(final PrefixAction prefixAction) {
        String c_code_prefix = prefixAction.getC_CodePrefix();
        if (!prefixAction.getExact())
            c_code_prefix = c_code_prefix + "*";
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle("Remove a prefix")
                .setMessage("Remove prefix " + c_code_prefix + "?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Context context = mActivity.getApplicationContext();

                        // delete filter rule
                        int row_cnt = mDbHelper.removeExist(prefixAction);

                        if (row_cnt == 1) {
                            // callback
                            if (mCallback != null)
                                mCallback.onPrefixDialogDone(prefixAction, DONE_REMOVE);
                        }
                        else {
                            Toast.makeText(context, "Error! Prefix not removed...", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        return dialog;
    }
}
