package com.example.sssshhift.features.smartauto;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import androidx.preference.PreferenceManager;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class CalendarEventChecker {
    private static final String TAG = "CalendarEventChecker";

    /**
     * Check and schedule calendar events within the specified time window
     * @param context Application context
     * @param windowStart Start time for event window
     * @param windowEnd End time for event window
     * @param busyEventsOnly Whether to only consider busy events
     */
    public static void checkAndScheduleEvents(Context context, long windowStart, long windowEnd, boolean busyEventsOnly) {
        Log.d(TAG, "Checking calendar events:");
        Log.d(TAG, "Window start: " + new Date(windowStart));
        Log.d(TAG, "Window end: " + new Date(windowEnd));
        Log.d(TAG, "Busy events only: " + busyEventsOnly);

        // Get the pre-event offset from settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int preEventOffset = prefs.getInt("auto_mode_pre_event_offset", 5);
        
        // Adjust window start to include pre-event offset
        long adjustedWindowStart = windowStart - (preEventOffset * 60L * 1000L);
        
        Log.d(TAG, "Adjusted window start (including pre-event offset): " + new Date(adjustedWindowStart));

        ContentResolver contentResolver = context.getContentResolver();
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, adjustedWindowStart);
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

        // Only get events that we haven't declined and are visible
        String selection = CalendarContract.Instances.VISIBLE + " = 1 AND " +
                         CalendarContract.Instances.SELF_ATTENDEE_STATUS + " != " + 
                         CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED;

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(builder.build(), projection, selection, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Found calendar events to process");
                int eventCount = cursor.getCount();
                Log.d(TAG, "Total events found: " + eventCount);

                do {
                    processCalendarEvent(context, cursor, busyEventsOnly);
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No calendar events found in the time window");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking calendar events: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                try {
                    cursor.close();
                    Log.d(TAG, "Successfully closed calendar cursor");
                } catch (Exception e) {
                    Log.e(TAG, "Error closing cursor: " + e.getMessage());
                }
            }
        }
    }

    @SuppressLint("Range")
    private static void processCalendarEvent(Context context, Cursor cursor, boolean busyEventsOnly) {
        try {
            String title = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE));
            long begin = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN));
            long end = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.END));
            int availability = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.AVAILABILITY));
            boolean isAllDay = cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)) == 1;

            // Skip all-day events
            if (isAllDay) {
                Log.d(TAG, "Skipping all-day event: " + title);
                return;
            }

            // Get current time and settings
            long currentTime = System.currentTimeMillis();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int preEventOffset = prefs.getInt("auto_mode_pre_event_offset", 5);

            // Calculate activation time with pre-event offset
            long activationTime = begin - (preEventOffset * 60L * 1000L);

            // Skip events that have already ended
            if (end <= currentTime) {
                Log.d(TAG, "Skipping ended event: " + title);
                return;
            }

            // For ongoing events, use current time as activation time
            if (currentTime > begin && currentTime < end) {
                Log.d(TAG, "Found ongoing event: " + title);
                activationTime = currentTime;
            }

            // Check if this event matches our criteria
            boolean shouldActivate = true;

            // Check if we should only handle busy events
            if (busyEventsOnly && availability != CalendarContract.Events.AVAILABILITY_BUSY) {
                Log.d(TAG, "Skipping non-busy event: " + title);
                shouldActivate = false;
            }

            // Check if event matches any keywords (if configured)
            Set<String> keywords = prefs.getStringSet("auto_mode_keywords", new HashSet<>());
            if (!keywords.isEmpty() && !containsKeyword(title, keywords)) {
                Log.d(TAG, "Event doesn't match any keywords: " + title);
                shouldActivate = false;
            }

            // Check if we're within the pre-event window
            boolean isWithinPreEventWindow = currentTime >= activationTime && currentTime < end;
            boolean isUpcomingEvent = activationTime > currentTime && activationTime - currentTime <= 5 * 60 * 1000; // Within next 5 minutes

            if (shouldActivate && (isWithinPreEventWindow || isUpcomingEvent)) {
                Log.d(TAG, String.format("Scheduling silent mode for event: %s", title));
                Log.d(TAG, String.format("- Current time: %s", new Date(currentTime)));
                Log.d(TAG, String.format("- Activation time: %s", new Date(activationTime)));
                Log.d(TAG, String.format("- Event start: %s", new Date(begin)));
                Log.d(TAG, String.format("- Event end: %s", new Date(end)));
                Log.d(TAG, String.format("- Is within pre-event window: %s", isWithinPreEventWindow));
                Log.d(TAG, String.format("- Is upcoming event: %s", isUpcomingEvent));
                
                // Schedule the ringer mode change
                SmartAutoAlarmManager.scheduleRingerModeChange(context, activationTime, end);
            } else {
                Log.d(TAG, String.format("Not scheduling event: %s (outside activation window)", title));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing calendar event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean containsKeyword(String title, Set<String> keywords) {
        if (title == null || keywords == null || keywords.isEmpty()) {
            return false;
        }

        String titleLower = title.toLowerCase().trim();
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty() && 
                titleLower.contains(keyword.toLowerCase().trim())) {
                return true;
            }
        }
        return false;
    }
} 