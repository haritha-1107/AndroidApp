package com.example.sssshhift.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.example.sssshhift.receivers.ProfileTimerReceiver;
import com.example.sssshhift.models.Profile;

import java.util.Calendar;

public class ProfileUtils {
    private static final String TAG = "ProfileUtils";

    public static void scheduleProfile(Context context, String profileName, boolean isStartTime, String time, String endTime) {
        try {
            // Schedule start time
            if (time != null && !time.isEmpty()) {
                scheduleProfileAlarm(context, profileName, time, true);
                Log.d(TAG, "Scheduled start alarm for profile: " + profileName + " at " + time);
            }

            // Schedule end time if provided
            if (endTime != null && !endTime.isEmpty()) {
                scheduleProfileAlarm(context, profileName, endTime, false);
                Log.d(TAG, "Scheduled end alarm for profile: " + profileName + " at " + endTime);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling profile: " + profileName, e);
            Toast.makeText(context, "Error scheduling profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void scheduleProfileAlarm(Context context, String profileName, String time, boolean isStartTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        // Check if app can schedule exact alarms (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Request exact alarm permission
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    Toast.makeText(context, "Please allow exact alarms for timer functionality", Toast.LENGTH_LONG).show();
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting exact alarm permission", e);
                }
            }
        }

        try {
            // Parse time (HH:mm format)
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Create calendar instance for today
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // If the time has already passed today, schedule for tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Create unique request code for this alarm
            int requestCode = generateRequestCode(profileName, isStartTime);

            // Create intent for the alarm receiver
            Intent intent = new Intent(context, ProfileTimerReceiver.class);
            intent.putExtra("profile_name", profileName);
            intent.putExtra("is_start_time", isStartTime);
            intent.putExtra("scheduled_time", time);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule the alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }

            Log.d(TAG, "Alarm scheduled for " + calendar.getTime() + " (Profile: " + profileName + ", Start: " + isStartTime + ")");

        } catch (Exception e) {
            Log.e(TAG, "Error creating alarm for profile: " + profileName, e);
            throw e;
        }
    }

    public static void cancelProfileAlarms(Context context, String profileName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        try {
            // Cancel start time alarm
            int startRequestCode = generateRequestCode(profileName, true);
            Intent startIntent = new Intent(context, ProfileTimerReceiver.class);
            PendingIntent startPendingIntent = PendingIntent.getBroadcast(
                    context,
                    startRequestCode,
                    startIntent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (startPendingIntent != null) {
                alarmManager.cancel(startPendingIntent);
                startPendingIntent.cancel();
                Log.d(TAG, "Cancelled start alarm for profile: " + profileName);
            }

            // Cancel end time alarm
            int endRequestCode = generateRequestCode(profileName, false);
            Intent endIntent = new Intent(context, ProfileTimerReceiver.class);
            PendingIntent endPendingIntent = PendingIntent.getBroadcast(
                    context,
                    endRequestCode,
                    endIntent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (endPendingIntent != null) {
                alarmManager.cancel(endPendingIntent);
                endPendingIntent.cancel();
                Log.d(TAG, "Cancelled end alarm for profile: " + profileName);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error cancelling alarms for profile: " + profileName, e);
        }
    }

    private static int generateRequestCode(String profileName, boolean isStartTime) {
        // Generate a unique request code based on profile name and type
        int hash = profileName.hashCode();
        return Math.abs(hash) + (isStartTime ? 1000 : 2000);
    }

    // Method to reschedule daily recurring alarms
    public static void rescheduleProfileForNextDay(Context context, String profileName, String time, boolean isStartTime) {
        scheduleProfileAlarm(context, profileName, time, isStartTime);
    }

    public static void scheduleProfile(Context context, Profile profile) {
        try {
            if (profile.getLocation() != null && !profile.getLocation().isEmpty()) {
                scheduleLocationBasedAlarm(context, profile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling profile: " + profile.getName(), e);
            Toast.makeText(context, "Error scheduling profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void scheduleLocationBasedAlarm(Context context, Profile profile) {
        try {
            // Parse location coordinates
            String[] coordinates = profile.getLocation().split(",");
            if (coordinates.length == 2) {
                double latitude = Double.parseDouble(coordinates[0].trim());
                double longitude = Double.parseDouble(coordinates[1].trim());
                
                // Create intent for the alarm
                Intent intent = new Intent(context, ProfileTimerReceiver.class);
                intent.putExtra("profile_id", profile.getId());
                intent.putExtra("profile_name", profile.getName());
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.putExtra("radius", profile.getRadius());
                
                // Set up the PendingIntent with a unique request code
                int requestCode = (int) (profile.getId() % Integer.MAX_VALUE); // Safe conversion to int
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Get the AlarmManager
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    // Schedule the alarm
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis(),
                                pendingIntent
                            );
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis(),
                                pendingIntent
                            );
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis(),
                            pendingIntent
                        );
                    }
                    Log.d(TAG, "Location-based alarm scheduled for profile: " + profile.getName() + 
                              " at location: " + latitude + "," + longitude);
                }
            } else {
                Log.e(TAG, "Invalid location format: " + profile.getLocation());
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing coordinates for profile: " + profile.getName(), e);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling profile: " + profile.getName(), e);
        }
    }

    public static void startLocationMonitoring(Context context, Profile profile) {
        try {
            if (profile == null || profile.getTriggerType() == null || !profile.getTriggerType().equals("location")) {
                Log.e(TAG, "Invalid profile or not a location-based profile");
                return;
            }

            // Parse location coordinates
            String[] coordinates = profile.getTriggerValue().split(",");
            if (coordinates.length != 2) {
                Log.e(TAG, "Invalid location coordinates format");
                return;
            }

            double latitude = Double.parseDouble(coordinates[0].trim());
            double longitude = Double.parseDouble(coordinates[1].trim());

            // Create intent for location monitoring
            Intent intent = new Intent(context, ProfileTimerReceiver.class);
            intent.putExtra("profile_id", profile.getId());
            intent.putExtra("profile_name", profile.getName());
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("is_location_monitoring", true);

            // Set up the PendingIntent with a unique request code
            int requestCode = (int) (profile.getId() % Integer.MAX_VALUE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Get the AlarmManager
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }

            // Set up a repeating alarm for location monitoring
            // Check every 15 minutes (900000 milliseconds)
            long intervalMillis = 15 * 60 * 1000;
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                intervalMillis,
                pendingIntent
            );

            Log.d(TAG, "Started location monitoring for profile: " + profile.getName());
            NotificationUtils.showProfileActivatedNotification(context, profile.getName());

        } catch (Exception e) {
            Log.e(TAG, "Error starting location monitoring for profile: " + profile.getName(), e);
            Toast.makeText(context, "Error starting location monitoring: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void stopLocationMonitoring(Context context, Profile profile) {
        try {
            if (profile == null) {
                Log.e(TAG, "Profile is null");
                return;
            }

            // Create the same intent that was used to start monitoring
            Intent intent = new Intent(context, ProfileTimerReceiver.class);
            intent.putExtra("profile_id", profile.getId());
            intent.putExtra("profile_name", profile.getName());
            intent.putExtra("is_location_monitoring", true);

            // Use the same request code that was used to start monitoring
            int requestCode = (int) (profile.getId() % Integer.MAX_VALUE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                // Get the AlarmManager
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    // Cancel the repeating alarm
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                    Log.d(TAG, "Stopped location monitoring for profile: " + profile.getName());
                }
            }

            // Show notification that profile is deactivated
            NotificationUtils.showProfileEndedNotification(context, profile.getName());

        } catch (Exception e) {
            Log.e(TAG, "Error stopping location monitoring for profile: " + profile.getName(), e);
            Toast.makeText(context, "Error stopping location monitoring: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}