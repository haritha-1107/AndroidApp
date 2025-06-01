package com.example.sssshhift.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.example.sssshhift.data.ProfileDatabaseHelper;
import com.example.sssshhift.utils.NotificationUtils;
import com.example.sssshhift.utils.PhoneSettingsManager;
import com.example.sssshhift.utils.ProfileUtils;

public class ProfileTimerReceiver extends BroadcastReceiver {
    private static final String TAG = "ProfileTimerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ProfileTimerReceiver triggered");

        try {
            String profileName = intent.getStringExtra("profile_name");
            boolean isStartTime = intent.getBooleanExtra("is_start_time", true);
            String scheduledTime = intent.getStringExtra("scheduled_time");

            if (profileName == null) {
                Log.e(TAG, "Profile name is null");
                return;
            }

            Log.d(TAG, "Processing alarm for profile: " + profileName +
                    ", isStartTime: " + isStartTime +
                    ", scheduledTime: " + scheduledTime);

            if (isStartTime) {
                // Activate the profile
                activateProfile(context, profileName);

                // Show notification
                NotificationUtils.showProfileActivatedNotification(context, profileName);

                // Reschedule for next day (daily recurring)
                ProfileUtils.rescheduleProfileForNextDay(context, profileName, scheduledTime, true);

            } else {
                // Deactivate the profile (end time)
                deactivateProfile(context, profileName);

                // Show notification
                NotificationUtils.showProfileEndedNotification(context, profileName);

                // Reschedule for next day (daily recurring)
                ProfileUtils.rescheduleProfileForNextDay(context, profileName, scheduledTime, false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing profile timer", e);
            Toast.makeText(context, "Error processing profile timer: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void activateProfile(Context context, String profileName) {
        Log.d(TAG, "Activating profile: " + profileName);

        try {
            // Get profile from database
            ProfileDatabaseHelper dbHelper = new ProfileDatabaseHelper(context);
            Cursor cursor = dbHelper.getProfileByName(profileName);

            if (cursor != null && cursor.moveToFirst()) {
                // Extract profile settings
                String ringerMode = cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_RINGER_MODE));
                String actions = cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_ACTIONS));

                // Apply ringer mode
                PhoneSettingsManager.setRingerMode(context, ringerMode);

                // Apply actions (Wi-Fi, Bluetooth, etc.)
                if (actions != null && !actions.isEmpty()) {
                    applyProfileActions(context, actions, true);
                }

                // Mark profile as active in database
                dbHelper.updateProfileActiveStatus(profileName, true);

                Log.d(TAG, "Profile activated successfully: " + profileName);

            } else {
                Log.e(TAG, "Profile not found in database: " + profileName);
            }

            if (cursor != null) {
                cursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error activating profile: " + profileName, e);
        }
    }

    private void deactivateProfile(Context context, String profileName) {
        Log.d(TAG, "Deactivating profile: " + profileName);

        try {
            // Get profile from database
            ProfileDatabaseHelper dbHelper = new ProfileDatabaseHelper(context);
            Cursor cursor = dbHelper.getProfileByName(profileName);

            if (cursor != null && cursor.moveToFirst()) {
                // Extract profile settings to reverse them
                String actions = cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_ACTIONS));

                // Restore normal ringer mode
                PhoneSettingsManager.setRingerMode(context, "normal");

                // Reverse actions (if any)
                if (actions != null && !actions.isEmpty()) {
                    applyProfileActions(context, actions, false);
                }

                // Mark profile as inactive in database
                dbHelper.updateProfileActiveStatus(profileName, false);

                Log.d(TAG, "Profile deactivated successfully: " + profileName);

            } else {
                Log.e(TAG, "Profile not found in database: " + profileName);
            }

            if (cursor != null) {
                cursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deactivating profile: " + profileName, e);
        }
    }

    private void applyProfileActions(Context context, String actions, boolean activate) {
        Log.d(TAG, "Applying profile actions: " + actions + ", activate: " + activate);

        if (actions == null || actions.isEmpty()) {
            return;
        }

        String[] actionArray = actions.split(",");

        for (String action : actionArray) {
            action = action.trim();

            switch (action) {
                case "wifi":
                    // Note: Direct Wi-Fi control requires system-level permissions
                    // Show notification instead for user to manually toggle
                    NotificationUtils.showWifiToggleNotification(context, activate);
                    break;

                case "bluetooth":
                    PhoneSettingsManager.toggleBluetooth(context, activate);
                    break;

                case "data":
                    // Mobile data toggle requires system permissions
                    // Show notification for manual toggle
                    Log.d(TAG, "Mobile data toggle requested: " + activate);
                    break;

                case "dnd":
                    if (activate) {
                        PhoneSettingsManager.enableDoNotDisturb(context);
                    } else {
                        PhoneSettingsManager.disableDoNotDisturb(context);
                    }
                    break;

                default:
                    Log.w(TAG, "Unknown action: " + action);
                    break;
            }
        }
    }
}