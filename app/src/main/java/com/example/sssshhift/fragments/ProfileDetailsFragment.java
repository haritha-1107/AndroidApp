package com.example.sssshhift.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import com.example.sssshhift.R;
import com.example.sssshhift.models.Profile;
import com.example.sssshhift.utils.ProfileUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileDetailsFragment extends Fragment {

    private static final String ARG_PROFILE_ID = "profile_id";
    private static final String ARG_PROFILE_NAME = "profile_name";
    private static final String ARG_TRIGGER_TYPE = "trigger_type";
    private static final String ARG_TRIGGER_VALUE = "trigger_value";
    private static final String ARG_RINGER_MODE = "ringer_mode";
    private static final String ARG_ACTIONS = "actions";
    private static final String ARG_IS_ACTIVE = "is_active";
    private static final String ARG_CREATED_AT = "created_at";
    private static final String ARG_END_TIME = "end_time";

    private Profile profile;

    // UI Components
    private TextView tvProfileName;
    private TextView tvTriggerType;
    private TextView tvTriggerValue;
    private TextView tvRingerMode;
    private TextView tvEndTime;
    private TextView tvCreatedAt;
    private TextView tvStatus;
    private TextView labelEndTime; // Added this field
    private Switch switchActive;
    private LinearLayout actionsContainer;
    private Button btnEdit;
    private Button btnDelete;
    private CardView cardTriggerInfo;
    private CardView cardActions;

    public ProfileDetailsFragment() {
        // Required empty public constructor
    }

    public static ProfileDetailsFragment newInstance(Profile profile) {
        ProfileDetailsFragment fragment = new ProfileDetailsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PROFILE_ID, profile.getId());
        args.putString(ARG_PROFILE_NAME, profile.getName());
        args.putString(ARG_TRIGGER_TYPE, profile.getTriggerType());
        args.putString(ARG_TRIGGER_VALUE, profile.getTriggerValue());
        args.putString(ARG_RINGER_MODE, profile.getRingerMode());
        args.putString(ARG_ACTIONS, profile.getActions());
        args.putBoolean(ARG_IS_ACTIVE, profile.isActive());
        args.putLong(ARG_CREATED_AT, profile.getCreatedAt());
        args.putString(ARG_END_TIME, profile.getEndTime());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            profile = new Profile();
            profile.setId(getArguments().getLong(ARG_PROFILE_ID));
            profile.setName(getArguments().getString(ARG_PROFILE_NAME));
            profile.setTriggerType(getArguments().getString(ARG_TRIGGER_TYPE));
            profile.setTriggerValue(getArguments().getString(ARG_TRIGGER_VALUE));
            profile.setRingerMode(getArguments().getString(ARG_RINGER_MODE));
            profile.setActions(getArguments().getString(ARG_ACTIONS));
            profile.setActive(getArguments().getBoolean(ARG_IS_ACTIVE));
            profile.setCreatedAt(getArguments().getLong(ARG_CREATED_AT));
            profile.setEndTime(getArguments().getString(ARG_END_TIME));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        populateData();
        setupListeners();
    }

    private void initViews(View view) {
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvTriggerType = view.findViewById(R.id.tv_trigger_type);
        tvTriggerValue = view.findViewById(R.id.tv_trigger_value);
        tvRingerMode = view.findViewById(R.id.tv_ringer_mode);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        tvCreatedAt = view.findViewById(R.id.tv_created_at);
        tvStatus = view.findViewById(R.id.tv_status);
        switchActive = view.findViewById(R.id.switch_active);
        actionsContainer = view.findViewById(R.id.actions_container);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnDelete = view.findViewById(R.id.btn_delete);
        cardTriggerInfo = view.findViewById(R.id.card_trigger_info);
        cardActions = view.findViewById(R.id.card_actions);
        labelEndTime = view.findViewById(R.id.label_end_time); // Added this line
    }

    private void populateData() {
        if (profile == null) return;

        tvProfileName.setText(profile.getName());

        // Format trigger type for display
        String triggerTypeDisplay = formatTriggerType(profile.getTriggerType());
        tvTriggerType.setText(triggerTypeDisplay);

        tvTriggerValue.setText(profile.getTriggerValue());

        // Format ringer mode for display
        String ringerModeDisplay = formatRingerMode(profile.getRingerMode());
        tvRingerMode.setText(ringerModeDisplay);

        // Show end time if available
        if (profile.getEndTime() != null && !profile.getEndTime().isEmpty()) {
            tvEndTime.setText(profile.getEndTime());
            tvEndTime.setVisibility(View.VISIBLE);
            labelEndTime.setVisibility(View.VISIBLE); // Fixed - use stored reference
        } else {
            tvEndTime.setVisibility(View.GONE);
            labelEndTime.setVisibility(View.GONE); // Fixed - use stored reference
        }

        // Format created date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        String createdDate = sdf.format(new Date(profile.getCreatedAt()));
        tvCreatedAt.setText(createdDate);

        // Set status
        switchActive.setChecked(profile.isActive());
        updateStatusText(profile.isActive());

        // Display actions if available
        displayActions(profile.getActions());
    }

    private String formatTriggerType(String triggerType) {
        if (triggerType == null) return "Unknown";

        switch (triggerType.toLowerCase()) {
            case "time":
                return "Time-based";
            case "location":
                return "Location-based";
            case "wifi":
                return "WiFi Network";
            case "bluetooth":
                return "Bluetooth Device";
            case "calendar":
                return "Calendar Event";
            default:
                return triggerType.substring(0, 1).toUpperCase() + triggerType.substring(1);
        }
    }

    private String formatRingerMode(String ringerMode) {
        if (ringerMode == null) return "Unknown";

        switch (ringerMode.toLowerCase()) {
            case "silent":
                return "Silent Mode";
            case "vibrate":
                return "Vibrate Mode";
            case "normal":
                return "Normal Mode";
            case "dnd":
                return "Do Not Disturb";
            default:
                return ringerMode.substring(0, 1).toUpperCase() + ringerMode.substring(1);
        }
    }

    private void displayActions(String actions) {
        actionsContainer.removeAllViews();

        if (actions == null || actions.isEmpty()) {
            TextView noActions = new TextView(getContext());
            noActions.setText("No additional actions configured");
            noActions.setTextColor(getResources().getColor(android.R.color.darker_gray));
            actionsContainer.addView(noActions);
            return;
        }

        // Parse and display actions (assuming they're comma-separated)
        String[] actionList = actions.split(",");
        for (String action : actionList) {
            TextView actionView = new TextView(getContext());
            actionView.setText("• " + action.trim());
            actionView.setPadding(0, 8, 0, 8);
            actionsContainer.addView(actionView);
        }
    }

    private void updateStatusText(boolean isActive) {
        if (isActive) {
            tvStatus.setText("Active");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvStatus.setText("Inactive");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void setupListeners() {
        switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleProfile(isChecked);
        });

        btnEdit.setOnClickListener(v -> {
            if (getActivity() instanceof OnProfileActionListener) {
                ((OnProfileActionListener) getActivity()).onEditProfile(profile);
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (getActivity() instanceof OnProfileActionListener) {
                ((OnProfileActionListener) getActivity()).onDeleteProfile(profile);
            }
        });
    }

    private void toggleProfile(boolean isActive) {
        try {
            profile.setActive(isActive);
            updateStatusText(isActive);

            if (isActive) {
                // Schedule the profile
                if ("time".equals(profile.getTriggerType())) {
                    ProfileUtils.scheduleProfile(requireContext(), profile.getName(),
                            true, profile.getTriggerValue(), profile.getEndTime());
                } else {
                    ProfileUtils.scheduleProfile(requireContext(), profile.getName(),
                            false, profile.getTriggerValue(), profile.getEndTime());
                }

                Toast.makeText(getContext(), "Profile activated", Toast.LENGTH_SHORT).show();
            } else {
                // Cancel the scheduled profile
                ProfileUtils.cancelProfileAlarms(requireContext(), profile.getName());
                Toast.makeText(getContext(), "Profile deactivated", Toast.LENGTH_SHORT).show();
            }

            // Notify the host activity about the change
            if (getActivity() instanceof OnProfileActionListener) {
                ((OnProfileActionListener) getActivity()).onProfileToggled(profile, isActive);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            // Revert the switch state
            switchActive.setChecked(!isActive);
        }
    }

    public interface OnProfileActionListener {
        void onEditProfile(Profile profile);
        void onDeleteProfile(Profile profile);
        void onProfileToggled(Profile profile, boolean isActive);
    }

    // Method to update the profile data (useful when returning from edit)
    public void updateProfile(Profile updatedProfile) {
        this.profile = updatedProfile;
        populateData();
    }
}