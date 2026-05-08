package com.example.coffeebean.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    CoffeeCoverUploadResponse uploadCoffeeCover(MultipartFile file);
}
