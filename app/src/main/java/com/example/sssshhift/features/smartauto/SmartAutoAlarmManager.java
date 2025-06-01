package com.example.sssshhift.features.smartauto;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.preference.PreferenceManager;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class SmartAutoAlarmManager {
    private static final String TAG = "SmartAutoAlarmManager";
    private static final String PREF_PREVIOUS_RINGER_MODE = "previous_ringer_mode_";
    private static final String PREF_ACTIVE_EVENTS = "active_calendar_events";
    private static final String PREF_EVENT_END_TIME = "event_end_time_";
    private static final int REVERT_BUFFER_TIME = 2 * 60 * 1000; // 2 minutes buffer

    /**
     * Get the set of active calendar events
     * @param context Application context
     * @return Set of active event keys in format "eventStart_eventEnd"
     */
    public static Set<String> getActiveEvents(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return new HashSet<>(prefs.getStringSet(PREF_ACTIVE_EVENTS, new HashSet<>()));
    }

    /**
     * Update the set of active calendar events
     * @param context Application context
     * @param activeEvents Updated set of active events
     */
    public static void updateActiveEvents(Context context, Set<String> activeEvents) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putStringSet(PREF_ACTIVE_EVENTS, activeEvents).apply();
    }

    /**
     * Clean up the stored ringer mode preference for a specific event
     * @param context Application context
     * @param eventStartTime Event start time in milliseconds
     */
    public static void cleanupRingerModePreference(Context context, long eventStartTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .remove(PREF_PREVIOUS_RINGER_MODE + eventStartTime)
            .remove(PREF_EVENT_END_TIME + eventStartTime)
            .apply();
        Log.d(TAG, "Cleaned up ringer mode preference for event at: " + new Date(eventStartTime));
    }

    public static void scheduleRingerModeChange(Context context, long eventStart, long eventEnd) {
        synchronized (SmartAutoAlarmManager.class) {
            Log.d(TAG, "Scheduling ringer mode change for event: " + new Date(eventStart) + " to " + new Date(eventEnd));

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager not available");
                return;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int preEventOffset = prefs.getInt("auto_mode_pre_event_offset", 5);
            boolean revertAfterEvent = prefs.getBoolean("auto_mode_revert_after_event", true);

            // Get current active events
            Set<String> activeEvents = getActiveEvents(context);
            String eventKey = eventStart + "_" + eventEnd;

            // Get current time and calculate silent mode time
            long currentTime = System.currentTimeMillis();
            long silentTime = eventStart - (preEventOffset * 60 * 1000);

            // Cancel any existing alarms for this event
            cancelScheduledChanges(context, eventStart);

            // Store event end time
            prefs.edit().putLong(PREF_EVENT_END_TIME + eventStart, eventEnd).apply();

            // Add event to active events
            activeEvents.add(eventKey);
            updateActiveEvents(context, activeEvents);

            // Schedule silent mode activation if we haven't passed the start time
            if (silentTime > currentTime) {
                scheduleAlarm(context, silentTime, true, eventStart, "ACTIVATE_SILENT");
                Log.d(TAG, "Scheduled silent mode activation for: " + new Date(silentTime));
            } else if (currentTime < eventEnd) {
                // If we're between start and end time, activate silent mode immediately
                changeRingerMode(context, true, eventStart);
                Log.d(TAG, "Activated silent mode immediately");
            }

            // Schedule multiple revert alarms for redundancy
            if (revertAfterEvent && currentTime < eventEnd) {
                // Schedule primary revert alarm at event end
                scheduleAlarm(context, eventEnd, false, eventStart, "PRIMARY_REVERT");
                Log.d(TAG, "Scheduled primary revert alarm for: " + new Date(eventEnd));

                // Schedule backup revert alarms
                scheduleAlarm(context, eventEnd + 60000, false, eventStart, "BACKUP_REVERT_1");
                scheduleAlarm(context, eventEnd + 120000, false, eventStart, "BACKUP_REVERT_2");
                scheduleAlarm(context, eventEnd + 300000, false, eventStart, "BACKUP_REVERT_3");
                Log.d(TAG, "Scheduled backup revert alarms");

                // Schedule a final cleanup check
                scheduleAlarm(context, eventEnd + REVERT_BUFFER_TIME, false, eventStart, "FINAL_CLEANUP");
            }
        }
    }

    private static void scheduleAlarm(Context context, long triggerTime, boolean toSilent, long eventStart, String type) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, SmartAutoReceiver.class);
        intent.putExtra("to_silent", toSilent);
        intent.putExtra("event_start", eventStart);
        intent.putExtra("trigger_time", triggerTime);
        intent.putExtra("alarm_type", type);
        
        // Make the intent unique for each alarm type
        String action = "com.example.sssshhift.SMART_AUTO_" + type + "_" + eventStart + "_" + triggerTime;
        intent.setAction(action);
        
        // Add flags to ensure delivery
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        // Create unique request code
        int requestCode = (int) ((eventStart + triggerTime + type.hashCode()) % Integer.MAX_VALUE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule with exact timing and wake up the device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d(TAG, String.format("Scheduled %s alarm for %s (Request code: %d)", type, new Date(triggerTime), requestCode));
    }

    public static void changeRingerMode(Context context, boolean toSilent, long eventStart) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = PREF_PREVIOUS_RINGER_MODE + eventStart;

        try {
            if (toSilent) {
                // Store current ringer mode before changing to silent
                int currentMode = audioManager.getRingerMode();
                prefs.edit().putInt(prefKey, currentMode).apply();
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                Log.d(TAG, "Changed to silent mode, stored previous mode: " + currentMode);
            } else {
                // Check if we should actually revert (no other active events)
                if (shouldRevertRingerMode(context, eventStart)) {
                    // Restore previous ringer mode
                    int previousMode = prefs.getInt(prefKey, AudioManager.RINGER_MODE_NORMAL);
                    audioManager.setRingerMode(previousMode);
                    Log.d(TAG, "Reverted to previous mode: " + previousMode);
                    
                    // Clean up the preference
                    cleanupRingerModePreference(context, eventStart);
                } else {
                    Log.d(TAG, "Skipping revert as other events are still active");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error changing ringer mode: " + e.getMessage());
        }
    }

    private static boolean shouldRevertRingerMode(Context context, long eventStart) {
        Set<String> activeEvents = getActiveEvents(context);
        long currentTime = System.currentTimeMillis();
        
        // Remove this event from active events
        String eventToRemove = null;
        for (String eventKey : activeEvents) {
            if (eventKey.startsWith(eventStart + "_")) {
                eventToRemove = eventKey;
                break;
            }
        }
        if (eventToRemove != null) {
            activeEvents.remove(eventToRemove);
            updateActiveEvents(context, activeEvents);
        }

        // Check remaining active events
        for (String eventKey : activeEvents) {
            try {
                String[] parts = eventKey.split("_");
                long eventEnd = Long.parseLong(parts[1]);
                if (eventEnd > currentTime) {
                    return false; // Found another active event
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing event key: " + eventKey);
            }
        }
        
        return true;
    }

    public static void cancelScheduledChanges(Context context, long eventStart) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Cancel all possible alarm types
        String[] types = {"ACTIVATE_SILENT", "PRIMARY_REVERT", "BACKUP_REVERT_1", 
                         "BACKUP_REVERT_2", "BACKUP_REVERT_3", "FINAL_CLEANUP"};
        
        for (String type : types) {
            Intent intent = new Intent(context, SmartAutoReceiver.class);
            String action = "com.example.sssshhift.SMART_AUTO_" + type + "_" + eventStart;
            intent.setAction(action);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (int) ((eventStart + type.hashCode()) % Integer.MAX_VALUE),
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, "Cancelled " + type + " alarm for event: " + eventStart);
            }
        }
    }
} 