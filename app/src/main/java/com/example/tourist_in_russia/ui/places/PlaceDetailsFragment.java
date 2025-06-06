package com.example.tourist_in_russia.ui.places;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.example.tourist_in_russia.R;
import com.example.tourist_in_russia.api.ApiService;
import com.example.tourist_in_russia.api.models.Place;
import com.example.tourist_in_russia.api.models.Review;
import com.example.tourist_in_russia.api.requests.ReviewCreateRequest;
import com.example.tourist_in_russia.databinding.FragmentPlaceDetailsBinding;
import com.example.tourist_in_russia.utils.UserPreferences;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PlaceDetailsFragment extends Fragment {
    private static final String TAG = "PlaceDetailsFragment";
    private FragmentPlaceDetailsBinding binding;
    private ApiService apiService;
    private ReviewsAdapter reviewsAdapter;
    private String currentPlaceId;
    private UserPreferences userPreferences;
    private boolean isAdmin = false;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiService.Factory.getInstance();
        userPreferences = new UserPreferences(requireContext());
        
        // Check if user is admin
        isAdmin = userPreferences.isAdmin();

        if (getArguments() != null) {
            currentPlaceId = getArguments().getString("placeId");
        }
        
        // Initialize ActivityResultLauncher for picking images
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadMainPhoto(uri);
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaceDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated: Starting fragment initialization");
        
        // Инициализация адаптера отзывов
        reviewsAdapter = new ReviewsAdapter(new ArrayList<>(), isAdmin, this::deleteReview);
        binding.reviewsRecyclerView.setAdapter(reviewsAdapter);
        binding.reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Show delete button only for admin
        binding.deletePlaceButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        binding.deletePlaceButton.setOnClickListener(v -> showDeletePlaceDialog());
        
        setupReviewSubmission();
        updateReviewInputVisibility();
        
        if (currentPlaceId != null) {
            Log.d(TAG, "onViewCreated: Loading details for place ID: " + currentPlaceId);
            loadPlaceDetails(currentPlaceId);
        } else {
            Log.e(TAG, "onViewCreated: Place ID not found in arguments");
            Toast.makeText(requireContext(), "Error: Place ID not found", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigateUp();
        }

        // Make photo clickable for admins
        if (isAdmin) {
            binding.placeImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }
    }

    private void updateReviewInputVisibility() {
        String token = userPreferences.getToken();
        boolean isLoggedIn = token != null;
        Log.d(TAG, "updateReviewInputVisibility: User is logged in: " + isLoggedIn);
        if (token != null) {
            Log.d(TAG, "updateReviewInputVisibility: Token exists, length: " + token.length());
        }
        
        // Show/hide the review input section
        View reviewSection = binding.getRoot().findViewById(R.id.reviewSection);
        if (reviewSection != null) {
            Log.d(TAG, "updateReviewInputVisibility: Found reviewSection view");
            reviewSection.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        } else {
            Log.e(TAG, "updateReviewInputVisibility: reviewSection view not found!");
        }
        
        binding.reviewRating.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        binding.reviewText.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        binding.submitReviewButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        
        // Show login prompt if not logged in
        if (!isLoggedIn) {
            Log.d(TAG, "updateReviewInputVisibility: User is not logged in, showing login prompt");
            binding.reviewText.setHint("Войдите, чтобы оставить отзыв");
            binding.reviewText.setEnabled(false);
        } else {
            Log.d(TAG, "updateReviewInputVisibility: User is logged in, enabling review input");
            binding.reviewText.setHint("Напишите ваш отзыв");
            binding.reviewText.setEnabled(true);
        }
    }

    private void setupReviewSubmission() {
        binding.submitReviewButton.setOnClickListener(v -> {
            String reviewText = binding.reviewText.getText().toString().trim();
            float rating = binding.reviewRating.getRating();

            if (reviewText.isEmpty()) {
                Toast.makeText(requireContext(), "Пожалуйста, напишите отзыв", Toast.LENGTH_SHORT).show();
                return;
            }

            if (rating == 0) {
                Toast.makeText(requireContext(), "Пожалуйста, поставьте оценку", Toast.LENGTH_SHORT).show();
                return;
            }

            submitReview(reviewText, (int) rating);
        });
    }

    private void submitReview(String text, int rating) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.submitReviewButton.setEnabled(false);

        // Get the token from SharedPreferences
        String token = userPreferences.getToken();

        if (token == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.submitReviewButton.setEnabled(true);
            Toast.makeText(requireContext(), "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }

        ReviewCreateRequest request = new ReviewCreateRequest(text, rating);
        apiService.createReview(currentPlaceId, "Bearer " + token, request).enqueue(new Callback<Review>() {
            @Override
            public void onResponse(@NonNull Call<Review> call, @NonNull Response<Review> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.submitReviewButton.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Отзыв успешно добавлен", Toast.LENGTH_SHORT).show();
                    // Clear input fields
                    binding.reviewText.setText("");
                    binding.reviewRating.setRating(0);
                    // Reload reviews
                    loadReviews(currentPlaceId);
                    // Reload place details to update average rating
                    loadPlaceDetails(currentPlaceId);
                } else if (response.code() == 400 && response.errorBody() != null) {
                    try {
                        String errorMessage = response.errorBody().string();
                        if (errorMessage.contains("already left a review")) {
                            Toast.makeText(requireContext(), "Вы уже оставили отзыв для этого места", Toast.LENGTH_LONG).show();
                            // Disable review input since user has already left a review
                            binding.reviewText.setEnabled(false);
                            binding.reviewRating.setEnabled(false);
                            binding.submitReviewButton.setEnabled(false);
                        } else {
                            Toast.makeText(requireContext(), "Ошибка при добавлении отзыва", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Ошибка при добавлении отзыва", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Ошибка при добавлении отзыва", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Review> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.submitReviewButton.setEnabled(true);
                Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaceDetails(String placeId) {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getPlaceById(placeId).enqueue(new Callback<Place>() {
            @Override
            public void onResponse(@NonNull Call<Place> call, @NonNull Response<Place> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    displayPlaceDetails(response.body());
                } else {
                    Toast.makeText(requireContext(), "Failed to load place details", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(PlaceDetailsFragment.this).navigateUp();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Place> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(PlaceDetailsFragment.this).navigateUp();
            }
        });
    }

    private void displayPlaceDetails(Place place) {
        // Загрузка изображения
        if (place.getMainPhotoPath() != null && !place.getMainPhotoPath().isEmpty()) {
            String imageUrl = ApiService.BASE_URL + place.getMainPhotoPath().replaceFirst("^/", "");
            Glide.with(this)
                .load(imageUrl)
                .into(binding.placeImage);
        }

        // Отображение названия и описания
        binding.placeTitle.setText(place.getName());
        binding.placeDescription.setText(place.getDescription());

        // Отображение рейтинга
        if (place.getAverageRating() != null && place.getAverageRating() > 0) {
            binding.placeRating.setRating(place.getAverageRating().floatValue());
            binding.placeRating.setVisibility(View.VISIBLE);
            binding.noRatingText.setVisibility(View.GONE);
        } else {
            binding.placeRating.setVisibility(View.GONE);
            binding.noRatingText.setVisibility(View.VISIBLE);
        }

        // Загрузка и отображение отзывов
        loadReviews(String.valueOf(place.getId()));
    }

    private void loadReviews(String placeId) {
        apiService.getPlaceReviews(placeId).enqueue(new Callback<List<Review>>() {
            @Override
            public void onResponse(@NonNull Call<List<Review>> call, @NonNull Response<List<Review>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reviewsAdapter.setReviews(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Review>> call, @NonNull Throwable t) {
                // Ошибка загрузки отзывов не критична, просто показываем пустой список
                reviewsAdapter.setReviews(new ArrayList<>());
            }
        });
    }

    private void uploadMainPhoto(android.net.Uri fileUri) {
        if (currentPlaceId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID места отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String token = userPreferences.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Toast.makeText(requireContext(), "Не удалось прочитать файл", Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.GONE);
                return;
            }

            File file = createTempFile(inputStream);
            RequestBody requestFile = RequestBody.create(file, MediaType.parse(requireContext().getContentResolver().getType(fileUri)));
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            apiService.uploadPlaceMainPhoto(currentPlaceId, "Bearer " + token, body).enqueue(new Callback<Place>() {
                @Override
                public void onResponse(@NonNull Call<Place> call, @NonNull Response<Place> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(requireContext(), "Фотография обновлена", Toast.LENGTH_SHORT).show();
                        loadPlaceDetails(currentPlaceId); // Reload to show new photo
                    } else {
                        String errorMessage = "Ошибка при загрузке фотографии";
                         if (response.errorBody() != null) {
                             try {
                                 errorMessage += ": " + response.errorBody().string();
                             } catch (IOException e) {
                                  Log.e(TAG, "Error reading error body", e);
                             }
                         }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Upload photo failed: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Place> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка сети при загрузке фотографии", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Upload photo network error", t);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file", e);
            Toast.makeText(requireContext(), "Ошибка при обработке файла", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
        }
    }
    
    private File createTempFile(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("upload_image", ".tmp", requireContext().getCacheDir());
        tempFile.deleteOnExit(); // Clean up the file when the app exits
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    private void deleteReview(Review review) {
        String token = userPreferences.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление отзыва")
            .setMessage("Вы уверены, что хотите удалить этот отзыв?")
            .setPositiveButton("Удалить", (dialog, which) -> {
                binding.progressBar.setVisibility(View.VISIBLE);
                apiService.deleteReview(String.valueOf(review.getId()), "Bearer " + token)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            binding.progressBar.setVisibility(View.GONE);
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Отзыв успешно удален", Toast.LENGTH_SHORT).show();
                                // Reload reviews and place details
                                loadReviews(currentPlaceId);
                                loadPlaceDetails(currentPlaceId);
                            } else {
                                Toast.makeText(requireContext(), "Ошибка при удалении отзыва", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showDeletePlaceDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление места")
            .setMessage("Вы уверены, что хотите удалить это место? Это действие нельзя отменить.")
            .setPositiveButton("Удалить", (dialog, which) -> deletePlace())
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void deletePlace() {
        String token = userPreferences.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.deletePlace(currentPlaceId, "Bearer " + token)
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Место успешно удалено", Toast.LENGTH_SHORT).show();
                        // Navigate back to places list
                        NavHostFragment.findNavController(PlaceDetailsFragment.this)
                            .navigate(R.id.action_placeDetailsFragment_to_placesFragment);
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при удалении места", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 