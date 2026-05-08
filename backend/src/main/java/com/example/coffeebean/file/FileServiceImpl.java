package com.example.coffeebean.file;

import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.config.FileStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final FileStorageProperties fileStorageProperties;

    public FileServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public CoffeeCoverUploadResponse uploadCoffeeCover(MultipartFile file) {
        validateFilePresent(file);
        validateFileSize(file);

        String originalFilename = cleanOriginalFilename(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());
        String extension = resolveExtension(contentType);
        validateFileSignature(file, contentType);
        String filename = UUID.randomUUID() + extension;
        Path targetDirectory = resolveCoffeeCoverDirectory();
        Path targetPath = targetDirectory.resolve(filename).normalize();
        if (!targetPath.startsWith(targetDirectory)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件存储路径异常");
        }

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }

        return new CoffeeCoverUploadResponse(
                buildPublicUrl(filename),
                filename,
                originalFilename,
                contentType,
                file.getSize()
        );
    }

    private void validateFilePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
    }

    private void validateFileSize(MultipartFile file) {
        DataSize maxSize = fileStorageProperties.getMaxSize();
        if (maxSize != null && file.getSize() > maxSize.toBytes()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小不能超过 " + formatDataSize(maxSize));
        }
    }

    private String cleanOriginalFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        String normalized = originalFilename.trim().replace("\\", "/");
        String filename = StringUtils.getFilename(normalized);
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        return filename.trim();
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只允许上传 jpg、png、webp 图片");
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveExtension(String contentType) {
        Set<String> allowedContentTypes = fileStorageProperties.getAllowedContentTypes() == null
                ? CONTENT_TYPE_EXTENSIONS.keySet()
                : fileStorageProperties.getAllowedContentTypes()
                .stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowedContentTypes.contains(contentType) || !CONTENT_TYPE_EXTENSIONS.containsKey(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只允许上传 jpg、png、webp 图片");
        }
        return CONTENT_TYPE_EXTENSIONS.get(contentType);
    }

    private void validateFileSignature(MultipartFile file, String contentType) {
        byte[] header;
        try (InputStream inputStream = file.getInputStream()) {
            header = inputStream.readNBytes(12);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取失败");
        }
        if (!matchesImageSignature(header, contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只允许上传 jpg、png、webp 图片");
        }
    }

    private boolean matchesImageSignature(byte[] header, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> hasAscii(header, 0, "RIFF") && hasAscii(header, 8, "WEBP");
            default -> false;
        };
    }

    private boolean startsWith(byte[] header, int... expectedBytes) {
        if (header.length < expectedBytes.length) {
            return false;
        }
        for (int i = 0; i < expectedBytes.length; i++) {
            if ((header[i] & 0xFF) != expectedBytes[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAscii(byte[] header, int offset, String expected) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        if (header.length < offset + expectedBytes.length) {
            return false;
        }
        for (int i = 0; i < expectedBytes.length; i++) {
            if (header[offset + i] != expectedBytes[i]) {
                return false;
            }
        }
        return true;
    }

    private Path resolveCoffeeCoverDirectory() {
        Path uploadRoot = Path.of(fileStorageProperties.getUploadPath()).toAbsolutePath().normalize();
        Path targetDirectory = uploadRoot.resolve(normalizePathSegment(fileStorageProperties.getCoffeeCoverDirectory()))
                .normalize();
        if (!targetDirectory.startsWith(uploadRoot)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件存储路径配置错误");
        }
        return targetDirectory;
    }

    private String buildPublicUrl(String filename) {
        return normalizePublicPrefix(fileStorageProperties.getPublicPrefix())
                + "/"
                + normalizePathSegment(fileStorageProperties.getCoffeeCoverDirectory())
                + "/"
                + filename;
    }

    private String normalizePublicPrefix(String publicPrefix) {
        if (!StringUtils.hasText(publicPrefix)) {
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

    private String normalizePathSegment(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件存储路径配置错误");
        }
        String normalized = value.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!StringUtils.hasText(normalized) || normalized.contains("..")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件存储路径配置错误");
        }
        return normalized;
    }

    private String formatDataSize(DataSize dataSize) {
        long bytes = dataSize.toBytes();
        long mb = 1024L * 1024L;
        if (bytes % mb == 0) {
            return bytes / mb + "MB";
        }
        if (bytes % 1024L == 0) {
            return bytes / 1024L + "KB";
        }
        return bytes + "B";
    }
}
