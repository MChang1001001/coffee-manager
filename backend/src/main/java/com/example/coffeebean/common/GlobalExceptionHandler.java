package com.example.coffeebean.common;

import com.example.coffeebean.config.FileStorageProperties;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final FileStorageProperties fileStorageProperties;

    public GlobalExceptionHandler(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(toHttpStatus(errorCode))
                .body(ApiResponse.fail(errorCode, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? ErrorCode.PARAM_ERROR.getMessage() : fieldError.getDefaultMessage();
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException exception) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR, exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException() {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestPartException() {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR, "文件不能为空"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        String message = "file".equals(exception.getParameterName()) ? "文件不能为空" : ErrorCode.PARAM_ERROR.getMessage();
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException() {
        String message = "文件大小不能超过 " + formatDataSize(fileStorageProperties.getMaxSize());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException() {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_ERROR, "文件上传请求格式错误"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.SYSTEM_ERROR));
    }

    private HttpStatus toHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BAD_REQUEST, PARAM_ERROR -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String formatDataSize(DataSize dataSize) {
        if (dataSize == null) {
            return "5MB";
        }
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
