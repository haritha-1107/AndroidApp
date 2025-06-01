package com.example.sssshhift.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.example.sssshhift.services.ProfileService;
import java.util.List;

public class GeofenceReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "GeofenceReceiver triggered");

        if (context == null || intent == null) {
            Log.e(TAG, "Context or Intent is null");
            return;
        }

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            Log.e(TAG, "Geofencing error: " + errorCode + " - " +
                    com.example.sssshhift.geofencing.GeofenceHelper.getErrorString(errorCode));
            return;
        }

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Log.d(TAG, "Geofence transition: " + getTransitionString(geofenceTransition));

        // Test that the reported transition was of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                for (Geofence geofence : triggeringGeofences) {
                    String geofenceId = geofence.getRequestId();
                    Log.d(TAG, "Processing geofence: " + geofenceId +
                            ", Transition: " + getTransitionString(geofenceTransition));

                    // Handle geofence transition
                    handleGeofenceTransition(context, geofenceId, geofenceTransition);
                }
            } else {
                Log.w(TAG, "No triggering geofences found");
            }
        } else {
            Log.w(TAG, "Invalid geofence transition type: " + geofenceTransition);
        }
    }

    private void handleGeofenceTransition(Context context, String geofenceId, int transitionType) {
        try {
            // Start ProfileService to handle the geofence transition
            Intent serviceIntent = new Intent(context, ProfileService.class);
            serviceIntent.setAction(ProfileService.ACTION_GEOFENCE_TRANSITION);
            serviceIntent.putExtra(ProfileService.EXTRA_GEOFENCE_ID, geofenceId);
            serviceIntent.putExtra(ProfileService.EXTRA_TRANSITION_TYPE, transitionType);

            // Start as foreground service for Android 8.0+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            Log.d(TAG, "Started ProfileService for geofence: " + geofenceId);
        } catch (Exception e) {
            Log.e(TAG, "Error starting ProfileService", e);
        }
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "ENTER";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "EXIT";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "DWELL";
            default:
                return "UNKNOWN(" + transitionType + ")";
        }
    }
}
