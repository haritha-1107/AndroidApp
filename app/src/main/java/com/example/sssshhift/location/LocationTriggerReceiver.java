package com.example.sssshhift.location;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.example.sssshhift.R;

import java.util.List;

public class LocationTriggerReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationTriggerReceiver";
    private static final String CHANNEL_ID = "location_trigger_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            String errorMessage = getErrorString(geofencingEvent.getErrorCode());
            Log.e(TAG, "Geofencing error: " + errorMessage);
            return;
        }

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        
        // Get the geofences that were triggered
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        
        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.e(TAG, "No geofence trigger found");
            return;
        }

        String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition);
        Log.d(TAG, "Geofence transition: " + geofenceTransitionDetails);

        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Log.d(TAG, "Entering geofence area");
                LocationUtils.handleLocationTrigger(context, true);
                showNotification(context, "Entered Silent Zone", "Your phone has been set to silent mode");
                break;

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Log.d(TAG, "Exiting geofence area");
                LocationUtils.handleLocationTrigger(context, false);
                showNotification(context, "Left Silent Zone", "Your phone's ringer mode has been restored");
                break;

            case Geofence.GEOFENCE_TRANSITION_DWELL:
                Log.d(TAG, "Dwelling in geofence area");
                break;

            default:
                Log.e(TAG, "Invalid geofence transition type: " + geofenceTransition);
        }
    }

    private void showNotification(Context context, String title, String message) {
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to show notification", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Triggers";
            String description = "Notifications for location-based sound profile changes";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private String getGeofenceTransitionDetails(int geofenceTransition) {
        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "GEOFENCE_TRANSITION_ENTER";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "GEOFENCE_TRANSITION_EXIT";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "GEOFENCE_TRANSITION_DWELL";
            default:
                return "UNKNOWN";
        }
    }

    private String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Geofence service is not available now";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Your app has registered too many geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "You have provided too many PendingIntents to the addGeofences() call";
            case GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION:
                return "Insufficient location permission";
            case GeofenceStatusCodes.GEOFENCE_REQUEST_TOO_FREQUENT:
                return "Location request is too frequent";
            default:
                return "Unknown geofence error: " + errorCode;
        }
    }
} 