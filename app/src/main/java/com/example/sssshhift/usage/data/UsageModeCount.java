package com.example.sssshhift.usage.data;

import androidx.room.ColumnInfo;

public class UsageModeCount {
    @ColumnInfo(name = "mode")
    private String mode;

    @ColumnInfo(name = "count")
    private int count;

    @ColumnInfo(name = "totalDuration")
    private long totalDuration;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public long getTotalDuration() { return totalDuration; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }
} 