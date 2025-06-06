package com.example.tourist_in_russia.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    
    private final SharedPreferences preferences;
    
    public AuthManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveToken(String token) {
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }
    
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }
    
    public void clearToken() {
        preferences.edit().remove(KEY_TOKEN).apply();
    }
    
    public boolean isAuthenticated() {
        return getToken() != null;
    }
} 