package com.example.sssshhift.services.timer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class TimerReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "TimerNotificationChannel";
    private static final int NOTIFICATION_ID = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int ringerMode = intent.getIntExtra("RINGER_MODE", AudioManager.RINGER_MODE_NORMAL);
            boolean isStart = intent.getBooleanExtra("IS_START", true);
            String profileName = intent.getStringExtra("PROFILE_NAME");
            
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            
            if (audioManager != null) {
                // Change ringer mode
                audioManager.setRingerMode(ringerMode);
                
                // Show notification to user
                showNotification(context, ringerMode, isStart, profileName);
            }
        }
    }

    private void showNotification(Context context, int ringerMode, boolean isStart, String profileName) {
        createNotificationChannel(context);
        
        String title = isStart ? "Profile Activated" : "Profile Deactivated";
        String message = getModeMessage(ringerMode, isStart, profileName);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private String getModeMessage(int ringerMode, boolean isStart, String profileName) {
        String action = isStart ? "activated" : "deactivated";
        String mode;
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_SILENT:
                mode = "Silent";
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                mode = "Vibrate";
                break;
            default:
                mode = "Normal";
        }
        return profileName + " has been " + action + " (" + mode + " mode)";
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for timer-based profile changes");
            
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
} 