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

import java.util.Calendar;

public class CalendarUtils {
    private static final String TAG = "CalendarUtils";
    private static final long BUFFER_TIME_MS = 60000; // 1 minute buffer

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
                    CalendarContract.Instances.AVAILABILITY
            };

            // Only get events from calendars that are visible/selected and not declined
            String selection = CalendarContract.Instances.VISIBLE + " = 1 AND " +
                             CalendarContract.Instances.SELF_ATTENDEE_STATUS + " != " + 
                             CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED + " AND " +
                             CalendarContract.Instances.BEGIN + " <= ? AND " +
                             CalendarContract.Instances.END + " >= ?";
            
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
                Log.d(TAG, "Found ongoing calendar event");
                return true;
            }

            Log.d(TAG, "No ongoing calendar events found");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking calendar events: " + e.getMessage(), e);
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

        try {
            ContentResolver contentResolver = context.getContentResolver();
            long currentTime = System.currentTimeMillis();

            Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(eventsUriBuilder, currentTime - 60000);
            ContentUris.appendId(eventsUriBuilder, currentTime + 60000);

            String[] projection = {
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END
            };

            String selection = CalendarContract.Instances.VISIBLE + " = 1";

            Cursor cursor = contentResolver.query(
                    eventsUriBuilder.build(),
                    projection,
                    selection,
                    null,
                    CalendarContract.Instances.BEGIN + " ASC"
            );

            if (cursor == null) return null;

            StringBuilder eventDetails = new StringBuilder();

            while (cursor.moveToNext()) {
                long eventStart = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN));
                long eventEnd = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE));

                if (currentTime >= eventStart && currentTime <= eventEnd) {
                    if (eventDetails.length() > 0) eventDetails.append(", ");
                    eventDetails.append(title != null ? title : "Untitled Event");
                }
            }

            cursor.close();
            return eventDetails.length() > 0 ? eventDetails.toString() : null;

        } catch (Exception e) {
            Log.e(TAG, "Error getting event details: " + e.getMessage());
            return null;
        }
    }
}