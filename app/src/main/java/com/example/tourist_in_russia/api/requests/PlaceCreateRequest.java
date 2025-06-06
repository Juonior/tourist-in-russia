package com.example.tourist_in_russia.api.requests;

public class PlaceCreateRequest {
    private String name;
    private String description;
    private Double latitude;
    private Double longitude;
    private String main_photo_path;

    public PlaceCreateRequest(String name, String description, Double latitude, Double longitude) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.main_photo_path = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getMain_photo_path() {
        return main_photo_path;
    }

    public void setMain_photo_path(String main_photo_path) {
        this.main_photo_path = main_photo_path;
    }
} 