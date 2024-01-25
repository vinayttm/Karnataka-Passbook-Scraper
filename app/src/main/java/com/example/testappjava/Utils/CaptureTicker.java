package com.example.testappjava.Utils;

import android.os.Handler;
import android.util.Log;

public class CaptureTicker {
    private final Handler handler = new Handler();
    private static final long CHECK_INTERVAL = 4000; // Check interval in milliseconds
    private final Runnable checkRunnable;

    public CaptureTicker(final Runnable callback) {
        checkRunnable = () -> {
            Log.d("Ticker", "Ticker is idle");
            callback.run();
        };
    }

    public void startChecking() {
        handler.postDelayed(checkRunnable, CHECK_INTERVAL);
    }

    public void removeCallback() {
        handler.removeCallbacks(checkRunnable);
    }

    public void setNotIdle() {
        removeCallback();
        handler.postDelayed(checkRunnable, CHECK_INTERVAL);
    }
}


