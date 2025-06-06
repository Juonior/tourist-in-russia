package com.example.tourist_in_russia.api.requests;

public class RegisterRequest {
    private String email;
    private String username;
    private String password;
    private boolean is_admin;

    public RegisterRequest(String email, String username, String password, boolean isAdmin) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.is_admin = isAdmin;
    }
} 