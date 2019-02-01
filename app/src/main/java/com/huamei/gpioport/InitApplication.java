package com.huamei.gpioport;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.huamei.gpioport.volley.BaseRequest;
import com.xuhao.android.libsocket.sdk.OkSocket;

import java.util.ArrayList;

/**
 * Created by kqw on 2016/10/26.
 * InitApplication
 */

public class InitApplication extends Application {
    public static Context applicationContext;
    private static InitApplication instance;
    private ArrayList<Activity> list = new ArrayList<Activity>();

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        instance = this;
        OkSocket.initialize(this, true);
        UnCeHandler catchExcep = new UnCeHandler(this);
        Thread.setDefaultUncaughtExceptionHandler(catchExcep);
    }

    public static InitApplication getInstance() {
        return instance;
    }

    public boolean isDebugMode() {
        ApplicationInfo info = getApplicationInfo();
        return (0 != ((info.flags) & ApplicationInfo.FLAG_DEBUGGABLE));
    }


    public void removeActivity(Activity a) {
        list.remove(a);
    }

    /**
     * 向Activity列表中添加Activity对象
     */
    public void addActivity(Activity a) {
        list.add(a);
    }

    /**
     * 关闭Activity列表中的所有Activity
     */
    public void finishActivity() {
        for (Activity activity : list) {
            if (null != activity) {
                activity.finish();
            }
        }
        //杀死该应用进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public RequestQueue mQueue;
    private DefaultRetryPolicy mPolicy;

    /**
     * 将请求加入队列并执行，默认不进行请求缓存，默认6秒超时，retry一次
     *
     * @param what
     * @param request
     * @param tag
     */
    public void addRequestQueue(int what, BaseRequest<?> request, Object tag) {
        addRequestQueue(what, request, false, tag);
    }

    /**
     * 默认6秒超时，retry一次
     *
     * @param what
     * @param request
     * @param shouldCache
     * @param tag
     */
    public void addRequestQueue(int what, BaseRequest<?> request,
                                boolean shouldCache, Object tag) {
        addRequestQueue(what, request, shouldCache, 8 * 1000, true, tag);
    }

    public void addRequestQueue(int what, BaseRequest<?> request,
                                boolean shouldCache, int initialTimeoutMs, boolean retry, Object tag) {
        if (mQueue == null) {
            mQueue = Volley.newRequestQueue(this);
        }
        if (mPolicy == null) {
            mPolicy = new DefaultRetryPolicy(initialTimeoutMs, retry ? 1 : 0,
                    1.0f);
        }
//        request.putHeader(getMap());
        request.setRetryPolicy(mPolicy);
        request.setWhat(what);
        request.setShouldCache(shouldCache);
        if (tag != null)
            request.setTag(tag);
        mQueue.add(request);
    }
}
