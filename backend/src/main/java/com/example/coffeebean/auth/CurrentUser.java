package com.example.coffeebean.auth;

public record CurrentUser(
        Long id,
        String username,
        String nickname,
        String avatarUrl
) {
}
