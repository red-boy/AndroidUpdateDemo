package com.devilwwj.update.http;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

/**
 * com.devilwwj.update.http
 * Created by devilwwj on 16/4/5.
 */
public class CheckUpdateHandler extends AsyncHttpResponseHandler {


    private final Context mContext;


    public CheckUpdateHandler(Context context) {
        this.mContext = context;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onSuccess(int i, Header[] headers, byte[] bytes) {
        if (bytes != null) {
            String response = new String(bytes);

            Log.d("update", response);
        }
    }

    @Override
    public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

    }

    @Override
    public void onCancel() {
        super.onCancel();
    }
}
