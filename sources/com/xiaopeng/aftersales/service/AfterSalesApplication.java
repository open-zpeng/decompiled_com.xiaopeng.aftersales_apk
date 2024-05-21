package com.xiaopeng.aftersales.service;

import android.app.Application;
/* loaded from: classes.dex */
public class AfterSalesApplication extends Application {
    private static AfterSalesApplication sInstance;

    @Override // android.app.Application
    public void onCreate() {
        sInstance = this;
        super.onCreate();
    }

    @Override // android.app.Application
    public void onTerminate() {
        super.onTerminate();
    }

    public static AfterSalesApplication getInstance() {
        return sInstance;
    }
}
