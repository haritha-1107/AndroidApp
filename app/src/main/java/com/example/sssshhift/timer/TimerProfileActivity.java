package com.example.sssshhift.timer;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

import com.example.sssshhift.R;

import java.util.Calendar;

public class TimerProfileActivity extends AppCompatActivity {
    private static final String TAG = "TimerProfileActivity";
    private TimerProfileManager timerManager;
    private TimePicker startTimePicker;
    private TimePicker endTimePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_profile);

        timerManager = new TimerProfileManager(this);
        
        // Initialize views
        startTimePicker = findViewById(R.id.startTimePicker);
        endTimePicker = findViewById(R.id.endTimePicker);
        Button createProfileButton = findViewById(R.id.createProfileButton);

        createProfileButton.setOnClickListener(v -> createTimerProfile());
        
        // Check and request necessary permissions
        if (!checkPermissions()) {
            Toast.makeText(this, "Please grant all required permissions", Toast.LENGTH_LONG).show();
        }
    }

    private void createTimerProfile() {
        if (!checkPermissions()) {
            Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_LONG).show();
            return;
        }

        // Get current date
        Calendar calendar = Calendar.getInstance();
        
        // Get selected times
        int startHour = startTimePicker.getHour();
        int startMinute = startTimePicker.getMinute();
        int endHour = endTimePicker.getHour();
        int endMinute = endTimePicker.getMinute();

        Log.d(TAG, "Selected times - Start: " + startHour + ":" + startMinute + 
              ", End: " + endHour + ":" + endMinute);

        // Create calendar instances for start and end times
        Calendar startTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();

        // Set start time
        startTime.set(Calendar.HOUR_OF_DAY, startHour);
        startTime.set(Calendar.MINUTE, startMinute);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        // Set end time
        endTime.set(Calendar.HOUR_OF_DAY, endHour);
        endTime.set(Calendar.MINUTE, endMinute);
        endTime.set(Calendar.SECOND, 0);
        endTime.set(Calendar.MILLISECOND, 0);

        // If the time has already passed today, set it for tomorrow
        if (startTime.before(calendar)) {
            startTime.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "Start time moved to tomorrow");
        }
        if (endTime.before(startTime)) {
            endTime.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "End time moved to tomorrow");
        }

        Log.d(TAG, "Scheduling profile - Start: " + startTime.getTime() + 
              ", End: " + endTime.getTime());

        // Schedule the profile
        boolean scheduled = timerManager.scheduleProfile(
            startTime.getTimeInMillis(),
            endTime.getTimeInMillis(),
            AudioManager.RINGER_MODE_SILENT,
            "Silent Profile"
        );

        if (scheduled) {
            // Save profile for persistence
            TimerBootReceiver.saveProfile(
                this,
                startTime.getTimeInMillis(),
                endTime.getTimeInMillis(),
                AudioManager.RINGER_MODE_SILENT,
                "Silent Profile"
            );

            Toast.makeText(this, "Timer profile scheduled successfully", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Profile scheduled and saved successfully");
        } else {
            Toast.makeText(this, "Failed to schedule timer profile", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to schedule profile");
        }
    }

    private boolean checkPermissions() {
        boolean allPermissionsGranted = true;

        // Check for DND permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
                allPermissionsGranted = false;
            }
        }

        // Check for notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
                allPermissionsGranted = false;
            }
        }

        // Check for exact alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                allPermissionsGranted = false;
            }
        }

        return allPermissionsGranted;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permissions when returning to the activity
        checkPermissions();
    }
} 