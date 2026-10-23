package com.dms.incident.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final Cloudinary cloudinary;

    @CircuitBreaker(name = "cloudinaryClient", fallbackMethod = "uploadFallback")
    public String upload(MultipartFile file, String folder) {

        // -----------------------------
        // Validate file
        // -----------------------------
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose a photo to upload"
            );
        }

        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Photo must be 10 MB or smaller"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !contentType.toLowerCase().startsWith("image/")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only image files can be uploaded"
            );
        }

        // -----------------------------
        // Debug information
        // -----------------------------
        log.info("Cloudinary upload request: filename={}, size={} bytes, contentType={}, folder={}",
                file.getOriginalFilename(), file.getSize(), contentType, folder);

        // -----------------------------
        // Upload to Cloudinary
        // -----------------------------
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"
                    )
            );

            Object secureUrl = result.get("secure_url");

            if (secureUrl == null || secureUrl.toString().isBlank()) {
                log.error("Cloudinary response did not contain secure_url");
                throw new IllegalStateException(
                        "Cloudinary did not return a secure image URL"
                );
            }

            return secureUrl.toString();

        } catch (IOException e) {
            log.error("Cloudinary IOException: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to send image to Cloudinary: " + e.getMessage(),
                    e
            );

        } catch (RuntimeException e) {
            log.error("Cloudinary Runtime/API error: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary upload failed: " + e.getMessage(),
                    e
            );
        }
    }

    public String uploadFallback(MultipartFile file, String folder, Throwable ex) {
        if (ex instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            throw rse;
        }
        log.error("Circuit breaker [cloudinaryClient] fallback activated. Reason: {}", ex.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Image storage service is currently unavailable. Please try again shortly.",
                ex
        );
    }
}
