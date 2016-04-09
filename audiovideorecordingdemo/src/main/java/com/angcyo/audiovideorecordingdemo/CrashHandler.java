package com.angcyo.audiovideorecordingdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

public class CrashHandler implements UncaughtExceptionHandler {

    public static final String INTENT_ACTION_RESTART_ACTIVITY = "com.dudu.crash";

    public static final String TAG = "CrashHandler";

    private static CrashHandler mInstance;

    private Context mContext;

    private UncaughtExceptionHandler mDefaultHandler;

    private CrashHandler() {

    }

    public static CrashHandler getInstance() {
        if (mInstance == null) {
            mInstance = new CrashHandler();
        }

        return mInstance;
    }

    private static void killCurrentProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    public static Class<? extends Activity> getLauncherActivity(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            try {
                return (Class<? extends Activity>) Class.forName(intent.getComponent().getClassName());
            } catch (ClassNotFoundException e) {
            }
        }
        return null;
    }

    private static Class<? extends Activity> getRestartActivityClassWithIntentFilter(Context context) {
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(
                new Intent().setAction(INTENT_ACTION_RESTART_ACTIVITY),
                PackageManager.GET_RESOLVED_FILTER);

        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo.packageName.equalsIgnoreCase(context.getPackageName())) {
                try {
                    return (Class<? extends Activity>) Class.forName(info.activityInfo.name);
                } catch (ClassNotFoundException e) {
                    //Should not happen, print it to the log!
                    Log.e("TAG", "Failed when resolving the restart activity class via intent filter, stack trace follows!", e);
                }
            }
        }

        return null;
    }

    public void restartApplicationWithIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
        killCurrentProcess();
    }

    private void restartApp() {
        restartApplicationWithIntent(new Intent(mContext, CrashHandler.getLauncherActivity(mContext)));
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
//        try {
//            Class<? extends Activity> restartClass = getRestartActivityClassWithIntentFilter(mContext);
//            if (restartClass != null) {
//                Intent intent = new Intent(mContext, restartClass);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//去掉动画效果
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                Bundle args = new Bundle();
//                args.putString("msg", getMsgFromThrowable(ex));
//                intent.putExtras(args);
//                mContext.startActivity(intent);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


//        getRestartActivityClassWithIntentFilter(mContext);

        ex.printStackTrace();
        restartApp();

        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    private String getMsgFromThrowable(Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
