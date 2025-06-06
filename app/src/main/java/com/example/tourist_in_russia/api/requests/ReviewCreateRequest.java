package com.example.tourist_in_russia.api.requests;

import com.google.gson.annotations.SerializedName;

public class ReviewCreateRequest {
    @SerializedName("content")
    private String text;
    
    private int rating;

    public ReviewCreateRequest(String text, int rating) {
        this.text = text;
        this.rating = rating;
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
} 