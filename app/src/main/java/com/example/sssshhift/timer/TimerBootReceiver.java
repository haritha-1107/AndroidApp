package com.example.sssshhift.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TimerBootReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerBootReceiver";
    private static final String PREFS_NAME = "TimerProfiles";
    private static final String PROFILES_KEY = "active_profiles";
    private static final long RESTORE_DELAY = 30000; // 30 seconds delay after boot

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        // Handle various system events
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_REBOOT:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
            case Intent.ACTION_TIMEZONE_CHANGED:
            case Intent.ACTION_TIME_CHANGED:
                // Acquire wake lock to ensure we complete our work
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                
                try {
                    if (powerManager != null) {
                        wakeLock = powerManager.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "Sssshhhift:TimerBootWakeLock"
                        );
                        wakeLock.acquire(10 * 60 * 1000L); // 10 minutes max
                    }

                    // Delay the restore to ensure system is fully booted
                    new Handler(Looper.getMainLooper()).postDelayed(
                        () -> restoreTimerProfiles(context), 
                        RESTORE_DELAY
                    );

                } finally {
                    if (wakeLock != null && wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                }
                break;
        }
    }

    private void restoreTimerProfiles(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String profilesJson = prefs.getString(PROFILES_KEY, "[]");

        try {
            JSONArray profiles = new JSONArray(profilesJson);
            TimerProfileManager profileManager = new TimerProfileManager(context);
            long currentTime = System.currentTimeMillis();

            Log.d(TAG, "Restoring " + profiles.length() + " timer profiles");

            for (int i = 0; i < profiles.length(); i++) {
                JSONObject profile = profiles.getJSONObject(i);
                long startTime = profile.getLong("startTime");
                long endTime = profile.getLong("endTime");
                int ringerMode = profile.getInt("ringerMode");
                String profileName = profile.getString("name");

                // Handle different scenarios
                if (endTime <= currentTime) {
                    // Profile has completely ended, remove it
                    removeProfile(context, startTime, endTime);
                    Log.d(TAG, "Removed expired profile: " + profileName);
                    
                } else if (startTime <= currentTime && currentTime < endTime) {
                    // We're in the middle of the profile, schedule the end and activate silent mode
                    profileManager.scheduleProfile(currentTime, endTime, ringerMode, profileName);
                    
                    // Immediately set the ringer mode
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null) {
                        audioManager.setRingerMode(ringerMode);
                    }
                    
                    Log.d(TAG, "Restored active profile: " + profileName + " (end time only)");
                    
                } else if (startTime > currentTime) {
                    // Profile hasn't started yet, schedule both start and end
                    profileManager.scheduleProfile(startTime, endTime, ringerMode, profileName);
                    Log.d(TAG, "Restored future profile: " + profileName);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error restoring timer profiles", e);
        }
    }

    // Static methods to save/remove profiles
    public static void saveProfile(Context context, long startTime, long endTime, 
                                 int ringerMode, String profileName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String profilesJson = prefs.getString(PROFILES_KEY, "[]");

        try {
            JSONArray profiles = new JSONArray(profilesJson);
            
            // Remove any existing profile with the same times
            JSONArray updatedProfiles = new JSONArray();
            for (int i = 0; i < profiles.length(); i++) {
                JSONObject existingProfile = profiles.getJSONObject(i);
                if (existingProfile.getLong("startTime") != startTime || 
                    existingProfile.getLong("endTime") != endTime) {
                    updatedProfiles.put(existingProfile);
                }
            }

            // Add the new profile
            JSONObject newProfile = new JSONObject();
            newProfile.put("startTime", startTime);
            newProfile.put("endTime", endTime);
            newProfile.put("ringerMode", ringerMode);
            newProfile.put("name", profileName);
            updatedProfiles.put(newProfile);

            // Save the updated profiles
            prefs.edit().putString(PROFILES_KEY, updatedProfiles.toString()).apply();
            Log.d(TAG, "Saved profile: " + profileName);

        } catch (JSONException e) {
            Log.e(TAG, "Error saving profile", e);
        }
    }

    public static void removeProfile(Context context, long startTime, long endTime) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String profilesJson = prefs.getString(PROFILES_KEY, "[]");

        try {
            JSONArray profiles = new JSONArray(profilesJson);
            JSONArray newProfiles = new JSONArray();

            for (int i = 0; i < profiles.length(); i++) {
                JSONObject profile = profiles.getJSONObject(i);
                if (profile.getLong("startTime") != startTime || 
                    profile.getLong("endTime") != endTime) {
                    newProfiles.put(profile);
                }
            }

            prefs.edit().putString(PROFILES_KEY, newProfiles.toString()).apply();
            Log.d(TAG, "Removed profile with start time: " + startTime + ", end time: " + endTime);

        } catch (JSONException e) {
            Log.e(TAG, "Error removing profile", e);
        }
    }
} 