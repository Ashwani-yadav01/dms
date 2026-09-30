package com.dms.incident.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryImageService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final Cloudinary cloudinary;

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
        System.out.println("========== CLOUDINARY UPLOAD ==========");
        System.out.println("Filename      : " + file.getOriginalFilename());
        System.out.println("Size          : " + file.getSize() + " bytes");
        System.out.println("Content Type  : " + contentType);
        System.out.println("Folder        : " + folder);
        System.out.println("Cloudinary    : " + cloudinary);
        System.out.println("=======================================");

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

            System.out.println("Cloudinary upload successful");

            Object secureUrl = result.get("secure_url");

            if (secureUrl == null || secureUrl.toString().isBlank()) {

                System.err.println(
                        "Cloudinary response did not contain secure_url"
                );

                throw new IllegalStateException(
                        "Cloudinary did not return a secure image URL"
                );
            }

            System.out.println("Secure URL: " + secureUrl);

            return secureUrl.toString();

        } catch (IOException e) {

            System.err.println("Cloudinary IOException:");
            e.printStackTrace();

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to send image to Cloudinary: " + e.getMessage(),
                    e
            );

        } catch (RuntimeException e) {

            System.err.println("Cloudinary Runtime/API error:");
            e.printStackTrace();

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary upload failed: " + e.getMessage(),
                    e
            );
        }
    }
}