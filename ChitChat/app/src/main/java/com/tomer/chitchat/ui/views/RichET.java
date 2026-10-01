package com.tomer.chitchat.ui.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

public class RichET extends androidx.appcompat.widget.AppCompatEditText {

    private String[] imgType;
    private keyCallBack callBack;
    final InputConnectionCompat.OnCommitContentListener callbk = new InputConnectionCompat.OnCommitContentListener() {
        @Override
        public boolean onCommitContent(@NonNull InputContentInfoCompat inputContentInfo, int flags, @Nullable Bundle opts) {
            if (BuildCompat.isAtLeastNMR1() && (flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                try {
                    inputContentInfo.requestPermission();
                } catch (Exception ignored) {
                    return false;
                }
            }
            boolean isSupp = false;
            for (final String mime : imgType) {
                if (inputContentInfo.getDescription().hasMimeType(mime)) {
                    isSupp = true;
                    break;
                }
            }
            if (!isSupp)
                return false;
            if (callBack != null)
                callBack.onCommitContent(inputContentInfo);
            return true;
        }
    };

    public RichET(Context context) {
        super(context);
        initView();
    }

    public RichET(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }


    public RichET(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    public InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
        final InputConnection ic = super.onCreateInputConnection(outAttrs);
        EditorInfoCompat.setContentMimeTypes(outAttrs, imgType);
        assert ic != null;
        return InputConnectionCompat.createWrapper(ic, outAttrs, callbk);
    }

    public void setKeyboardInputCall(keyCallBack b) {
        this.callBack = b;
    }

    private void initView() {
        imgType = new String[]{"image/png", "image/gif", "image/jpeg", "image/webp"};
    }

    public interface keyCallBack {
        void onCommitContent(InputContentInfoCompat infoCompat);
    }

}
