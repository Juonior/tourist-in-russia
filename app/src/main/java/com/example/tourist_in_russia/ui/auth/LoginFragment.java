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
import androidx.navigation.Navigation;

import com.example.tourist_in_russia.R;
import com.example.tourist_in_russia.api.ApiService;
import com.example.tourist_in_russia.api.responses.TokenResponse;
import com.example.tourist_in_russia.api.responses.UserProfile;
import com.example.tourist_in_russia.databinding.FragmentLoginBinding;
import com.example.tourist_in_russia.utils.UserPreferences;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private ApiService apiService;
    private UserPreferences userPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = ApiService.Factory.getInstance();
        userPreferences = new UserPreferences(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.loginButton.setText("Войти");
        binding.registerButton.setText("Регистрация");

        binding.loginButton.setOnClickListener(v -> login());
        binding.registerButton.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment);
        });
        
        // Set hints for input fields if they are set in code
        // Check your dialog_login.xml for actual hints
        // if (binding.usernameInput.getHint() != null && binding.usernameInput.getHint().equals("Username")) {
        //     binding.usernameInput.setHint("Имя пользователя");
        // }
        // if (binding.passwordInput.getHint() != null && binding.passwordInput.getHint().equals("Password")) {
        //     binding.passwordInput.setHint("Пароль");
        // }
    }

    private void login() {
        String username = binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        // First, get the token
        apiService.login(username, password).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();
                    Log.d(TAG, "Got token, now getting user profile");
                    // Save token
                    userPreferences.saveToken(token);
                    
                    // Then get user profile with the token
                    apiService.getProfile("Bearer " + token).enqueue(new Callback<UserProfile>() {
                        @Override
                        public void onResponse(@NonNull Call<UserProfile> call, @NonNull Response<UserProfile> profileResponse) {
                            binding.progressBar.setVisibility(View.GONE);
                            if (profileResponse.isSuccessful() && profileResponse.body() != null) {
                                UserProfile profile = profileResponse.body();
                                Log.d(TAG, "Got user profile, isAdmin: " + profile.isAdmin());
                                // Save admin status
                                userPreferences.setAdmin(profile.isAdmin());
                                
                                if (profile.isAdmin()) {
                                    Log.d(TAG, "User is admin, navigating to places");
                                    Navigation.findNavController(requireView())
                                        .navigate(R.id.action_loginFragment_to_placesFragment);
                                } else {
                                    Log.d(TAG, "User is not admin, navigating to profile");
                                    Navigation.findNavController(requireView())
                                        .navigate(R.id.action_loginFragment_to_profileFragment);
                                }
                            } else {
                                Log.e(TAG, "Failed to get user profile: " + profileResponse.code());
                                Toast.makeText(requireContext(), "Failed to get user profile", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                            binding.progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Error getting user profile", t);
                            Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Login failed: " + response.code());
                    Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Login error", t);
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