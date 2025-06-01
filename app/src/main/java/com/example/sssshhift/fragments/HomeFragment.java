package com.example.sssshhift.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import com.example.sssshhift.MainActivity;
import com.example.sssshhift.R;
import com.example.sssshhift.activities.AddProfileActivity;
import com.example.sssshhift.provider.ProfileContentProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private TextView tvActiveProfiles;
    private TextView tvTotalProfiles;
    private TextView tvLastActivity;
    private CardView cardQuickAdd;
    private CardView cardViewProfiles;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);

        initViews(view);
        setupClickListeners();
        loadDashboardData();

        return view;
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvActiveProfiles = view.findViewById(R.id.tv_active_profiles_count);
        tvTotalProfiles = view.findViewById(R.id.tv_total_profiles_count);
        tvLastActivity = view.findViewById(R.id.tv_last_activity);
        cardQuickAdd = view.findViewById(R.id.card_quick_add);
        cardViewProfiles = view.findViewById(R.id.card_view_profiles);
    }

    private void setupClickListeners() {
        cardQuickAdd.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddProfileActivity.class);
            startActivity(intent);
        });

        cardViewProfiles.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // Switch to profiles tab
                mainActivity.findViewById(R.id.nav_profiles).performClick();
            }
        });
    }

    private void loadDashboardData() {
        try {
            // Set welcome message with current time
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String currentTime = timeFormat.format(new Date());
            int hour = Integer.parseInt(currentTime.split(":")[0]);

            String greeting;
            if (hour < 12) {
                greeting = "Good Morning";
            } else if (hour < 17) {
                greeting = "Good Afternoon";
            } else {
                greeting = "Good Evening";
            }

            tvWelcome.setText(greeting + "!");

            // Get profile statistics
            Cursor cursor = requireContext().getContentResolver().query(
                    ProfileContentProvider.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );

            int totalProfiles = 0;
            int activeProfiles = 0;

            if (cursor != null) {
                totalProfiles = cursor.getCount();

                while (cursor.moveToNext()) {
                    int isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active"));
                    if (isActive == 1) {
                        activeProfiles++;
                    }
                }
                cursor.close();
            }

            tvTotalProfiles.setText(String.valueOf(totalProfiles));
            tvActiveProfiles.setText(String.valueOf(activeProfiles));

            // Set last activity (you can customize this based on your needs)
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            tvLastActivity.setText("Last updated: " + dateFormat.format(new Date()));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading dashboard data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh dashboard data when returning to this fragment
        loadDashboardData();
    }

    public void refreshDashboard() {
        loadDashboardData();
    }
}