package com.example.sssshhift.location;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.sssshhift.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "LocationPickerActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng selectedLocation;
    private static final float DEFAULT_ZOOM = 15f;
    private TextView locationInfoText;
    private EditText searchInput;
    private ImageButton searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Initialize UI elements
        locationInfoText = findViewById(R.id.location_info_text);
        searchInput = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);
        
        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup search functionality
        setupSearch();

        // Setup current location button
        FloatingActionButton fabCurrentLocation = findViewById(R.id.fab_current_location);
        fabCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        // Setup save location button
        FloatingActionButton fabSaveLocation = findViewById(R.id.fab_save_location);
        fabSaveLocation.setOnClickListener(v -> saveSelectedLocation());
    }

    private void setupSearch() {
        // Handle search button click
        searchButton.setOnClickListener(v -> performSearch());

        // Handle keyboard search action
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String searchQuery = searchInput.getText().toString().trim();
        if (searchQuery.isEmpty()) {
            Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(searchQuery, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                selectedLocation = location;
                updateMapLocation(location);
                updateLocationInfo(location);
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error searching for location: " + e.getMessage());
            Toast.makeText(this, "Error searching for location", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Set up map UI settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        
        // Set up map click listener
        mMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            updateMapLocation(latLng);
            updateLocationInfo(latLng);
        });

        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private void updateLocationInfo(LatLng latLng) {
        // Use Geocoder to get address information
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
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
                
                // Update the TextView with location info
                String locationInfo = String.format("Selected Location:\n%s\nLat: %.6f, Long: %.6f",
                        addressText, latLng.latitude, latLng.longitude);
                locationInfoText.setText(locationInfo);
            } else {
                // If no address found, just show coordinates
                String locationInfo = String.format("Selected Location:\nLat: %.6f, Long: %.6f",
                        latLng.latitude, latLng.longitude);
                locationInfoText.setText(locationInfo);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address: " + e.getMessage());
            // Show only coordinates if geocoding fails
            String locationInfo = String.format("Selected Location:\nLat: %.6f, Long: %.6f",
                    latLng.latitude, latLng.longitude);
            locationInfoText.setText(locationInfo);
        }
    }

    private void updateMapLocation(LatLng location) {
        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Selected Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM));
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void getCurrentLocation() {
        if (!checkLocationPermission()) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        selectedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        updateMapLocation(selectedLocation);
                        updateLocationInfo(selectedLocation);
                    } else {
                        Toast.makeText(this, "Unable to get current location",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error getting location: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void saveSelectedLocation() {
        if (selectedLocation != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedLocation.latitude);
            resultIntent.putExtra("longitude", selectedLocation.longitude);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Please select a location first",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                        getCurrentLocation();
                    }
                }
            } else {
                Toast.makeText(this, "Location permission is required",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
} 