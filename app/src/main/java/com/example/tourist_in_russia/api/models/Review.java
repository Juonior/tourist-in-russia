package com.example.tourist_in_russia.api.models;

import com.google.gson.annotations.SerializedName;

public class Review {
    private String id;
    
    @SerializedName("content")
    private String text;
    
    private int rating;
    private String authorName;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("user")
    private ReviewUser user;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public ReviewUser getUser() {
        return user;
    }

    public void setUser(ReviewUser user) {
        this.user = user;
    }

    public static class ReviewUser {
        private int id;
        private String username;
        private String email;
        
        @SerializedName("avatar_path")
        private String avatarPath;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getAvatarPath() {
            return avatarPath;
        }

        public void setAvatarPath(String avatarPath) {
            this.avatarPath = avatarPath;
        }
    }
} 