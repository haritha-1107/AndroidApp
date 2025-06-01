package com.example.sssshhift.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sssshhift.R;
import java.util.List;

public class LocationSearchAdapter extends RecyclerView.Adapter<LocationSearchAdapter.ViewHolder> {

    public static class LocationItem {
        private String name;
        private double latitude;
        private double longitude;

        public LocationItem(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getName() { return name; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }

    public interface OnLocationClickListener {
        void onLocationClick(LocationItem location);
    }

    private List<LocationItem> locations;
    private OnLocationClickListener clickListener;

    public LocationSearchAdapter(List<LocationItem> locations, OnLocationClickListener clickListener) {
        this.locations = locations;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationItem location = locations.get(position);
        holder.locationName.setText(location.getName());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onLocationClick(location);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView locationName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            locationName = itemView.findViewById(R.id.location_name);
        }
    }
}