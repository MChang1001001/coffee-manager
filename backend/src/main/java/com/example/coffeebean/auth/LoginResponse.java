package com.example.coffeebean.auth;

public record LoginResponse(
        String token,
        UserProfileResponse user
) {
}
