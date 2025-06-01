package com.example.sssshhift.utils;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.sssshhift.R;
import com.example.sssshhift.MainActivity;

public class NotificationUtils {
    private static final String CHANNEL_ID = "profile_notifications";
    private static final String CHANNEL_NAME = "Profile Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for profile activations";

    private static final int NOTIFICATION_ID_ACTIVATED = 1001;
    private static final int NOTIFICATION_ID_ENDED = 1002;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void showProfileActivatedNotification(Context context, String profileName) {
        showNotification(
                context,
                NOTIFICATION_ID_ACTIVATED,
                "Profile Activated",
                "'" + profileName + "' profile is now active",
                "🔇"
        );
    }

    public static void showProfileEndedNotification(Context context, String profileName) {
        showNotification(
                context,
                NOTIFICATION_ID_ENDED,
                "Profile Ended",
                "'" + profileName + "' profile has ended",
                "🔊"
        );
    }

    private static void showNotification(Context context, int notificationId, String title, String content, String icon) {
        // Create intent for when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_profile) // Make sure this icon exists
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    public static void cancelAllNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }
    // Add this method to your NotificationUtils.java class

    @SuppressLint("MissingPermission")
    public static void showWifiToggleNotification(Context context, boolean enable) {
        String title = "Profile Action Required";
        String message = "Please " + (enable ? "enable" : "disable") + " WiFi to complete profile activation";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wifi) // Use your WiFi icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Add action button to open WiFi settings
        Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        PendingIntent wifiPendingIntent = PendingIntent.getActivity(
                context,
                0,
                wifiSettingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.ic_settings, "Open WiFi Settings", wifiPendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(WIFI_TOGGLE_NOTIFICATION_ID, builder.build());
    }

    // Add these constants to your NotificationUtils class
    private static final int WIFI_TOGGLE_NOTIFICATION_ID = 1002;


    public static void showPermissionRequiredNotification(Context context) {
        String title = "Calendar Permission Needed";
        String message = "Please allow calendar access for automatic profile activation.";

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar) // Ensure this icon exists or use an existing one
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_settings, "Open Settings", pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(1003, builder.build());
    }


}