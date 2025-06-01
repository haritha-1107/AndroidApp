package com.example.sssshhift.location;

import com.google.android.gms.maps.model.LatLng;

public class LocationProfile {
    private final String id;
    private final String name;
    private final LatLng location;
    private final int ringerMode;
    private final float radiusMeters;

    public LocationProfile(String id, String name, LatLng location, int ringerMode, float radiusMeters) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.ringerMode = ringerMode;
        this.radiusMeters = radiusMeters;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LatLng getLocation() {
        return location;
    }

    public int getRingerMode() {
        return ringerMode;
    }

    public float getRadiusMeters() {
        return radiusMeters;
    }
} 