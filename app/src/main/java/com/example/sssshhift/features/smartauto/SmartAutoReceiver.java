package com.example.sssshhift.features.smartauto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;
import androidx.preference.PreferenceManager;

public class SmartAutoReceiver extends BroadcastReceiver {
    private static final String TAG = "SmartAutoReceiver";
    private static final String WAKE_LOCK_TAG = "com.example.sssshhift:SmartAutoWakeLock";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Acquire wake lock to ensure we complete our work
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
        );
        wakeLock.acquire(60000); // 60 seconds timeout

        try {
            if (intent == null || !intent.hasExtra("event_start")) {
                Log.e(TAG, "Received invalid intent");
                return;
            }

            long eventStart = intent.getLongExtra("event_start", 0);
            boolean toSilent = intent.getBooleanExtra("to_silent", false);
            String alarmType = intent.getStringExtra("alarm_type");

            Log.d(TAG, String.format("Received alarm: type=%s, eventStart=%d, toSilent=%b",
                    alarmType, eventStart, toSilent));

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            long eventEnd = prefs.getLong("event_end_time_" + eventStart, 0);
            long currentTime = System.currentTimeMillis();

            // Handle different alarm types
            switch (alarmType) {
                case "ACTIVATE_SILENT":
                    if (currentTime < eventEnd) {
                        SmartAutoAlarmManager.changeRingerMode(context, true, eventStart);
                    }
                    break;

                case "PRIMARY_REVERT":
                    if (currentTime >= eventEnd) {
                        SmartAutoAlarmManager.changeRingerMode(context, false, eventStart);
                    }
                    break;

                case "BACKUP_REVERT_1":
                case "BACKUP_REVERT_2":
                case "BACKUP_REVERT_3":
                    // Only attempt revert if we're still in silent mode and past event end
                    if (currentTime >= eventEnd) {
                        SmartAutoAlarmManager.changeRingerMode(context, false, eventStart);
                    }
                    break;

                case "FINAL_CLEANUP":
                    // Final cleanup - force revert if needed and clean up
                    if (currentTime >= eventEnd) {
                        SmartAutoAlarmManager.changeRingerMode(context, false, eventStart);
                        SmartAutoAlarmManager.cleanupRingerModePreference(context, eventStart);
                    }
                    break;

                default:
                    Log.w(TAG, "Unknown alarm type: " + alarmType);
                    break;
            }

            // Schedule next worker check to ensure everything is in order
            SmartAutoWorker.scheduleWork(context);

        } catch (Exception e) {
            Log.e(TAG, "Error processing alarm: " + e.getMessage(), e);
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
} 