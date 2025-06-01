package com.example.sssshhift.geofencing;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.example.sssshhift.location.LocationUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main controller class for managing all geofencing operations
 */
public class GeofenceManager {

    private static final String TAG = "GeofenceManager";
    private static GeofenceManager instance;

    private Context context;
    private GeofenceHelper geofenceHelper;
    private Map<String, GeofenceData> activeGeofences;

    /**
     * Data class to hold geofence information
     */
    public static class GeofenceData {
        private String id;
        private String profileId;
        private double latitude;
        private double longitude;
        private float radius;
        private String name;
        private boolean isActive;

        public GeofenceData(String id, String profileId, double latitude, double longitude, float radius, String name) {
            this.id = id;
            this.profileId = profileId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.radius = radius;
            this.name = name;
            this.isActive = false;
        }

        // Getters and setters
        public String getId() { return id; }
        public String getProfileId() { return profileId; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public float getRadius() { return radius; }
        public String getName() { return name; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { this.isActive = active; }
    }

    public interface GeofenceManagerCallback {
        void onGeofenceAdded(String geofenceId, boolean success, String message);
        void onGeofenceRemoved(String geofenceId, boolean success, String message);
        void onError(String error);
    }

    private GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofenceHelper = new GeofenceHelper(this.context);
        this.activeGeofences = new HashMap<>();
    }

    public static synchronized GeofenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new GeofenceManager(context);
        }
        return instance;
    }

    /**
     * Add a geofence for a profile
     */
    public void addProfileGeofence(String profileId, String locationName, double latitude, double longitude,
                                   float radius, GeofenceManagerCallback callback) {

        // Add location services check FIRST
        if (!LocationUtils.isLocationEnabled(context)) {
            String error = "Location services are disabled. Please enable them in device settings.";
            Log.e(TAG, error);
            if (callback != null) callback.onError(error);
            return;
        }

        // Validate input
        if (!LocationUtils.areCoordinatesValid(latitude, longitude)) {
            String error = "Invalid coordinates: " + latitude + ", " + longitude;
            Log.e(TAG, error);
            if (callback != null) callback.onError(error);
            return;
        }

        // Create unique geofence ID
        String geofenceId = "profile_" + profileId + "_" + System.currentTimeMillis();

        // Create geofence data
        GeofenceData geofenceData = new GeofenceData(geofenceId, profileId, latitude, longitude, radius, locationName);

        // Add geofence using helper
        geofenceHelper.addGeofence(geofenceId, latitude, longitude, radius, new GeofenceHelper.GeofenceCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Geofence added successfully: " + geofenceId);
                geofenceData.setActive(true);
                activeGeofences.put(geofenceId, geofenceData);

                if (callback != null) {
                    callback.onGeofenceAdded(geofenceId, true, message);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to add geofence: " + error);
                if (callback != null) {
                    callback.onGeofenceAdded(geofenceId, false, error);
                }
            }
        });
    }

    /**
     * Remove a specific geofence
     */
    public void removeGeofence(String geofenceId, GeofenceManagerCallback callback) {
        if (!activeGeofences.containsKey(geofenceId)) {
            String error = "Geofence not found: " + geofenceId;
            Log.w(TAG, error);
            if (callback != null) callback.onError(error);
            return;
        }

        geofenceHelper.removeGeofence(geofenceId, new GeofenceHelper.GeofenceCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Geofence removed successfully: " + geofenceId);
                activeGeofences.remove(geofenceId);

                if (callback != null) {
                    callback.onGeofenceRemoved(geofenceId, true, message);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to remove geofence: " + error);
                if (callback != null) {
                    callback.onGeofenceRemoved(geofenceId, false, error);
                }
            }
        });
    }

    /**
     * Remove all geofences for a specific profile
     */
    public void removeProfileGeofences(String profileId, GeofenceManagerCallback callback) {
        List<String> geofencesToRemove = new ArrayList<>();

        for (GeofenceData geofenceData : activeGeofences.values()) {
            if (geofenceData.getProfileId().equals(profileId)) {
                geofencesToRemove.add(geofenceData.getId());
            }
        }

        if (geofencesToRemove.isEmpty()) {
            Log.d(TAG, "No geofences found for profile: " + profileId);
            return;
        }

        geofenceHelper.removeGeofences(geofencesToRemove, new GeofenceHelper.GeofenceCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Profile geofences removed successfully: " + profileId);

                // Remove from active geofences
                for (String geofenceId : geofencesToRemove) {
                    activeGeofences.remove(geofenceId);
                }

                if (callback != null) {
                    callback.onGeofenceRemoved(profileId, true, message);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to remove profile geofences: " + error);
                if (callback != null) {
                    callback.onGeofenceRemoved(profileId, false, error);
                }
            }
        });
    }

    /**
     * Remove all geofences
     */
    public void removeAllGeofences(GeofenceManagerCallback callback) {
        geofenceHelper.removeAllGeofences(new GeofenceHelper.GeofenceCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "All geofences removed successfully");
                activeGeofences.clear();

                if (callback != null) {
                    callback.onGeofenceRemoved("all", true, message);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to remove all geofences: " + error);
                if (callback != null) {
                    callback.onGeofenceRemoved("all", false, error);
                }
            }
        });
    }

    /**
     * Get all active geofences
     */
    public List<GeofenceData> getActiveGeofences() {
        return new ArrayList<>(activeGeofences.values());
    }

    /**
     * Get geofences for a specific profile
     */
    public List<GeofenceData> getProfileGeofences(String profileId) {
        List<GeofenceData> profileGeofences = new ArrayList<>();
        for (GeofenceData geofenceData : activeGeofences.values()) {
            if (geofenceData.getProfileId().equals(profileId)) {
                profileGeofences.add(geofenceData);
            }
        }
        return profileGeofences;
    }

    /**
     * Check if a profile has any active geofences
     */
    public boolean hasProfileGeofences(String profileId) {
        for (GeofenceData geofenceData : activeGeofences.values()) {
            if (geofenceData.getProfileId().equals(profileId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get geofence data by ID
     */
    public GeofenceData getGeofenceData(String geofenceId) {
        return activeGeofences.get(geofenceId);
    }

    /**
     * Check if location services are available
     */
    public boolean isLocationServicesAvailable() {
        return LocationUtils.isLocationEnabled(context) &&
                LocationUtils.hasLocationPermissions(context);
    }

    /**
     * Get total number of active geofences
     */
    public int getActiveGeofenceCount() {
        return activeGeofences.size();
    }



}