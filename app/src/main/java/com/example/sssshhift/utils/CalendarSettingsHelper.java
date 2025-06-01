// Add this to your SettingsFragment.java or create a separate CalendarSettingsHelper

package com.example.sssshhift.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CalendarSettingsHelper {
    private static final String PREF_CALENDAR_MONITORING_ENABLED = "calendar_monitoring_enabled";
    private static final String PREF_CALENDAR_CHECK_INTERVAL = "calendar_check_interval";
    private static final String PREF_CALENDAR_BUFFER_TIME = "calendar_buffer_time";

    private SharedPreferences sharedPreferences;

    public CalendarSettingsHelper(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Check if calendar monitoring is enabled in settings
     */
    public boolean isCalendarMonitoringEnabled() {
        return sharedPreferences.getBoolean(PREF_CALENDAR_MONITORING_ENABLED, true);
    }

    /**
     * Enable or disable calendar monitoring
     */
    public void setCalendarMonitoringEnabled(boolean enabled) {
        sharedPreferences.edit()
                .putBoolean(PREF_CALENDAR_MONITORING_ENABLED, enabled)
                .apply();
    }

    /**
     * Get calendar check interval in milliseconds (default: 30 seconds)
     */
    public long getCalendarCheckInterval() {
        return sharedPreferences.getLong(PREF_CALENDAR_CHECK_INTERVAL, 30000);
    }

    /**
     * Set calendar check interval
     */
    public void setCalendarCheckInterval(long intervalMs) {
        sharedPreferences.edit()
                .putLong(PREF_CALENDAR_CHECK_INTERVAL, intervalMs)
                .apply();
    }

    /**
     * Get buffer time before/after events in minutes (default: 1 minute)
     */
    public int getCalendarBufferTime() {
        return sharedPreferences.getInt(PREF_CALENDAR_BUFFER_TIME, 1);
    }

    /**
     * Set buffer time before/after events
     */
    public void setCalendarBufferTime(int bufferMinutes) {
        sharedPreferences.edit()
                .putInt(PREF_CALENDAR_BUFFER_TIME, bufferMinutes)
                .apply();
    }

    /**
     * Reset all calendar settings to defaults
     */
    public void resetToDefaults() {
        sharedPreferences.edit()
                .putBoolean(PREF_CALENDAR_MONITORING_ENABLED, true)
                .putLong(PREF_CALENDAR_CHECK_INTERVAL, 30000)
                .putInt(PREF_CALENDAR_BUFFER_TIME, 1)
                .apply();
    }
}

// Sample code for SettingsFragment to add calendar toggle
/*
// Add this to your SettingsFragment.java

private void setupCalendarSettings() {
    CalendarSettingsHelper calendarSettings = new CalendarSettingsHelper(getContext());

    // Create toggle switch for calendar monitoring
    SwitchCompat calendarToggle = findViewById(R.id.switch_calendar_monitoring);
    calendarToggle.setChecked(calendarSettings.isCalendarMonitoringEnabled());

    calendarToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
        calendarSettings.setCalendarMonitoringEnabled(isChecked);

        // Toggle the service in MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).toggleCalendarMonitoring(isChecked);
        }
    });

    // Add info text
    TextView calendarInfo = findViewById(R.id.text_calendar_info);
    calendarInfo.setText("Automatically activate silent mode during calendar events");
}
*/