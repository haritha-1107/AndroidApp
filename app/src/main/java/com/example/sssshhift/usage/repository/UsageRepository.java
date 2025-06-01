package com.example.sssshhift.usage.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

import com.example.sssshhift.usage.data.UsageLog;
import com.example.sssshhift.usage.data.UsageModeCount;
import com.example.sssshhift.usage.data.PeakHourCount;
import com.example.sssshhift.usage.data.UsageDatabase;
import com.example.sssshhift.usage.data.UsageLogDao;

import java.util.List;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsageRepository {
    private final UsageLogDao usageLogDao;
    private final ExecutorService executorService;
    private final LiveData<List<UsageLog>> recentLogs;
    private final LiveData<UsageModeCount> mostUsedMode;
    private final LiveData<PeakHourCount> peakHour;
    private final LiveData<Long> silentModeDuration;

    public UsageRepository(Application application) {
        UsageDatabase database = Room.databaseBuilder(
            application,
            UsageDatabase.class,
            "usage_database"
        ).build();

        usageLogDao = database.usageLogDao();
        executorService = Executors.newSingleThreadExecutor();
        
        // Get timestamp for 7 days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        long weekAgoTimestamp = calendar.getTimeInMillis();
        
        // Initialize LiveData with proper time filtering
        recentLogs = usageLogDao.getLogsAfter(weekAgoTimestamp);
        mostUsedMode = usageLogDao.getMostUsedMode(weekAgoTimestamp);
        peakHour = usageLogDao.getPeakHour(weekAgoTimestamp);
        silentModeDuration = usageLogDao.getTotalDurationForMode("silent", weekAgoTimestamp);
    }

    public LiveData<List<UsageLog>> getRecentLogs() {
        return recentLogs;
    }

    public LiveData<UsageModeCount> getMostUsedMode() {
        return mostUsedMode;
    }

    public LiveData<PeakHourCount> getPeakHour() {
        return peakHour;
    }

    public LiveData<Long> getSilentModeDuration() {
        return silentModeDuration;
    }

    public void insert(UsageLog log) {
        executorService.execute(() -> usageLogDao.insert(log));
    }
} 