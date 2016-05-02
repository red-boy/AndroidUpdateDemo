package com.devilwwj.update.http;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.devilwwj.update.R;
import com.devilwwj.update.utils.ToastUtils;
import com.devilwwj.update.utils.Utils;
import com.loopj.android.http.RequestHandle;

/**
 * com.devilwwj.update.http
 * Created by devilwwj on 16/4/5.
 */
public class AppUpdateManager implements CheckUpdateAsynResponseHandler.OnCheckUpdateListener {
    private Context mContext;

    // 版本号
    private String versionName = null;

    // 下载地址
    private String downloadUrl = null;


    // 下载进度
    public static final int MSG_DOWNLOAD_PROGRESS = 0;
    // 下载结果
    public static final int MSG_DOWNLOAD_RESULT = 1;

    // 下载成功
    public static final int FLAG_DOWNLOAD_SUCCESS = 0;
    // 取消下载
    public static final int FLAG_CANCEL_UPDATE = -1;
    // 空间不足
    public static final int FLAG_NO_ENOUGH_SPACE = 1;
    // 下载失败或者写入失败
    public static final int FLAG_DOWNLOAD_ERROR = 2;

    // handler用到的key
    public static final String KEY_DOWNLOAD_RESULT = "downloadResult";
    public static final String KEY_FILENAME = "fileName";
    public static final String KEY_PERCENT = "percent";



    private static AppUpdateManager instance;

    // 定义更新服务
    private AppUpdateService updateService = null;
    // 更新服务连接
    private ServiceConnection updateServiceConnection = null;

    // 进度对话框
    private ProgressDialog progressDialog = null;

    // 请求句柄
    private RequestHandle updateHandle;

    // 是否已绑定服务
    private boolean isBinded = false;

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
        initServiceConnection();
        initProgressDialog();

    }

    /**
     * 初始化ProgressDialog
     */
    private void initProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle(mContext.getString(R.string.update_app));
            progressDialog.setMax(100);
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // 取消下载
                    updateHandle.cancel(true);
                    ToastUtils.showShort(mContext, R.string.tip_cancelupdate);
                    unBindService();
                }
            });
        }
    }

    /**
     * 取消绑定服务
     */
    private void unBindService() {
        Intent intent = new Intent(mContext, AppUpdateService.class);
        if (isBinded) {
            Log.d("service", "unbind service");
            mContext.unbindService(updateServiceConnection);
            mContext.stopService(intent);
            updateService = null;
            isBinded = false;
        } else {
            Log.d("service", "not binded");
        }
    }

    /**
     * 初始化ServiceConnection
     */
    private void initServiceConnection() {
        updateServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d("ServiceConnection", "service connected");
                AppUpdateService.AppUpdateBinder aBinder = (AppUpdateService.AppUpdateBinder) service;
                updateService = aBinder.getService();

                isBinded = true;
                updateService.setHandler(handler);
                // TODO: 执行下载任务
                updateService.downloadStart(downloadUrl, versionName);

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d("ServiceConnection", "service disconnected");
                updateService = null;
                isBinded = false;

            }
        };
    }


    // 定义检查更新的方法
    public void checkUpdate() {
        updateHandle = HttpRequestHelper.getInstance().checkUpdate(null, new CheckUpdateAsynResponseHandler(mContext, this));
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
     *
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
     *
     * @param versionName 版本名
     * @param features    更新信息
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
                // 这里启动服务下载apk
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

    // 用来显示ui的Handler
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DOWNLOAD_PROGRESS: // 更新进度
                    updateProgress(msg);
                    break;
                case MSG_DOWNLOAD_RESULT: // 下载结果
                    handleDownloadResult(msg);
                    break;
            }
        }
    };

    /**
     * 更新下载进度
     *
     * @param msg
     */
    private void updateProgress(Message msg) {

        // 得到下载进度百分比
        int precentage = msg.getData().getInt("percent");
        // 下载100%才算下载完成
        if (precentage == 100) {
            ToastUtils.showShort(mContext, R.string.tip_downloadsuccess);
        }
        // 设置progress进度
        progressDialog.setProgress(precentage);
        progressDialog.show();

    }


    public void handleDownloadResult(Message msg) {

        // 拿到下载结果
        String fileName = msg.getData().getString(KEY_FILENAME);
        int downloadResult = msg.getData().getInt(KEY_DOWNLOAD_RESULT);

        switch (downloadResult) {
            case FLAG_CANCEL_UPDATE: // 取消下载
                ToastUtils.showShort(mContext, R.string.tip_cancelupdate);
                break;
            case FLAG_DOWNLOAD_ERROR: // 下载错误
                ToastUtils.showShort(mContext, R.string.tip_update_error);
                break;
            case FLAG_NO_ENOUGH_SPACE: // 空间不足
                ToastUtils.showShort(mContext, R.string.tip_no_enough_space);
                break;
            case FLAG_DOWNLOAD_SUCCESS: // 下载成功
                ToastUtils.showShort(mContext, R.string.tip_downloadsuccess);
                break;
        }

        progressDialog.dismiss();

        if (updateService != null) {
            unBindService();
        }
    }

}
