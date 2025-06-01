package com.example.sssshhift.location;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.example.sssshhift.location.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocationProfileManager {
    private static final String TAG = "LocationProfileManager";
    private static final String PREFS_NAME = "location_profiles";
    private static final float DEFAULT_RADIUS_METERS = 100f;

    private final Context context;
    private final SharedPreferences prefs;
    private final AudioManager audioManager;
    private final NotificationManager notificationManager;

    public LocationProfileManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    public String createLocationProfile(String name, LatLng location, int ringerMode, float radiusMeters) {
        if (!checkPermissions()) {
            Log.e(TAG, "Required permissions not granted");
            return null;
        }

        String profileId = UUID.randomUUID().toString();
        try {
            // Save profile details
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(profileId + "_name", name);
            editor.putFloat(profileId + "_lat", (float) location.latitude);
            editor.putFloat(profileId + "_lng", (float) location.longitude);
            editor.putInt(profileId + "_mode", ringerMode);
            editor.putFloat(profileId + "_radius", radiusMeters);
            editor.apply();

            // Create and register geofence
            Geofence geofence = new Geofence.Builder()
                .setRequestId(profileId)
                .setCircularRegion(location.latitude, location.longitude, radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

            LocationServices.getGeofencingClient(context)
                .addGeofences(geofencingRequest, LocationReceiver.getPendingIntent(context))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofence registered successfully for profile: " + name);
                    
                    // Check if user is already in the geofence
                    Location currentLocation = LocationUtils.getLastKnownLocation(context);
                    if (currentLocation != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            location.latitude, location.longitude,
                            results
                        );
                        
                        // If within radius, trigger the profile immediately
                        if (results[0] <= radiusMeters) {
                            Log.d(TAG, "User is already in geofence area. Triggering profile immediately.");
                            changeRingerMode(ringerMode, name);
                            showNotification(context, name, ringerMode, true);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to register geofence: " + e.getMessage());
                    // Clean up saved preferences if geofence registration fails
                    editor.remove(profileId + "_name");
                    editor.remove(profileId + "_lat");
                    editor.remove(profileId + "_lng");
                    editor.remove(profileId + "_mode");
                    editor.remove(profileId + "_radius");
                    editor.apply();
                });

            return profileId;
        } catch (Exception e) {
            Log.e(TAG, "Error creating location profile: " + e.getMessage());
            return null;
        }
    }

    public void removeLocationProfile(String profileId) {
        try {
            // Remove geofence
            List<String> geofenceIds = new ArrayList<>();
            geofenceIds.add(profileId);
            LocationServices.getGeofencingClient(context)
                .removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence removed successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to remove geofence: " + e.getMessage()));

            // Remove profile data
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(profileId + "_name");
            editor.remove(profileId + "_lat");
            editor.remove(profileId + "_lng");
            editor.remove(profileId + "_mode");
            editor.remove(profileId + "_radius");
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error removing location profile: " + e.getMessage());
        }
    }

    public boolean changeRingerMode(int ringerMode, String profileName) {
        try {
            // Check DND permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!notificationManager.isNotificationPolicyAccessGranted()) {
                    Log.e(TAG, "DND permission not granted");
                    return false;
                }
            }

            // Change ringer mode
            audioManager.setRingerMode(ringerMode);
            
            // Verify the change
            if (audioManager.getRingerMode() == ringerMode) {
                Log.d(TAG, "Ringer mode changed successfully for profile: " + profileName);
                return true;
            } else {
                Log.e(TAG, "Failed to change ringer mode");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error changing ringer mode: " + e.getMessage());
            return false;
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                Log.e(TAG, "DND permission not granted");
                return false;
            }
        }
        return LocationUtils.hasLocationPermissions(context);
    }

    public List<LocationProfile> getAllProfiles() {
        List<LocationProfile> profiles = new ArrayList<>();
        try {
            for (String key : prefs.getAll().keySet()) {
                if (key.endsWith("_name")) {
                    String profileId = key.replace("_name", "");
                    String name = prefs.getString(profileId + "_name", "");
                    float lat = prefs.getFloat(profileId + "_lat", 0);
                    float lng = prefs.getFloat(profileId + "_lng", 0);
                    int mode = prefs.getInt(profileId + "_mode", AudioManager.RINGER_MODE_NORMAL);
                    float radius = prefs.getFloat(profileId + "_radius", DEFAULT_RADIUS_METERS);

                    profiles.add(new LocationProfile(
                        profileId, name, new LatLng(lat, lng), mode, radius
                    ));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profiles: " + e.getMessage());
        }
        return profiles;
    }

    private void showNotification(Context context, String profileName, int ringerMode, boolean isEntering) {
        String title = isEntering ? "Profile Activated" : "Profile Deactivated";
        String message = isEntering ? 
            "Entered location zone for " + profileName :
            "Left location zone for " + profileName;

        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "location_profiles")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

            notificationManager.notify(profileName.hashCode(), builder.build());
        }
    }
} 