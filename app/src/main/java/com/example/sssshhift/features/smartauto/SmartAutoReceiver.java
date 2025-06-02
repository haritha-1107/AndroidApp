package com.example.sssshhift.features.smartauto;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import androidx.preference.PreferenceManager;

import java.util.Date;

public class SmartAutoReceiver extends BroadcastReceiver {
    private static final String TAG = "SmartAutoReceiver";
    private static final String WAKE_LOCK_TAG = "com.example.sssshhift:SmartAutoWakeLock";
    private static final int WAKE_LOCK_TIMEOUT = 30000; // 30 seconds

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        );
        
        // Acquire wake lock with timeout
        wakeLock.acquire(WAKE_LOCK_TIMEOUT);

        try {
            if (intent == null) {
                Log.e(TAG, "Received null intent");
                return;
            }

            String action = intent.getAction();
            Log.d(TAG, "Received action: " + action);

            // Handle system events
            if (action != null && (
                action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                action.equals(Intent.ACTION_TIMEZONE_CHANGED) ||
                action.equals(Intent.ACTION_TIME_CHANGED))) {
                handleSystemEvent(context);
                return;
            }

            // Verify we have the required extras
            if (!intent.hasExtra("event_start") || !intent.hasExtra("alarm_type")) {
                Log.e(TAG, "Missing required extras in intent");
                return;
            }

            long eventStart = intent.getLongExtra("event_start", 0);
            long eventEnd = intent.getLongExtra("event_end", 0);
            boolean toSilent = intent.getBooleanExtra("to_silent", false);
            String alarmType = intent.getStringExtra("alarm_type");
            long triggerTime = intent.getLongExtra("trigger_time", 0);

            Log.d(TAG, String.format("Processing alarm: type=%s, eventStart=%s, eventEnd=%s, toSilent=%b, triggerTime=%s",
                    alarmType,
                    new Date(eventStart),
                    new Date(eventEnd),
                    toSilent,
                    new Date(triggerTime)));

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            long currentTime = System.currentTimeMillis();

            // Verify the feature is still enabled
            if (!prefs.getBoolean("auto_mode_enabled", false)) {
                Log.d(TAG, "Smart Auto Mode is disabled, ignoring alarm");
                return;
            }

            // Check if we have DND permission
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                (notificationManager == null || !notificationManager.isNotificationPolicyAccessGranted())) {
                Log.e(TAG, "DND permission not granted, cannot change ringer mode");
                return;
            }

            // Handle different alarm types
            switch (alarmType) {
                case "ACTIVATE_PRIMARY":
                case "ACTIVATE_BACKUP_1":
                case "ACTIVATE_BACKUP_2":
                    if (currentTime < eventEnd && !SmartAutoAlarmManager.isInSilentMode(context)) {
                        Log.d(TAG, "Activating silent mode for event starting at " + new Date(eventStart));
                        SmartAutoAlarmManager.changeRingerMode(context, true, eventStart);
                    } else {
                        Log.d(TAG, "Skipping activation - event ended or already in silent mode");
                    }
                    break;

                case "REVERT_PRIMARY":
                case "REVERT_BACKUP_1":
                case "REVERT_BACKUP_2":
                case "REVERT_BACKUP_3":
                    if (SmartAutoAlarmManager.shouldRevertRingerMode(context, eventStart)) {
                        Log.d(TAG, "Reverting from silent mode at " + new Date(currentTime));
                        SmartAutoAlarmManager.changeRingerMode(context, false, eventStart);
                    } else {
                        Log.d(TAG, "Skipping revert - other events still active");
                    }
                    break;

                case "FINAL_CLEANUP":
                    Log.d(TAG, "Performing final cleanup for event");
                    if (SmartAutoAlarmManager.shouldRevertRingerMode(context, eventStart) && 
                        SmartAutoAlarmManager.isInSilentMode(context)) {
                        Log.d(TAG, "Final cleanup - forcing revert from silent mode");
                        SmartAutoAlarmManager.changeRingerMode(context, false, eventStart);
                    }
                    SmartAutoAlarmManager.cleanupRingerModePreference(context, eventStart);
                    // Schedule next work to ensure continuous monitoring
                    SmartAutoWorker.scheduleWork(context);
                    break;

                default:
                    Log.w(TAG, "Unknown alarm type: " + alarmType);
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing alarm: " + e.getMessage(), e);
            e.printStackTrace();
        } finally {
            try {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                    Log.d(TAG, "Wake lock released");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error releasing wake lock", e);
            }
        }
    }

    private void handleSystemEvent(Context context) {
        Log.d(TAG, "Handling system event - rescheduling alarms");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("auto_mode_enabled", false)) {
            SmartAutoAlarmManager.rescheduleEventsAfterBoot(context);
            SmartAutoWorker.scheduleWork(context);
            Log.d(TAG, "Successfully rescheduled alarms after system event");
        } else {
            Log.d(TAG, "Smart Auto Mode is disabled, skipping reschedule");
        }
    }
}