package com.example.sssshhift.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.sssshhift.R;
import com.example.sssshhift.fragments.OnboardingFragment;

public class OnboardingAdapter extends FragmentStateAdapter {

    public OnboardingAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return OnboardingFragment.newInstance(
                        "Silent Focus",
                        "Keep your phone silent when you are busy and stay focused on your goal to achieve.",
                        R.drawable.ic_meditation
                );
            case 1:
                return OnboardingFragment.newInstance(
                        "Smart Reminders",
                        "Set intelligent reminders that automatically adjust your phone's ringer mode for important tasks.",
                        R.drawable.ic_reminder
                );
            case 2:
                return OnboardingFragment.newInstance(
                        "Location Aware",
                        "Automatically detect when you enter libraries, meetings, or quiet zones and adjust settings accordingly.",
                        R.drawable.ic_location1
                );
            default:
                return OnboardingFragment.newInstance(
                        "Welcome",
                        "Get started with your smart phone assistant.",
                        R.drawable.ic_welcome
                );
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}