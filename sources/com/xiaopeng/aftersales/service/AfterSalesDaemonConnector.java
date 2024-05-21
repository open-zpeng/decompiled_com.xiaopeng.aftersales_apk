package com.xiaopeng.aftersales.service;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AfterSalesDaemonConnector implements Runnable, Handler.Callback {
    private static final int FACTORY_BUILD_SPECIAL = 6;
    private static final int MSG_WHAT_SOCKET_READ = 1;
    private static final String TAG = "AfterSalesDaemonConnector";
    private static final String XP_AFTERSALES_LOCAL_SOCKET = "xpaftersalesd";
    private final int BUFFER_SIZE;
    private Handler mBackgroundHandler;
    private volatile HandlerThread mBackgroundThread;
    private BufferedReader mBufferedReader;
    private Handler mCallbackHandler;
    private IAfterSalesDaemonCallbacks mCallbacks;
    private final Object mDaemonLock;
    private final Looper mLooper;
    private OutputStream mOutputStream;
    private LocalSocket mSocket;
    private static final String BUILD_SPECIAL_PROPERTIES = "ro.xiaopeng.special";
    private static final int BUILD_SPECIAL = SystemProperties.getInt(BUILD_SPECIAL_PROPERTIES, 0);

    /* JADX INFO: Access modifiers changed from: package-private */
    public AfterSalesDaemonConnector(IAfterSalesDaemonCallbacks callbacks) {
        this(callbacks, null);
    }

    AfterSalesDaemonConnector(IAfterSalesDaemonCallbacks callbacks, Looper looper) {
        this.mDaemonLock = new Object();
        this.BUFFER_SIZE = 4096;
        if (looper == null) {
            this.mBackgroundThread = new HandlerThread("AftersalesBackgroundHandler", 10);
            this.mBackgroundThread.start();
            this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
            this.mLooper = this.mBackgroundThread.getLooper();
        } else {
            this.mLooper = looper;
        }
        this.mCallbacks = callbacks;
    }

    public void release() {
    }

    @Override // java.lang.Runnable
    public void run() {
        this.mCallbackHandler = new Handler(this.mLooper, this);
        while (true) {
            try {
            } catch (Exception e) {
                Slog.e(TAG, "Error in NativeDaemonConnector: " + e);
                SystemClock.sleep(5000L);
            }
            if (BUILD_SPECIAL == 6) {
                Slog.i(TAG, "This is factory bin");
                return;
            }
            listenToSocket();
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        AfterSalesDaemonEvent event = (AfterSalesDaemonEvent) msg.obj;
        try {
            this.mCallbacks.onEvent(event);
            return true;
        } catch (Exception e) {
            Slog.e(TAG, "Error handling '" + event + "': " + e);
            return true;
        }
    }

    private void listenToSocket() throws IOException {
        try {
            try {
                this.mSocket = new LocalSocket();
                LocalSocketAddress address = new LocalSocketAddress(XP_AFTERSALES_LOCAL_SOCKET, LocalSocketAddress.Namespace.RESERVED);
                this.mSocket.connect(address);
                this.mBufferedReader = new BufferedReader(new InputStreamReader(this.mSocket.getInputStream()));
                synchronized (this.mDaemonLock) {
                    this.mOutputStream = this.mSocket.getOutputStream();
                }
                this.mCallbacks.onDaemonConnected();
                while (true) {
                    String rawEvent = this.mBufferedReader.readLine();
                    Slog.d(TAG, "readLine rawEvent : " + rawEvent);
                    AfterSalesDaemonEvent event = AfterSalesDaemonEvent.parseRawEvent(rawEvent);
                    if (event != null && (AfterSalesDaemonEvent.XP_AFTERSALES_RESPONSE.equalsIgnoreCase(event.getFlag()) || AfterSalesDaemonEvent.XP_AFTERSALES_REQ.equalsIgnoreCase(event.getFlag()))) {
                        Message msg = this.mCallbackHandler.obtainMessage(1, event);
                        if (!this.mCallbackHandler.sendMessage(msg)) {
                            Slog.e(TAG, "mCallbackHandler.sendMessage fail");
                        }
                    }
                }
            } catch (IOException ex) {
                Slog.e(TAG, "Communications error: " + ex);
                throw ex;
            }
        } catch (Throwable th) {
            synchronized (this.mDaemonLock) {
                if (this.mOutputStream != null) {
                    try {
                        Slog.e(TAG, "closing stream ");
                        this.mOutputStream.close();
                    } catch (IOException e) {
                        Slog.e(TAG, "Failed closing output stream: " + e);
                    }
                    this.mOutputStream = null;
                }
                try {
                    if (this.mSocket != null) {
                        this.mSocket.close();
                    }
                } catch (IOException ex2) {
                    Slog.e(TAG, "Failed closing mSocket: " + ex2);
                }
                throw th;
            }
        }
    }

    public void waitForCallbacks() {
        if (Thread.currentThread() == this.mLooper.getThread()) {
            throw new IllegalStateException("Must not call this method on callback thread");
        }
        final CountDownLatch latch = new CountDownLatch(1);
        this.mCallbackHandler.post(new Runnable() { // from class: com.xiaopeng.aftersales.service.AfterSalesDaemonConnector.1
            @Override // java.lang.Runnable
            public void run() {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Slog.wtf(TAG, "Interrupted while waiting for unsolicited response handling", e);
        }
    }

    public boolean execute(int cmd, int action, Object... args) {
        boolean res = false;
        AfterSalesDaemonEvent event = new AfterSalesDaemonEvent(AfterSalesDaemonEvent.XP_AFTERSALES_REQ, cmd, action, args);
        synchronized (this.mDaemonLock) {
            if (this.mOutputStream == null) {
                Slog.e(TAG, "missing output stream");
            } else {
                try {
                    this.mOutputStream.write(event.toRawEvent().getBytes(StandardCharsets.UTF_8));
                    res = true;
                    Slog.d(TAG, "write succeed: " + event.toRawEvent());
                } catch (IOException e) {
                    Slog.e(TAG, "problem sending command:" + e);
                }
            }
        }
        return res;
    }
}
