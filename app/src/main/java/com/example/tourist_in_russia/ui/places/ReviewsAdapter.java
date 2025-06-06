package com.example.tourist_in_russia.ui.places;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.tourist_in_russia.R;
import com.example.tourist_in_russia.api.ApiService;
import com.example.tourist_in_russia.api.models.Review;
import com.example.tourist_in_russia.databinding.ItemReviewBinding;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {
    private List<Review> reviews = new ArrayList<>();
    private final boolean isAdmin;
    private final OnReviewDeleteListener deleteListener;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());

    public interface OnReviewDeleteListener {
        void onReviewDelete(Review review);
    }

    public ReviewsAdapter(List<Review> reviews, boolean isAdmin, OnReviewDeleteListener listener) {
        this.reviews = reviews;
        this.isAdmin = isAdmin;
        this.deleteListener = listener;
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        outputFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ReviewViewHolder(binding, isAdmin, deleteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final ItemReviewBinding binding;
        private final boolean isAdmin;
        private final OnReviewDeleteListener deleteListener;
        private final SimpleDateFormat inputFormat;
        private final SimpleDateFormat outputFormat;

        public ReviewViewHolder(ItemReviewBinding binding, boolean isAdmin, OnReviewDeleteListener deleteListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.isAdmin = isAdmin;
            this.deleteListener = deleteListener;
            
            // Формат даты из API (ISO 8601)
            inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            // Желаемый формат вывода
            outputFormat = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
            outputFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        }

        public void bind(Review review) {
            if (review.getUser() != null) {
                binding.reviewUsername.setText(review.getUser().getUsername());
                
                // Загрузка аватара пользователя
                if (review.getUser().getAvatarPath() != null && !review.getUser().getAvatarPath().isEmpty()) {
                    String imageUrl = ApiService.BASE_URL + review.getUser().getAvatarPath().replaceFirst("^/", "");
                    Glide.with(binding.userAvatar)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(binding.userAvatar);
                } else {
                    binding.userAvatar.setImageResource(R.drawable.ic_profile);
                }
            } else {
                binding.reviewUsername.setText("Неизвестный пользователь");
                binding.userAvatar.setImageResource(R.drawable.ic_profile);
            }
            
            // Форматирование и отображение даты
            try {
                if (review.getCreatedAt() != null) {
                    String formattedDate = outputFormat.format(inputFormat.parse(review.getCreatedAt()));
                    binding.reviewDate.setText(formattedDate);
                } else {
                    binding.reviewDate.setText("");
                }
            } catch (ParseException e) {
                binding.reviewDate.setText("");
            }
            
            binding.reviewRating.setRating(review.getRating());
            binding.reviewText.setText(review.getText());

            // Show delete button only for admin
            binding.deleteReviewButton.setVisibility(isAdmin ? ViewGroup.VISIBLE : ViewGroup.GONE);
            binding.deleteReviewButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onReviewDelete(review);
                }
            });
        }
    }
} 