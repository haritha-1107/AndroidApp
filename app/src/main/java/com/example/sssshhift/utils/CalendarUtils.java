package com.example.sssshhift.utils;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarUtils {
    private static final String TAG = "CalendarUtils";
    private static final long BUFFER_TIME_MS = 120000; // 2 minute buffer for more reliable detection
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    /**
     * Check if there's an ongoing calendar event
     * @param context Application context
     * @return true if there's an ongoing event, false otherwise
     */
    public static boolean isEventOngoing(Context context) {
        // Check for calendar permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Calendar permission not granted");
            return false;
        }

        Cursor cursor = null;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            long currentTime = System.currentTimeMillis();
            
            Log.d(TAG, "Checking for events at: " + DATE_FORMAT.format(new Date(currentTime)));

            // Query for events happening now (with buffer)
            Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(eventsUriBuilder, currentTime - BUFFER_TIME_MS);
            ContentUris.appendId(eventsUriBuilder, currentTime + BUFFER_TIME_MS);

            String[] projection = {
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.AVAILABILITY,
                    CalendarContract.Instances.CALENDAR_ID
            };

            // Only get events from calendars that are visible/selected and not declined
            String selection = CalendarContract.Instances.VISIBLE + " = 1 AND " +
                             CalendarContract.Instances.SELF_ATTENDEE_STATUS + " != " + 
                             CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED + " AND " +
                             CalendarContract.Instances.BEGIN + " <= ? AND " +
                             CalendarContract.Instances.END + " >= ? AND " +
                             CalendarContract.Instances.ALL_DAY + " = 0"; // Exclude all-day events
            
            String[] selectionArgs = new String[] {
                String.valueOf(currentTime),
                String.valueOf(currentTime)
            };

            cursor = contentResolver.query(
                eventsUriBuilder.build(),
                projection,
                selection,
                selectionArgs,
                null
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE));
                    long begin = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN));
                    long end = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END));
                    int availability = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Instances.AVAILABILITY));
                    
                    Log.d(TAG, String.format("Found ongoing event: '%s' (%s to %s)", 
                        title,
                        DATE_FORMAT.format(new Date(begin)),
                        DATE_FORMAT.format(new Date(end))
                    ));
                    
                    // Additional check for availability (BUSY events only)
                    if (availability == CalendarContract.Events.AVAILABILITY_BUSY) {
                        Log.d(TAG, "Event is marked as BUSY - will activate silent mode");
                        return true;
                    } else {
                        Log.d(TAG, "Event is not marked as BUSY - skipping");
                    }
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "No ongoing calendar events found that require silent mode");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking calendar events: " + e.getMessage(), e);
            e.printStackTrace();
            return false;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * Get details of current ongoing events
     * @param context Application context
     * @return String with event details, or null if no events
     */
    public static String getCurrentEventDetails(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Cursor cursor = null;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            long currentTime = System.currentTimeMillis();

            Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(eventsUriBuilder, currentTime - BUFFER_TIME_MS);
            ContentUris.appendId(eventsUriBuilder, currentTime + BUFFER_TIME_MS);

            String[] projection = {
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END
            };

            String selection = CalendarContract.Instances.VISIBLE + " = 1 AND " +
                             CalendarContract.Instances.ALL_DAY + " = 0 AND " +
                             CalendarContract.Instances.AVAILABILITY + " = " + 
                             CalendarContract.Events.AVAILABILITY_BUSY;

            cursor = contentResolver.query(
                    eventsUriBuilder.build(),
                    projection,
                    selection,
                    null,
                    CalendarContract.Instances.BEGIN + " ASC"
            );

            if (cursor == null) {
                Log.w(TAG, "Cursor is null when getting event details");
                return null;
            }

            StringBuilder eventDetails = new StringBuilder();
            int eventCount = 0;

            while (cursor.moveToNext()) {
                long eventStart = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN));
                long eventEnd = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE));

                // Skip stale events
                if (currentTime - eventStart > 24 * 60 * 60 * 1000) {
                    Log.d(TAG, "Skipping stale event: " + title);
                    continue;
                }

                if (currentTime >= eventStart && currentTime <= eventEnd) {
                    if (eventDetails.length() > 0) eventDetails.append(", ");
                    eventDetails.append(title != null ? title : "Untitled Event");
                    eventCount++;
                    
                    Log.d(TAG, String.format("Active event %d: '%s' (%s to %s)",
                        eventCount,
                        title,
                        new Date(eventStart),
                        new Date(eventEnd)
                    ));
                }
            }

            Log.d(TAG, "Found " + eventCount + " active events");
            return eventDetails.length() > 0 ? eventDetails.toString() : null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting event details: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                try {
                    cursor.close();
                    Log.d(TAG, "Successfully closed cursor");
                } catch (Exception e) {
                    Log.e(TAG, "Error closing cursor: " + e.getMessage());
                }
            }
        }
    }
}