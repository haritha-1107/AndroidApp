package com.example.sssshhift.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sssshhift.MainActivity;
import com.example.sssshhift.R;
import com.example.sssshhift.activities.EditProfileActivity;
import com.example.sssshhift.adapters.ProfileAdapter;
import com.example.sssshhift.models.Profile;
import com.example.sssshhift.provider.ProfileContentProvider;
import com.example.sssshhift.utils.NotificationUtils;
import com.example.sssshhift.utils.PhoneSettingsManager;
import com.example.sssshhift.utils.ProfileUtils;
import java.util.ArrayList;
import java.util.List;

public class ProfilesFragment extends Fragment implements ProfileAdapter.OnProfileInteractionListener {
    private static final String TAG = "ProfilesFragment";

    private RecyclerView recyclerView;
    private ProfileAdapter adapter;
    private List<Profile> profileList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_profiles, container, false);
            initViews(view);
            loadProfiles();
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error creating view", e);
            Toast.makeText(getContext(), "Error initializing profiles", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void initViews(View view) {
        try {
            recyclerView = view.findViewById(R.id.profiles_recycler_view);
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in layout");
                return;
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            profileList = new ArrayList<>();
            adapter = new ProfileAdapter(profileList, this);
            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(getContext(), "Error setting up profiles list", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfiles() {
        if (profileList == null) {
            profileList = new ArrayList<>();
        }
        profileList.clear();

        try {
            if (getContext() == null) {
                Log.e(TAG, "Context is null");
                return;
            }

            Cursor cursor = requireContext().getContentResolver().query(
                    ProfileContentProvider.CONTENT_URI,
                    null,
                    null,
                    null,
                    "created_at DESC"
            );

            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        try {
                            Profile profile = new Profile();
                            profile.setId(cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
                            profile.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                            profile.setTriggerType(cursor.getString(cursor.getColumnIndexOrThrow("trigger_type")));
                            profile.setTriggerValue(cursor.getString(cursor.getColumnIndexOrThrow("trigger_value")));
                            profile.setRingerMode(cursor.getString(cursor.getColumnIndexOrThrow("ringer_mode")));
                            profile.setActions(cursor.getString(cursor.getColumnIndexOrThrow("actions")));
                            profile.setActive(cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1);
                            profile.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));

                            // Load end time if available
                            int endTimeIndex = cursor.getColumnIndex("end_time");
                            if (endTimeIndex != -1) {
                                profile.setEndTime(cursor.getString(endTimeIndex));
                            }

                            profileList.add(profile);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading profile from cursor", e);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profiles", e);
            Toast.makeText(getContext(), "Error loading profiles", Toast.LENGTH_SHORT).show();
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void refreshProfiles() {
        if (isAdded() && getContext() != null) {
            loadProfiles();
        }
    }

    @Override
    public void onProfileToggle(Profile profile, boolean isActive) {
        if (!isAdded() || getContext() == null) return;

        try {
            // Update database first
            ContentValues values = new ContentValues();
            values.put("is_active", isActive ? 1 : 0);

            String selection = "_id = ?";
            String[] selectionArgs = {String.valueOf(profile.getId())};

            int updatedRows = requireContext().getContentResolver().update(
                    ProfileContentProvider.CONTENT_URI,
                    values,
                    selection,
                    selectionArgs
            );

            if (updatedRows > 0) {
                // Update local profile object
                profile.setActive(isActive);

                // Handle based on profile type
                if ("location".equals(profile.getTriggerType())) {
                    handleLocationProfileToggle(profile, isActive);
                } else if ("time".equals(profile.getTriggerType())) {
                    handleTimeProfileToggle(profile, isActive);
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling profile", e);
            Toast.makeText(requireContext(), "Error toggling profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLocationProfileToggle(Profile profile, boolean isActive) {
        if (!isAdded() || getContext() == null) return;

        try {
            if (isActive) {
                // Start location monitoring
                ProfileUtils.startLocationMonitoring(requireContext(), profile);
            } else {
                // Stop location monitoring
                ProfileUtils.stopLocationMonitoring(requireContext(), profile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling location profile toggle", e);
            Toast.makeText(requireContext(), "Error updating location profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleTimeProfileToggle(Profile profile, boolean isActive) {
        if (!isAdded() || getContext() == null) return;

        try {
            if (isActive) {
                // Schedule the profile
                ProfileUtils.scheduleProfile(requireContext(), profile.getName(), true, profile.getTriggerValue(), profile.getEndTime());
            } else {
                // Cancel the scheduled profile
                ProfileUtils.cancelProfileAlarms(requireContext(), profile.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling time profile toggle", e);
            Toast.makeText(requireContext(), "Error updating time profile", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProfileEdit(Profile profile) {
        if (!isAdded() || getContext() == null) return;

        try {
            Intent editIntent = new Intent(getContext(), EditProfileActivity.class);
            editIntent.putExtra(EditProfileActivity.EXTRA_PROFILE_ID, profile.getId());
            startActivity(editIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening edit screen", e);
            Toast.makeText(getContext(), "Error opening edit screen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProfileDelete(Profile profile) {
        if (!isAdded() || getContext() == null) return;

        try {
            // First cancel any scheduled alarms
            ProfileUtils.cancelProfileAlarms(requireContext(), profile.getName());

            // Then delete from database
            String selection = "_id = ?";
            String[] selectionArgs = {String.valueOf(profile.getId())};

            int deletedRows = requireContext().getContentResolver().delete(
                    ProfileContentProvider.CONTENT_URI,
                    selection,
                    selectionArgs
            );

            if (deletedRows > 0) {
                Toast.makeText(getContext(), "Profile deleted: " + profile.getName(), Toast.LENGTH_SHORT).show();
                loadProfiles(); // Refresh the list
            } else {
                Toast.makeText(getContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting profile", e);
            Toast.makeText(getContext(), "Error deleting profile", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && getContext() != null) {
            loadProfiles();
        }
    }

    @Override
    public void onProfileDetails(Profile profile) {
        if (!isAdded() || getContext() == null) return;

        try {
            // Create and show ProfileDetailsFragment
            ProfileDetailsFragment detailsFragment = ProfileDetailsFragment.newInstance(profile);

            // Navigate to details fragment
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, detailsFragment)
                        .addToBackStack("profile_details")
                        .commit();

                // Hide the FAB when viewing details
                if (getActivity() instanceof MainActivity) {
                    View fab = getActivity().findViewById(R.id.fab_add_profile);
                    if (fab != null) {
                        fab.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening profile details", e);
            Toast.makeText(getContext(), "Error opening profile details", Toast.LENGTH_SHORT).show();
        }
    }
}