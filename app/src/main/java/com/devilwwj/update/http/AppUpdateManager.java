package com.devilwwj.update.http;

import android.content.Context;

/**
 * com.devilwwj.update.http
 * Created by devilwwj on 16/4/5.
 */
public class AppUpdateManager {
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


    public void checkUpdate() {
        HttpRequestHelper.getInstance().checkUpdate(null, new CheckUpdateHandler(mContext));
    }

}
