package com.example.coffeebean.file;

import com.example.coffeebean.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/coffee-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CoffeeCoverUploadResponse> uploadCoffeeCover(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileService.uploadCoffeeCover(file));
    }
}
