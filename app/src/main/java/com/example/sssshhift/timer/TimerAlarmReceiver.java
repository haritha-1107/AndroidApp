package com.example.sssshhift.timer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.app.Service;
import android.content.ComponentName;
import android.media.AudioAttributes;
import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Color;

import com.example.sssshhift.MainActivity;
import com.example.sssshhift.R;

public class TimerAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerAlarmReceiver";
    private static final String CHANNEL_ID = "timer_notification_channel";
    private static final String CHANNEL_NAME = "Timer Notifications";
    private static final int NOTIFICATION_ID = 1001;
    private static final int MAX_RETRIES = 15; // Increased retries
    private static final long RETRY_DELAY = 1000; // 1 second
    private static final String PREFS_NAME = "timer_receiver_prefs";
    private static final String KEY_LAST_EXECUTED = "last_executed_";
    private static final long EXECUTION_WINDOW = 60000; // 1 minute

    private PowerManager.WakeLock wakeLock;
    private Handler handler;
    private int retryCount = 0;
    private AudioManager audioManager;
    private NotificationManager notificationManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "Null intent or action received");
            return;
        }

        // Initialize handler on main thread
        handler = new Handler(Looper.getMainLooper());

        // Get system services
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a full wake lock
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK | 
            PowerManager.ACQUIRE_CAUSES_WAKEUP | 
            PowerManager.ON_AFTER_RELEASE,
            "Sssshhhift:TimerFullWakeLock"
        );
        
        wakeLock.acquire(10*60*1000L); // 10 minutes max

        try {
            // Force screen on
            PowerManager.WakeLock screenWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Sssshhhift:ScreenWakeLock"
            );
            screenWakeLock.acquire(5000);

            String alarmType = intent.getStringExtra("ALARM_TYPE");
            long alarmTime = intent.getLongExtra("ALARM_TIME", 0);
            
            if (shouldProcessAlarm(context, alarmTime, alarmType)) {
                // Start aggressive ringer mode change
                startRingerModeChange(context, intent);
            } else {
                Log.d(TAG, "Skipping redundant alarm of type: " + alarmType);
                releaseWakeLock();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive: " + e.getMessage());
            releaseWakeLock();
        }
    }

    private void startRingerModeChange(Context context, Intent intent) {
        int targetMode = intent.getIntExtra("RINGER_MODE", AudioManager.RINGER_MODE_NORMAL);
        boolean isStart = intent.getBooleanExtra("IS_START", true);
        String profileName = intent.getStringExtra("PROFILE_NAME");

        // Start a repeated attempt to change the ringer mode
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!changeRingerMode(context, targetMode, isStart, profileName)) {
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                        Log.d(TAG, "Retry attempt " + retryCount);
                        handler.postDelayed(this, RETRY_DELAY);
                    } else {
                        Log.e(TAG, "Failed to change ringer mode after " + MAX_RETRIES + " attempts");
                        showErrorNotification(context);
                        releaseWakeLock();
                    }
                } else {
                    Log.d(TAG, "Successfully changed ringer mode");
                    releaseWakeLock();
                }
            }
        });
    }

    private boolean changeRingerMode(Context context, int targetMode, boolean isStart, String profileName) {
        try {
            // Get system services
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (audioManager == null) {
                Log.e(TAG, "AudioManager is null");
                return false;
            }

            // Method 1: Direct audio manager
            audioManager.setRingerMode(targetMode);
            Thread.sleep(100); // Small delay to let the change take effect

            // Method 2: Interruption filter (for Android M and above)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                try {
                    int filter = NotificationManager.INTERRUPTION_FILTER_ALL;
                    switch (targetMode) {
                        case AudioManager.RINGER_MODE_SILENT:
                            filter = NotificationManager.INTERRUPTION_FILTER_NONE;
                            break;
                        case AudioManager.RINGER_MODE_VIBRATE:
                            filter = NotificationManager.INTERRUPTION_FILTER_PRIORITY;
                            break;
                    }
                    notificationManager.setInterruptionFilter(filter);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting interruption filter: " + e.getMessage());
                }
            }

            // Method 3: Audio attributes (for Android P and above)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    AudioAttributes.Builder builder = new AudioAttributes.Builder();
                    builder.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE);
                    builder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, 
                        targetMode == AudioManager.RINGER_MODE_SILENT ? 0 : audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                        0);
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting audio attributes: " + e.getMessage());
                }
            }

            // Verify the change
            int currentMode = audioManager.getRingerMode();
            if (currentMode != targetMode) {
                Log.w(TAG, "Ringer mode verification failed. Expected: " + getRingerModeName(targetMode) + 
                          ", Current: " + getRingerModeName(currentMode));
                
                // Try one more time with a longer delay
                audioManager.setRingerMode(targetMode);
                Thread.sleep(500);
                
                currentMode = audioManager.getRingerMode();
                if (currentMode != targetMode) {
                    Log.e(TAG, "Failed to change ringer mode after retry");
                    return false;
                }
            }

            // Show notification
            String action = isStart ? "activated" : "deactivated";
            String message = String.format("%s profile %s - %s mode", 
                profileName, action, getRingerModeName(targetMode));
            showNotification(context, message);

            Log.d(TAG, "Successfully changed ringer mode to " + getRingerModeName(targetMode));
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error changing ringer mode: " + e.getMessage());
            return false;
        }
    }

    private void showSuccessNotification(Context context, int ringerMode, boolean isStart, String profileName) {
        try {
            createNotificationChannel(context);
            String title = isStart ? "Profile Activated" : "Profile Ended";
            String message = String.format("%s has %s - Phone is now in %s mode", 
                profileName, isStart ? "started" : "ended", getRingerModeName(ringerMode));

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true);

            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing success notification: " + e.getMessage());
        }
    }

    private void showErrorNotification(Context context) {
        try {
            createNotificationChannel(context);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Profile Change Failed")
                .setContentText("Unable to change ringer mode. Please check app permissions.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR);

            notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing error notification: " + e.getMessage());
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing wake lock: " + e.getMessage());
        }
    }

    private boolean shouldProcessAlarm(Context context, long alarmTime, String alarmType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastExecuted = prefs.getLong(KEY_LAST_EXECUTED + alarmTime, 0);
        long currentTime = System.currentTimeMillis();

        // If this is the first execution or we're outside the execution window
        if (lastExecuted == 0 || currentTime - lastExecuted > EXECUTION_WINDOW) {
            // Mark this alarm as executed
            prefs.edit().putLong(KEY_LAST_EXECUTED + alarmTime, currentTime).apply();
            return true;
        }

        // For EARLY type alarms, only process if no other alarm has executed
        if ("EARLY".equals(alarmType)) {
            return false;
        }

        // For BACKUP type alarms, only process if we're more than 30 seconds past the intended time
        if ("BACKUP".equals(alarmType)) {
            return currentTime - alarmTime > 30000;
        }

        return false;
    }

    private String getRingerModeName(int ringerMode) {
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_SILENT:
                return "Silent";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "Vibrate";
            case AudioManager.RINGER_MODE_NORMAL:
                return "Normal";
            default:
                return "Unknown";
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for timer events");
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 500, 200, 500});
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

                NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }

    private void showNotification(Context context, String message) {
        try {
            createNotificationChannel(context);

            // Create an intent that opens the app when notification is tapped
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
                .setContentTitle("Timer Profile Update")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500});

            // Show the notification
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                Log.d(TAG, "Notification shown: " + message);
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
        }
    }
} 