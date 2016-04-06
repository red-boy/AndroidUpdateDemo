package com.devilwwj.update.http;

import android.content.Context;

/**
 * com.devilwwj.update.http
 * Created by devilwwj on 16/4/5.
 */
public class AppUpdateManager implements ChenkUpdateAsynResponseHandler.OnCheckUpdateListener{
    private Context mContext;

    private static AppUpdateManager instance;

    public static AppUpdateManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AppUpdateManager.class) {
                instance = new AppUpdateManager(context);
            }
        }
        return instance;
    }

    public AppUpdateManager(Context context) {
        this.mContext = context;
    }


    // 定义检查更新的方法
    public void checkUpdate() {
        HttpRequestHelper.getInstance().checkUpdate(null, new ChenkUpdateAsynResponseHandler(mContext, this));
    }

    @Override
    public void onSuccess(UpdateInfo updateInfo) {
        // 相应操作
    }

    @Override
    public void onFailure() {

    }
}
