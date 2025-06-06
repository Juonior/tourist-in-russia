package com.example.tourist_in_russia.ui.places;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.tourist_in_russia.R;
import com.example.tourist_in_russia.api.ApiService;
import com.example.tourist_in_russia.api.models.Place;
import java.util.ArrayList;
import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {
    private List<Place> places = new ArrayList<>();
    private final OnPlaceClickListener listener;

    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
    }

    public PlacesAdapter(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = places.get(position);
        holder.bind(place, listener);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final TextView ratingTextView;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.placeImage);
            nameTextView = itemView.findViewById(R.id.placeName);
            descriptionTextView = itemView.findViewById(R.id.placeDescription);
            ratingTextView = itemView.findViewById(R.id.placeRating);
        }

        public void bind(Place place, OnPlaceClickListener listener) {
            nameTextView.setText(place.getName());
            descriptionTextView.setText(place.getDescription());
            
            // Handle rating display
            Double rating = place.getAverageRating();
            if (rating != null) {
                ratingTextView.setText(String.format("%.1f", rating));
                ratingTextView.setVisibility(View.VISIBLE);
            } else {
                ratingTextView.setVisibility(View.GONE);
            }

            // Load image using Glide
            if (place.getMainPhotoPath() != null) {
                String imageUrl = ApiService.BASE_URL + place.getMainPhotoPath().replaceFirst("^/", "");
                Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_place)
                    .error(R.drawable.error_image)
                    .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_place);
            }

            itemView.setOnClickListener(v -> listener.onPlaceClick(place));
        }
    }
} 