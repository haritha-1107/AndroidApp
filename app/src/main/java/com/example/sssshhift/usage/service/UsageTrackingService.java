package com.example.sssshhift.usage.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.sssshhift.R;
import com.example.sssshhift.activities.UsageInsightsActivity;
import com.example.sssshhift.usage.data.UsageLog;
import com.example.sssshhift.usage.repository.UsageRepository;
import java.util.Date;

public class UsageTrackingService extends Service {
    private static final String TAG = "UsageTrackingService";
    private static final String CHANNEL_ID = "usage_tracking_channel";
    private static final int NOTIFICATION_ID = 1001;

    private UsageRepository repository;
    private Date startTime;
    private String currentMode;
    private boolean isTracking = false;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new UsageRepository(getApplication());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("mode")) {
            String newMode = intent.getStringExtra("mode");
            Log.d(TAG, "Received mode change: " + newMode);
            
            // Log previous session if exists
            if (startTime != null && currentMode != null && isTracking) {
                logUsageSession();
            }
            
            // Start new session
            startTime = new Date();
            currentMode = newMode;
            isTracking = true;
            
            // Start foreground service with notification
            startForeground(NOTIFICATION_ID, createNotification());
            
            Log.d(TAG, "Started tracking new session: " + currentMode);
        }
        
        return START_STICKY;
    }

    private void logUsageSession() {
        try {
            Date endTime = new Date();
            long durationMinutes = (endTime.getTime() - startTime.getTime()) / (60 * 1000);
            
            if (durationMinutes > 0) {
                UsageLog log = new UsageLog(currentMode, startTime, endTime, durationMinutes);
                repository.insert(log);
                Log.d(TAG, String.format("Logged usage: %s, duration: %d minutes", currentMode, durationMinutes));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging usage session: " + e.getMessage(), e);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, UsageInsightsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Profile Active")
            .setContentText("Currently in " + currentMode + " mode")
            .setSmallIcon(R.drawable.ic_notifications_off)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Usage Tracking",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tracks active sound profiles");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (startTime != null && currentMode != null && isTracking) {
            logUsageSession();
        }
        isTracking = false;
        Log.d(TAG, "Service destroyed, final log completed");
    }
} 