package com.example.tourist_in_russia.ui.places;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.tourist_in_russia.R;
import com.example.tourist_in_russia.api.ApiService;
import com.example.tourist_in_russia.api.models.Place;
import com.example.tourist_in_russia.api.requests.PlaceCreateRequest;
import com.example.tourist_in_russia.databinding.FragmentPlacesBinding;
import com.example.tourist_in_russia.utils.UserPreferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class PlacesFragment extends Fragment {
    private static final String TAG = "PlacesFragment";
    private FragmentPlacesBinding binding;
    private ApiService apiService;
    private PlacesAdapter placesAdapter;
    private UserPreferences userPreferences;
    private Double selectedLatitude;
    private Double selectedLongitude;
    private androidx.appcompat.app.AlertDialog createPlaceDialog;
    private TextView locationText;
    private String tempPlaceName;
    private String tempPlaceDescription;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiService.Factory.getInstance();
        userPreferences = new UserPreferences(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlacesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "PlacesFragment created");
        Log.d(TAG, "User preferences initialized: " + (userPreferences != null));
        
        setupRecyclerView();
        setupAddPlaceButton();
        setupLocationSelectionListener();
        loadPlaces();
    }

    private void setupRecyclerView() {
        placesAdapter = new PlacesAdapter(place -> {
            Bundle args = new Bundle();
            args.putString("placeId", String.valueOf(place.getId()));
            Navigation.findNavController(requireView())
                .navigate(R.id.action_placesFragment_to_placeDetailsFragment, args);
        });
        binding.placesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.placesRecyclerView.setAdapter(placesAdapter);
    }

    private void setupAddPlaceButton() {
        // Show add button only for admin users
        boolean isAdmin = userPreferences.isAdmin();
        Log.d(TAG, "User admin status: " + isAdmin);
        Log.d(TAG, "User token: " + (userPreferences.getToken() != null ? "exists" : "null"));
        
        if (isAdmin) {
            Log.d(TAG, "Showing add place button for admin user");
            binding.addPlaceButton.setVisibility(View.VISIBLE);
            binding.addPlaceButton.setOnClickListener(v -> showCreatePlaceDialog());
        } else {
            Log.d(TAG, "Hiding add place button for non-admin user");
            binding.addPlaceButton.setVisibility(View.GONE);
        }
    }

    private void setupLocationSelectionListener() {
        getParentFragmentManager().setFragmentResultListener("location_selection", this, (requestKey, result) -> {
            selectedLatitude = result.getDouble("latitude");
            selectedLongitude = result.getDouble("longitude");
            
            // Update the location text in the currently shown dialog, if it exists
            if (createPlaceDialog != null && createPlaceDialog.isShowing() && locationText != null) {
                locationText.setText(String.format("Выбрана точка: %.4f, %.4f", selectedLatitude, selectedLongitude));
            }
            
            // Show the create place dialog again
            showCreatePlaceDialog();
        });
    }

    private void showCreatePlaceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_place, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        View selectLocationButton = dialogView.findViewById(R.id.selectLocationButton);
        locationText = dialogView.findViewById(R.id.locationText);

        // Restore text from temporary storage if returning from map
        if (tempPlaceName != null) {
            nameInput.setText(tempPlaceName);
            tempPlaceName = null; // Clear after restoring
        }
        if (tempPlaceDescription != null) {
            descriptionInput.setText(tempPlaceDescription);
            tempPlaceDescription = null; // Clear after restoring
        }

        // Reset selected location initially, but check if we're returning from map
        if (selectedLatitude == null || selectedLongitude == null) {
            selectedLatitude = null;
            selectedLongitude = null;
            locationText.setText("Точка не выбрана");
        } else {
            // If returning from map, update the text
            locationText.setText(String.format("Выбрана точка: %.4f, %.4f", selectedLatitude, selectedLongitude));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить место")
            .setView(dialogView)
            .setPositiveButton("Создать", null) // Set to null initially
            .setNegativeButton("Отмена", null);

        createPlaceDialog = builder.create();
        createPlaceDialog.show();

        // Set up location selection
        selectLocationButton.setOnClickListener(v -> {
            // Save current text before dismissing dialog
            if (createPlaceDialog != null && createPlaceDialog.isShowing()) {
                TextInputEditText currentNameInput = createPlaceDialog.findViewById(R.id.nameInput);
                TextInputEditText currentDescriptionInput = createPlaceDialog.findViewById(R.id.descriptionInput);
                if (currentNameInput != null) {
                    tempPlaceName = currentNameInput.getText().toString();
                }
                if (currentDescriptionInput != null) {
                    tempPlaceDescription = currentDescriptionInput.getText().toString();
                }
            }

            createPlaceDialog.dismiss();
            // Navigate to map for location selection
            Bundle args = new Bundle();
            args.putBoolean("isLocationSelection", true);
            Navigation.findNavController(requireView())
                .navigate(R.id.action_placesFragment_to_map, args);
        });

        // Override positive button click
        createPlaceDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Log.d(TAG, "Create button clicked");
            String name = nameInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            Log.d(TAG, "Place Name: " + name);
            Log.d(TAG, "Place Description: " + description);
            Log.d(TAG, "Selected Latitude: " + selectedLatitude);
            Log.d(TAG, "Selected Longitude: " + selectedLongitude);

            if (name.isEmpty()) {
                Log.d(TAG, "Name is empty, showing error");
                nameInput.setError("Введите название");
                return;
            }
            Log.d(TAG, "Name validation passed.");

            if (description.isEmpty()) {
                Log.d(TAG, "Description is empty, showing error");
                descriptionInput.setError("Введите описание");
                return;
            }
            Log.d(TAG, "Description validation passed.");

            if (selectedLatitude == null || selectedLongitude == null) {
                Log.d(TAG, "Location not selected, showing toast");
                Toast.makeText(requireContext(), "Выберите местоположение на карте", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Location validation passed.");

            Log.d(TAG, "Calling createPlace method...");
            createPlace(name, description);
            createPlaceDialog.dismiss();
        });
    }

    private void createPlace(String name, String description) {
        String token = userPreferences.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        PlaceCreateRequest request = new PlaceCreateRequest(name, description, selectedLatitude, selectedLongitude);
        apiService.createPlace("Bearer " + token, request).enqueue(new Callback<Place>() {
            @Override
            public void onResponse(@NonNull Call<Place> call, @NonNull Response<Place> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Место успешно создано", Toast.LENGTH_SHORT).show();
                    loadPlaces(); // Reload places list
                } else {
                    Toast.makeText(requireContext(), "Ошибка при создании места", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Place> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaces() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getPlaces(0, 100, true).enqueue(new Callback<List<Place>>() {
            @Override
            public void onResponse(@NonNull Call<List<Place>> call, @NonNull Response<List<Place>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    placesAdapter.setPlaces(response.body());
                } else {
                    Toast.makeText(requireContext(), "Failed to load places", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Place>> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 