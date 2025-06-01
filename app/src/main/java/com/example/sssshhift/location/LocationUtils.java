package com.example.sssshhift.location;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;

public class LocationUtils {
    private static final String TAG = "LocationUtils";
    private static final String PREF_LOCATION_ENABLED = "location_trigger_enabled";
    private static final String PREF_LOCATION_LAT = "location_latitude";
    private static final String PREF_LOCATION_LNG = "location_longitude";
    private static final String PREF_PREVIOUS_RINGER_MODE = "previous_ringer_mode_location";
    private static final float DEFAULT_GEOFENCE_RADIUS = 100; // meters
    private static final String GEOFENCE_ID = "silent_mode_geofence";

    // Permission request codes
    public static final int REQUEST_LOCATION_PERMISSIONS = 1001;
    public static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 1002;

    private static GeofencingClient geofencingClient;
    private static PendingIntent geofencePendingIntent;

    // Required permissions for different Android versions
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final String BACKGROUND_LOCATION_PERMISSION =
            Manifest.permission.ACCESS_BACKGROUND_LOCATION;

    public static void initializeGeofencing(Context context) {
        geofencingClient = LocationServices.getGeofencingClient(context);
    }

    /**
     * Check if basic location permissions are granted
     */
    public static boolean hasLocationPermissions(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return false;
        }
        return true;
    }

    /**
     * Check if background location permission is granted (Android 10+)
     */
    public static boolean hasBackgroundLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, BACKGROUND_LOCATION_PERMISSION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not required for older Android versions
    }

    /**
     * Check if all required location permissions are granted
     */
    public static boolean hasAllLocationPermissions(Context context) {
        return hasLocationPermissions(context) && 
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                       == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request location permissions
     */
    public static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                LOCATION_PERMISSIONS,
                REQUEST_LOCATION_PERMISSIONS
        );
    }

    /**
     * Request background location permission
     */
    public static void requestBackgroundLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{BACKGROUND_LOCATION_PERMISSION},
                    REQUEST_BACKGROUND_LOCATION_PERMISSION
            );
        }
    }

    /**
     * Check if location services are enabled
     */
    public static boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public static void enableLocationTrigger(Context context, double latitude, double longitude) {
        if (!hasAllLocationPermissions(context)) {
            Log.e(TAG, "Missing required location permissions");
            return;
        }

        if (!isLocationEnabled(context)) {
            Log.e(TAG, "Location services are disabled");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putBoolean(PREF_LOCATION_ENABLED, true)
                .putFloat(PREF_LOCATION_LAT, (float) latitude)
                .putFloat(PREF_LOCATION_LNG, (float) longitude)
                .apply();

        // Initialize geofencing if needed
        if (geofencingClient == null) {
            initializeGeofencing(context);
        }

        // Remove any existing geofences before adding new ones
        removeGeofence(context, () -> {
            setupGeofence(context, latitude, longitude);
        });
    }

    private static void setupGeofence(Context context, double latitude, double longitude) {
        if (!hasLocationPermissions(context)) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        Log.d(TAG, "Setting up geofence at: " + latitude + ", " + longitude);

        try {
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(GEOFENCE_ID)
                    .setCircularRegion(latitude, longitude, DEFAULT_GEOFENCE_RADIUS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | 
                                      Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setNotificationResponsiveness(5000) // 5 seconds
                    .setLoiteringDelay(30000) // 30 seconds for dwell
                    .build();

            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing fine location permission");
                return;
            }

            geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent(context))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Geofence added successfully");
                        // Store the timestamp of when the geofence was added
                        PreferenceManager.getDefaultSharedPreferences(context)
                                .edit()
                                .putLong("geofence_setup_time", System.currentTimeMillis())
                                .apply();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add geofence", e);
                        // Notify the user about the failure
                        notifyGeofenceError(context, e.getMessage());
                    });

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when adding geofence", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid geofence parameters", e);
        }
    }

    private static void removeGeofence(Context context, Runnable onComplete) {
        if (geofencingClient != null) {
            geofencingClient.removeGeofences(getGeofencePendingIntent(context))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Geofence removed successfully");
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove geofence", e);
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
        } else {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private static PendingIntent getGeofencePendingIntent(Context context) {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, LocationTriggerReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        return geofencePendingIntent;
    }

    public static void disableLocationTrigger(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putBoolean(PREF_LOCATION_ENABLED, false)
                .remove(PREF_LOCATION_LAT)
                .remove(PREF_LOCATION_LNG)
                .apply();

        removeGeofence(context, () -> {
            // Revert ringer mode after geofence is removed
            revertRingerMode(context);
        });
    }

    private static void notifyGeofenceError(Context context, String error) {
        Intent intent = new Intent("com.example.sssshhift.GEOFENCE_ERROR");
        intent.putExtra("error_message", error);
        context.sendBroadcast(intent);
    }

    public static void handleLocationTrigger(Context context, boolean entering) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (audioManager != null) {
            try {
                if (entering) {
                    // Store current ringer mode before changing to silent
                    int currentMode = audioManager.getRingerMode();
                    Log.d(TAG, "Current ringer mode: " + getRingerModeName(currentMode));
                    
                    prefs.edit()
                        .putInt(PREF_PREVIOUS_RINGER_MODE, currentMode)
                        .apply();

                    // Change to silent mode
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    Log.d(TAG, "Changed to silent mode (location-based)");
                } else {
                    // Only revert if we're still in silent mode
                    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                        revertRingerMode(context);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception when changing ringer mode", e);
            }
        } else {
            Log.e(TAG, "AudioManager is null");
        }
    }

    private static void revertRingerMode(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (audioManager != null) {
            int previousMode = prefs.getInt(PREF_PREVIOUS_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
            audioManager.setRingerMode(previousMode);
            Log.d(TAG, "Reverted to previous mode: " + previousMode);
        }
    }

    public static boolean isLocationTriggerEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_LOCATION_ENABLED, false);
    }

    /**
     * Calculate distance between two points in meters
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Format distance for display
     */
    public static String formatDistance(double distanceInMeters) {
        if (distanceInMeters < 1000) {
            return String.format("%.0f m", distanceInMeters);
        } else {
            return String.format("%.1f km", distanceInMeters / 1000);
        }
    }

    /**
     * Check if coordinates are valid
     */
    public static boolean areCoordinatesValid(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    public static Location getLastKnownLocation(Context context) {
        if (!hasLocationPermissions(context)) {
            return null;
        }

        try {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            Task<Location> task = fusedLocationClient.getLastLocation();
            return task.getResult();
        } catch (SecurityException e) {
            Log.e(TAG, "Error getting last known location", e);
            return null;
        }
    }

    private static String getRingerModeName(int mode) {
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                return "Silent";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "Vibrate";
            case AudioManager.RINGER_MODE_NORMAL:
                return "Normal";
            default:
                return "Unknown";
        }
    }
} 