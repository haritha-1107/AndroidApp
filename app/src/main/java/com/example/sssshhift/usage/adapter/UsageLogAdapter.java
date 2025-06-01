package com.example.sssshhift.usage.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sssshhift.R;
import com.example.sssshhift.usage.data.UsageLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class UsageLogAdapter extends RecyclerView.Adapter<UsageLogAdapter.ViewHolder> {
    private List<UsageLog> usageLogs = new ArrayList<>();
    private final Context context;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public UsageLogAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usage_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UsageLog log = usageLogs.get(position);
        
        // Set mode name and icon
        holder.modeName.setText(formatModeName(log.getMode()));
        holder.modeIcon.setImageResource(getModeIcon(log.getMode()));
        
        // Set duration
        holder.duration.setText(formatDuration(log.getDurationMinutes()));
        
        // Set relative timestamp
        holder.timestamp.setText(getRelativeTimeSpan(log.getStartTime()));
    }

    @Override
    public int getItemCount() {
        return usageLogs.size();
    }

    public void setUsageLogs(List<UsageLog> logs) {
        this.usageLogs = logs;
        notifyDataSetChanged();
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

    private int getModeIcon(String mode) {
        if (mode == null) return R.drawable.ic_notifications;
        switch (mode.toLowerCase()) {
            case "silent":
                return R.drawable.ic_notifications_off;
            case "vibrate":
                return R.drawable.ic_phone_vibrate;
            default:
                return R.drawable.ic_notifications;
        }
    }

    private String formatDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " min";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + "h";
            }
            return String.format(Locale.getDefault(), "%dh %dm", hours, remainingMinutes);
        }
    }

    private String getRelativeTimeSpan(Date startTime) {
        if (startTime == null) return "";
        
        long diffInMillis = System.currentTimeMillis() - startTime.getTime();
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        
        if (diffInMinutes < 1) {
            return "just now";
        } else if (diffInMinutes < 60) {
            return diffInMinutes + "m ago";
        } else if (diffInMinutes < 24 * 60) {
            long hours = TimeUnit.MINUTES.toHours(diffInMinutes);
            return hours + "h ago";
        } else {
            return timeFormat.format(startTime);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView modeIcon;
        final TextView modeName;
        final TextView duration;
        final TextView timestamp;

        ViewHolder(View itemView) {
            super(itemView);
            modeIcon = itemView.findViewById(R.id.mode_icon);
            modeName = itemView.findViewById(R.id.mode_name);
            duration = itemView.findViewById(R.id.duration);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
} 