package com.example.sssshhift.usage.data;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface UsageLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UsageLog log);

    @Query("SELECT * FROM usage_logs WHERE startTime >= :timestamp ORDER BY startTime DESC")
    LiveData<List<UsageLog>> getLogsAfter(long timestamp);

    @Query("SELECT mode as mode, COUNT(*) as count, SUM(durationMinutes) as totalDuration " +
           "FROM usage_logs " +
           "WHERE startTime >= :timestamp " +
           "GROUP BY mode ORDER BY count DESC LIMIT 1")
    LiveData<UsageModeCount> getMostUsedMode(long timestamp);

    @Query("SELECT strftime('%H', datetime(startTime/1000, 'unixepoch', 'localtime')) as hour, " +
           "COUNT(*) as count " +
           "FROM usage_logs " +
           "WHERE startTime >= :timestamp " +
           "GROUP BY hour ORDER BY count DESC LIMIT 1")
    LiveData<PeakHourCount> getPeakHour(long timestamp);

    @Query("SELECT SUM(durationMinutes) FROM usage_logs " +
           "WHERE mode = :mode AND startTime >= :timestamp")
    LiveData<Long> getTotalDurationForMode(String mode, long timestamp);
} 