package com.example.sssshhift.receivers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import android.util.Log;
import android.app.NotificationManager;
import android.telephony.TelephonyManager;

import com.example.sssshhift.provider.ProfileContentProvider;
import com.example.sssshhift.models.Profile;
import com.example.sssshhift.utils.NotificationUtils;
import com.example.sssshhift.utils.ProfileUtils;

import java.lang.reflect.Method;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AlarmReceiver triggered!");

        String profileName = intent.getStringExtra("profile_name");
        boolean isStartAlarm = intent.getBooleanExtra("is_start_alarm", true);

        Log.d(TAG, "Profile: " + profileName + ", IsStart: " + isStartAlarm);

        if (profileName == null) {
            Log.e(TAG, "Profile name is null!");
            return;
        }

        // Get profile details from database
        Profile profile = getProfileByName(context, profileName);
        if (profile != null) {
            // IMPORTANT: Only proceed if profile is active in database
            if (!profile.isActive()) {
                Log.d(TAG, "Profile is disabled, skipping: " + profileName);
                return;
            }

            if (isStartAlarm) {
                Log.d(TAG, "Starting profile: " + profileName);
                applyProfile(context, profile);

                // Show notification
                NotificationUtils.showProfileActivatedNotification(context, profileName);

                // Reschedule for next day (both start and end if applicable)
                if ("time".equals(profile.getTriggerType())) {
                    ProfileUtils.scheduleProfile(context, profileName, true, profile.getTriggerValue(), profile.getEndTime());
                    Log.d(TAG, "Rescheduled for tomorrow: " + profileName);
                }
            } else {
                Log.d(TAG, "Ending profile: " + profileName);
                endProfile(context, profile);

                // Show end notification
                NotificationUtils.showProfileEndedNotification(context, profileName);

                // The rescheduling is handled by the start alarm above
            }
        } else {
            Log.e(TAG, "Profile not found: " + profileName);
        }
    }

    private Profile getProfileByName(Context context, String profileName) {
        try {
            String selection = "name = ?";
            String[] selectionArgs = {profileName};

            Cursor cursor = context.getContentResolver().query(
                    ProfileContentProvider.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                Profile profile = new Profile();
                profile.setId(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
                profile.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                profile.setTriggerType(cursor.getString(cursor.getColumnIndexOrThrow("trigger_type")));
                profile.setTriggerValue(cursor.getString(cursor.getColumnIndexOrThrow("trigger_value")));
                profile.setRingerMode(cursor.getString(cursor.getColumnIndexOrThrow("ringer_mode")));
                profile.setActions(cursor.getString(cursor.getColumnIndexOrThrow("actions")));
                profile.setActive(cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1);
                profile.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));

                // Load end time if available
                int endTimeIndex = cursor.getColumnIndex("end_time");
                if (endTimeIndex != -1) {
                    profile.setEndTime(cursor.getString(endTimeIndex));
                }

                cursor.close();
                return profile;
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting profile from database", e);
        }
        return null;
    }

    private void applyProfile(Context context, Profile profile) {
        Log.d(TAG, "Applying profile settings: " + profile.getName());

        // Apply ringer mode
        applyRingerMode(context, profile.getRingerMode());

        // Apply other actions
        applyActions(context, profile.getActions());
    }

    private void endProfile(Context context, Profile profile) {
        Log.d(TAG, "Ending profile settings: " + profile.getName());

        // Revert to normal ringer mode
        applyRingerMode(context, "normal");

        // Revert other actions (turn off what was turned on)
        revertActions(context, profile.getActions());
    }

    private void applyRingerMode(Context context, String ringerMode) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {
                switch (ringerMode.toLowerCase()) {
                    case "silent":
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        Log.d(TAG, "Set to silent mode");
                        break;
                    case "vibrate":
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        Log.d(TAG, "Set to vibrate mode");
                        break;
                    case "normal":
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        Log.d(TAG, "Set to normal mode");
                        break;
                    default:
                        Log.w(TAG, "Unknown ringer mode: " + ringerMode);
                }
            } else {
                Log.e(TAG, "AudioManager is null!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying ringer mode", e);
        }
    }

    private void applyActions(Context context, String actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        String[] actionArray = actions.split(",");

        for (String action : actionArray) {
            action = action.trim();
            switch (action.toLowerCase()) {
                case "wifi":
                    toggleWifi(context, true);
                    break;
                case "bluetooth":
                    toggleBluetooth(context, true);
                    break;
                case "data":
                    toggleMobileData(context, true);
                    break;
                case "dnd":
                    toggleDoNotDisturb(context, true);
                    break;
            }
        }
    }

    private void revertActions(Context context, String actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        String[] actionArray = actions.split(",");

        for (String action : actionArray) {
            action = action.trim();
            switch (action.toLowerCase()) {
                case "wifi":
                    toggleWifi(context, false);
                    break;
                case "bluetooth":
                    toggleBluetooth(context, false);
                    break;
                case "data":
                    toggleMobileData(context, false);
                    break;
                case "dnd":
                    toggleDoNotDisturb(context, false);
                    break;
            }
        }
    }

    private void toggleWifi(Context context, boolean enable) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            if (wifiManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - Try multiple approaches

                    // Method 1: Try direct WifiManager (still works on some devices/ROMs)
                    try {
                        if (enable != wifiManager.isWifiEnabled()) {
                            wifiManager.setWifiEnabled(enable);
                            Log.d(TAG, "WiFi " + (enable ? "enabled" : "disabled") + " using WifiManager");
                            return; // Success!
                        }
                    } catch (SecurityException e) {
                        Log.w(TAG, "WifiManager direct toggle failed, trying reflection", e);
                    }

                    // Method 2: Try reflection approach
                    try {
                        Method method = wifiManager.getClass().getDeclaredMethod("setWifiEnabled", boolean.class);
                        method.setAccessible(true);
                        method.invoke(wifiManager, enable);
                        Log.d(TAG, "WiFi " + (enable ? "enabled" : "disabled") + " using reflection");
                        return; // Success!
                    } catch (Exception reflectionException) {
                        Log.w(TAG, "Reflection method failed", reflectionException);
                    }

                    // Method 3: Try shell command approach (requires root or special permissions)
                    try {
                        String command = enable ? "svc wifi enable" : "svc wifi disable";
                        Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                        Log.d(TAG, "WiFi " + (enable ? "enabled" : "disabled") + " using shell command");
                        return; // May work on rooted devices
                    } catch (Exception shellException) {
                        Log.w(TAG, "Shell command failed", shellException);
                    }

                    // Method 4: Fallback - Show user notification
                    showWifiToggleNotification(context, enable);

                } else {
                    // Android 9 and below - Direct control still works
                    wifiManager.setWifiEnabled(enable);
                    Log.d(TAG, "WiFi " + (enable ? "enabled" : "disabled"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling WiFi", e);
            showWifiToggleNotification(context, enable);
        }
    }

    private void showWifiToggleNotification(Context context, boolean enable) {
        String message = "Profile activated! Please " + (enable ? "enable" : "disable") + " WiFi manually";
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        // Also show a persistent notification
        NotificationUtils.showWifiToggleNotification(context, enable);

        Log.d(TAG, "Showed WiFi toggle notification to user");
    }
    private void toggleBluetooth(Context context, boolean enable) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth not supported on this device");
                return;
            }

            // Check current state and toggle if needed
            boolean isCurrentlyEnabled = bluetoothAdapter.isEnabled();

            if (enable && !isCurrentlyEnabled) {
                // Need to enable Bluetooth
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ - Request user permission
                    try {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(enableBtIntent);
                        Log.d(TAG, "Requested Bluetooth enable permission");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to request Bluetooth enable", e);
                    }
                } else {
                    // Android 12 and below - Direct enable
                    try {
                        bluetoothAdapter.enable();
                        Log.d(TAG, "Bluetooth enabled");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to enable Bluetooth", e);
                    }
                }
            } else if (!enable && isCurrentlyEnabled) {
                // Need to disable Bluetooth
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ - Cannot disable programmatically
                    Log.d(TAG, "Cannot disable Bluetooth programmatically on Android 13+");
                    Toast.makeText(context, "Please manually disable Bluetooth", Toast.LENGTH_LONG).show();
                } else {
                    // Android 12 and below - Direct disable
                    try {
                        bluetoothAdapter.disable();
                        Log.d(TAG, "Bluetooth disabled");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to disable Bluetooth", e);
                    }
                }
            } else {
                // Already in desired state
                Log.d(TAG, "Bluetooth already " + (enable ? "enabled" : "disabled"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling Bluetooth", e);
        }
    }

    private void toggleMobileData(Context context, boolean enable) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Modern Android - Use Settings panel
                Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.d(TAG, "Opened mobile data settings");
            } else {
                // Older Android - Use reflection (may not work on all devices)
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    Method setMobileDataEnabledMethod = telephonyManager.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
                    setMobileDataEnabledMethod.invoke(telephonyManager, enable);
                    Log.d(TAG, "Mobile data " + (enable ? "enabled" : "disabled"));
                } catch (Exception reflectionException) {
                    Log.e(TAG, "Failed to toggle mobile data via reflection", reflectionException);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling mobile data", e);
        }
    }

    private void toggleDoNotDisturb(Context context, boolean enable) {
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (notificationManager.isNotificationPolicyAccessGranted()) {
                        if (enable) {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                            Log.d(TAG, "Do Not Disturb enabled");
                        } else {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                            Log.d(TAG, "Do Not Disturb disabled");
                        }
                    } else {
                        // Request DND permission
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        Log.d(TAG, "Requesting Do Not Disturb permission");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling Do Not Disturb", e);
        }
    }
}