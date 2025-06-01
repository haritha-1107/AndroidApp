package com.example.sssshhift.timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.app.NotificationManager;
import android.util.Log;
import android.os.PowerManager;
import android.provider.Settings;

public class TimerProfileManager {
    private static final String TAG = "TimerProfileManager";
    private static final String PREFS_NAME = "timer_manager_prefs";
    private static final String KEY_LAST_SET_TIME = "last_set_time_";
    
    private final Context context;
    private final AlarmManager alarmManager;
    private final AudioManager audioManager;
    private final NotificationManager notificationManager;
    private final PowerManager powerManager;
    private final SharedPreferences prefs;

    public TimerProfileManager(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean scheduleProfile(long startTime, long endTime, int ringerMode, String profileName) {
        try {
            // Check if we have permission to schedule exact alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return false;
                }
            }

            // Check if we have DND access
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!notificationManager.isNotificationPolicyAccessGranted()) {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return false;
                }
            }

            // Schedule start time with redundancy
            boolean startScheduled = scheduleAlarmWithRedundancy(startTime, ringerMode, true, profileName);
            
            // Schedule end time with redundancy
            boolean endScheduled = scheduleAlarmWithRedundancy(endTime, AudioManager.RINGER_MODE_NORMAL, false, profileName);

            // Save the profile for persistence
            if (startScheduled && endScheduled) {
                TimerBootReceiver.saveProfile(context, startTime, endTime, ringerMode, profileName);
            }

            return startScheduled && endScheduled;
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling profile", e);
            return false;
        }
    }

    private boolean scheduleAlarmWithRedundancy(long triggerTime, int ringerMode, boolean isStart, String profileName) {
        try {
            // Create intents with different flags for redundancy
            PendingIntent primaryIntent = createPendingIntent(triggerTime, ringerMode, isStart, profileName);
            PendingIntent backupIntent = createBackupPendingIntent(triggerTime, ringerMode, isStart, profileName);
            PendingIntent earlyIntent = createEarlyPendingIntent(triggerTime, ringerMode, isStart, profileName);
            PendingIntent extraIntent = createExtraBackupPendingIntent(triggerTime, ringerMode, isStart, profileName);
            
            // Cancel any existing alarms first
            alarmManager.cancel(primaryIntent);
            alarmManager.cancel(backupIntent);
            alarmManager.cancel(earlyIntent);
            alarmManager.cancel(extraIntent);

            // Schedule multiple alarms with different methods for maximum reliability
            
            // 1. Primary alarm using setAlarmClock (highest priority, shows in system UI)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AlarmManager.AlarmClockInfo alarmInfo = new AlarmManager.AlarmClockInfo(triggerTime, null);
                alarmManager.setAlarmClock(alarmInfo, primaryIntent);
            }

            // 2. Backup alarm using setExactAndAllowWhileIdle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, backupIntent);
            }

            // 3. Early alarm (2 minutes before) using setAlarmClock
            long earlyTime = triggerTime - (2 * 60 * 1000); // 2 minutes earlier
            if (earlyTime > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AlarmManager.AlarmClockInfo earlyAlarmInfo = new AlarmManager.AlarmClockInfo(earlyTime, null);
                    alarmManager.setAlarmClock(earlyAlarmInfo, earlyIntent);
                }
            }

            // 4. Extra backup using setAndAllowWhileIdle (30 seconds before)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime - 30000, extraIntent);
            }

            // For older versions, use setExact as fallback
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, primaryIntent);
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime - 30000, extraIntent);
            }

            // Save the scheduled alarm details
            saveScheduledAlarm(triggerTime, ringerMode, isStart, profileName);

            Log.d(TAG, String.format("Scheduled multiple alarms for %s at %d (early: %d)", 
                (isStart ? "start" : "end"), triggerTime, earlyTime));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alarm: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void saveScheduledTime(long time, boolean isStart) {
        prefs.edit().putLong(KEY_LAST_SET_TIME + (isStart ? "start" : "end"), time).apply();
    }

    private void saveScheduledAlarm(long triggerTime, int ringerMode, boolean isStart, String profileName) {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            String prefix = isStart ? "start_" : "end_";
            editor.putLong(prefix + "time", triggerTime);
            editor.putInt(prefix + "mode", ringerMode);
            editor.putString(prefix + "name", profileName);
            editor.putLong(prefix + "set_at", System.currentTimeMillis());
            editor.apply();
            
            Log.d(TAG, String.format("Saved alarm details - Time: %d, Mode: %d, Name: %s", 
                triggerTime, ringerMode, profileName));
        } catch (Exception e) {
            Log.e(TAG, "Error saving alarm details: " + e.getMessage());
        }
    }

    private PendingIntent createPendingIntent(long time, int ringerMode, boolean isStart, String profileName) {
        Intent intent = new Intent(context, TimerAlarmReceiver.class);
        String action = isStart ? "com.example.sssshhift.START_TIMER" : "com.example.sssshhift.END_TIMER";
        intent.setAction(action);
        intent.putExtra("RINGER_MODE", ringerMode);
        intent.putExtra("IS_START", isStart);
        intent.putExtra("PROFILE_NAME", profileName);
        intent.putExtra("ALARM_TIME", time);
        intent.putExtra("ALARM_TYPE", "PRIMARY");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, (int) ((time / 1000) % Integer.MAX_VALUE) + (isStart ? 100000 : 200000), intent, flags);
    }

    private PendingIntent createBackupPendingIntent(long time, int ringerMode, boolean isStart, String profileName) {
        Intent intent = new Intent(context, TimerAlarmReceiver.class);
        String action = isStart ? "com.example.sssshhift.START_TIMER_BACKUP" : "com.example.sssshhift.END_TIMER_BACKUP";
        intent.setAction(action);
        intent.putExtra("RINGER_MODE", ringerMode);
        intent.putExtra("IS_START", isStart);
        intent.putExtra("PROFILE_NAME", profileName);
        intent.putExtra("ALARM_TIME", time);
        intent.putExtra("ALARM_TYPE", "BACKUP");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, (int) ((time / 1000) % Integer.MAX_VALUE) + (isStart ? 300000 : 400000), intent, flags);
    }

    private PendingIntent createEarlyPendingIntent(long time, int ringerMode, boolean isStart, String profileName) {
        Intent intent = new Intent(context, TimerAlarmReceiver.class);
        String action = isStart ? "com.example.sssshhift.START_TIMER_EARLY" : "com.example.sssshhift.END_TIMER_EARLY";
        intent.setAction(action);
        intent.putExtra("RINGER_MODE", ringerMode);
        intent.putExtra("IS_START", isStart);
        intent.putExtra("PROFILE_NAME", profileName);
        intent.putExtra("ALARM_TIME", time);
        intent.putExtra("ALARM_TYPE", "EARLY");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, (int) ((time / 1000) % Integer.MAX_VALUE) + (isStart ? 500000 : 600000), intent, flags);
    }

    private PendingIntent createExtraBackupPendingIntent(long time, int ringerMode, boolean isStart, String profileName) {
        Intent intent = new Intent(context, TimerAlarmReceiver.class);
        String action = isStart ? "com.example.sssshhift.START_TIMER_EXTRA" : "com.example.sssshhift.END_TIMER_EXTRA";
        intent.setAction(action);
        intent.putExtra("RINGER_MODE", ringerMode);
        intent.putExtra("IS_START", isStart);
        intent.putExtra("PROFILE_NAME", profileName);
        intent.putExtra("ALARM_TIME", time);
        intent.putExtra("ALARM_TYPE", "EXTRA");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, (int) ((time / 1000) % Integer.MAX_VALUE) + (isStart ? 700000 : 800000), intent, flags);
    }

    private boolean checkPermissions() {
        // Check for DND permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                Log.e(TAG, "DND permission not granted");
                return false;
            }
        }

        // Check for exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms");
                return false;
            }
        }

        // Check if app is on battery optimization whitelist
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                Log.w(TAG, "App is not on battery optimization whitelist");
                // Don't return false here, just warn
            }
        }

        return true;
    }

    public void cancelProfile(long startTime, long endTime) {
        try {
            // Cancel all variants of the alarms
            cancelAlarm(createPendingIntent(startTime, AudioManager.RINGER_MODE_NORMAL, true, ""));
            cancelAlarm(createBackupPendingIntent(startTime, AudioManager.RINGER_MODE_NORMAL, true, ""));
            cancelAlarm(createEarlyPendingIntent(startTime, AudioManager.RINGER_MODE_NORMAL, true, ""));
            cancelAlarm(createExtraBackupPendingIntent(startTime, AudioManager.RINGER_MODE_NORMAL, true, ""));
            
            cancelAlarm(createPendingIntent(endTime, AudioManager.RINGER_MODE_NORMAL, false, ""));
            cancelAlarm(createBackupPendingIntent(endTime, AudioManager.RINGER_MODE_NORMAL, false, ""));
            cancelAlarm(createEarlyPendingIntent(endTime, AudioManager.RINGER_MODE_NORMAL, false, ""));
            cancelAlarm(createExtraBackupPendingIntent(endTime, AudioManager.RINGER_MODE_NORMAL, false, ""));
            
            // Clear saved alarm details
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("start_time")
                  .remove("start_mode")
                  .remove("start_name")
                  .remove("start_set_at")
                  .remove("end_time")
                  .remove("end_mode")
                  .remove("end_name")
                  .remove("end_set_at")
                  .apply();
            
            Log.d(TAG, "Successfully cancelled profile and cleared saved data");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling profile: " + e.getMessage());
        }
    }

    private void cancelAlarm(PendingIntent pendingIntent) {
        try {
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling alarm: " + e.getMessage());
        }
    }
} 