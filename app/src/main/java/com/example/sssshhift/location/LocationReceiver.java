package com.example.sssshhift.location;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.List;

public class LocationReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationReceiver";
    private static final String PREFS_NAME = "location_profiles";
    private static final String CHANNEL_ID = "location_profiles";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);
        
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        
        if (geofencingEvent == null) {
            Log.e(TAG, "Null GeofencingEvent received");
            return;
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "GeofencingEvent error: " + geofencingEvent.getErrorCode());
            return;
        }

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        
        // Get the geofences that triggered this event
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        
        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.e(TAG, "No geofence trigger found");
            return;
        }

        for (Geofence geofence : triggeringGeofences) {
            String profileId = geofence.getRequestId();
            handleGeofenceTransition(context, profileId, geofenceTransition);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Profiles",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for location-based profile changes");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void handleGeofenceTransition(Context context, String profileId, int transition) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        if (audioManager == null) {
            Log.e(TAG, "AudioManager is null");
            return;
        }

        String name = prefs.getString(profileId + "_name", "Unknown Profile");
        int mode = prefs.getInt(profileId + "_mode", AudioManager.RINGER_MODE_NORMAL);

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Change to profile's ringer mode when entering
            Log.d(TAG, "Entering geofence for profile: " + name);
            audioManager.setRingerMode(mode);
            showNotification(context, name, "Profile Activated", "Entered location zone");
        } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Change back to normal mode when exiting
            Log.d(TAG, "Exiting geofence for profile: " + name);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            showNotification(context, name, "Profile Deactivated", "Left location zone");
        }
    }

    private void showNotification(Context context, String profileName, String title, String message) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message + " for " + profileName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

            notificationManager.notify(profileName.hashCode(), builder.build());
        }
    }

    public static android.app.PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, LocationReceiver.class);
        return android.app.PendingIntent.getBroadcast(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_MUTABLE
        );
    }
} 