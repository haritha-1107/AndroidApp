package com.example.sssshhift.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.sssshhift.data.ProfileDatabaseHelper;
import com.example.sssshhift.location.LocationPickerActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.sssshhift.R;
import com.example.sssshhift.provider.ProfileContentProvider;
import com.example.sssshhift.utils.PermissionUtils;
import com.example.sssshhift.utils.ProfileUtils;
import com.example.sssshhift.models.Profile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity implements LocationListener {

    // UI Components
    private TextInputLayout profileNameLayout;
    private TextInputEditText profileNameEdit;
    private RadioGroup triggerTypeGroup;
    private RadioButton timeRadio, locationRadio;
    private LinearLayout timeContainer, locationContainer;
    private TextView selectedTimeText, selectedLocationText;
    private MaterialButton selectTimeBtn, selectLocationBtn, getCurrentLocationBtn;
    private ChipGroup ringerModeGroup, actionsGroup;
    private Chip silentChip, vibrateChip, normalChip;
    private Chip wifiChip, bluetoothChip, dataChip, dndChip;
    private MaterialButton saveProfileBtn;

    // End Time Components
    private SwitchMaterial enableEndTimeSwitch;
    private MaterialButton selectEndTimeBtn;
    private TextView selectedEndTimeText, durationPreviewText;

    // Data
    private String selectedTime = "";
    private String selectedEndTime = "";
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private LocationManager locationManager;
    private boolean isLocationSelected = false;

    // Profile data
    private Profile currentProfile;
    private long profileId;
    private String originalProfileName;

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int LOCATION_PICKER_REQUEST_CODE = 2001;
    public static final String EXTRA_PROFILE_ID = "profile_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_profile);

        // Get profile ID from intent
        profileId = getIntent().getLongExtra(EXTRA_PROFILE_ID, -1);
        if (profileId == -1) {
            Toast.makeText(this, "Error: Profile not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initViews();
        loadProfileData();
        setupListeners();
        setupLocationManager();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }
    }

    private void initViews() {
        // Profile Name
        profileNameLayout = findViewById(R.id.profile_name_layout);
        profileNameEdit = findViewById(R.id.profile_name_edit);

        // Trigger Type Selection
        triggerTypeGroup = findViewById(R.id.trigger_type_group);
        timeRadio = findViewById(R.id.time_radio);
        locationRadio = findViewById(R.id.location_radio);

        // Containers
        timeContainer = findViewById(R.id.time_container);
        locationContainer = findViewById(R.id.location_container);

        // Time Selection
        selectedTimeText = findViewById(R.id.selected_time_text);
        selectTimeBtn = findViewById(R.id.select_time_btn);

        // End Time Components
        enableEndTimeSwitch = findViewById(R.id.enable_end_time_switch);
        selectEndTimeBtn = findViewById(R.id.select_end_time_btn);
        selectedEndTimeText = findViewById(R.id.selected_end_time_text);
        durationPreviewText = findViewById(R.id.duration_preview_text);

        // Location Selection
        selectedLocationText = findViewById(R.id.selected_location_text);
        selectLocationBtn = findViewById(R.id.select_location_btn);
        getCurrentLocationBtn = findViewById(R.id.get_current_location_btn);

        // Ringer Mode Chips
        ringerModeGroup = findViewById(R.id.ringer_mode_group);
        silentChip = findViewById(R.id.silent_chip);
        vibrateChip = findViewById(R.id.vibrate_chip);
        normalChip = findViewById(R.id.normal_chip);

        // Action Chips
        actionsGroup = findViewById(R.id.actions_group);
        wifiChip = findViewById(R.id.wifi_chip);
        bluetoothChip = findViewById(R.id.bluetooth_chip);
        dataChip = findViewById(R.id.data_chip);
        dndChip = findViewById(R.id.dnd_chip);

        // Save Button
        saveProfileBtn = findViewById(R.id.save_profile_btn);
        saveProfileBtn.setText("Update Profile");
    }

    private void loadProfileData() {
        try {
            Uri profileUri = Uri.withAppendedPath(ProfileContentProvider.CONTENT_URI, String.valueOf(profileId));
            Cursor cursor = getContentResolver().query(profileUri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                currentProfile = new Profile();

                // Load profile data from cursor
                currentProfile.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_ID)));
                currentProfile.setName(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_NAME)));
                currentProfile.setTriggerType(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_TRIGGER_TYPE)));
                currentProfile.setTriggerValue(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_TRIGGER_VALUE)));

                // Handle end_time column (might be null)
                int endTimeIndex = cursor.getColumnIndex(ProfileDatabaseHelper.COLUMN_END_TIME);
                if (endTimeIndex != -1 && !cursor.isNull(endTimeIndex)) {
                    currentProfile.setEndTime(cursor.getString(endTimeIndex));
                }

                currentProfile.setRingerMode(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_RINGER_MODE)));
                currentProfile.setActions(cursor.getString(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_ACTIONS)));
                currentProfile.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_IS_ACTIVE)) == 1);
                currentProfile.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProfileDatabaseHelper.COLUMN_CREATED_AT)));

                cursor.close();

                // Store original name for scheduling updates
                originalProfileName = currentProfile.getName();

                // Populate UI with profile data
                populateUI();

            } else {
                Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                finish();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            android.util.Log.e("EditProfileActivity", "Error loading profile", e);
            finish();
        }
    }

    private void populateUI() {
        // Set profile name
        profileNameEdit.setText(currentProfile.getName());

        // Set trigger type
        if ("time".equals(currentProfile.getTriggerType())) {
            timeRadio.setChecked(true);
            showTimeContainer();

            // Set start time
            selectedTime = currentProfile.getTriggerValue();
            selectedTimeText.setText("Selected: " + selectedTime);
            selectedTimeText.setVisibility(View.VISIBLE);

            // Set end time if available
            if (currentProfile.getEndTime() != null && !currentProfile.getEndTime().isEmpty()) {
                selectedEndTime = currentProfile.getEndTime();
                enableEndTimeSwitch.setChecked(true);
                selectEndTimeBtn.setEnabled(true);
                selectedEndTimeText.setText("End Time: " + selectedEndTime);
                selectedEndTimeText.setVisibility(View.VISIBLE);
                updateDurationPreview();
            }

        } else if ("location".equals(currentProfile.getTriggerType())) {
            locationRadio.setChecked(true);
            showLocationContainer();

            // Parse location coordinates
            String[] coords = currentProfile.getTriggerValue().split(",");
            if (coords.length == 2) {
                try {
                    selectedLatitude = Double.parseDouble(coords[0]);
                    selectedLongitude = Double.parseDouble(coords[1]);
                    isLocationSelected = true;

                    // Try to get address name
                    try {
                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(selectedLatitude, selectedLongitude, 1);

                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String locationName = address.getFeatureName() + ", " + address.getLocality();
                            selectedLocationText.setText("Selected: " + locationName);
                        } else {
                            selectedLocationText.setText("Selected: " + selectedLatitude + ", " + selectedLongitude);
                        }
                    } catch (IOException e) {
                        selectedLocationText.setText("Selected: " + selectedLatitude + ", " + selectedLongitude);
                    }

                    selectedLocationText.setVisibility(View.VISIBLE);
                } catch (NumberFormatException e) {
                    android.util.Log.e("EditProfileActivity", "Error parsing coordinates", e);
                }
            }
        }

        // Set ringer mode
        switch (currentProfile.getRingerMode()) {
            case "silent":
                silentChip.setChecked(true);
                break;
            case "vibrate":
                vibrateChip.setChecked(true);
                break;
            case "normal":
            default:
                normalChip.setChecked(true);
                break;
        }

        // Set actions
        String[] actions = currentProfile.getActionsList();
        for (String action : actions) {
            switch (action.trim()) {
                case "wifi":
                    wifiChip.setChecked(true);
                    break;
                case "bluetooth":
                    bluetoothChip.setChecked(true);
                    break;
                case "data":
                    dataChip.setChecked(true);
                    break;
                case "dnd":
                    dndChip.setChecked(true);
                    break;
            }
        }
    }

    private void setupListeners() {
        // Trigger type selection
        triggerTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.time_radio) {
                showTimeContainer();
            } else if (checkedId == R.id.location_radio) {
                showLocationContainer();
            }
        });

        // Time selection
        selectTimeBtn.setOnClickListener(v -> showTimePicker(true));

        // End time functionality
        enableEndTimeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectEndTimeBtn.setEnabled(isChecked);
            if (!isChecked) {
                selectedEndTime = "";
                selectedEndTimeText.setVisibility(View.GONE);
                durationPreviewText.setVisibility(View.GONE);
            }
        });

        selectEndTimeBtn.setOnClickListener(v -> showTimePicker(false));

        // Location selection
        selectLocationBtn.setOnClickListener(v -> showLocationPicker());
        getCurrentLocationBtn.setOnClickListener(v -> getCurrentLocation());

        // Save profile
        saveProfileBtn.setOnClickListener(v -> updateProfile());
    }

    private void setupLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void showTimeContainer() {
        timeContainer.setVisibility(View.VISIBLE);
        locationContainer.setVisibility(View.GONE);
    }

    private void showLocationContainer() {
        timeContainer.setVisibility(View.GONE);
        locationContainer.setVisibility(View.VISIBLE);
    }

    private void showTimePicker(boolean isStartTime) {
        // Parse current time to set as default
        int defaultHour = 12;
        int defaultMinute = 0;

        String timeToEdit = isStartTime ? selectedTime : selectedEndTime;
        if (!TextUtils.isEmpty(timeToEdit)) {
            try {
                String[] parts = timeToEdit.split(":");
                defaultHour = Integer.parseInt(parts[0]);
                defaultMinute = Integer.parseInt(parts[1]);
            } catch (Exception e) {
                // Use defaults if parsing fails
            }
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    String timeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);

                    if (isStartTime) {
                        selectedTime = timeString;
                        selectedTimeText.setText("Selected: " + selectedTime);
                        selectedTimeText.setVisibility(View.VISIBLE);
                    } else {
                        selectedEndTime = timeString;
                        selectedEndTimeText.setText("End Time: " + selectedEndTime);
                        selectedEndTimeText.setVisibility(View.VISIBLE);
                    }

                    updateDurationPreview();
                },
                defaultHour, defaultMinute, false
        );

        if (isStartTime) {
            timePickerDialog.setTitle("Select Start Time");
        } else {
            timePickerDialog.setTitle("Select End Time");
        }

        timePickerDialog.show();
    }

    private void updateDurationPreview() {
        if (!TextUtils.isEmpty(selectedTime) && !TextUtils.isEmpty(selectedEndTime)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date startDate = sdf.parse(selectedTime);
                Date endDate = sdf.parse(selectedEndTime);

                if (startDate != null && endDate != null) {
                    long durationMillis = endDate.getTime() - startDate.getTime();

                    // Handle overnight duration
                    if (durationMillis < 0) {
                        durationMillis += 24 * 60 * 60 * 1000; // Add 24 hours
                    }

                    long durationHours = durationMillis / (60 * 60 * 1000);
                    long durationMinutes = (durationMillis % (60 * 60 * 1000)) / (60 * 1000);

                    String durationText;
                    if (durationHours > 0 && durationMinutes > 0) {
                        durationText = String.format(Locale.getDefault(),
                                "Duration: %d hours %d minutes (%s - %s)",
                                durationHours, durationMinutes, selectedTime, selectedEndTime);
                    } else if (durationHours > 0) {
                        durationText = String.format(Locale.getDefault(),
                                "Duration: %d hours (%s - %s)",
                                durationHours, selectedTime, selectedEndTime);
                    } else {
                        durationText = String.format(Locale.getDefault(),
                                "Duration: %d minutes (%s - %s)",
                                durationMinutes, selectedTime, selectedEndTime);
                    }

                    durationPreviewText.setText(durationText);
                    durationPreviewText.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                durationPreviewText.setVisibility(View.GONE);
            }
        } else {
            durationPreviewText.setVisibility(View.GONE);
        }
    }

    private void showLocationPicker() {
        Intent intent = new Intent(this, LocationPickerActivity.class);
        intent.putExtra("auto_confirm", true); // Enable auto-confirmation for current location

        // Pass current location if available
        if (isLocationSelected) {
            intent.putExtra("current_latitude", selectedLatitude);
            intent.putExtra("current_longitude", selectedLongitude);
        }

        startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (!PermissionUtils.hasLocationPermission(this)) {
            PermissionUtils.requestLocationPermission(this);
            return;
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1,
                    this
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    1,
                    this
            );

            Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();

        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        selectedLatitude = location.getLatitude();
        selectedLongitude = location.getLongitude();
        isLocationSelected = true;

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(selectedLatitude, selectedLongitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String locationName = address.getFeatureName() + ", " + address.getLocality();
                selectedLocationText.setText("Selected: " + locationName);
            } else {
                selectedLocationText.setText("Selected: " + selectedLatitude + ", " + selectedLongitude);
            }
        } catch (IOException e) {
            selectedLocationText.setText("Selected: " + selectedLatitude + ", " + selectedLongitude);
        }

        selectedLocationText.setVisibility(View.VISIBLE);

        // Stop location updates after getting location
        locationManager.removeUpdates(this);
        Toast.makeText(this, "Location updated successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle location provider status changes if needed
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Handle location provider enabled
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Handle location provider disabled
        Toast.makeText(this, "Please enable " + provider + " for location services", Toast.LENGTH_SHORT).show();
    }

    private void updateProfile() {
        // Validate input
        if (!validateInput()) {
            return;
        }

        // Gather profile data
        String profileName = profileNameEdit.getText().toString().trim();
        boolean isTimeBased = timeRadio.isChecked();
        String ringerMode = getSelectedRingerMode();
        String actions = getSelectedActions();

        // Create ContentValues with correct column names
        ContentValues values = new ContentValues();

        values.put(ProfileDatabaseHelper.COLUMN_NAME, profileName);
        values.put(ProfileDatabaseHelper.COLUMN_TRIGGER_TYPE, isTimeBased ? "time" : "location");

        // Set trigger_value based on type
        if (isTimeBased) {
            values.put(ProfileDatabaseHelper.COLUMN_TRIGGER_VALUE, selectedTime);
            // Add end_time if enabled
            if (enableEndTimeSwitch.isChecked() && !TextUtils.isEmpty(selectedEndTime)) {
                values.put(ProfileDatabaseHelper.COLUMN_END_TIME, selectedEndTime);
            } else {
                values.putNull(ProfileDatabaseHelper.COLUMN_END_TIME);
            }
        } else {
            // For location-based, store coordinates as "lat,lng"
            String locationValue = selectedLatitude + "," + selectedLongitude;
            values.put(ProfileDatabaseHelper.COLUMN_TRIGGER_VALUE, locationValue);
            values.putNull(ProfileDatabaseHelper.COLUMN_END_TIME);
        }

        values.put(ProfileDatabaseHelper.COLUMN_RINGER_MODE, ringerMode);
        values.put(ProfileDatabaseHelper.COLUMN_ACTIONS, actions);

        try {
            // Update profile in database using ContentProvider
            Uri profileUri = Uri.withAppendedPath(ProfileContentProvider.CONTENT_URI, String.valueOf(profileId));
            int rowsUpdated = getContentResolver().update(profileUri, values, null, null);

            if (rowsUpdated > 0) {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                // Update scheduling if it's time-based and the profile is active
                if (isTimeBased && currentProfile.isActive()) {
                    // Cancel old scheduling
                    ProfileUtils.cancelProfileAlarms(this, originalProfileName);

                    // Schedule with new settings
                    ProfileUtils.scheduleProfile(this, profileName, true, selectedTime, selectedEndTime);
                }

                // Return to previous activity
                finish();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            android.util.Log.e("EditProfileActivity", "Error updating profile", e);
        }
    }

    private boolean validateInput() {
        String profileName = profileNameEdit.getText().toString().trim();

        // Validate profile name
        if (TextUtils.isEmpty(profileName)) {
            profileNameLayout.setError("Profile name is required");
            profileNameEdit.requestFocus();
            return false;
        } else {
            profileNameLayout.setError(null);
        }

        // Validate trigger type specific requirements
        if (timeRadio.isChecked()) {
            if (TextUtils.isEmpty(selectedTime)) {
                Toast.makeText(this, "Please select a start time", Toast.LENGTH_SHORT).show();
                return false;
            }

            // If end time is enabled, validate it
            if (enableEndTimeSwitch.isChecked() && TextUtils.isEmpty(selectedEndTime)) {
                Toast.makeText(this, "Please select an end time or disable end time", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (locationRadio.isChecked()) {
            if (!isLocationSelected) {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // Validate ringer mode selection
        if (getSelectedRingerMode().isEmpty()) {
            Toast.makeText(this, "Please select a ringer mode", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getSelectedRingerMode() {
        if (silentChip.isChecked()) {
            return "silent";
        } else if (vibrateChip.isChecked()) {
            return "vibrate";
        } else if (normalChip.isChecked()) {
            return "normal";
        }
        return "";
    }

    private String getSelectedActions() {
        StringBuilder actions = new StringBuilder();

        if (wifiChip.isChecked()) {
            actions.append("wifi,");
        }
        if (bluetoothChip.isChecked()) {
            actions.append("bluetooth,");
        }
        if (dataChip.isChecked()) {
            actions.append("data,");
        }
        if (dndChip.isChecked()) {
            actions.append("dnd,");
        }

        // Remove trailing comma
        if (actions.length() > 0) {
            actions.setLength(actions.length() - 1);
        }

        return actions.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required to get current location", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up location updates if still active
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra("latitude", 0.0);
            selectedLongitude = data.getDoubleExtra("longitude", 0.0);
            float radius = data.getFloatExtra("radius", 100f);
            boolean isCurrentLocation = data.getBooleanExtra("isCurrentLocation", false);

            isLocationSelected = true;
            
            // Get location name from coordinates
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(selectedLatitude, selectedLongitude, 1);
                String locationName = "Unknown Location";
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    if (address.getMaxAddressLineIndex() >= 0) {
                        locationName = address.getAddressLine(0);
                    }
                }
                selectedLocationText.setText("Selected: " + locationName);
            } catch (IOException e) {
                selectedLocationText.setText(String.format("Selected: %.6f, %.6f", selectedLatitude, selectedLongitude));
            }
            
            selectedLocationText.setVisibility(View.VISIBLE);

            // If this is current location, immediately apply the profile
            if (isCurrentLocation) {
                updateProfile();
            } else {
                Toast.makeText(this, "Location updated successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }
}