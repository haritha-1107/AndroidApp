package com.example.sssshhift.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import com.example.sssshhift.usage.data.UsageLog;
import com.example.sssshhift.usage.repository.UsageRepository;
import java.util.Date;

public class ModeTrackingService extends Service {
    private static final String TAG = "ModeTrackingService";
    private static final String ACTION_MODE_CHANGED = "com.example.sssshhift.MODE_CHANGED";
    private static final String EXTRA_MODE = "mode";
    
    private UsageRepository repository;
    private String currentMode;
    private Date currentModeStartTime;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new UsageRepository(getApplication());
        Log.d(TAG, "ModeTrackingService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_MODE_CHANGED.equals(intent.getAction())) {
            String newMode = intent.getStringExtra(EXTRA_MODE);
            handleModeChange(newMode);
        }
        return START_STICKY;
    }

    private void handleModeChange(String newMode) {
        if (newMode == null) return;

        Date now = new Date();
        Log.d(TAG, "Mode changed to: " + newMode);

        // Log previous mode duration if exists
        if (currentMode != null && currentModeStartTime != null) {
            long durationMinutes = (now.getTime() - currentModeStartTime.getTime()) / (60 * 1000);
            if (durationMinutes > 0) {
                UsageLog log = new UsageLog(currentMode, currentModeStartTime, now, durationMinutes);
                repository.insert(log);
                Log.d(TAG, String.format("Logged usage: %s mode for %d minutes", currentMode, durationMinutes));
            }
        }

        // Start tracking new mode
        currentMode = newMode;
        currentModeStartTime = now;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Log final session if exists
        if (currentMode != null && currentModeStartTime != null) {
            Date now = new Date();
            long durationMinutes = (now.getTime() - currentModeStartTime.getTime()) / (60 * 1000);
            if (durationMinutes > 0) {
                UsageLog log = new UsageLog(currentMode, currentModeStartTime, now, durationMinutes);
                repository.insert(log);
                Log.d(TAG, String.format("Final log: %s mode for %d minutes", currentMode, durationMinutes));
            }
        }
    }

    public static Intent createModeChangeIntent(String mode) {
        Intent intent = new Intent(ACTION_MODE_CHANGED);
        intent.putExtra(EXTRA_MODE, mode);
        return intent;
    }
} 