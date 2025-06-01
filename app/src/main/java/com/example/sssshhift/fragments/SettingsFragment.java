package com.example.sssshhift.fragments;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.sssshhift.MainActivity;
import com.example.sssshhift.R;
import com.example.sssshhift.location.LocationUtils;
import com.example.sssshhift.utils.PermissionUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    // UI Components
    private SwitchMaterial calendarMonitoringSwitch;
    private SwitchMaterial locationBasedSwitch;
    private SwitchMaterial notificationsSwitch;
    private SwitchMaterial vibrationFeedbackSwitch;

    private LinearLayout permissionsSection;
    private TextView dndPermissionStatus;
    private TextView locationPermissionStatus;
    private TextView calendarPermissionStatus;
    private TextView backgroundLocationStatus;

    private MaterialCardView aboutCard;
    private MaterialCardView helpCard;
    private MaterialCardView privacyCard;
    private MaterialCardView exportDataCard;
    private MaterialCardView clearDataCard;

    private SharedPreferences sharedPreferences;

    // Preference keys
    private static final String PREF_CALENDAR_MONITORING = "calendar_monitoring_enabled";
    private static final String PREF_LOCATION_BASED = "location_based_enabled";
    private static final String PREF_NOTIFICATIONS = "notifications_enabled";
    private static final String PREF_VIBRATION_FEEDBACK = "vibration_feedback_enabled";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initViews(view);
        initPreferences();
        setupListeners();
        updatePermissionStatus();

        return view;
    }

    private void initViews(View view) {
        // Feature switches
        calendarMonitoringSwitch = view.findViewById(R.id.calendar_monitoring_switch);
        locationBasedSwitch = view.findViewById(R.id.location_based_switch);
        notificationsSwitch = view.findViewById(R.id.notifications_switch);
        vibrationFeedbackSwitch = view.findViewById(R.id.vibration_feedback_switch);

        // Permission status
        permissionsSection = view.findViewById(R.id.permissions_section);
        dndPermissionStatus = view.findViewById(R.id.dnd_permission_status);
        locationPermissionStatus = view.findViewById(R.id.location_permission_status);
        calendarPermissionStatus = view.findViewById(R.id.calendar_permission_status);
        backgroundLocationStatus = view.findViewById(R.id.background_location_status);

        // Other settings cards
        aboutCard = view.findViewById(R.id.about_card);
        helpCard = view.findViewById(R.id.help_card);
        privacyCard = view.findViewById(R.id.privacy_card);
        exportDataCard = view.findViewById(R.id.export_data_card);
        clearDataCard = view.findViewById(R.id.clear_data_card);
    }

    private void initPreferences() {
        sharedPreferences = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);

        // Load saved preferences
        calendarMonitoringSwitch.setChecked(sharedPreferences.getBoolean(PREF_CALENDAR_MONITORING, true));
        locationBasedSwitch.setChecked(sharedPreferences.getBoolean(PREF_LOCATION_BASED, true));
        notificationsSwitch.setChecked(sharedPreferences.getBoolean(PREF_NOTIFICATIONS, true));
        vibrationFeedbackSwitch.setChecked(sharedPreferences.getBoolean(PREF_VIBRATION_FEEDBACK, true));
    }

    private void setupListeners() {
        // Calendar monitoring toggle
        calendarMonitoringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !hasCalendarPermission()) {
                // Reset switch and request permission
                calendarMonitoringSwitch.setChecked(false);
                showCalendarPermissionDialog();
                return;
            }

            savePreference(PREF_CALENDAR_MONITORING, isChecked);

            // Toggle calendar monitoring in MainActivity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleCalendarMonitoring(isChecked);
            }
        });

        // Location based toggle
        locationBasedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !hasLocationPermissions()) {
                // Reset switch and request permission
                locationBasedSwitch.setChecked(false);
                showLocationPermissionDialog();
                return;
            }

            savePreference(PREF_LOCATION_BASED, isChecked);
            Toast.makeText(getContext(),
                    isChecked ? "Location-based profiles enabled" : "Location-based profiles disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // Notifications toggle
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(PREF_NOTIFICATIONS, isChecked);
            Toast.makeText(getContext(),
                    isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // Vibration feedback toggle
        vibrationFeedbackSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(PREF_VIBRATION_FEEDBACK, isChecked);
            Toast.makeText(getContext(),
                    isChecked ? "Vibration feedback enabled" : "Vibration feedback disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // Permission status clicks - open system settings
        dndPermissionStatus.setOnClickListener(v -> openDNDSettings());
        locationPermissionStatus.setOnClickListener(v -> openLocationSettings());
        calendarPermissionStatus.setOnClickListener(v -> openAppSettings());
        backgroundLocationStatus.setOnClickListener(v -> openAppSettings());

        // Other setting cards
        aboutCard.setOnClickListener(v -> showAboutDialog());
        helpCard.setOnClickListener(v -> showHelpDialog());
        privacyCard.setOnClickListener(v -> showPrivacyDialog());
        exportDataCard.setOnClickListener(v -> exportUserData());
        clearDataCard.setOnClickListener(v -> showClearDataDialog());
    }

    private void savePreference(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    private void updatePermissionStatus() {
        // Update DND permission status
        if (hasDNDPermission()) {
            dndPermissionStatus.setText("✓ Granted");
            dndPermissionStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            dndPermissionStatus.setText("✗ Not Granted - Tap to Grant");
            dndPermissionStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        }

        // Update location permission status
        if (hasLocationPermissions()) {
            locationPermissionStatus.setText("✓ Granted");
            locationPermissionStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            locationPermissionStatus.setText("✗ Not Granted - Tap to Grant");
            locationPermissionStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        }

        // Update calendar permission status
        if (hasCalendarPermission()) {
            calendarPermissionStatus.setText("✓ Granted");
            calendarPermissionStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            calendarPermissionStatus.setText("✗ Not Granted - Tap to Grant");
            calendarPermissionStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        }

        // Update background location status
        if (hasBackgroundLocationPermission()) {
            backgroundLocationStatus.setText("✓ Granted");
            backgroundLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            backgroundLocationStatus.setText("✗ Not Granted - Tap to Grant");
            backgroundLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        }
    }

    // Permission check methods
    private boolean hasDNDPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager)
                    requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return true;
    }

    private boolean hasLocationPermissions() {
        return LocationUtils.hasLocationPermissions(requireContext());
    }

    private boolean hasBackgroundLocationPermission() {
        return LocationUtils.hasBackgroundLocationPermission(requireContext());
    }

    private boolean hasCalendarPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Permission dialog methods
    private void showCalendarPermissionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Calendar Permission Required")
                .setMessage("To enable calendar monitoring, please grant calendar access in the app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLocationPermissionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Location Permission Required")
                .setMessage("To enable location-based profiles, please grant location access in the app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Settings opening methods
    private void openDNDSettings() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);
    }

    private void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    // Dialog methods for other settings
    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("About SsssHhift")
                .setMessage("SsssHhift - Smart Profile Manager\n\n" +
                        "Version: 1.0.0\n" +
                        "Automatically manage your phone's settings based on time, location, and calendar events.\n\n" +
                        "Created with ❤️ for seamless mobile experience.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Help & Support")
                .setMessage("How to use SsssHhift:\n\n" +
                        "1. Create profiles with different ringer modes and actions\n" +
                        "2. Set time-based or location-based triggers\n" +
                        "3. Enable profiles to activate automatically\n" +
                        "4. Grant necessary permissions for full functionality\n\n" +
                        "For more help, contact support or visit our website.")
                .setPositiveButton("OK", null)
                .setNeutralButton("Contact Support", (dialog, which) -> {
                    // You can add email intent here
                    Toast.makeText(getContext(), "Support contact: support@sssshhift.com", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Privacy Policy")
                .setMessage("Privacy & Data Usage:\n\n" +
                        "• Location data is used only for triggering profiles and is not shared\n" +
                        "• Calendar data is accessed only to detect events for silent mode\n" +
                        "• All data is stored locally on your device\n" +
                        "• No personal information is transmitted to external servers\n" +
                        "• You can delete all data anytime from settings")
                .setPositiveButton("OK", null)
                .show();
    }

    private void exportUserData() {
        // Implement data export functionality
        Toast.makeText(getContext(), "Data export functionality coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("This will delete all your profiles and settings. This action cannot be undone.\n\nAre you sure you want to continue?")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    // Show confirmation dialog
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Final Confirmation")
                            .setMessage("Last chance! This will permanently delete all your data.")
                            .setPositiveButton("Yes, Delete Everything", (d, w) -> clearAllData())
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllData() {
        try {
            // Clear all profiles from database
            requireContext().getContentResolver().delete(
                    com.example.sssshhift.provider.ProfileContentProvider.CONTENT_URI,
                    null,
                    null
            );

            // Clear shared preferences
            sharedPreferences.edit().clear().apply();

            // Reset switches
            calendarMonitoringSwitch.setChecked(false);
            locationBasedSwitch.setChecked(false);
            notificationsSwitch.setChecked(false);
            vibrationFeedbackSwitch.setChecked(false);

            Toast.makeText(getContext(), "All data cleared successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error clearing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update permission status when returning to this fragment
        updatePermissionStatus();
    }
}