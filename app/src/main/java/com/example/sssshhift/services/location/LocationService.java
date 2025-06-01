package com.example.sssshhift.services.location;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.content.Context;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import android.app.PendingIntent;

public class LocationService extends Service {
    private GeofencingClient geofencingClient;
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        geofencingClient = LocationServices.getGeofencingClient(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            double latitude = intent.getDoubleExtra("LATITUDE", 0);
            double longitude = intent.getDoubleExtra("LONGITUDE", 0);
            float radius = intent.getFloatExtra("RADIUS", 100); // Default 100 meters
            int ringerMode = intent.getIntExtra("RINGER_MODE", AudioManager.RINGER_MODE_NORMAL);
            String geofenceId = intent.getStringExtra("GEOFENCE_ID");

            setupGeofence(latitude, longitude, radius, ringerMode, geofenceId);
        }
        return START_STICKY;
    }

    private void setupGeofence(double latitude, double longitude, float radius, 
                             int ringerMode, String geofenceId) {
        Geofence geofence = new Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | 
                              Geofence.GEOFENCE_TRANSITION_EXIT)
            .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build();

        Intent intent = new Intent(this, LocationReceiver.class);
        intent.putExtra("RINGER_MODE", ringerMode);
        intent.putExtra("GEOFENCE_ID", geofenceId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        geofencingClient.addGeofences(geofencingRequest, pendingIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 