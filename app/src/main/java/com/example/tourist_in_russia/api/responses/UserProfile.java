package com.example.tourist_in_russia.api.responses;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    @SerializedName("id")
    private int id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("is_admin")
    private boolean isAdmin;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("avatar_path")
    private String avatarPath;

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getPhotoPath() {
        return avatarPath;
    }
} 