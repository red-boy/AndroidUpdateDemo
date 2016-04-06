package com.devilwwj.update.http;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import com.devilwwj.update.R;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * com.devilwwj.update.http
 * Created by devilwwj on 16/4/6.
 */
public class ChenkUpdateAsynResponseHandler extends JsonHttpResponseHandler {

    private Context mContext;
    private ProgressDialog progressDialog;

    private OnCheckUpdateListener listener;
    public ChenkUpdateAsynResponseHandler(Context context, OnCheckUpdateListener listener) {
        this.mContext  = context;
        this.listener = listener;

        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(context.getString(R.string.tip_requesting));
        progressDialog.setCancelable(true);
    }



    public interface OnCheckUpdateListener {
        void onSuccess(UpdateInfo updateInfo);
        void onFailure();
    }

    @Override
    public void onStart() {
        super.onStart();
        progressDialog.show();
    }


    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        super.onSuccess(statusCode, headers, response);

        Log.d("info", response.toString());


        UpdateInfo updateInfo = new UpdateInfo();
        // 对数据进行解析
        JSONObject ireaderObj = response.optJSONObject("android_ireader");
        updateInfo.setName(ireaderObj.optString("name"));
        updateInfo.setVersionName(ireaderObj.optString("app_version"));
        updateInfo.setVersionCode(ireaderObj.optInt("version_code"));
        updateInfo.setFeatures(ireaderObj.optString("features"));
        updateInfo.setUpdateUrl(ireaderObj.optString("update_url"));


        // 解析json数据
        listener.onSuccess(updateInfo);


    }

    @Override
    public void onFinish() {
        super.onFinish();
        progressDialog.dismiss();
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
        super.onFailure(statusCode, headers, responseString, throwable);

        listener.onFailure();
    }


    @Override
    public void onCancel() {
        super.onCancel();
        progressDialog.cancel();
    }
}
