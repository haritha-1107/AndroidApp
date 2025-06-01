package com.example.sssshhift.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sssshhift.R;
import com.example.sssshhift.usage.adapter.UsageLogAdapter;
import com.example.sssshhift.usage.viewmodel.UsageViewModel;
import com.example.sssshhift.usage.data.UsageLog;
import com.example.sssshhift.usage.data.UsageModeCount;
import com.example.sssshhift.usage.data.PeakHourCount;

import android.widget.TextView;
import java.util.List;

public class UsageInsightsActivity extends AppCompatActivity {
    private UsageViewModel viewModel;
    private UsageLogAdapter adapter;
    private TextView totalSilentTime;
    private TextView totalProfilesActivated;
    private TextView mostUsedMode;
    private TextView mostUsedModeCount;
    private TextView peakHour;
    private TextView peakHourCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_insights);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Usage Insights");
        }

        // Initialize Views
        totalSilentTime = findViewById(R.id.total_silent_time);
        totalProfilesActivated = findViewById(R.id.total_profiles_activated);
        mostUsedMode = findViewById(R.id.most_used_mode);
        mostUsedModeCount = findViewById(R.id.most_used_mode_count);
        peakHour = findViewById(R.id.peak_hour);
        peakHourCount = findViewById(R.id.peak_hour_count);

        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recent_activity_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsageLogAdapter(this);  // Pass context here
        recyclerView.setAdapter(adapter);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(UsageViewModel.class);

        // Observe Data Changes
        viewModel.getTotalSilentTime().observe(this, minutes -> {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            String timeText = hours > 0 ? 
                String.format("%dh %dm", hours, remainingMinutes) : 
                String.format("%dm", remainingMinutes);
            totalSilentTime.setText(timeText);
        });

        viewModel.getTotalProfilesActivated().observe(this, count -> 
            totalProfilesActivated.setText(String.valueOf(count)));

        viewModel.getMostUsedMode().observe(this, modeCount -> {
            if (modeCount != null) {
                mostUsedMode.setText(formatModeName(modeCount.getMode()));
                mostUsedModeCount.setText(String.format("Used %d times", modeCount.getCount()));
            }
        });

        viewModel.getPeakHour().observe(this, peakHourData -> {
            if (peakHourData != null) {
                peakHour.setText(String.format("%02d:00", peakHourData.getHour()));
                peakHourCount.setText(String.format("%d activations", peakHourData.getCount()));
            }
        });

        viewModel.getRecentActivity().observe(this, logs -> {
            adapter.setUsageLogs(logs);
        });

        // Apply animations
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down));
    }

    private String formatModeName(String mode) {
        if (mode == null) return "Unknown";
        String[] parts = mode.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase());
        }
        return formatted.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 