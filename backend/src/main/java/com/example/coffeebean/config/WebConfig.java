package com.example.coffeebean.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    public WebConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Path.of(fileStorageProperties.getUploadPath()).toAbsolutePath().normalize();
        String publicPrefix = normalizePublicPrefix(fileStorageProperties.getPublicPrefix());
        String resourceLocation = uploadPath.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }
        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations(resourceLocation);
    }

    private String normalizePublicPrefix(String publicPrefix) {
        if (publicPrefix == null || publicPrefix.isBlank()) {
            return "/uploads";
        }
        String normalized = publicPrefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
