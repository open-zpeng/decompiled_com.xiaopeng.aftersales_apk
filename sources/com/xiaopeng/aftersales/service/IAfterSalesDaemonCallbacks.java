package com.xiaopeng.aftersales.service;
/* loaded from: classes.dex */
interface IAfterSalesDaemonCallbacks {
    void onDaemonConnected();

    void onEvent(AfterSalesDaemonEvent afterSalesDaemonEvent);
}
