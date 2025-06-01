package com.example.sssshhift.services.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class TimerService extends Service {
    private static final String CHANNEL_ID = "TimerServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private AlarmManager alarmManager;
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            long startTime = intent.getLongExtra("START_TIME", 0);
            long endTime = intent.getLongExtra("END_TIME", 0);
            int ringerMode = intent.getIntExtra("RINGER_MODE", AudioManager.RINGER_MODE_NORMAL);
            String profileName = intent.getStringExtra("PROFILE_NAME");
            
            // Schedule start of profile
            scheduleRingerModeChange(startTime, ringerMode, true, profileName);
            
            // Schedule end of profile (reset to normal)
            scheduleRingerModeChange(endTime, AudioManager.RINGER_MODE_NORMAL, false, profileName);
        }
        return START_STICKY;
    }

    private void scheduleRingerModeChange(long triggerTime, int ringerMode, boolean isStart, String profileName) {
        Intent intent = new Intent(this, TimerReceiver.class);
        intent.putExtra("RINGER_MODE", ringerMode);
        intent.putExtra("IS_START", isStart);
        intent.putExtra("PROFILE_NAME", profileName);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            (int) triggerTime, // Use time as request code to make it unique
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Use setAlarmClock instead of setExactAndAllowWhileIdle for higher reliability
        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
            triggerTime,
            pendingIntent
        );
        
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Profile Timer Active")
            .setContentText("Managing your sound profiles")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 