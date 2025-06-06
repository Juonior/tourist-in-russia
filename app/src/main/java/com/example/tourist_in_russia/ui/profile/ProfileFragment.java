package com.example.tourist_in_russia.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.tourist_in_russia.R;
import com.example.tourist_in_russia.api.ApiService;
import com.example.tourist_in_russia.api.requests.PasswordChangeRequest;
import com.example.tourist_in_russia.api.responses.UserProfile;
import com.example.tourist_in_russia.utils.UserPreferences;
import com.example.tourist_in_russia.databinding.FragmentProfileBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private ApiService apiService;
    private UserPreferences userPreferences;
    private NavController navController;
    private String authToken;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    uploadPhoto(imageUri);
                }
            }
        }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiService.Factory.getInstance();
        userPreferences = new UserPreferences(requireContext());
        navController = NavHostFragment.findNavController(this);
        authToken = "Bearer " + userPreferences.getToken();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupClickListeners();
        
        // Check if user is authenticated
        if (!userPreferences.isLoggedIn()) {
            Log.d(TAG, "onViewCreated: User is not logged in, navigating to login");
            navController.navigate(R.id.action_profileFragment_to_loginFragment);
            return;
        }
        
        loadProfile();
    }

    private void setupClickListeners() {
        binding.changeAvatarButton.setOnClickListener(v -> openImagePicker());
        binding.changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        binding.logoutButton.setOnClickListener(v -> logout());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImage.launch(intent);
    }

    private void uploadPhoto(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            File tempFile = File.createTempFile("photo", ".jpg", requireContext().getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), tempFile);
            MultipartBody.Part photo = MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);

            binding.progressBar.setVisibility(View.VISIBLE);
            apiService.uploadPhoto(authToken, photo).enqueue(new Callback<UserProfile>() {
                @Override
                public void onResponse(@NonNull Call<UserProfile> call, @NonNull Response<UserProfile> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        displayProfile(response.body());
                        Toast.makeText(requireContext(), "Photo updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to update photo", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.currentPasswordInput);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPasswordInput);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Смена пароля")
            .setView(dialogView)
            .setPositiveButton("Изменить", (dialog, which) -> {
                String currentPassword = currentPasswordInput.getText().toString();
                String newPassword = newPasswordInput.getText().toString();
                String confirmPassword = confirmPasswordInput.getText().toString();

                if (newPassword.equals(confirmPassword)) {
                    changePassword(currentPassword, newPassword);
                } else {
                    Toast.makeText(requireContext(), "Passwords don't match", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.changePassword(authToken, new PasswordChangeRequest(currentPassword, newPassword))
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to change password", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void logout() {
        Log.d(TAG, "logout: Clearing user preferences and navigating to login");
        userPreferences.clear();
        navController.navigate(R.id.action_profileFragment_to_loginFragment);
    }

    private void loadProfile() {
        if (!userPreferences.isLoggedIn()) {
            Log.d(TAG, "loadProfile: User is not logged in, skipping profile load");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getProfile(authToken).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(@NonNull Call<UserProfile> call, @NonNull Response<UserProfile> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    displayProfile(response.body());
                } else if (response.code() == 401) {
                    // Token is invalid or expired
                    Log.d(TAG, "loadProfile: Unauthorized response, clearing preferences and navigating to login");
                    userPreferences.clear();
                    navController.navigate(R.id.action_profileFragment_to_loginFragment);
                } else {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayProfile(UserProfile profile) {
        binding.usernameText.setText(profile.getUsername());
        binding.emailText.setText(profile.getEmail());

        // Показываем или скрываем чип администратора
        binding.adminChip.setVisibility(profile.isAdmin() ? View.VISIBLE : View.GONE);

        String photoPath = profile.getPhotoPath();
        Log.d(TAG, "Profile photo path: " + photoPath);

        if (photoPath != null && !photoPath.isEmpty()) {
            String imageUrl = ApiService.BASE_URL + photoPath.replaceFirst("^/", "");
            Log.d(TAG, "Loading profile photo from URL: " + imageUrl);
            
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.error_image)
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Failed to load image: " + e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "Image loaded successfully");
                        return false;
                    }
                })
                .into(binding.avatarImage);
        } else {
            Log.d(TAG, "No photo path available, showing placeholder");
            binding.avatarImage.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 