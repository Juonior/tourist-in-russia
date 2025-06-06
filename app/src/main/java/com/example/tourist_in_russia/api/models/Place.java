package com.example.tourist_in_russia.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Place {
    private int id;
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    
    @SerializedName("main_photo_path")
    private String mainPhotoPath;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("average_rating")
    private Double averageRating;
    
    @SerializedName("photos")
    private List<String> photos;
    
    private List<Review> reviews;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getMainPhotoPath() {
        return mainPhotoPath;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
} 