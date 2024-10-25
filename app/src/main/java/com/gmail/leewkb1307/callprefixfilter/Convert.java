package com.gmail.leewkb1307.callprefixfilter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Convert {
    private Pattern mPattern;
    private int mError;

    public static final int ERROR_NONE = 0;
    public static final int ERROR_TOKEN_LT_4 = 1;
    public static final int ERROR_TOKEN_GT_4 = 2;
    public static final int ERROR_HEADER_BAD = 3;
    public static final int ERROR_NO_PREFIX = 4;
    public static final int ERROR_ACTION_BAD = 5;
    public static final int ERROR_C_CODE_BAD = 6;
    public static final int ERROR_PREFIX_BAD = 7;
    public static final int ERROR_EXACT_BAD = 8;

    public Convert() {
        String strPhone = "[0-9\\+\\*#\\- \\.,;\\(\\)/N]+";
        mPattern = Pattern.compile(strPhone);

        mError = ERROR_NONE;
    }

    public static String prefixAction2TextLine(PrefixAction prefixAction) {
        String action = prefixAction.getAction();
        String prefix = prefixAction.getPrefix();
        String c_code = prefixAction.getC_Code();
        boolean is_exact = prefixAction.getExact();
        String str_action = (action.equals(PrefixAction.ACTION_ALLOW)) ? "allow" : "block";
        String str_exact = (is_exact) ? "true" : "false";
        if (prefix.contains(",")) {
            prefix = "\"" + prefix + "\"";
        }
        if (c_code.contains(",")) {
            c_code = "\"" + c_code + "\"";
        }
        return str_action + "," + c_code + "," + prefix + "," + str_exact + "\n";
    }

    private static List<String> textLine2WordList(@NonNull String textLine) {
        // parse the text line to tokens
        List<String> wordList = new ArrayList<>();
        while (textLine != null && !textLine.isEmpty()) {
            try {
                String[] keywords;
                if (textLine.startsWith("\"")) {
                    // handle double quotes
                    keywords = textLine.substring(1).split("\",?", 2);
                }
                else {
                    // handle comma
                    keywords = textLine.split(",", 2);
                }
                wordList.add(keywords[0]);
                textLine = keywords[1];
            }
            catch (ArrayIndexOutOfBoundsException e) {
                textLine = null;
            }
        }

        return wordList;
    }

    private static int checkWordListSize(List<String> wordList) {
        int errorCode;

        if (wordList.size() < 4) {
            errorCode = ERROR_TOKEN_LT_4;
        } else if (wordList.size() > 4) {
            errorCode = ERROR_TOKEN_GT_4;
        } else {
            errorCode = ERROR_NONE;
        }

        return errorCode;
    }

    public static int checkCSVheader(@NonNull String textLine) {
        int errorCode;

        // parse the text line to tokens
        List<String> wordList = Convert.textLine2WordList(textLine);

        errorCode = checkWordListSize(wordList);

        if (errorCode == ERROR_NONE) {
            if (!wordList.get(0).equals("action") ||
                    !wordList.get(1).equals("c_code") ||
                    !wordList.get(2).equals("prefix") ||
                    !wordList.get(3).equals("exact")) {
                errorCode = ERROR_HEADER_BAD;
            }
        }

        return errorCode;
    }

    public PrefixAction textLine2PrefixAction(@NonNull String textLine) {
        PrefixAction prefixAction = new PrefixAction();

        // parse the text line to tokens
        List<String> wordList = Convert.textLine2WordList(textLine);

        mError = checkWordListSize(wordList);

        if (mError == ERROR_NONE) {
            String str_action = wordList.get(0);
            String str_c_code = wordList.get(1);
            String str_prefix = wordList.get(2);
            String str_exact  = wordList.get(3);

            if (str_prefix.isEmpty() && str_c_code.isEmpty()) {
                mError = ERROR_NO_PREFIX;
            }

            if (mError == ERROR_NONE) {
                String action;
                if (str_action.isEmpty() || str_action.equals("block")) {
                    action = PrefixAction.ACTION_BLOCK;
                }
                else if (str_action.equals("allow")) {
                    action = PrefixAction.ACTION_ALLOW;
                }
                else {
                    action = null;
                    mError = ERROR_ACTION_BAD;
                }

                if (action != null) {
                    prefixAction.setAction(action);
                }
            }

            if (mError == ERROR_NONE) {
                if (!str_c_code.isEmpty() && !mPattern.matcher(str_c_code).matches()) {
                    mError = ERROR_C_CODE_BAD;
                } else {
                    prefixAction.setC_Code(str_c_code);
                }
            }

            if (mError == ERROR_NONE) {
                if (!str_prefix.isEmpty() && !mPattern.matcher(str_prefix).matches()) {
                    mError = ERROR_PREFIX_BAD;
                } else {
                    prefixAction.setPrefix(str_prefix);
                }
            }

            if (mError == ERROR_NONE) {
                boolean is_exact;
                if (str_exact.isEmpty() || str_exact.equals("false")) {
                    is_exact = false;
                }
                else if (str_exact.equals("true")) {
                    is_exact = true;
                }
                else if (str_exact.equals("FALSE")) {
                    is_exact = false;
                }
                else if (str_exact.equals("TRUE")) {
                    is_exact = true;
                }
                else {
                    is_exact = false;
                    mError = ERROR_EXACT_BAD;
                }
                prefixAction.setExact(is_exact);
            }
        }

        return (mError == ERROR_NONE) ? prefixAction : null;
    }

    public int getErrorCode() {
        return mError;
    }

    public static String getErrorMessage(int errorCode) {
        String errMesg;

        switch(errorCode) {
            case ERROR_NONE:
                errMesg = "no error";
                break;
            case ERROR_TOKEN_LT_4:
                errMesg = "less than 4 fields";
                break;
            case ERROR_TOKEN_GT_4:
                errMesg = "more than 4 fields";
                break;
            case ERROR_HEADER_BAD:
                errMesg = "heading line is invalid";
                break;
            case ERROR_NO_PREFIX:
                errMesg = "no valid prefix";
                break;
            case ERROR_ACTION_BAD:
                errMesg = "action field is invalid";
                break;
            case ERROR_C_CODE_BAD:
                errMesg = "c_code field is invalid";
                break;
            case ERROR_PREFIX_BAD:
                errMesg = "prefix field is invalid";
                break;
            case ERROR_EXACT_BAD:
                errMesg = "exact field is invalid";
                break;
            default:
                errMesg = "unknown error";
                break;
        }

        return errMesg;
    }
}
