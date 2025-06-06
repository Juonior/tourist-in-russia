package com.example.tourist_in_russia.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.tourist_in_russia.R;
import com.example.tourist_in_russia.api.ApiService;
import com.example.tourist_in_russia.api.responses.RegisterResponse;
import com.example.tourist_in_russia.api.requests.RegistrationRequest;
import com.example.tourist_in_russia.api.responses.TokenResponse;
import com.example.tourist_in_russia.databinding.FragmentRegisterBinding;
import com.example.tourist_in_russia.utils.UserPreferences;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";
    private FragmentRegisterBinding binding;
    private ApiService apiService;
    private UserPreferences userPreferences;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiService.Factory.getInstance();
        userPreferences = new UserPreferences(requireContext());
        navController = NavHostFragment.findNavController(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.registerButton.setOnClickListener(v -> register());
        binding.loginButton.setOnClickListener(v -> navController.navigate(R.id.action_registerFragment_to_loginFragment));
    }

    private void register() {
        String username = binding.usernameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordInput.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "Passwords don't match", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.registerButton.setEnabled(false);

        RegistrationRequest request = new RegistrationRequest(username, email, password);
        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful()) {
                    // После успешной регистрации получаем токен
                    getToken(username, password);
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);
                    Toast.makeText(requireContext(), "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.registerButton.setEnabled(true);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getToken(String username, String password) {
        apiService.login(username, password).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.registerButton.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();
                    userPreferences.saveToken(tokenResponse.getAccessToken());
                    navController.navigate(R.id.action_registerFragment_to_placesFragment);
                } else {
                    Toast.makeText(requireContext(), "Failed to get token", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.registerButton.setEnabled(true);
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