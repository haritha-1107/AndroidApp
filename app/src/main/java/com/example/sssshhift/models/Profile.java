package com.example.sssshhift.models;

public class Profile {
    private long id;
    private String name;
    private String triggerType; // "time" or "location"
    private String triggerValue; // time string or lat,lng
    private String endTime; // NEW: end time for duration-based profiles
    private String ringerMode; // "silent", "vibrate", "normal"
    private String actions; // comma-separated actions
    private boolean isActive;
    private long createdAt;

    // Constructors
    public Profile() {}

    public Profile(long id, String name, String triggerType, String triggerValue,
                   String endTime, String ringerMode, String actions, boolean isActive, long createdAt) {
        this.id = id;
        this.name = name;
        this.triggerType = triggerType;
        this.triggerValue = triggerValue;
        this.endTime = endTime;
        this.ringerMode = ringerMode;
        this.actions = actions;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getTriggerValue() { return triggerValue; }
    public void setTriggerValue(String triggerValue) { this.triggerValue = triggerValue; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getRingerMode() { return ringerMode; }
    public void setRingerMode(String ringerMode) { this.ringerMode = ringerMode; }

    public String getActions() { return actions; }
    public void setActions(String actions) { this.actions = actions; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Utility methods
    public String getFormattedTrigger() {
        if ("time".equals(triggerType)) {
            if (endTime != null && !endTime.isEmpty()) {
                return "⏰ " + triggerValue + " - " + endTime;
            } else {
                return "⏰ " + triggerValue;
            }
        } else {
            return "📍 Location-based";
        }
    }

    public String getFormattedRingerMode() {
        switch (ringerMode) {
            case "silent": return "🔇 Silent";
            case "vibrate": return "📳 Vibrate";
            case "normal": return "🔊 Normal";
            default: return "🔊 Normal";
        }
    }

    public String[] getActionsList() {
        if (actions == null || actions.trim().isEmpty()) {
            return new String[0];
        }
        return actions.split(",");
    }

    public boolean hasDuration() {
        return endTime != null && !endTime.isEmpty();
    }

    // Location-related methods
    public String getLocation() {
        return "location".equals(triggerType) ? triggerValue : null;
    }

    public float getRadius() {
        // Default radius in meters if not specified
        return 100f;
    }
}