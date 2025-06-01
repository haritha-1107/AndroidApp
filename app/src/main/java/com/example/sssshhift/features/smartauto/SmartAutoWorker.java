package com.example.sssshhift.features.smartauto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Date;

public class SmartAutoWorker extends Worker {
    private static final String TAG = "SmartAutoWorker";
    private static final String WORK_NAME = "smart_auto_calendar_check";

    public SmartAutoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            // Check if feature is enabled
            boolean isEnabled = prefs.getBoolean("auto_mode_enabled", false);
            Log.d(TAG, "Smart Auto Mode enabled: " + isEnabled);
            
            if (!isEnabled) {
                Log.d(TAG, "Smart Auto Mode is disabled, skipping calendar check");
                return Result.success();
            }

            // Check calendar permission
            boolean hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                    == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Calendar permission granted: " + hasPermission);
            if (!hasPermission) {
                Log.e(TAG, "Calendar permission not granted, scheduling retry");
                return Result.retry();
            }

            // Check DND permission
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                (notificationManager == null || !notificationManager.isNotificationPolicyAccessGranted())) {
                Log.e(TAG, "DND permission not granted, scheduling retry");
                return Result.retry();
            }

            // Get current settings
            Set<String> keywords = prefs.getStringSet("auto_mode_keywords", new HashSet<>());
            int preEventOffset = prefs.getInt("auto_mode_pre_event_offset", 5);
            boolean revertAfterEvent = prefs.getBoolean("auto_mode_revert_after_event", true);
            boolean busyEventsOnly = prefs.getBoolean("auto_mode_busy_events_only", true);
            
            Log.d(TAG, "Current settings - Keywords: " + keywords + 
                       ", Pre-event offset: " + preEventOffset + 
                       ", Revert after event: " + revertAfterEvent +
                       ", Busy events only: " + busyEventsOnly);

            // Calculate time window (next 24 hours)
            long now = System.currentTimeMillis();
            long windowStart = now;
            long windowEnd = now + TimeUnit.HOURS.toMillis(24);

            checkUpcomingEvents(context, windowStart, windowEnd, keywords, preEventOffset, busyEventsOnly);
            
            // Schedule next check
            scheduleNextCheck(context);
            
            return Result.success();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception in calendar worker: " + e.getMessage(), e);
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Error in calendar worker: " + e.getMessage(), e);
            // Only retry if it's not a fatal error
            if (e instanceof IllegalArgumentException || 
                e instanceof NullPointerException || 
                e instanceof IllegalStateException) {
                return Result.failure();
            }
            return Result.retry();
        }
    }

    private void checkUpcomingEvents(Context context, long windowStart, long windowEnd, Set<String> keywords, int preEventOffset, boolean busyEventsOnly) {
        Log.d(TAG, "Starting calendar check with settings:");
        Log.d(TAG, "Keywords: " + keywords);
        Log.d(TAG, "Pre-event offset: " + preEventOffset + " minutes");
        Log.d(TAG, "Busy events only: " + busyEventsOnly);

        Log.d(TAG, "Checking events between: " + new Date(windowStart) + " and " + new Date(windowEnd));

        // Clean up old events first
        cleanupOldEvents(context);

        ContentResolver contentResolver = context.getContentResolver();
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, windowStart);
        ContentUris.appendId(builder, windowEnd);

        String[] projection = new String[]{
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.AVAILABILITY,
                CalendarContract.Instances.CALENDAR_ID
        };

        // Query calendar events
        try (Cursor cursor = contentResolver.query(builder.build(), projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Found calendar events to check");
                int eventCount = cursor.getCount();
                Log.d(TAG, "Total events found: " + eventCount);

                do {
                    @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
                    @SuppressLint("Range") long begin = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));
                    @SuppressLint("Range") long end = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.END));
                    @SuppressLint("Range") int availability = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.AVAILABILITY));
                    @SuppressLint("Range") long calendarId = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID));

                    Log.d(TAG, "Checking event: " + title);
                    Log.d(TAG, "Event details - Start: " + new Date(begin) + 
                               ", End: " + new Date(end) + 
                               ", Calendar ID: " + calendarId + 
                               ", Availability: " + availability);

                    if (isEventMatch(title, availability, busyEventsOnly, keywords)) {
                        Log.d(TAG, "Event matches criteria, scheduling ringer mode change");
                        scheduleRingerModeChange(context, begin, end);
                    } else {
                        Log.d(TAG, "Event does not match criteria - " +
                                  "Title match: " + (title != null && containsKeyword(title, keywords)) +
                                  ", Busy check: " + (!busyEventsOnly || availability == CalendarContract.Events.AVAILABILITY_BUSY));
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No calendar events found in the time window");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying calendar", e);
            throw e;
        }
    }

    private void cleanupOldEvents(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> activeEvents = SmartAutoAlarmManager.getActiveEvents(context);
        Set<String> updatedEvents = new HashSet<>();
        long currentTime = System.currentTimeMillis();

        for (String eventKey : activeEvents) {
            try {
                String[] parts = eventKey.split("_");
                long eventEnd = Long.parseLong(parts[1]);
                if (eventEnd >= currentTime) {
                    updatedEvents.add(eventKey);
                } else {
                    Log.d(TAG, "Removing expired event: " + eventKey);
                    // Clean up any associated preferences
                    SmartAutoAlarmManager.cleanupRingerModePreference(context, Long.parseLong(parts[0]));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing event key: " + eventKey, e);
            }
        }

        if (activeEvents.size() != updatedEvents.size()) {
            Log.d(TAG, "Cleaned up " + (activeEvents.size() - updatedEvents.size()) + " expired events");
            SmartAutoAlarmManager.updateActiveEvents(context, updatedEvents);
        }
    }

    private boolean containsKeyword(String title, Set<String> keywords) {
        String titleLower = title.toLowerCase().trim();
        for (String keyword : keywords) {
            if (titleLower.contains(keyword.toLowerCase().trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isEventMatch(String title, int availability, boolean busyEventsOnly, Set<String> keywords) {
        if (busyEventsOnly && availability != CalendarContract.Events.AVAILABILITY_BUSY) {
            Log.d(TAG, "Event skipped: not marked as busy");
            return false;
        }

        if (title == null) {
            Log.d(TAG, "Event skipped: null title");
            return false;
        }

        if (keywords == null || keywords.isEmpty()) {
            Log.d(TAG, "Event skipped: no keywords defined");
            return false;
        }

        String titleLower = title.toLowerCase().trim();
        Log.d(TAG, "Checking title '" + titleLower + "' against keywords: " + keywords);

        for (String keyword : keywords) {
            String keywordLower = keyword.toLowerCase().trim();
            if (titleLower.contains(keywordLower)) {
                Log.d(TAG, "Event matched keyword: " + keyword);
                return true;
            }
        }
        
        Log.d(TAG, "Event did not match any keywords");
        return false;
    }

    private void scheduleRingerModeChange(Context context, long eventStart, long eventEnd) {
        Log.d(TAG, "Scheduling ringer mode change for event:");
        Log.d(TAG, "Event start: " + new Date(eventStart));
        Log.d(TAG, "Event end: " + new Date(eventEnd));
        SmartAutoAlarmManager.scheduleRingerModeChange(context, eventStart, eventEnd);
    }

    private void scheduleNextCheck(Context context) {
        // Schedule next check in 15 minutes
        WorkManager.getInstance(context).enqueueUniqueWork(
            "smart_auto_next_check",
            ExistingWorkPolicy.REPLACE,
            new OneTimeWorkRequest.Builder(SmartAutoWorker.class)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()
        );
    }

    public static void scheduleWork(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        // Schedule periodic work every 15 minutes
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                SmartAutoWorker.class,
                15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        // Schedule immediate one-time work
        OneTimeWorkRequest immediateWork = new OneTimeWorkRequest.Builder(SmartAutoWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager workManager = WorkManager.getInstance(context);
        
        // Enqueue immediate check
        workManager.enqueue(immediateWork);
        
        // Schedule periodic checks
        workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWork);
        
        Log.d(TAG, "Scheduled immediate and periodic work for calendar checks");
    }

    public static void cancelWork(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Cancelled all scheduled work");
    }
} 