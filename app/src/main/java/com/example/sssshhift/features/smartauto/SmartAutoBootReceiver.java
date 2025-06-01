package com.example.sssshhift.features.smartauto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SmartAutoBootReceiver extends BroadcastReceiver {
    private static final String TAG = "SmartAutoBootReceiver";
    private static final String PREF_LAST_BOOT_TIME = "last_boot_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "Received null intent or action");
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isEnabled = prefs.getBoolean("auto_mode_enabled", false);
        
        // Handle various system events that might require rescheduling
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_REBOOT:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
            case Intent.ACTION_TIMEZONE_CHANGED:
            case Intent.ACTION_TIME_CHANGED:
                handleSystemEvent(context, prefs, action);
                break;
                
            case Intent.ACTION_PACKAGE_DATA_CLEARED:
                if (intent.getData() != null && 
                    intent.getData().getSchemeSpecificPart().equals(context.getPackageName())) {
                    Log.d(TAG, "App data cleared, resetting preferences");
                    resetPreferences(prefs);
                }
                break;
        }
    }

    private void handleSystemEvent(Context context, SharedPreferences prefs, String action) {
        long currentTime = System.currentTimeMillis();
        long lastBootTime = prefs.getLong(PREF_LAST_BOOT_TIME, 0);
        boolean isEnabled = prefs.getBoolean("auto_mode_enabled", false);

        // Prevent duplicate handling within short time window (30 seconds)
        if (currentTime - lastBootTime < 30000) {
            Log.d(TAG, "Skipping duplicate event handling within 30 seconds");
            return;
        }

        // Update last boot time
        prefs.edit().putLong(PREF_LAST_BOOT_TIME, currentTime).apply();

        if (isEnabled) {
            Log.d(TAG, "Smart Auto Mode is enabled, handling " + action);
            
            // Cancel any existing work first
            SmartAutoWorker.cancelWork(context);

            // Clean up any stale alarms
            cleanupStaleAlarms(context);

            // Schedule new work
            SmartAutoWorker.scheduleWork(context);

            Log.d(TAG, "Successfully rescheduled work after " + action);
        } else {
            Log.d(TAG, "Smart Auto Mode is disabled, not scheduling work");
        }
    }

    private void cleanupStaleAlarms(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long currentTime = System.currentTimeMillis();

        // Clean up active events
        Set<String> activeEvents = SmartAutoAlarmManager.getActiveEvents(context);
        if (!activeEvents.isEmpty()) {
            Set<String> updatedEvents = new HashSet<>();
            
            for (String eventKey : activeEvents) {
                try {
                    String[] parts = eventKey.split("_");
                    long eventEnd = Long.parseLong(parts[1]);
                    if (eventEnd >= currentTime) {
                        updatedEvents.add(eventKey);
                    } else {
                        Log.d(TAG, "Removing stale event: " + eventKey);
                        // Clean up associated preferences
                        SmartAutoAlarmManager.cleanupRingerModePreference(context, Long.parseLong(parts[0]));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing event key: " + eventKey, e);
                }
            }

            if (activeEvents.size() != updatedEvents.size()) {
                Log.d(TAG, "Cleaned up " + (activeEvents.size() - updatedEvents.size()) + " stale events");
                SmartAutoAlarmManager.updateActiveEvents(context, updatedEvents);
            }
        }
    }

    private void resetPreferences(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("auto_mode_enabled", false)
              .putStringSet("auto_mode_keywords", new HashSet<>(Arrays.asList("meeting", "team")))
              .putInt("auto_mode_pre_event_offset", 5)
              .putBoolean("auto_mode_revert_after_event", true)
              .putBoolean("auto_mode_busy_events_only", true)
              .apply();
        Log.d(TAG, "Reset preferences to defaults");
    }
} 