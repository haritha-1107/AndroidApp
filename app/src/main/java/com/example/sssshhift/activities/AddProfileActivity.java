package com.example.sssshhift.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.example.sssshhift.utils.PhoneSettingsManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddProfileActivity extends AppCompatActivity implements LocationListener {

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

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int LOCATION_PICKER_REQUEST_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_profile);

        setupToolbar();
        initViews();
        setupListeners();
        setupLocationManager();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Profile");
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

        // Set default selections
        normalChip.setChecked(true);
        timeRadio.setChecked(true);
        showTimeContainer();
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
        saveProfileBtn.setOnClickListener(v -> saveProfile());
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
                12, 0, false
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
        startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE);
    }

    private void saveProfile() {
        if (!validateInput()) {
            return;
        }

        // Prepare profile data
        ContentValues values = new ContentValues();
        values.put(ProfileDatabaseHelper.COLUMN_NAME, profileNameEdit.getText().toString().trim());
        values.put(ProfileDatabaseHelper.COLUMN_TRIGGER_TYPE, locationRadio.isChecked() ? "location" : "time");
        
        // Store either time or location coordinates in TRIGGER_VALUE based on trigger type
        if (locationRadio.isChecked()) {
            String locationValue = selectedLatitude + "," + selectedLongitude;
            values.put(ProfileDatabaseHelper.COLUMN_TRIGGER_VALUE, locationValue);
        } else {
            values.put(ProfileDatabaseHelper.COLUMN_TRIGGER_VALUE, selectedTime);
        }
        
        values.put(ProfileDatabaseHelper.COLUMN_END_TIME, selectedEndTime);
        values.put(ProfileDatabaseHelper.COLUMN_RINGER_MODE, getSelectedRingerMode());
        values.put(ProfileDatabaseHelper.COLUMN_ACTIONS, getSelectedActions());
        values.put(ProfileDatabaseHelper.COLUMN_IS_ACTIVE, locationRadio.isChecked() ? 1 : 0); // Activate location-based profiles by default
        values.put(ProfileDatabaseHelper.COLUMN_CREATED_AT, System.currentTimeMillis()); // Add creation timestamp

        // Save profile
        Uri uri = getContentResolver().insert(ProfileContentProvider.CONTENT_URI, values);

        if (uri != null) {
            Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();

            // If this is a location-based profile using current location, activate it immediately
            if (locationRadio.isChecked()) {
                long profileId = Long.parseLong(uri.getLastPathSegment());
                String profileName = profileNameEdit.getText().toString().trim();
                
                // Get the last known location
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLocation != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(),
                                selectedLatitude, selectedLongitude, results);
                        
                        // If we're within the radius (e.g., 100 meters)
                        if (results[0] <= 100) {
                            // Apply the profile settings immediately
                            String ringerMode = getSelectedRingerMode();
                            PhoneSettingsManager.setRingerMode(this, ringerMode);
                            PhoneSettingsManager.applyActions(this, getSelectedActions());
                            
                            // Store this as the active profile
                            getSharedPreferences("profile_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("active_location_profile", profileName)
                                .apply();
                            
                            Toast.makeText(this, "Profile activated", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (!PermissionUtils.hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Show loading indicator
        getCurrentLocationBtn.setEnabled(false);
        getCurrentLocationBtn.setText("Getting location...");

        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
            handleLocationUpdate(lastLocation);
        } else {
            // Request location updates
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    this);
        }
    }

    private void handleLocationUpdate(Location location) {
        selectedLatitude = location.getLatitude();
        selectedLongitude = location.getLongitude();
        isLocationSelected = true;

        // Update UI
        getCurrentLocationBtn.setEnabled(true);
        getCurrentLocationBtn.setText("Get Current Location");

        // Get address for the location
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(selectedLatitude, selectedLongitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = "";
                
                // Get the most detailed address possible
                if (address.getMaxAddressLineIndex() > 0) {
                    addressText = address.getAddressLine(0);
                } else {
                    // Build address from components
                    if (address.getLocality() != null) {
                        addressText += address.getLocality() + ", ";
                    }
                    if (address.getAdminArea() != null) {
                        addressText += address.getAdminArea();
                    }
                }
                
                selectedLocationText.setText(addressText);
                selectedLocationText.setVisibility(View.VISIBLE);
            }
        } catch (IOException e) {
            String locationText = String.format(Locale.getDefault(),
                    "Lat: %.6f, Long: %.6f",
                    selectedLatitude,
                    selectedLongitude);
            selectedLocationText.setText(locationText);
            selectedLocationText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Remove location updates after getting the first update
        locationManager.removeUpdates(this);
        handleLocationUpdate(location);
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
                saveProfile();
            } else {
                Toast.makeText(this, "Location selected successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }

}