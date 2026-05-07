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
        String publicPrefix = fileStorageProperties.getPublicPrefix();
        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}
