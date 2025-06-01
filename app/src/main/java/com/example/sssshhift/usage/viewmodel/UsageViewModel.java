package com.example.sssshhift.usage.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.sssshhift.usage.data.UsageLog;
import com.example.sssshhift.usage.data.UsageModeCount;
import com.example.sssshhift.usage.data.PeakHourCount;
import com.example.sssshhift.usage.repository.UsageRepository;

import java.util.List;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class UsageViewModel extends AndroidViewModel {
    private final UsageRepository repository;
    private final LiveData<List<UsageLog>> recentActivity;
    private final LiveData<Long> totalSilentTime;
    private final LiveData<Integer> totalProfilesActivated;
    private final LiveData<UsageModeCount> mostUsedMode;
    private final LiveData<PeakHourCount> peakHour;

    public UsageViewModel(Application application) {
        super(application);
        repository = new UsageRepository(application);
        recentActivity = repository.getRecentLogs();
        
        // Calculate total silent time
        totalSilentTime = Transformations.map(recentActivity, logs -> {
            long totalMinutes = 0;
            if (logs != null) {
                for (UsageLog log : logs) {
                    if ("SILENT".equalsIgnoreCase(log.getMode())) {
                        totalMinutes += log.getDurationMinutes();
                    }
                }
            }
            return totalMinutes;
        });

        // Count total profiles activated
        totalProfilesActivated = Transformations.map(recentActivity, logs -> 
            logs != null ? logs.size() : 0);

        // Calculate most used mode
        mostUsedMode = repository.getMostUsedMode();

        // Get peak activity hour
        peakHour = repository.getPeakHour();
    }

    public LiveData<List<UsageLog>> getRecentActivity() {
        return recentActivity;
    }

    public LiveData<Long> getTotalSilentTime() {
        return totalSilentTime;
    }

    public LiveData<Integer> getTotalProfilesActivated() {
        return totalProfilesActivated;
    }

    public LiveData<UsageModeCount> getMostUsedMode() {
        return mostUsedMode;
    }

    public LiveData<PeakHourCount> getPeakHour() {
        return peakHour;
    }
} 