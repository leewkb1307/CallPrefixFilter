package com.gmail.leewkb1307.callprefixfilter;

import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeSet;

class FileSelector {
    private static final String PARENT_DIR = "..";

    private final AppCompatActivity mActivity;
    private AlertDialog mAlertDialog;
    private FileSelectedListener mFileListener;
    private ListView mListView;
    private TextView mTextView;
    private TextView mTextName;
    private EditText mEditText;
    private boolean mEditName;
    private File mCurrentPath;
    private String mFileName;
    private ArrayAdapter<String> mAdapter;
    private ListView mListVoid;

    // file selection event handling
    public interface FileSelectedListener {
        void fileSelected(File directory, String filename);
    }

    public FileSelector setFileListener(FileSelectedListener fileListener) {
        mFileListener = fileListener;
        return this;
    }

    public FileSelector(AppCompatActivity activity, String msgTitle, File pathInit) {
        mActivity = activity;

        mFileName = null;
        mCurrentPath = pathInit;
        mEditName = false;

        LayoutInflater factory = LayoutInflater.from(mActivity);
        View fileSelectorView = factory.inflate(R.layout.input_file_selector, null);
        mTextView = (TextView) fileSelectorView.findViewById(R.id.text_current_path);
        mListView = (ListView) fileSelectorView.findViewById(R.id.list_filename);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
                String fileSelected = (String) mListView.getItemAtPosition(which);
                File selectedFile = getSelectedFile(fileSelected);
                boolean isDirSelected = selectedFile.isDirectory();
                mFileName = (isDirSelected) ? null : fileSelected;
                refreshDialogButton();
                if (isDirSelected) {
                    refresh(selectedFile);
                }
                else if (!mEditName) {
                    mTextView.setText(selectedFile.getPath());
                }
            }
        });
        mAdapter = new ArrayAdapter<>(activity, R.layout.item_filename);
        mListView.setAdapter(mAdapter);
        mListVoid = (ListView) fileSelectorView.findViewById(R.id.list_voided);
        mTextName = (TextView) fileSelectorView.findViewById(R.id.hint_filename);
        mEditText = (EditText) fileSelectorView.findViewById(R.id.edit_filename);
        mEditText.addTextChangedListener(editTextWatcher);
        AlertDialog.Builder builder = new Builder(activity);
        builder.setTitle(msgTitle);
        builder.setView(fileSelectorView);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mFileListener != null) {
                    if (mEditName) {
                        mFileName = mEditText.getText().toString();
                    }
                    if (mFileName != null && !mFileName.isEmpty()) {
                        mFileListener.fileSelected(mCurrentPath, mFileName);
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        mAlertDialog = builder.create();
        mAlertDialog.setOnShowListener(dialogShowListener);
        refresh(mCurrentPath);
    }

    public void setEditFile(boolean isEdit) {
        if (isEdit) {
            mListVoid.setVisibility(View.GONE);
            mTextName.setVisibility(View.VISIBLE);
            mEditText.setVisibility(View.VISIBLE);
        }
        else {
            mTextName.setVisibility(View.GONE);
            mEditText.setVisibility(View.GONE);
            mListVoid.setVisibility(View.VISIBLE);
        }
        mEditName = isEdit;
    }

    public void setEditName(String fileName) {
        mEditText.setText(fileName);
    }

    public AlertDialog showDialog() {
        mAlertDialog.show();
        return mAlertDialog;
    }

    /**
     * Sort, filter and display the files for the given path.
     */
    private void refresh(File path) {
        if (path.exists() && path.isDirectory() && path.canRead()) {
            mCurrentPath = path;
            mTextView.setText(mCurrentPath.getPath());

            // find em all
            TreeSet<String> dirs = new TreeSet<>();
            TreeSet<String> files = new TreeSet<>();
            for(File file : Objects.requireNonNull(path.listFiles())) {
                if(!file.canRead())
                    continue;
                if(file.isDirectory()) {
                    dirs.add(file.getName());
                } else {
                    files.add(file.getName());
                }
            }

            // convert to an array
            ArrayList<String> fileList = new ArrayList<>(dirs.size() + files.size() + 1);
            File parentFile = path.getParentFile();
            if (parentFile != null && parentFile.canRead())
                fileList.add(PARENT_DIR);
            fileList.addAll(dirs);
            fileList.addAll(files);

            // refresh the user interface
            mAdapter.clear();
            for (String fileName : fileList) {
                mAdapter.add(fileName);
            }
            mAdapter.notifyDataSetChanged();
        } else {
            mTextView.setText(mCurrentPath.getPath());
            Toast.makeText(mActivity, "Cannot access " + path + "...", Toast.LENGTH_SHORT).show();
        }
    }

    private DialogInterface.OnShowListener dialogShowListener = new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialog) {
            refreshDialogButton();
        }
    };

    private TextWatcher editTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            refreshDialogButton();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start,
        int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start,
        int before, int count) {
        }
    };

    private void refreshDialogButton() {
        Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button != null) {
            boolean toEnable;

            if (mEditName) {
                String editText = mEditText.getText().toString();
                toEnable = !editText.isEmpty();
            }
            else {
                toEnable = (mFileName != null);
            }
            button.setEnabled(toEnable);
        }
    }

    /**
     * Convert a relative filename into an actual File object.
     */
    private File getSelectedFile(String fileSelected) {
        if (fileSelected.equals(PARENT_DIR)) {
            return mCurrentPath.getParentFile();
        } else {
            return new File(mCurrentPath, fileSelected);
        }
    }
}
