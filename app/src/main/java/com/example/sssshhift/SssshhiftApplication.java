package com.example.sssshhift;

import android.app.Application;
import android.util.Log;
import androidx.work.Configuration;
import androidx.work.WorkManager;

public class SssshhiftApplication extends Application implements Configuration.Provider {
    private static final String TAG = "SssshhiftApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Initialize WorkManager with the configuration
            WorkManager.initialize(this, getWorkManagerConfiguration());
            Log.d(TAG, "WorkManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing WorkManager: " + e.getMessage(), e);
        }
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build();
    }
} 