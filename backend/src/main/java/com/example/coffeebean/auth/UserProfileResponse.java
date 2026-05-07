package com.example.coffeebean.auth;

public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl
) {
}
