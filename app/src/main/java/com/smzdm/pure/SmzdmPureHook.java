package com.smzdm.pure;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class SmzdmPureHook extends XposedModule {
    public static final String TAG = "SMZDMPure";
    private static final String TARGET_PKG = "com.smzdm.client.android";
    private static final AtomicBoolean BASE_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean APP_HOOKED = new AtomicBoolean(false);

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        Log.i(TAG, "SMZDMPurify API 102 module loaded.");
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET_PKG.equals(param.getPackageName())) {
            return;
        }
        Log.i(TAG, "SMZDMPurify onPackageReady: " + param.getPackageName());
        installBaseHooks(this, param.getClassLoader());
        tryInstallAppHooks(this, param.getClassLoader());
    }

    private static void installBaseHooks(XposedInterface xposed, ClassLoader cl) {
        if (!BASE_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method appOnCreate = Application.class.getDeclaredMethod("onCreate");
            xposed.hook(appOnCreate).intercept(chain -> {
                Object app = chain.getThisObject();
                if (app instanceof Application) {
                    tryInstallAppHooks(xposed, ((Application) app).getClassLoader());
                }
                return chain.proceed();
            });

            Method actOnCreate = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
            xposed.hook(actOnCreate).intercept(chain -> {
                Object act = chain.getThisObject();
                if (act instanceof Activity) {
                    tryInstallAppHooks(xposed, act.getClass().getClassLoader());
                }
                return chain.proceed();
            });

            Method actOnResume = Activity.class.getDeclaredMethod("onResume");
            xposed.hook(actOnResume).intercept(chain -> {
                Object res = chain.proceed();
                Object act = chain.getThisObject();
                if (act instanceof Activity) {
                    Activity activity = (Activity) act;
                    String actName = activity.getClass().getName();
                    if ("com.smzdm.client.android.app.HomeActivity".equals(actName)) {
                        SmzdmLayoutPurifier.purifyHomeActivity(activity);
                    } else if (activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
                        SmzdmLayoutPurifier.purgeViewTree(activity.getWindow().getDecorView());
                    }
                }
                return res;
            });
        } catch (Throwable t) {
            Log.e(TAG, "installBaseHooks failed: " + t);
        }
    }

    static void tryInstallAppHooks(XposedInterface xposed, ClassLoader cl) {
        if (cl == null || APP_HOOKED.get()) {
            return;
        }
        try {
            if (hasSmzdmClasses(cl) && APP_HOOKED.compareAndSet(false, true)) {
                SmzdmAdPurifier.install(xposed, cl);
                SmzdmLayoutPurifier.install(xposed, cl);
                SmzdmCommentFilter.install(xposed, cl);
                Log.i(TAG, "All SMZDMPurify hooks installed on ClassLoader: " + cl);
            }
        } catch (Throwable t) {
            Log.e(TAG, "tryInstallAppHooks failed: " + t);
        }
    }

    private static boolean hasSmzdmClasses(ClassLoader cl) {
        return ReflectUtils.findClass("com.smzdm.client.android.app.HomeActivity", cl) != null
                || ReflectUtils.findClass("com.smzdm.client.android.app.home.newstyle.MainActivity", cl) != null
                || ReflectUtils.findClass("com.smzdm.client.android.app.home.M", cl) != null
                || ReflectUtils.findClass("T5.ViewOnClickListenerC0576A", cl) != null;
    }
}
