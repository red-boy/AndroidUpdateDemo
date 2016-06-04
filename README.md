# Android课程-App更新策略
@(Android)



# 第一节 课程介绍
**概述**

App更新是应用当中很常见的一个功能，基本上联网的app都应该具备这样的功能，对于更新迭代比较快速的产品，应用更新升级能让用户以最快的速度体验到最新功能和特性，对提高拉新留存率有不错的效果。本次课程来教会大家如何去实现这样的一个功能，分析具体的应用场景和技术，来让开发者满足产品的需求。

**课程目标**

学会如何通过AsyncHttp这样的框架来完成API网络请求
学会如何创建并绑定一个service
学会如何通过HttpURLConnection来下载文件
学会如何通过Handler来更新进度 5. 学会如何操作Notification
完整实现一个app更新需求

**课程大纲**

第一节：课程介绍
第二节：检查更新实现
第三节：定义Service&绑定后台服务
第四节：实现进度更新
第五节：实现通知栏提醒&进度更新
第六节：测试&总结

**最后**

本次课程面向初学者，主要目的是想让他们了解App中更新功能模块是如何实现的，通过本次课程实现的Demo方便童鞋们后面去扩展，主要还是针对使用场景来进行调整，如果想观看视频课程，后续公众号会送上，课程再录制当中，谢谢。

# 第二节 检查更新实现



这节课来正式讲解App更新策略的第一部分，**检查更新实现**，本节课来教大家如何集成android-async-http这个比较优秀的网络库，由于Android6.0已经抛弃使用HttpClient这个类，所以集成这个网络库的时候一定要选择最新的版本，这里是1.4.9，如果大家以后想换成其他网络库，比如Volley，Okhttp再或者是retrofit这样比较流行的Http网络请求库，只要针对相应的方法替换成新的网络库的方法就行了。

我们来看如何实现检查更新，说白了就是通过调用服务端给我们提供的API接口，我们通过网络库发送HTTP GET的请求，返回服务端的接口数据，并对它进行解析，这里一般是JSON格式的数据。

**定义接口地址常量**
```
 // 这里替换你的服务器域名
    public static final String DOMAIN = "http://www.infzm.com/";
    // url, 检查更新API
    public static final String CHECK_UPDATE = DOMAIN
            + "mobilesource/goal/app.config.php";
```

**定义请求方法**

```java
	/**
	 * 检查更新
	 * 
	 * @param params
	 * @param responseHandler
	 */
	public RequestHandle checkUpdate(RequestParams params,
									 AsyncHttpResponseHandler responseHandler) {
		LogUtils.e("checkUpdate", URLHelper.CHECK_UPDATE + "?" + params);
		return client.get(URLHelper.CHECK_UPDATE, params, responseHandler);
```

**定义回调方法**

```java
package com.devilwwj.updateapkdemo.update;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.devilwwj.updateapkdemo.GlobalSettings;
import com.devilwwj.updateapkdemo.R;
import com.devilwwj.updateapkdemo.utils.Utils;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * 检查更新的句柄
 * @author wwj_748
 *
 */
public class CheckUpdateAsyncHandler extends JsonHttpResponseHandler{
	
	private Context mContext;
	
	private ProgressDialog progressDialog;
	
	OnCheckUpdateListener checkUpdateListener;
	
	interface OnCheckUpdateListener {
		void onSuccess(UpdateInfo updateInfo);
		void onFailure();
	}
	
	public CheckUpdateAsyncHandler(Context context, OnCheckUpdateListener onCheckUpdateListener) {
		this.mContext = context;
		this.checkUpdateListener = onCheckUpdateListener;
		progressDialog = new ProgressDialog(mContext);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage(mContext.getString(R.string.tip_requesting));
		progressDialog.setCancelable(true);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		progressDialog.show();
	}
	
	@Override
	public void onFailure(int statusCode, Header[] headers,
			Throwable throwable, JSONObject errorResponse) {
		super.onFailure(statusCode, headers, throwable, errorResponse);
		Map<String, Object> versionInfoMap = GlobalSettings
				.getNewVersionInfo(mContext);

		boolean canUpdate = (Boolean) versionInfoMap.get("updatable");

		if (canUpdate) {
				progressDialog.dismiss();
				Toast.makeText(mContext, "连接失败，请稍后重试",
						Toast.LENGTH_SHORT).show();
		} else {
				progressDialog.dismiss();
		}
		
		checkUpdateListener.onFailure();
	}
	
	
	@Override
	public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
		super.onSuccess(statusCode, headers, response);
		Log.d("updateInfo", response.toString());
		
		try {
			JSONObject firstObject = (JSONObject) response.get("android_ireader");
			
			// 最新版本号
			String versionName = firstObject.optString("app_version");
			// 版本号
			int versionCode = firstObject.optInt("version_code");
			versionCode = 47;
			
			// 更新内容
			String features = firstObject.getString("features");
			// sdk版本
			String sdkVersion = firstObject.optString("sdk_version");
			// 系统版本
			String osVersion = firstObject.optString("os_version");
			
			// 获取当前版本号
			int currentVersionCode = Utils.getAppVersionCode(mContext);
			// apk下载地址
			String url = firstObject.optString("update_url");
			
			boolean isCanUpdate;
			// 获取上次强制更新的版本——last_force_update
			int last_force_update_version = firstObject.optInt("last_force_update_version");
			
			if (currentVersionCode < last_force_update_version) {
				isCanUpdate = true;
			} else {
				if (versionCode <= currentVersionCode) {
					isCanUpdate = false;
				} else {
					isCanUpdate = true;
				}
			}
			
			// 保存更新信息到sharedPerences
			GlobalSettings.saveNewVersionInfo(mContext, versionName, url, features, isCanUpdate);
			UpdateInfo updateInfo = new UpdateInfo();
			updateInfo.setVersionName(versionName);
			updateInfo.setVersionCode(versionCode);
			updateInfo.setFeatures(features);
			updateInfo.setSdkVersion(sdkVersion);
			updateInfo.setOsVersion(osVersion);
			updateInfo.setUpdateUrl(url);
			updateInfo.setLastForceUpdate(last_force_update_version);
			
			checkUpdateListener.onSuccess(updateInfo);
			
		} catch(JSONException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onFinish() {
		super.onFinish();
		progressDialog.dismiss();
	}
	
	@Override
	public void onCancel() {
		super.onCancel();
		progressDialog.cancel();
	}
}


```


**定义更新管理类**
```
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
        HttpRequestHelper.getInstance().checkUpdate(null, new CheckUpdateAsyncHandler(mContext))
    }

}


```

**调用检查更新方法**
```java
public class MainActivity extends AppCompatActivity {


    private Button btnCheckUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCheckUpdate = (Button) findViewById(R.id.btn_checkupdate);

        btnCheckUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 这里去检查更新
                AppUpdateManager.getInstance(MainActivity.this).checkUpdate();

            }
        });
    }



}

```


**最后**
本节课的内容基本上就这么多啦，代码大家可以[http://git.oschina.net/devilwwj/AndroidUpdateDemo](http://git.oschina.net/devilwwj/AndroidUpdateDemo)去下载，去下载，每一节课的代码都会上传到这个地方，代码会随着开发会有点变化，大家一定要跟上节奏哦。

如果大家想看视频的话可这里：[http://edu.csdn.net/course/detail/2139](http://edu.csdn.net/course/detail/2139)


# 第三节 定义更新Service

上节课给大家讲了，如何实现检查更新，通过Android-async-http这个网络库进行Http请求服务器API，响应返回更新信息。这节课来将如何通过绑定一个Service来启动一个服务，并通过它来下载我们的Apk文件。

首先我们创建一个后台服务跟我们的Activity相关联，这里取名AppUpdateService，因为我们是需要跟Activity绑定在一起，所以需要通过bindService的方式来启动服务，启动服务成功之后，则开启一个线程来下载apk，在下载的过程中，我们可以通过handler来更新提示我们下载的进度，这是本节课要实现的主要功能。

![绑定后台Service](http://img.blog.csdn.net/20160418224149693)

![后台线程下载apk](http://img.blog.csdn.net/20160418224239710)

![提示更新](http://img.blog.csdn.net/20160418224341976)

![这里写图片描述](http://img.blog.csdn.net/20160418224415150)

最新一节代码已经上传，可以到以下地址去下载：
http://git.oschina.net/devilwwj/AndroidUpdateDemo





# 第四节 实现进度更新


前面的课程已经实现了通过Http请求API检查更新，定义了后台下载服务实现了apk下载，如果没有看过前面的课程可以看以下博文：

[App更新策略课程-检查更新实现](http://blog.csdn.net/wwj_748/article/details/51137641)
[App更新策略课程-定义后台更新服务](http://blog.csdn.net/wwj_748/article/details/51184436)

配套视频地址：

[App更新策略视频](http://edu.csdn.net/course/detail/2139)

本节课主要是接着上面的课程继续完善App更新功能，这次主要是在后台根据下载进度，通过handler来发送消息的方式来更新进度条，具体的效果如下图所示：

![App进度更新](http://img.blog.csdn.net/20160502200355389)

我们要实现的效果就跟上面一样，点击更新之后，就弹出更新对话框不停的更新进度，进度达到100的时候，提示下载成功并消除对话框。

代码片段：

![这里写图片描述](http://img.blog.csdn.net/20160502201049273)

![这里写图片描述](http://img.blog.csdn.net/20160502201122165)

![这里写图片描述](http://img.blog.csdn.net/20160502201243295)


![这里写图片描述](http://img.blog.csdn.net/20160502201328105)

代码已经上传，大家可以down下来:
[http://git.oschina.net/devilwwj/AndroidUpdateDemo](http://git.oschina.net/devilwwj/AndroidUpdateDemo)




# 第五节 实现通知栏进度更新

上节课给大家介绍了如何实现下载进度更新，本节课将会给大家介绍如何实现通知栏消息提醒和通知栏的下载进度更新，如果还没有学习前面的课程的同学可以先学习前面内容：

[App更新策略课程-检查更新实现](http://blog.csdn.net/wwj_748/article/details/51137641)
[App更新策略课程-定义后台更新服务](http://blog.csdn.net/wwj_748/article/details/51184436)
[App更新策略课程-实现进度更新](http://blog.csdn.net/wwj_748/article/details/51298060)

配套视频教程：
[App更新策略课程](http://edu.csdn.net/course/detail/2139)

先来看一下我们本节课要实现的效果：

![通知栏进度更新](http://img.blog.csdn.net/20160604102416079)

![下载完毕](http://img.blog.csdn.net/20160604102449986)

上一节我们实现了在前台时进度条更新，现在我们考虑一个场景：
>1.  用户按了Home键回到后台，这个时候用户是看不到更新进度的，因为当前Activity已经不可见了，但是更新下载还在后台服务进行着，那么我们就应该在通知栏显示我们的进度更新，让用户感知到我们的更新是正在进行的。
> 2. 当用户想回到前台，则可以通过点击通知栏消息跳转回前台界面，继续完成更新进度。

上面是我们初步的需求，下面来看具体实现：
代码位置：**AppUpdateService.java**

**获取系统通知服务**

![获取通知服务](http://img.blog.csdn.net/20160604103531599)

**构建Notification**

![初始化Notification](http://img.blog.csdn.net/20160604102237235)

通过NotificationCompat.Builder创建一个builder对象来构建Notification，设置标题、icon、是否自动cancel、自定义通知栏视图、设置PendingIntent。


**更新通知栏进度**

![更新通知栏进度](http://img.blog.csdn.net/20160604103736925)

在代码上可以看到这样的逻辑，如果当前activity在后台时，在消息通知栏显示下载进度，通过获取Notification的contentVew来设置相应的内容，并通过NotificationManager来显示通知。

基本在通知栏显示的逻辑就这样，我们下载成功之后还有显示结果消息通知：
代码位置：AppUpdateManager.java

![下载结果通知](http://img.blog.csdn.net/20160604104700610)

这里通过不同的flag在通知栏显示不同的消息，具体实现这里就不再截图，大家可以去看看代码：

http://git.oschina.net/devilwwj/AndroidUpdateDemo

本节课的内容就这么多，如果有任何疑问可以直接留言，欢迎拍砖。


# 第六节 测试&总结

App更新策略课程一共有六节，本节是最后一节，将会给大家总结一下我们App更新的时候有那些常见场景，并依据这些场景简单编写一下测试用例，还将会和大家探讨一下app更新模块还可以继续扩展什么功能。如果没有看过前面的课程的同学可以先学习前面的课程：


[App更新策略课程-检查更新实现](http://blog.csdn.net/wwj_748/article/details/51137641)
[App更新策略课程-定义后台更新服务](http://blog.csdn.net/wwj_748/article/details/51184436)
[App更新策略课程-实现进度更新](http://blog.csdn.net/wwj_748/article/details/51298060)
[App更新策略课程-实现通知栏进度更新](http://blog.csdn.net/wwj_748/article/details/51583320)


配套视频教程：
[App更新策略课程](http://edu.csdn.net/course/detail/2139)

代码地址：
[http://git.oschina.net/devilwwj/AndroidUpdateDemo](http://git.oschina.net/devilwwj/AndroidUpdateDemo)


## 测试用例

这里我只是简单写了一下应用更新是最常出现的场景的测试用例，测试人员在测试这样的功能模块的时候，可能会考虑到更多的测试场景，这就需要我们不停的去优化测试流程去尽量满足真实的用户场景，比如考虑更多操作失败时的场景，例如网络切换时是否提醒用户网络已经被切换了、网络连接失败时是否提示用户、下载失败之后的提示，避免重复提醒用户更新等等。

![测试用例](http://img.blog.csdn.net/20160604174438366)

本次课程只提供一个初步的解决方案，让没有这方面经验的同学能明白app更新策略是如何实现的，并没有做到完全适用于用户的各种应用场景，这需要同学们自己在实际开发中根据需求去优化这个解决方案。

## 效果图

![更新对话框](http://img.blog.csdn.net/20160604182531272)

![更新进度](http://img.blog.csdn.net/20160604182705538)

## 总结

**梳理流程图**

![更新流程图](http://img.blog.csdn.net/20160604175301180)

这个是最简单的更新流程图，更新的流程大致都是类似的，我们首先会去请求服务端获取更新信息，这个是**检查更新**的过程。

一般情况我们会拿到相应的json数据，举个例子：
![更新信息](http://img.blog.csdn.net/20160604175713665)

我们通过这样的json数据，拿到具体的更新信息，这里我们关注一下**version_code**，我们通过比对本地的versionCode跟服务端返回的versionCode，如果前者大，则无更新内容，如果后者大，则提示用户更新下载；

检查到有更新之后，就给用户弹窗通知用户下载，点击『取消』就不更新，点击『确定』则启动后台服务异步下载apk，下载成功之后如果在前台，我们可以直接跳转到安装界面完成安装；如果在后台，下载成功之后会在通知栏显示下载结果，点击通知栏消息也可以跳转到安装界面。具体的操作流程可以根据你们的需求来调整，不一定要按照我这里的操作流程。

完成安装之后，我们整个的app更新策略就完成了。那么我们还可以做些什么功能？如果只是简单的实现更新，还不算特别复杂，就怕我们伟大的产品经理提出伟（keng）大（die）的想法，比如要求app具备以下功能：

1. 强制更新（流氓行为，用户可能会骂街）
2. 断点续传（操作流程又会复杂很多）
3. 增量更新（百度一下是啥吧）

这绝对有可能有这样的需求，这里留给同学们自己私下去实现，本课程就不继续讲解这方面的内容。

好啦，关于App更新策略课程已经完结，大家可以看到只是简单的一个更新模块，就需要我们考虑很多问题，如果让你自己去实现你会怎么做，希望本套课程能给同学们带来一定的帮助，毕竟更新升级是每个应用最基本的模块之一，最后谢谢同学们的耐心观看。

