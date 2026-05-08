package com.example.coffeebean.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {

    private String uploadPath = "uploads";
    private String publicPrefix = "/uploads";
    private String coffeeCoverDirectory = "coffee-covers";
    private DataSize maxSize = DataSize.ofMegabytes(5);
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    ));

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getPublicPrefix() {
        return publicPrefix;
    }

    public void setPublicPrefix(String publicPrefix) {
        this.publicPrefix = publicPrefix;
    }

    public String getCoffeeCoverDirectory() {
        return coffeeCoverDirectory;
    }

    public void setCoffeeCoverDirectory(String coffeeCoverDirectory) {
        this.coffeeCoverDirectory = coffeeCoverDirectory;
    }

    public DataSize getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(DataSize maxSize) {
        this.maxSize = maxSize;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
