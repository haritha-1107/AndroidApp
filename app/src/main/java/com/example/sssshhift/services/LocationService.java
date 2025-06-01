package com.example.sssshhift.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.sssshhift.R;
import com.example.sssshhift.MainActivity;
import com.example.sssshhift.data.ProfileDatabaseHelper;
import com.example.sssshhift.models.Profile;
import com.example.sssshhift.provider.ProfileContentProvider;
import com.example.sssshhift.utils.LocationHandler;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationService extends Service implements LocationHandler.LocationUpdateListener {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private LocationHandler locationHandler;
    private List<Profile> locationBasedProfiles;

    @Override
    public void onCreate() {
        super.onCreate();
        locationHandler = new LocationHandler(this);
        locationHandler.setLocationUpdateListener(this);
        locationBasedProfiles = new ArrayList<>();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        loadLocationBasedProfiles();
        locationHandler.startLocationUpdates();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationUpdate(Location location) {
        Log.d(TAG, "Location update received: " + location.getLatitude() + ", " + location.getLongitude());
        checkAndTriggerProfiles(location);
    }

    @Override
    public void onLocationError(String error) {
        Log.e(TAG, "Location error: " + error);
    }

    private void loadLocationBasedProfiles() {
        locationBasedProfiles.clear();
        ContentResolver resolver = getContentResolver();
        String selection = ProfileDatabaseHelper.COLUMN_TRIGGER_TYPE + "=? AND " + 
                          ProfileDatabaseHelper.COLUMN_IS_ACTIVE + "=?";
        String[] selectionArgs = new String[]{"location", "1"};

        try (Cursor cursor = resolver.query(
                ProfileContentProvider.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Profile profile = new Profile();
                    profile.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_ID)));
                    profile.setName(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_NAME)));
                    profile.setTriggerType(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_TRIGGER_TYPE)));
                    profile.setTriggerValue(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_TRIGGER_VALUE)));
                    profile.setRingerMode(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_RINGER_MODE)));
                    profile.setActions(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_ACTIONS)));
                    locationBasedProfiles.add(profile);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading location-based profiles", e);
        }
    }

    private void checkAndTriggerProfiles(Location currentLocation) {
        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        for (Profile profile : locationBasedProfiles) {
            try {
                String[] coords = profile.getTriggerValue().split(",");
                if (coords.length == 2) {
                    double lat = Double.parseDouble(coords[0]);
                    double lng = Double.parseDouble(coords[1]);
                    LatLng profileLocation = new LatLng(lat, lng);

                    if (LocationHandler.isInGeofence(currentLatLng, profileLocation, 100)) {
                        // Apply profile settings
                        LocationHandler.applyRingerMode(this, profile.getRingerMode());
                        // TODO: Apply other actions (WiFi, Bluetooth, etc.)
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing profile: " + profile.getName(), e);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Monitoring location for profile triggers")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationHandler != null) {
            locationHandler.stopLocationUpdates();
        }
    }
} 