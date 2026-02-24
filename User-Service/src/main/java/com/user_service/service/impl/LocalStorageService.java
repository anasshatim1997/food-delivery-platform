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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Pattern SAFE_FOLDER_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

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
        validateFolder(folder);
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload"
        );
        String extension = getAndValidateExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = rootLocation.resolve(folder).normalize();
            if (!targetDir.startsWith(rootLocation)) {
                throw new FileStorageException("Invalid upload folder path");
            }
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedFilename).normalize();
            if (!targetPath.startsWith(targetDir)) {
                throw new FileStorageException("Invalid target file path");
            }
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
            if (!fileUrl.startsWith(baseUrl + "/")) {
                log.warn("File URL does not match base URL: {}", fileUrl);
                throw new FileStorageException("Invalid file URL");
            }

            String relativePath = fileUrl.substring((baseUrl + "/").length());

            String[] parts = relativePath.split("/");
            if (parts.length != 2) {
                throw new FileStorageException("Invalid file URL structure");
            }

            String folder = parts[0];
            String filename = parts[1];

            validateFolder(folder);
            validateStoredFilename(filename);

            Path filePath = rootLocation.resolve(folder).resolve(filename).normalize();
            if (!filePath.startsWith(rootLocation)) {
                log.warn("Attempted path traversal with URL: {}", fileUrl);
                throw new FileStorageException("Invalid file path");
            }
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", fileUrl, e.getMessage());
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    public Path resolveFilePath(String folder, String filename) {
        validateFolder(folder);
        validateStoredFilename(filename);

        Path filePath = rootLocation.resolve(folder).resolve(filename).normalize();
        if (!filePath.startsWith(rootLocation)) {
            throw new FileStorageException("Invalid file path");
        }
        return filePath;
    }

    private void validateFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw new FileStorageException("Folder name must not be empty");
        }
        if (!SAFE_FOLDER_PATTERN.matcher(folder).matches()) {
            throw new FileStorageException("Invalid folder name. Only alphanumeric characters, hyphens and underscores are allowed");
        }
    }

    private void validateStoredFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new FileStorageException("Filename must not be empty");
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 1 || dotIndex == filename.length() - 1) {
            throw new FileStorageException("Invalid stored filename");
        }
        String namePart = filename.substring(0, dotIndex);
        String ext = filename.substring(dotIndex + 1).toLowerCase();
        try {
            UUID.fromString(namePart);
        } catch (IllegalArgumentException e) {
            throw new FileStorageException("Invalid stored filename format");
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new FileStorageException("Invalid file extension in stored filename");
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
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException("Invalid file type. Only JPEG, PNG and WebP images are allowed");
        }
        validateMagicBytes(file, contentType.toLowerCase());
    }

    private void validateMagicBytes(MultipartFile file, String contentType) {
        byte[] expectedMagic = MAGIC_BYTES.get(contentType.equals("image/jpg") ? "image/jpeg" : contentType);
        if (expectedMagic == null) return;

        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(expectedMagic.length);
            for (int i = 0; i < expectedMagic.length; i++) {
                if (i >= header.length || header[i] != expectedMagic[i]) {
                    throw new FileStorageException(
                            "File content does not match the declared type. Possible file spoofing detected."
                    );
                }
            }
        } catch (IOException e) {
            throw new FileStorageException("Could not read file for validation", e);
        }
    }

    private String getAndValidateExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new FileStorageException("File must have a valid extension");
        }
        String ext = filename.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new FileStorageException("Invalid file extension: " + ext);
        }
        return ext;
    }
}