package com.example.sssshhift.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    private static final String TAG = "PermissionHelper";

    // Permission request codes
    public static final int REQUEST_CALENDAR_PERMISSION = 100;
    public static final int REQUEST_LOCATION_PERMISSIONS = 101;
    public static final int REQUEST_BACKGROUND_LOCATION = 102;
    public static final int REQUEST_ALL_PERMISSIONS = 200;

    // Required permissions
    private static final String[] BASIC_PERMISSIONS = {
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // ==================== CALENDAR PERMISSION METHODS ====================

    /**
     * Check if calendar permission is granted
     */
    public static boolean hasCalendarPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request calendar permission
     */
    public static void requestCalendarPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_CALENDAR},
                REQUEST_CALENDAR_PERMISSION);
    }

    /**
     * Check if should show rationale for calendar permission
     */
    public static boolean shouldShowCalendarPermissionRationale(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALENDAR);
    }

    // ==================== LOCATION PERMISSION METHODS ====================

    /**
     * Check if location permissions are granted
     */
    public static boolean hasLocationPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if background location permission is granted (Android 10+)
     */
    public static boolean hasBackgroundLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not required for older versions
    }

    /**
     * Check if all location permissions (including background) are granted
     */
    public static boolean hasAllLocationPermissions(Context context) {
        return hasLocationPermissions(context) && hasBackgroundLocationPermission(context);
    }

    /**
     * Request basic location permissions
     */
    public static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION_PERMISSIONS);
    }

    /**
     * Request background location permission (Android 10+)
     */
    public static void requestBackgroundLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_BACKGROUND_LOCATION);
        }
    }

    /**
     * Check if should show rationale for location permissions
     */
    public static boolean shouldShowLocationPermissionRationale(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /**
     * Check if should show rationale for background location permission
     */
    public static boolean shouldShowBackgroundLocationRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        return false;
    }

    // ==================== DO NOT DISTURB PERMISSION METHODS ====================

    /**
     * Check if Do Not Disturb permission is granted
     */
    public static boolean hasDoNotDisturbPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
        }
        return true; // Not required for older versions
    }

    /**
     * Alternative method name for compatibility with existing code
     */
    public static boolean isDoNotDisturbPermissionGranted(Context context) {
        return hasDoNotDisturbPermission(context);
    }

    /**
     * Request Do Not Disturb permission with explanation dialog
     */
    public static void requestDoNotDisturbPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasDoNotDisturbPermission(activity)) {
                showDoNotDisturbPermissionDialog(activity);
            }
        }
    }

    /**
     * Show dialog explaining Do Not Disturb permission requirement
     */
    private static void showDoNotDisturbPermissionDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Do Not Disturb Permission Required")
                .setMessage("This app needs access to Do Not Disturb settings to automatically " +
                        "change your phone to silent mode when you enter specific locations. " +
                        "Please grant this permission in the next screen.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    openDoNotDisturbSettings(activity);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Log.w(TAG, "User cancelled Do Not Disturb permission request");
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Open Do Not Disturb settings page
     */
    public static void openDoNotDisturbSettings(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                activity.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening Do Not Disturb settings: " + e.getMessage());
        }
    }

    // ==================== COMPREHENSIVE PERMISSION METHODS ====================

    /**
     * Check if all required permissions are granted (including background location)
     */
    public static boolean areAllPermissionsGranted(Context context) {
        return hasCalendarPermission(context) &&
                hasAllLocationPermissions(context) &&
                hasDoNotDisturbPermission(context);
    }

    /**
     * Check if all basic permissions (excluding background location) are granted
     */
    public static boolean areBasicPermissionsGranted(Context context) {
        return hasCalendarPermission(context) &&
                hasLocationPermissions(context) &&
                hasDoNotDisturbPermission(context);
    }

    /**
     * Request all basic permissions at once
     */
    public static void requestAllBasicPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, BASIC_PERMISSIONS, REQUEST_ALL_PERMISSIONS);
    }

    /**
     * Get array of missing basic permissions
     */
    public static String[] getMissingBasicPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();

        for (String permission : BASIC_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        return missingPermissions.toArray(new String[0]);
    }

    /**
     * Get list of all missing permissions (including special permissions)
     */
    public static List<String> getAllMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();

        if (!hasCalendarPermission(context)) {
            missingPermissions.add("Calendar Permission");
        }

        if (!hasLocationPermissions(context)) {
            missingPermissions.add("Location Permission");
        }

        if (!hasBackgroundLocationPermission(context)) {
            missingPermissions.add("Background Location Permission");
        }

        if (!hasDoNotDisturbPermission(context)) {
            missingPermissions.add("Do Not Disturb Access");
        }

        return missingPermissions;
    }

    /**
     * Show comprehensive permission request dialog
     */
    public static void showPermissionRequiredDialog(Activity activity) {
        List<String> missingPermissions = getAllMissingPermissions(activity);

        if (missingPermissions.isEmpty()) {
            return; // All permissions already granted
        }

        StringBuilder message = new StringBuilder();
        message.append("This app requires the following permissions to work properly:\n\n");

        for (String permission : missingPermissions) {
            switch (permission) {
                case "Calendar Permission":
                    message.append("• Calendar Access - To read your calendar events\n");
                    break;
                case "Location Permission":
                    message.append("• Location Permission - To detect when you enter/exit specific areas\n");
                    break;
                case "Background Location Permission":
                    message.append("• Background Location - To work even when the app is closed\n");
                    break;
                case "Do Not Disturb Access":
                    message.append("• Do Not Disturb Access - To automatically change sound settings\n");
                    break;
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage(message.toString())
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    requestPermissionsSequentially(activity);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Request permissions in the correct sequence
     */
    private static void requestPermissionsSequentially(Activity activity) {
        // Request basic permissions first
        if (!areBasicPermissionsGranted(activity)) {
            String[] missingBasic = getMissingBasicPermissions(activity);
            if (missingBasic.length > 0) {
                ActivityCompat.requestPermissions(activity, missingBasic, REQUEST_ALL_PERMISSIONS);
                return;
            }
        }

        // Then request background location if needed
        if (!hasBackgroundLocationPermission(activity) && hasLocationPermissions(activity)) {
            requestBackgroundLocationPermission(activity);
            return;
        }

        // Finally request Do Not Disturb if needed
        if (!hasDoNotDisturbPermission(activity)) {
            requestDoNotDisturbPermission(activity);
        }
    }

    // ==================== PERMISSION RESULT HANDLERS ====================

    /**
     * Handle permission result for calendar permission
     */
    public static boolean handleCalendarPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CALENDAR_PERMISSION) {
            return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    /**
     * Handle permission result for location permissions
     */
    public static boolean handleLocationPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return grantResults.length > 0;
        }
        return false;
    }

    /**
     * Handle permission result for background location
     */
    public static boolean handleBackgroundLocationResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_BACKGROUND_LOCATION) {
            return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    /**
     * Handle permission result for all basic permissions
     */
    public static boolean handleAllPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return grantResults.length > 0;
        }
        return false;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get permission display name for user-friendly messages
     */
    public static String getPermissionDisplayName(String permission) {
        switch (permission) {
            case Manifest.permission.READ_CALENDAR:
                return "Calendar";
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Location";
            case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                return "Background Location";
            default:
                return "Unknown Permission";
        }
    }

    /**
     * Log current permission status for debugging
     */
    public static void logPermissionStatus(Context context) {
        Log.d(TAG, "=== Permission Status ===");
        Log.d(TAG, "Calendar: " + hasCalendarPermission(context));
        Log.d(TAG, "Location: " + hasLocationPermissions(context));
        Log.d(TAG, "Background Location: " + hasBackgroundLocationPermission(context));
        Log.d(TAG, "Do Not Disturb: " + hasDoNotDisturbPermission(context));
        Log.d(TAG, "All Permissions: " + areAllPermissionsGranted(context));
    }

    /**
     * Check if the app has been granted all critical permissions for core functionality
     */
    public static boolean hasCriticalPermissions(Context context) {
        return hasLocationPermissions(context) && hasDoNotDisturbPermission(context);
    }
}