package com.example.sssshhift.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

public class LocationHandler {
    private static final String TAG = "LocationHandler";
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_INTERVAL = 5000; // 5 seconds
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationUpdateListener locationUpdateListener;

    public interface LocationUpdateListener {
        void onLocationUpdate(Location location);
        void onLocationError(String error);
    }

    public LocationHandler(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationUpdateListener = listener;
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
            LOCATION_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        if (!hasLocationPermission()) {
            if (locationUpdateListener != null) {
                locationUpdateListener.onLocationError("Location permission not granted");
            }
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
            .setIntervalMillis(UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && locationUpdateListener != null) {
                    locationUpdateListener.onLocationUpdate(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            if (locationUpdateListener != null) {
                locationUpdateListener.onLocationError("Security exception: " + e.getMessage());
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation() {
        if (!hasLocationPermission()) {
            if (locationUpdateListener != null) {
                locationUpdateListener.onLocationError("Location permission not granted");
            }
            return;
        }

        try {
            // Get last known location for immediate update
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && locationUpdateListener != null) {
                        locationUpdateListener.onLocationUpdate(location);
                    }
                })
                .addOnFailureListener(e -> {
                    if (locationUpdateListener != null) {
                        locationUpdateListener.onLocationError("Failed to get location: " + e.getMessage());
                    }
                });
        } catch (SecurityException e) {
            if (locationUpdateListener != null) {
                locationUpdateListener.onLocationError("Security exception: " + e.getMessage());
            }
        }
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    public static boolean isInGeofence(LatLng currentLocation, LatLng geofenceCenter, float radiusMeters) {
        float[] distance = new float[1];
        Location.distanceBetween(
            currentLocation.latitude, currentLocation.longitude,
            geofenceCenter.latitude, geofenceCenter.longitude,
            distance
        );
        return distance[0] <= radiusMeters;
    }

    public static void applyRingerMode(Context context, String ringerMode) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            switch (ringerMode.toLowerCase()) {
                case "silent":
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    break;
                case "vibrate":
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    break;
                case "normal":
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    break;
            }
            Toast.makeText(context, "Ringer mode changed to " + ringerMode, Toast.LENGTH_SHORT).show();
        }
    }
} 