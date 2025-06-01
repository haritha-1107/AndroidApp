package com.example.sssshhift.usage.data;

import androidx.room.ColumnInfo;

public class PeakHourCount {
    @ColumnInfo(name = "hour")
    private int hour;

    @ColumnInfo(name = "count")
    private int count;

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
} 