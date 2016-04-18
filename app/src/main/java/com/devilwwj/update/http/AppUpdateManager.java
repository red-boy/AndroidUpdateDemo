package com.devilwwj.update.http;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.devilwwj.update.utils.ToastUtils;
import com.devilwwj.update.utils.Utils;

/**
 * com.devilwwj.update.http
 * Created by devilwwj on 16/4/5.
 */
public class AppUpdateManager implements CheckUpdateAsynResponseHandler.OnCheckUpdateListener{
    private Context mContext;

    // 版本号
    private String versionName = null;

    // 下载地址
    private String downloadUrl = null;


    // 下载进度
    public static final int MSG_DOWNLOAD_PROGRESS = 0;
    // 下载结果
    public static final int MSG_DOWNLOAD_RESULT = 1;

    // 用来显示ui的Handler
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DOWNLOAD_PROGRESS: // 更新进度
                    int percent = msg.getData().getInt("percent");
                    ToastUtils.showShort(mContext, "已下载" + percent + "%");
                    break;
                case MSG_DOWNLOAD_RESULT: // 下载结果

                    break;
            }
        }
    };


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

    // 定义更新服务
    private AppUpdateService updateService = null;
    // 更新服务连接
    private ServiceConnection updateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("ServiceConnection", "service connected");
            AppUpdateService.AppUpdateBinder aBinder = (AppUpdateService.AppUpdateBinder) service;
            updateService = aBinder.getService();

            updateService.setHandler(handler);
            // TODO: 执行下载任务
            updateService.downloadStart(downloadUrl, versionName);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("ServiceConnection", "service disconnected");
            updateService = null;


        }
    };


    // 定义检查更新的方法
    public void checkUpdate() {
        HttpRequestHelper.getInstance().checkUpdate(null, new CheckUpdateAsynResponseHandler(mContext, this));
    }


    /**
     * 混合调用
     * 先以startService方式启动服务
     * 再以bindService方式绑定服务
     */
    private void bindUpdateService() {
        Intent intent = new Intent(mContext, AppUpdateService.class);
        mContext.startService(intent);
        mContext.bindService(intent, updateServiceConnection, Context.BIND_AUTO_CREATE);
    }


    /**
     * 获取到更新信息回调
     * @param updateInfo
     */
    @Override
    public void onSuccess(UpdateInfo updateInfo) {
        // 相应操作
        versionName = updateInfo.getVersionName();
        downloadUrl = updateInfo.getUpdateUrl();

        // 首先获取当前版本
        int currentVersionCode = Utils.getAppVersionCode(mContext);

        // 如果当前版本低于服务器版本，则需要弹出更新对话框
        if (updateInfo.getVersionCode() <= currentVersionCode) {
            ToastUtils.showShort(mContext, "已经是最新版");
        } else {
            showUpdateDialog(versionName, updateInfo.getFeatures());
        }
    }

    /**
     * 显示更新对话框
     * @param versionName 版本名
     * @param features 更新信息
     */
    private void showUpdateDialog(String versionName, String features) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
        alertBuilder.setTitle("更新应用 V" + versionName);
        alertBuilder.setMessage(features);
        alertBuilder.setCancelable(true);
        alertBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // 这里启动服务器下载apk
                bindUpdateService();
            }
        });
        alertBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        // 显示对话框
        alertBuilder.create().show();
    }


    /**
     * 获取更新信息失败回调
     */
    @Override
    public void onFailure() {

    }
}
