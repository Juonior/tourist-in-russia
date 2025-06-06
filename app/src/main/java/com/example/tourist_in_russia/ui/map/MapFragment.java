package com.example.tourist_in_russia.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import com.example.tourist_in_russia.R;
import com.example.tourist_in_russia.api.ApiService;
import com.example.tourist_in_russia.api.models.Place;
import com.example.tourist_in_russia.databinding.FragmentMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private FragmentMapBinding binding;
    private ApiService apiService;
    private GoogleMap mMap;
    private Marker marker;
    private Double selectedLatitude;
    private Double selectedLongitude;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiService.Factory.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the map
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction()
            .replace(R.id.mapView, mapFragment)
            .commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
        // Check if we're in location selection mode
        boolean isLocationSelection = getArguments() != null && getArguments().getBoolean("isLocationSelection", false);
        if (isLocationSelection) {
            setupLocationSelection();
        } else {
            setupNormalMode();
        }
    }

    private void setupLocationSelection() {
        // Hide normal UI elements
        
        // Set up map click listener
        mMap.setOnMapClickListener(latLng -> {
            // Remove previous marker if exists
            if (marker != null) {
                marker.remove();
            }
            
            // Add new marker
            marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Выбранное местоположение"));
            
            // Save coordinates
            selectedLatitude = latLng.latitude;
            selectedLongitude = latLng.longitude;
            
            // Show confirm button
            binding.confirmLocationButton.setVisibility(View.VISIBLE);
        });

        // Set up confirm button click listener
        binding.confirmLocationButton.setOnClickListener(v -> {
            if (selectedLatitude != null && selectedLongitude != null) {
                // Pass coordinates back to PlacesFragment
                Bundle result = new Bundle();
                result.putDouble("latitude", selectedLatitude);
                result.putDouble("longitude", selectedLongitude);
                getParentFragmentManager().setFragmentResult("location_selection", result);
                
                // Navigate back
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Toast.makeText(requireContext(), "Выберите местоположение на карте", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNormalMode() {
        // Show normal UI elements
        binding.confirmLocationButton.setVisibility(View.GONE);
        loadPlaces();
    } 

    private void loadPlaces() {
        apiService.getPlaces(0, 100, true).enqueue(new Callback<List<Place>>() {
            @Override
            public void onResponse(@NonNull Call<List<Place>> call, @NonNull Response<List<Place>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Place place : response.body()) {
                        LatLng location = new LatLng(place.getLatitude(), place.getLongitude());
                        mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(place.getName())
                                .snippet(place.getDescription()));
                    }
                    // Center map on Moscow
                    LatLng moscow = new LatLng(55.7558, 37.6173);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(moscow, 10));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Place>> call, @NonNull Throwable t) {
                // Handle error
            }
        });
    }

    private void showPlaceDetails(Place place) {
        Bundle args = new Bundle();
        args.putString("placeId", String.valueOf(place.getId()));
        Navigation.findNavController(requireView())
            .navigate(R.id.action_mapFragment_to_placeDetailsFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 