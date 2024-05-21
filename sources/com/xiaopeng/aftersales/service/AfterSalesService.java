package com.xiaopeng.aftersales.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class AfterSalesService extends Service {
    private static final String TAG = "AfterSalesService";
    private AfterSalesImpl mAfterSalesImpl;

    @Override // android.app.Service
    public void onCreate() {
        Slog.i(TAG, "AfterSalesService onCreate");
        this.mAfterSalesImpl = AfterSalesImpl.getInstance(this);
        super.onCreate();
    }

    @Override // android.app.Service
    public void onDestroy() {
        Slog.i(TAG, "AfterSalesService onDestroy");
        AfterSalesImpl.releaseInstance();
        super.onDestroy();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 1;
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.mAfterSalesImpl;
    }

    @Override // android.app.Service
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("*dump AfterSalesService*");
        writer.println("*dump HAL*");
        writer.println("*dump services*");
        AfterSalesImpl.getInstance(this).dump(writer);
    }
}
