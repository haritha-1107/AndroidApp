package com.example.sssshhift.features.smartauto;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class SmartAutoAlarmManager {
    private static final String TAG = "SmartAutoAlarmManager";
    private static final String PREF_PREVIOUS_RINGER_MODE = "previous_ringer_mode_";
    private static final String PREF_ACTIVE_EVENTS = "active_calendar_events";
    private static final String PREF_EVENT_END_TIME = "event_end_time_";
    private static final String PREF_LAST_SILENT_ACTIVATION = "last_silent_activation";
    private static final int REVERT_BUFFER_TIME = 2 * 60 * 1000; // 2 minutes buffer
    private static final int SILENT_MODE_NOTIFICATION_ID = 12345;

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

    public static boolean isInSilentMode(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
    }

    public static boolean shouldRevertRingerMode(Context context, long eventStart) {
        Set<String> activeEvents = getActiveEvents(context);
        long currentTime = System.currentTimeMillis();
        
        Log.d(TAG, "Checking if should revert ringer mode for event at: " + new Date(eventStart));
        Log.d(TAG, "Current active events before cleanup: " + activeEvents);
        
        // First, clean up any expired events
        Set<String> updatedEvents = new HashSet<>();
        boolean foundStaleEvents = false;

        for (String eventKey : activeEvents) {
            try {
                String[] parts = eventKey.split("_");
                long start = Long.parseLong(parts[0]);
                long end = Long.parseLong(parts[1]);
                
                // Consider an event stale if:
                // 1. It started more than 12 hours ago OR
                // 2. It ended more than 10 minutes ago OR
                // 3. Its duration is more than 24 hours
                boolean isStale = (currentTime - start > 12 * 60 * 60 * 1000) || // More than 12 hours old
                                (currentTime > end + 10 * 60 * 1000) ||          // Ended more than 10 minutes ago
                                (end - start > 24 * 60 * 60 * 1000);             // Duration > 24 hours

                if (isStale) {
                    Log.d(TAG, "Found stale event from: " + new Date(start) + " to " + new Date(end));
                    foundStaleEvents = true;
                    continue;
                }
                
                // Only keep events that:
                // 1. Haven't ended yet (including buffer time)
                // 2. Aren't the current event we're checking
                // 3. Are actually in progress (start time has passed or within buffer)
                // 4. Are not more than 12 hours in the future
                if (currentTime <= end + REVERT_BUFFER_TIME && 
                    start != eventStart && 
                    currentTime >= start - REVERT_BUFFER_TIME &&
                    start - currentTime <= 12 * 60 * 60 * 1000) {
                    updatedEvents.add(eventKey);
                    Log.d(TAG, "Keeping active event: " + new Date(start) + " to " + new Date(end));
                } else {
                    Log.d(TAG, "Removing expired/current event: " + new Date(start) + " to " + new Date(end));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing event key: " + eventKey);
            }
        }

        // If we found stale events, do a deep cleanup
        if (foundStaleEvents) {
            Log.d(TAG, "Found stale events, performing deep cleanup");
            cleanupAllStaleEvents(context);
        }
        
        // Update the active events list
        updateActiveEvents(context, updatedEvents);
        Log.d(TAG, "Active events after cleanup: " + updatedEvents);
        
        // If there are no other active events, we should revert
        if (updatedEvents.isEmpty()) {
            Log.d(TAG, "No other active events found, will revert ringer mode");
            return true;
        }
        
        // Double check that the remaining events are actually active
        boolean hasActiveEvent = false;
        for (String eventKey : updatedEvents) {
            try {
                String[] parts = eventKey.split("_");
                long otherStart = Long.parseLong(parts[0]);
                long otherEnd = Long.parseLong(parts[1]);
                
                // An event is considered active if:
                // 1. We're currently within its time range
                // 2. It has actually started (or about to start within buffer)
                // 3. It hasn't ended yet
                // 4. It's not a stale event
                if (currentTime >= otherStart - REVERT_BUFFER_TIME && 
                    currentTime <= otherEnd &&
                    currentTime - otherStart < 12 * 60 * 60 * 1000) {  // Changed from 24 to 12 hours
                    Log.d(TAG, "Found active event from " + new Date(otherStart) + " to " + new Date(otherEnd));
                    hasActiveEvent = true;
                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking event: " + eventKey);
            }
        }
        
        Log.d(TAG, hasActiveEvent ? "Found active events, keeping silent mode" : "No currently active events found, will revert ringer mode");
        return !hasActiveEvent;
    }

    private static void cleanupAllStaleEvents(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> activeEvents = getActiveEvents(context);
        long currentTime = System.currentTimeMillis();
        
        // Clean up all preferences related to stale events
        SharedPreferences.Editor editor = prefs.edit();
        
        for (String eventKey : activeEvents) {
            try {
                String[] parts = eventKey.split("_");
                long start = Long.parseLong(parts[0]);
                long end = Long.parseLong(parts[1]);
                
                // Use same stale event criteria as in shouldRevertRingerMode
                boolean isStale = (currentTime - start > 12 * 60 * 60 * 1000) ||  // More than 12 hours old
                                (currentTime > end + 10 * 60 * 1000) ||           // Ended more than 10 minutes ago
                                (end - start > 24 * 60 * 60 * 1000);              // Duration > 24 hours
                
                if (isStale) {
                    // Clean up all related preferences
                    editor.remove(PREF_PREVIOUS_RINGER_MODE + start);
                    editor.remove(PREF_EVENT_END_TIME + start);
                    Log.d(TAG, "Cleaned up preferences for stale event: " + new Date(start));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up stale event: " + eventKey);
            }
        }
        
        editor.apply();
    }

    public static void scheduleRingerModeChange(Context context, long activationTime, long eventEnd) {
        synchronized (SmartAutoAlarmManager.class) {
            Log.d(TAG, "Scheduling ringer mode change:");
            Log.d(TAG, String.format("- Activation time: %s", new Date(activationTime)));
            Log.d(TAG, String.format("- Event end: %s", new Date(eventEnd)));

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager not available");
                return;
            }

            long currentTime = System.currentTimeMillis();

            // Get current active events
            Set<String> activeEvents = getActiveEvents(context);
            String eventKey = activationTime + "_" + eventEnd;

            // First clean up any existing alarms for this event
            cancelScheduledChanges(context, activationTime);

            // Only proceed if the event hasn't ended
            if (currentTime >= eventEnd) {
                Log.d(TAG, "Event has already ended, skipping alarm scheduling");
                return;
            }

            // Store event end time
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit()
                .putLong(PREF_EVENT_END_TIME + activationTime, eventEnd)
                .apply();

            // Add to active events if it's a future event
            if (currentTime < eventEnd) {
                activeEvents.add(eventKey);
                updateActiveEvents(context, activeEvents);
            }

            // Calculate multiple activation times for redundancy
            long[] activationTimes = {
                activationTime - 5 * 60 * 1000,  // 5 minutes before
                activationTime - 2 * 60 * 1000,  // 2 minutes before
                activationTime                    // At exact time
            };

            // Calculate multiple revert times for redundancy
            long[] revertTimes = {
                eventEnd,                        // At exact end time
                eventEnd + 1 * 60 * 1000,       // 1 minute after
                eventEnd + 3 * 60 * 1000,       // 3 minutes after
                eventEnd + 5 * 60 * 1000        // 5 minutes after (final cleanup)
            };

            // Schedule activation alarms
            for (int i = 0; i < activationTimes.length; i++) {
                if (activationTimes[i] > currentTime) {
                    scheduleAlarm(context, alarmManager, activationTimes[i], eventEnd, activationTime,
                            "ACTIVATE_" + (i == 0 ? "PRIMARY" : "BACKUP_" + i), true);
                }
            }

            // Schedule revert alarms
            for (int i = 0; i < revertTimes.length; i++) {
                if (revertTimes[i] > currentTime) {
                    String type = i == revertTimes.length - 1 ? "FINAL_CLEANUP" :
                                (i == 0 ? "REVERT_PRIMARY" : "REVERT_BACKUP_" + i);
                    scheduleAlarm(context, alarmManager, revertTimes[i], eventEnd, activationTime,
                            type, false);
                }
            }

            Log.d(TAG, "Successfully scheduled all alarms for event");
        }
    }

    private static void scheduleAlarm(Context context, AlarmManager alarmManager, long triggerTime,
                                    long eventEnd, long eventStart, String type, boolean toSilent) {
        Intent intent = new Intent(context, SmartAutoReceiver.class);
        String action = "com.example.sssshhift.SMART_AUTO_" + type + "_" + eventStart;
        intent.setAction(action);
        intent.putExtra("event_start", eventStart);
        intent.putExtra("event_end", eventEnd);
        intent.putExtra("trigger_time", triggerTime);
        intent.putExtra("to_silent", toSilent);
        intent.putExtra("alarm_type", type);
        
        // Add flags to ensure delivery in all states
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        
        // Create unique request code
        int requestCode = (int) ((eventStart ^ triggerTime + type.hashCode()) % Integer.MAX_VALUE);

        // Use FLAG_UPDATE_CURRENT to ensure the intent extras are preserved
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    // Use setAlarmClock for highest priority and doze mode bypass
                    alarmManager.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(triggerTime, null),
                        pendingIntent
                    );
                    Log.d(TAG, String.format("Scheduled exact alarm for %s using setAlarmClock", type));
                } else {
                    // Fallback to setExactAndAllowWhileIdle if exact alarms not allowed
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    );
                    Log.d(TAG, String.format("Scheduled exact alarm for %s using setExactAndAllowWhileIdle", type));
                }
            } else {
                // For older versions, use setAlarmClock for maximum reliability
                alarmManager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(triggerTime, null),
                    pendingIntent
                );
                Log.d(TAG, String.format("Scheduled exact alarm for %s using setAlarmClock (legacy)", type));
            }

            Log.d(TAG, String.format("Successfully scheduled %s alarm for %s", type, new Date(triggerTime)));
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void changeRingerMode(Context context, boolean toSilent, long eventStart) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = PREF_PREVIOUS_RINGER_MODE + eventStart;
        long eventEnd = prefs.getLong(PREF_EVENT_END_TIME + eventStart, 0);
        long currentTime = System.currentTimeMillis();

        try {
            // First check if this is a stale event
            if (currentTime - eventStart > 24 * 60 * 60 * 1000) {
                Log.d(TAG, "Ignoring ringer mode change for stale event from: " + new Date(eventStart));
                cleanupRingerModePreference(context, eventStart);
                return;
            }

            if (toSilent) {
                // Verify the event hasn't ended before activating silent mode
                if (eventEnd > 0 && currentTime >= eventEnd) {
                    Log.d(TAG, "Event has already ended, skipping silent mode activation");
                    return;
                }

                // Store current ringer mode before changing to silent
                int currentMode = audioManager.getRingerMode();
                if (currentMode != AudioManager.RINGER_MODE_SILENT) {
                    prefs.edit()
                        .putInt(prefKey, currentMode)
                        .putLong(PREF_LAST_SILENT_ACTIVATION, System.currentTimeMillis())
                        .apply();
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    Log.d(TAG, "Changed to silent mode, stored previous mode: " + currentMode);
                    
                    // Show the persistent notification
                    showSilentModeNotification(context);
                }
            } else {
                Log.d(TAG, "Attempting to revert ringer mode for event at: " + new Date(eventStart));
                // Check if we should revert
                if (shouldRevertRingerMode(context, eventStart)) {
                    // Verify the event has actually ended
                    if (eventEnd > 0 && currentTime < eventEnd) {
                        Log.d(TAG, "Event hasn't ended yet, keeping silent mode");
                        return;
                    }

                    // Force revert to previous mode
                    int previousMode = prefs.getInt(prefKey, AudioManager.RINGER_MODE_NORMAL);
                    audioManager.setRingerMode(previousMode);
                    Log.d(TAG, "Reverted to previous mode: " + previousMode);
                    
                    // Cancel the persistent notification
                    cancelSilentModeNotification(context);
                    
                    // Clean up the preference
                    cleanupRingerModePreference(context, eventStart);
                    
                    // Also clean up active events for this event
                    Set<String> activeEvents = getActiveEvents(context);
                    activeEvents.removeIf(key -> key.startsWith(eventStart + "_"));
                    updateActiveEvents(context, activeEvents);
                } else {
                    Log.d(TAG, "Keeping silent mode as other events are still active");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error changing ringer mode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void cancelScheduledChanges(Context context, long eventStart) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Cancel all possible alarm types
        String[] types = {"ACTIVATE_PRIMARY", "ACTIVATE_BACKUP_1", "ACTIVATE_BACKUP_2",
                         "REVERT_PRIMARY", "REVERT_BACKUP_1", "REVERT_BACKUP_2", 
                         "REVERT_BACKUP_3", "FINAL_CLEANUP"};
        
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

    /**
     * Reschedule all active events after device reboot
     * @param context Application context
     */
    public static void rescheduleEventsAfterBoot(Context context) {
        Set<String> activeEvents = getActiveEvents(context);
        long currentTime = System.currentTimeMillis();
        
        Log.d(TAG, "Rescheduling events after device boot. Active events: " + activeEvents.size());
        
        for (String eventKey : activeEvents) {
            try {
                String[] parts = eventKey.split("_");
                long eventStart = Long.parseLong(parts[0]);
                long eventEnd = Long.parseLong(parts[1]);
                
                // Only reschedule if the event hasn't ended
                if (eventEnd > currentTime) {
                    Log.d(TAG, "Rescheduling event: " + new Date(eventStart) + " to " + new Date(eventEnd));
                    scheduleRingerModeChange(context, eventStart, eventEnd);
                } else {
                    Log.d(TAG, "Skipping expired event: " + new Date(eventStart) + " to " + new Date(eventEnd));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling event: " + eventKey, e);
            }
        }
        
        // Clean up expired events
        cleanupExpiredEvents(context);
    }

    /**
     * Clean up any expired events and their associated preferences
     * @param context Application context
     */
    public static void cleanupExpiredEvents(Context context) {
        Set<String> activeEvents = getActiveEvents(context);
        Set<String> updatedEvents = new HashSet<>();
        long currentTime = System.currentTimeMillis();
        
        for (String eventKey : activeEvents) {
            try {
                String[] parts = eventKey.split("_");
                long eventStart = Long.parseLong(parts[0]);
                long eventEnd = Long.parseLong(parts[1]);
                
                if (eventEnd > currentTime) {
                    updatedEvents.add(eventKey);
                } else {
                    Log.d(TAG, "Removing expired event: " + new Date(eventStart) + " to " + new Date(eventEnd));
                    cleanupRingerModePreference(context, eventStart);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up event: " + eventKey, e);
            }
        }
        
        if (activeEvents.size() != updatedEvents.size()) {
            Log.d(TAG, "Cleaned up " + (activeEvents.size() - updatedEvents.size()) + " expired events");
            updateActiveEvents(context, updatedEvents);
        }
    }

    private static void showSilentModeNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "silent_mode_channel",
                "Silent Mode Status",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows when silent mode is active");
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "silent_mode_channel")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle("Silent Mode Active")
            .setContentText("Calendar event in progress")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        // Show the notification
        notificationManager.notify(SILENT_MODE_NOTIFICATION_ID, builder.build());
    }

    private static void cancelSilentModeNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(SILENT_MODE_NOTIFICATION_ID);
        }
    }
} 