package com.jack.core.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * 相比于RxJava的ThrottleLast操作符，更加flex，可以动态改变ThrottleLast的时间值
 */
public class ThrottleLastHandler<T> implements Closeable {
    private static final String TAG = "Util.ThrottleLast";

    private final ThrottleLastDelayFactory factory;
    private final OnThrottleLastResult<T> onThrottleLastResult;
    private final Handler handler;

    private static final int THROTTLE_LAST_SYNC_ID = 10000;
    private T current;
    private boolean isPeriodFirstSync = true;
    private long periodDispatchUptimeMillis;
    private long lastSyncResultUptimeMillis;

    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == THROTTLE_LAST_SYNC_ID) {
                final long l = SystemClock.uptimeMillis();
                if (l >= periodDispatchUptimeMillis && !isPeriodFirstSync) {
                    isPeriodFirstSync = true;
                    lastSyncResultUptimeMillis = l;
                    onThrottleLastResult.onThrottleLast(current);
                    Log.i(TAG, String.format("sync result uptime millis [%d]ms", lastSyncResultUptimeMillis));
                } else {
                    Log.i(TAG, l + " => ignore to send");
                }

                return true;    // do not future dispatch;
            }

            return false;
        }
    };

    public ThrottleLastHandler(Looper looper, ThrottleLastDelayFactory factory,
                               OnThrottleLastResult<T> onThrottleLastResult) {
        this.handler = new Handler(looper, callback);
        this.factory = factory;
        this.onThrottleLastResult = onThrottleLastResult;
        handler.removeMessages(THROTTLE_LAST_SYNC_ID);
    }

    /**
     * 接收信号
     */
    public void onSync(T object) {
        this.current = object;
        final int delayTime = Math.max(factory.delayInMill(), 0);
        final long currentUpTimeMillis = SystemClock.uptimeMillis();
        final long futureUptimeMillis = currentUpTimeMillis + delayTime;

        if (isPeriodFirstSync) {
            Log.i(TAG, String.format("receive period first msg, uptimeMillis: %d, delayTime: %d", currentUpTimeMillis, delayTime));
            isPeriodFirstSync = false;
            periodDispatchUptimeMillis = futureUptimeMillis;
        }
        handler.sendEmptyMessageAtTime(THROTTLE_LAST_SYNC_ID, futureUptimeMillis);
    }

    @Override
    public void close() throws IOException {
        handler.removeMessages(THROTTLE_LAST_SYNC_ID);
    }

    public interface ThrottleLastDelayFactory {
        int delayInMill();
    }

    public interface OnThrottleLastResult<T> {
        void onThrottleLast(T t);
    }
}
