package com.example.tourist_in_russia.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class UserPreferences {
    private static final String TAG = "UserPreferences";
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_IS_ADMIN = "is_admin";

    private final SharedPreferences prefs;

    public UserPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "UserPreferences initialized");
    }

    public void saveToken(String token) {
        Log.d(TAG, "Saving token: " + (token != null ? "exists" : "null"));
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        String token = prefs.getString(KEY_TOKEN, null);
        Log.d(TAG, "Getting token: " + (token != null ? "exists" : "null"));
        return token;
    }

    public void saveUserId(String userId) {
        Log.d(TAG, "Saving user ID: " + userId);
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        String userId = prefs.getString(KEY_USER_ID, null);
        Log.d(TAG, "Getting user ID: " + userId);
        return userId;
    }

    public void setAdmin(boolean isAdmin) {
        Log.d(TAG, "Setting admin status: " + isAdmin);
        prefs.edit().putBoolean(KEY_IS_ADMIN, isAdmin).apply();
    }

    public boolean isAdmin() {
        boolean isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false);
        Log.d(TAG, "Getting admin status: " + isAdmin);
        return isAdmin;
    }

    public boolean isLoggedIn() {
        boolean isLoggedIn = getToken() != null;
        Log.d(TAG, "Checking login status: " + isLoggedIn);
        return isLoggedIn;
    }

    public void clear() {
        Log.d(TAG, "Clearing all preferences");
        prefs.edit().clear().apply();
    }
} 