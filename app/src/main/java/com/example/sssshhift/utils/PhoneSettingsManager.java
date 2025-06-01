package com.example.sssshhift.utils;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

public class PhoneSettingsManager {
    private static final String TAG = "PhoneSettingsManager";

    public static void setRingerMode(Context context, String ringerMode) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                Log.e(TAG, "AudioManager is null");
                return;
            }

            int mode;
            switch (ringerMode.toLowerCase()) {
                case "silent":
                    mode = AudioManager.RINGER_MODE_SILENT;
                    break;
                case "vibrate":
                    mode = AudioManager.RINGER_MODE_VIBRATE;
                    break;
                case "normal":
                default:
                    mode = AudioManager.RINGER_MODE_NORMAL;
                    break;
            }

            // Check if we have Do Not Disturb permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted()) {
                    Log.w(TAG, "Do Not Disturb permission not granted, cannot change ringer mode");
                    // Request permission through notification
                    NotificationUtils.showPermissionRequiredNotification(context);
                    return;
                }
            }

            audioManager.setRingerMode(mode);
            Log.d(TAG, "Ringer mode set to: " + ringerMode);

        } catch (Exception e) {
            Log.e(TAG, "Error setting ringer mode to: " + ringerMode, e);
        }
    }

    public static void toggleBluetooth(Context context, boolean enable) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth not supported on this device");
                return;
            }

            if (enable && !bluetoothAdapter.isEnabled()) {
                // Note: BluetoothAdapter.enable() is deprecated in API 33+
                // For newer versions, you should use Intent to ask user
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Log.d(TAG, "Bluetooth enable/disable requires user interaction on Android 13+");
                    // Could show notification to user to manually enable Bluetooth
                } else {
                    @SuppressWarnings("deprecation")
                    boolean result = bluetoothAdapter.enable();
                    Log.d(TAG, "Bluetooth enable requested: " + result);
                }
            } else if (!enable && bluetoothAdapter.isEnabled()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Log.d(TAG, "Bluetooth enable/disable requires user interaction on Android 13+");
                } else {
                    @SuppressWarnings("deprecation")
                    boolean result = bluetoothAdapter.disable();
                    Log.d(TAG, "Bluetooth disable requested: " + result);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error toggling Bluetooth: " + enable, e);
        }
    }

    public static void enableDoNotDisturb(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
                    // Set Do Not Disturb mode
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                    Log.d(TAG, "Do Not Disturb enabled");
                } else {
                    Log.w(TAG, "Do Not Disturb permission not granted");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling Do Not Disturb", e);
        }
    }

    public static void disableDoNotDisturb(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted()) {
                    // Restore normal notification mode
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                    Log.d(TAG, "Do Not Disturb disabled");
                } else {
                    Log.w(TAG, "Do Not Disturb permission not granted");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling Do Not Disturb", e);
        }
    }

    public static boolean isDoNotDisturbEnabled(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager != null) {
                    int filter = notificationManager.getCurrentInterruptionFilter();
                    return filter != NotificationManager.INTERRUPTION_FILTER_ALL;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Do Not Disturb status", e);
        }
        return false;
    }

    public static String getCurrentRingerMode(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int mode = audioManager.getRingerMode();
                switch (mode) {
                    case AudioManager.RINGER_MODE_SILENT:
                        return "silent";
                    case AudioManager.RINGER_MODE_VIBRATE:
                        return "vibrate";
                    case AudioManager.RINGER_MODE_NORMAL:
                    default:
                        return "normal";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current ringer mode", e);
        }
        return "normal";
    }

    public static void applyActions(Context context, String actions) {
        Log.d(TAG, "Applying profile actions: " + actions);

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
                    NotificationUtils.showWifiToggleNotification(context, true);
                    break;

                case "bluetooth":
                    toggleBluetooth(context, true);
                    break;

                case "data":
                    // Mobile data toggle requires system permissions
                    // Show notification for manual toggle
                    Log.d(TAG, "Mobile data toggle requested");
                    break;

                case "dnd":
                    enableDoNotDisturb(context);
                    break;

                default:
                    Log.w(TAG, "Unknown action: " + action);
                    break;
            }
        }
    }

    public static void deactivateProfile(Context context, String actions) {
        Log.d(TAG, "Deactivating profile actions");

        // Reset ringer mode to normal
        setRingerMode(context, "normal");

        // If there were no actions, we're done
        if (actions == null || actions.isEmpty()) {
            return;
        }

        // Reverse each action
        String[] actionArray = actions.split(",");
        for (String action : actionArray) {
            action = action.trim();

            switch (action) {
                case "wifi":
                    // Show notification to manually disable WiFi
                    NotificationUtils.showWifiToggleNotification(context, false);
                    break;

                case "bluetooth":
                    toggleBluetooth(context, false);
                    break;

                case "data":
                    // Show notification for manual data toggle
                    Log.d(TAG, "Mobile data disable requested");
                    break;

                case "dnd":
                    disableDoNotDisturb(context);
                    break;

                default:
                    Log.w(TAG, "Unknown action to deactivate: " + action);
                    break;
            }
        }
    }
}