package com.user_service.service.impl;

import com.user_service.exception.FileStorageException;
import com.user_service.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Value("${storage.local.upload-dir:uploads}")
    private String uploadDir;

    @Value("${storage.local.base-url:http://localhost:8001/files}")
    private String baseUrl;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("Local storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize local storage at: " + rootLocation, e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload"
        );
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = rootLocation.resolve(folder);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = baseUrl + "/" + folder + "/" + storedFilename;
            log.info("File uploaded successfully: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to store file {}: {}", storedFilename, e.getMessage());
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            String relativePath = fileUrl.replace(baseUrl + "/", "");
            Path filePath = rootLocation.resolve(relativePath).normalize();

            if (!filePath.startsWith(rootLocation)) {
                log.warn("Attempted path traversal attack with URL: {}", fileUrl);
                throw new FileStorageException("Invalid file path");
            }

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        } catch (FileStorageException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", fileUrl, e.getMessage());
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot upload an empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds the maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException(
                    "Invalid file type. Only JPEG, PNG and WebP images are allowed"
            );
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "bin";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}