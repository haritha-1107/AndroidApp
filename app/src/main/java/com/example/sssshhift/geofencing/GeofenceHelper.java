package com.example.sssshhift.geofencing;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.example.sssshhift.receivers.GeofenceReceiver;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.location.GeofenceStatusCodes;

public class GeofenceHelper {
    private static final String TAG = "GeofenceHelper";
    private static final float GEOFENCE_RADIUS_IN_METERS = 100f;
    private static final long GEOFENCE_EXPIRATION_TIME = Geofence.NEVER_EXPIRE;

    private GeofencingClient geofencingClient;
    private Context context;
    private PendingIntent geofencePendingIntent;

    public interface GeofenceCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public GeofenceHelper(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    private GeofencingRequest getGeofencingRequest(List<Geofence> geofenceList) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // Set initial trigger to ENTER for immediate activation
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceReceiver.class);

        // Use correct flags based on Android version
        int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        } else {
            flags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        geofencePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                flags
        );
        return geofencePendingIntent;
    }

    public Geofence createGeofence(String geofenceId, double latitude, double longitude, float radius) {
        return new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(GEOFENCE_EXPIRATION_TIME)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(30000) // 30 seconds loitering delay
                .build();
    }

    public Geofence createGeofence(String geofenceId, double latitude, double longitude) {
        return createGeofence(geofenceId, latitude, longitude, GEOFENCE_RADIUS_IN_METERS);
    }

    public void addGeofence(String geofenceId, double latitude, double longitude, GeofenceCallback callback) {
        addGeofence(geofenceId, latitude, longitude, GEOFENCE_RADIUS_IN_METERS, callback);
    }

    public void addGeofence(String geofenceId, double latitude, double longitude, float radius, GeofenceCallback callback) {
        List<Geofence> geofenceList = new ArrayList<>();
        geofenceList.add(createGeofence(geofenceId, latitude, longitude, radius));
        addGeofences(geofenceList, callback);
    }

    public void addGeofences(List<Geofence> geofenceList, GeofenceCallback callback) {
        try {
            Log.d(TAG, "Adding " + geofenceList.size() + " geofences");

            geofencingClient.addGeofences(getGeofencingRequest(geofenceList), getGeofencePendingIntent())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Geofences added successfully");
                                if (callback != null) {
                                    callback.onSuccess("Geofences added successfully");
                                }
                            } else {
                                Exception exception = task.getException();
                                String error = "Failed to add geofences";
                                if (exception != null) {
                                    error += ": " + exception.getMessage();
                                    Log.e(TAG, error, exception);
                                }
                                if (callback != null) {
                                    callback.onFailure(error);
                                }
                            }
                        }
                    });
        } catch (SecurityException e) {
            String error = "Location permission not granted: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onFailure(error);
            }
        } catch (Exception e) {
            String error = "Error adding geofences: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onFailure(error);
            }
        }
    }

    public void removeGeofences(List<String> geofenceIds, GeofenceCallback callback) {
        Log.d(TAG, "Removing geofences: " + geofenceIds);

        geofencingClient.removeGeofences(geofenceIds)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Geofences removed successfully");
                            if (callback != null) {
                                callback.onSuccess("Geofences removed successfully");
                            }
                        } else {
                            Exception exception = task.getException();
                            String error = "Failed to remove geofences";
                            if (exception != null) {
                                error += ": " + exception.getMessage();
                                Log.e(TAG, error, exception);
                            }
                            if (callback != null) {
                                callback.onFailure(error);
                            }
                        }
                    }
                });
    }

    public void removeGeofence(String geofenceId, GeofenceCallback callback) {
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(geofenceId);
        removeGeofences(geofenceIds, callback);
    }

    public void removeAllGeofences(GeofenceCallback callback) {
        Log.d(TAG, "Removing all geofences");

        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "All geofences removed successfully");
                            if (callback != null) {
                                callback.onSuccess("All geofences removed successfully");
                            }
                        } else {
                            Exception exception = task.getException();
                            String error = "Failed to remove all geofences";
                            if (exception != null) {
                                error += ": " + exception.getMessage();
                                Log.e(TAG, error, exception);
                            }
                            if (callback != null) {
                                callback.onFailure(error);
                            }
                        }
                    }
                });
    }

    public static String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Geofence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error: " + errorCode;
        }
    }
}