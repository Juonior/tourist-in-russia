package com.example.tourist_in_russia.api.responses;

public class TokenResponse {
    private String access_token;
    private String token_type;

    public String getAccessToken() {
        return access_token;
    }

    public String getTokenType() {
        return token_type;
    }
} 