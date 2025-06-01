package com.example.sssshhift.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.sssshhift.R;
import com.google.android.gms.location.Geofence;

public class ProfileService extends Service {

    private static final String TAG = "ProfileService";
    private static final String CHANNEL_ID = "GeofenceServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_GEOFENCE_TRANSITION = "com.example.sssshhift.GEOFENCE_TRANSITION";
    public static final String EXTRA_GEOFENCE_ID = "geofence_id";
    public static final String EXTRA_TRANSITION_TYPE = "transition_type";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "ProfileService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ProfileService started");

        if (intent != null && ACTION_GEOFENCE_TRANSITION.equals(intent.getAction())) {
            String geofenceId = intent.getStringExtra(EXTRA_GEOFENCE_ID);
            int transitionType = intent.getIntExtra(EXTRA_TRANSITION_TYPE, -1);

            Log.d(TAG, "Handling geofence transition - ID: " + geofenceId + ", Type: " + transitionType);

            // Start foreground service
            startForeground(NOTIFICATION_ID, createNotification());

            // Handle the geofence transition
            handleGeofenceTransition(geofenceId, transitionType);
        }

        return START_NOT_STICKY;
    }

    private void handleGeofenceTransition(String geofenceId, int transitionType) {
        try {
            if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.d(TAG, "Entered geofence: " + geofenceId);
                // TODO: Activate silent profile or perform desired action
                activateProfileForGeofence(geofenceId);
            } else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Log.d(TAG, "Exited geofence: " + geofenceId);
                // TODO: Deactivate silent profile or revert action
                deactivateProfileForGeofence(geofenceId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling geofence transition", e);
        } finally {
            // Stop the service after handling
            stopSelf();
        }
    }

    private void activateProfileForGeofence(String geofenceId) {
        // TODO: Implement your profile activation logic here
        Log.d(TAG, "Activating profile for geofence: " + geofenceId);
        // Example: Set phone to silent mode, change notification settings, etc.
    }

    private void deactivateProfileForGeofence(String geofenceId) {
        // TODO: Implement your profile deactivation logic here
        Log.d(TAG, "Deactivating profile for geofence: " + geofenceId);
        // Example: Restore previous phone settings
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Geofence Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Service for handling geofence transitions");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Monitoring location-based profiles")
                .setSmallIcon(R.drawable.ic_location_on) // Make sure this icon exists
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ProfileService destroyed");
    }
}