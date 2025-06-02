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
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Date;

public class SmartAutoWorker extends Worker {
    private static final String TAG = "SmartAutoWorker";
    private static final String WORK_NAME = "SmartAutoPeriodicWork";
    private static final long WORK_INTERVAL_MINUTES = 15;

    public SmartAutoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Starting periodic calendar check");
            
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            // Only proceed if the feature is enabled
            if (!prefs.getBoolean("auto_mode_enabled", false)) {
                Log.d(TAG, "Smart Auto Mode is disabled, skipping work");
                return Result.success();
            }

            // Clean up any stale events first
            SmartAutoAlarmManager.cleanupExpiredEvents(context);

            // Check and schedule upcoming events
            long windowStart = System.currentTimeMillis();
            long windowEnd = windowStart + (24 * 60 * 60 * 1000); // Next 24 hours

            try {
                CalendarEventChecker.checkAndScheduleEvents(
                    context,
                    windowStart,
                    windowEnd,
                    true // Only consider busy events
                );
                Log.d(TAG, "Successfully checked and scheduled calendar events");
            return Result.success();
            } catch (Exception e) {
                Log.e(TAG, "Error checking calendar events: " + e.getMessage());
                e.printStackTrace();
                // Don't fail the work, retry on next schedule
                return Result.success();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in worker: " + e.getMessage());
            e.printStackTrace();
            return Result.success(); // Don't retry, wait for next schedule
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

        // Get current active events before making any changes
        Set<String> activeEvents = SmartAutoAlarmManager.getActiveEvents(context);
        long currentTime = System.currentTimeMillis();

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
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.SELF_ATTENDEE_STATUS
        };

        // Only get events that we haven't declined
        String selection = CalendarContract.Instances.SELF_ATTENDEE_STATUS + " != " + 
                         CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED;

        try (Cursor cursor = contentResolver.query(builder.build(), projection, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Found calendar events to check");
                int eventCount = cursor.getCount();
                Log.d(TAG, "Total events found: " + eventCount);

                do {
                    @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
                    @SuppressLint("Range") long begin = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));
                    @SuppressLint("Range") long end = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.END));
                    @SuppressLint("Range") int availability = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.AVAILABILITY));
                    @SuppressLint("Range") boolean isAllDay = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)) == 1;

                    // Skip all-day events
                    if (isAllDay) {
                        Log.d(TAG, "Skipping all-day event: " + title);
                        continue;
                    }

                    // Calculate activation time with pre-event offset
                    long activationTime = begin - (preEventOffset * 60L * 1000L);

                    // Skip events that have already ended
                    if (end <= currentTime) {
                        Log.d(TAG, "Skipping ended event: " + title);
                        continue;
                    }

                    // Skip events where even the pre-event time has passed
                    if (activationTime <= currentTime) {
                        Log.d(TAG, "Skipping event where pre-event time has passed: " + title);
                        continue;
                    }

                    // Check if this event matches our criteria
                    boolean shouldActivate = true;

                    // Check if we should only handle busy events
                    if (busyEventsOnly && availability != CalendarContract.Events.AVAILABILITY_BUSY) {
                        Log.d(TAG, "Skipping non-busy event: " + title);
                        shouldActivate = false;
                    }

                    // Check if event matches keywords (if any are set)
                    if (!keywords.isEmpty() && !containsKeyword(title, keywords)) {
                        Log.d(TAG, "Event doesn't match any keywords: " + title);
                        shouldActivate = false;
                    }

                    if (shouldActivate) {
                        Log.d(TAG, String.format("Scheduling silent mode for event: %s", title));
                        Log.d(TAG, String.format("- Pre-event activation time: %s", new Date(activationTime)));
                        Log.d(TAG, String.format("- Event start: %s", new Date(begin)));
                        Log.d(TAG, String.format("- Event end: %s", new Date(end)));
                        
                        // Schedule the ringer mode change with the calculated pre-event time
                        SmartAutoAlarmManager.scheduleRingerModeChange(context, activationTime, end);
                    } else {
                        Log.d(TAG, "Event doesn't meet criteria for silent mode: " + title);
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

    public static boolean isWorkScheduled(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        ListenableFuture<List<WorkInfo>> future = workManager.getWorkInfosForUniqueWork(WORK_NAME);
        try {
            List<WorkInfo> workInfos = future.get();
            for (WorkInfo workInfo : workInfos) {
                if (workInfo.getState() == WorkInfo.State.RUNNING || 
                    workInfo.getState() == WorkInfo.State.ENQUEUED) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking work status", e);
        }
        return false;
    }

    public static void scheduleWork(Context context) {
        try {
            Log.d(TAG, "Scheduling periodic work");
        
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                SmartAutoWorker.class,
                WORK_INTERVAL_MINUTES,
                TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("calendar_check")
                .build();

            // Use REPLACE policy to ensure we don't queue multiple workers
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );
        
            Log.d(TAG, "Successfully scheduled periodic work");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling work: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void cancelWork(Context context) {
        try {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
            Log.d(TAG, "Cancelled periodic work");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling work: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 