package com.example.sssshhift.services.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class LocationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        
        if (geofencingEvent != null && !geofencingEvent.hasError()) {
            int geofenceTransition = geofencingEvent.getGeofenceTransition();
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // When entering the geofence, apply the specified ringer mode
                int ringerMode = intent.getIntExtra("RINGER_MODE", AudioManager.RINGER_MODE_NORMAL);
                if (audioManager != null) {
                    audioManager.setRingerMode(ringerMode);
                }
            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                // When exiting the geofence, reset to normal mode
                if (audioManager != null) {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
            }
        }
    }
} 