package com.example.coffeebean.file;

public record CoffeeCoverUploadResponse(
        String url,
        String filename,
        String originalFilename,
        String contentType,
        long size
) {
}
