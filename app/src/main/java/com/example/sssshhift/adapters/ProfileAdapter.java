package com.example.sssshhift.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.button.MaterialButton;
import com.example.sssshhift.R;
import com.example.sssshhift.models.Profile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    private List<Profile> profileList;
    private OnProfileInteractionListener listener;

    public interface OnProfileInteractionListener {
        void onProfileToggle(Profile profile, boolean isActive);
        void onProfileEdit(Profile profile);
        void onProfileDelete(Profile profile);
        void onProfileDetails(Profile profile);
    }

    public ProfileAdapter(List<Profile> profileList, OnProfileInteractionListener listener) {
        this.profileList = profileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        Profile profile = profileList.get(position);
        holder.bind(profile);
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    class ProfileViewHolder extends RecyclerView.ViewHolder {

        private MaterialCardView cardView;
        private TextView profileName;
        private TextView triggerInfo;
        private TextView ringerModeText;
        private TextView createdAtText;
        private ChipGroup actionsChipGroup;
        private SwitchMaterial activeSwitch;
        private MaterialButton editButton;
        private MaterialButton deleteButton;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.profile_card);
            profileName = itemView.findViewById(R.id.profile_name);
            triggerInfo = itemView.findViewById(R.id.trigger_info);
            ringerModeText = itemView.findViewById(R.id.ringer_mode_text);
            createdAtText = itemView.findViewById(R.id.created_at_text);
            actionsChipGroup = itemView.findViewById(R.id.actions_chip_group);
            activeSwitch = itemView.findViewById(R.id.active_switch);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        public void bind(Profile profile) {
            // Set profile name
            profileName.setText(profile.getName());

            // Set trigger information
            triggerInfo.setText(profile.getFormattedTrigger());

            // Set ringer mode
            ringerModeText.setText(profile.getFormattedRingerMode());

            // Set created date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            createdAtText.setText("Created: " + dateFormat.format(new Date(profile.getCreatedAt())));

            // Set active switch
            activeSwitch.setChecked(profile.isActive());
            activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onProfileToggle(profile, isChecked);
                }
            });

            // Setup action chips
            setupActionChips(profile);

            // Set card elevation based on active state
            if (profile.isActive()) {
                cardView.setCardElevation(8f);
                cardView.setStrokeWidth(2);
            } else {
                cardView.setCardElevation(2f);
                cardView.setStrokeWidth(1);
            }

            // Set click listeners
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProfileEdit(profile);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProfileDelete(profile);
                }
            });

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProfileDetails(profile);
                }
            });
        }

        private void setupActionChips(Profile profile) {
            actionsChipGroup.removeAllViews();

            String[] actions = profile.getActionsList();
            for (String action : actions) {
                if (action.trim().isEmpty()) continue;

                Chip chip = new Chip(itemView.getContext());
                chip.setClickable(false);
                chip.setChipBackgroundColorResource(R.color.chip_background_inactive);
                chip.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                chip.setTextSize(10f);

                switch (action.trim()) {
                    case "wifi":
                        chip.setText("📶 Wi-Fi");
                        break;
                    case "bluetooth":
                        chip.setText("🔵 Bluetooth");
                        break;
                    case "data":
                        chip.setText("📱 Data");
                        break;
                    case "dnd":
                        chip.setText("🚫 DND");
                        break;
                    default:
                        chip.setText(action.trim());
                        break;
                }

                actionsChipGroup.addView(chip);
            }

            // Show "No actions" if empty
            if (actions.length == 0 || (actions.length == 1 && actions[0].trim().isEmpty())) {
                Chip noActionsChip = new Chip(itemView.getContext());
                noActionsChip.setText("No additional actions");
                noActionsChip.setClickable(false);
                noActionsChip.setChipBackgroundColorResource(R.color.chip_background_inactive);
                noActionsChip.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                noActionsChip.setTextSize(10f);
                actionsChipGroup.addView(noActionsChip);
            }
        }
    }
}