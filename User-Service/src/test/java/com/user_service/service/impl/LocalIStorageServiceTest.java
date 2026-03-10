package com.user_service.service.impl;

import com.user_service.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LocalIStorageServiceTest {

    private LocalIStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalIStorageService();
        ReflectionTestUtils.setField(storageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(storageService, "baseUrl", "http://localhost:8001/files");
        storageService.init();
    }

    @Nested
    class UploadFile {

        @Test
        void uploadsJpegFileSuccessfully() throws IOException {
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);

            String url = storageService.uploadFile(file, "avatars");

            assertThat(url).startsWith("http://localhost:8001/files/avatars/");
            assertThat(url).endsWith(".jpg");
        }

        @Test
        void uploadsPngFileSuccessfully() throws IOException {
            byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x01};
            MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", pngBytes);

            String url = storageService.uploadFile(file, "avatars");

            assertThat(url).startsWith("http://localhost:8001/files/avatars/");
            assertThat(url).endsWith(".png");
        }

        @Test
        void uploadsWebpFileSuccessfully() throws IOException {
            byte[] webpBytes = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x01, 0x02, 0x03, 0x57, 0x45, 0x42, 0x50};
            MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", webpBytes);

            String url = storageService.uploadFile(file, "avatars");

            assertThat(url).startsWith("http://localhost:8001/files/avatars/");
            assertThat(url).endsWith(".webp");
        }

        @Test
        void throwsWhenFileIsEmpty() {
            MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> storageService.uploadFile(file, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Cannot upload an empty file");
        }

        @Test
        void throwsWhenFileIsNull() {
            assertThatThrownBy(() -> storageService.uploadFile(null, "avatars"))
                    .isInstanceOf(FileStorageException.class);
        }

        @Test
        void throwsWhenFileSizeExceedsLimit() {
            byte[] largeContent = new byte[6 * 1024 * 1024];
            largeContent[0] = (byte) 0xFF;
            largeContent[1] = (byte) 0xD8;
            largeContent[2] = (byte) 0xFF;
            MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", largeContent);

            assertThatThrownBy(() -> storageService.uploadFile(file, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("5MB");
        }

        @Test
        void throwsWhenContentTypeIsInvalid() {
            byte[] bytes = new byte[]{0x25, 0x50, 0x44, 0x46};
            MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", bytes);

            assertThatThrownBy(() -> storageService.uploadFile(file, "docs"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        void throwsWhenContentTypeIsNull() {
            byte[] bytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", null, bytes);

            assertThatThrownBy(() -> storageService.uploadFile(file, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        void throwsWhenExtensionIsInvalid() {
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            MockMultipartFile file = new MockMultipartFile("file", "photo.gif", "image/jpeg", jpegBytes);

            assertThatThrownBy(() -> storageService.uploadFile(file, "avatars"))
                    .isInstanceOf(FileStorageException.class);
        }

        @Test
        void throwsWhenMagicBytesMismatchContentType() {
            byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
            MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", pngBytes);

            assertThatThrownBy(() -> storageService.uploadFile(file, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("File content does not match");
        }

        @Test
        void throwsWhenFolderIsEmpty() {
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);

            assertThatThrownBy(() -> storageService.uploadFile(file, ""))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Folder name must not be empty");
        }

        @Test
        void throwsWhenFolderContainsPathTraversal() {
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);

            assertThatThrownBy(() -> storageService.uploadFile(file, "../etc"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("path traversal");
        }

        @Test
        void throwsWhenFolderContainsInvalidCharacters() {
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);

            assertThatThrownBy(() -> storageService.uploadFile(file, "folder name!"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid folder name");
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void deletesExistingFileSuccessfully() throws IOException {
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);
            String url = storageService.uploadFile(file, "avatars");

            storageService.deleteFile(url);

            String filename = url.substring(url.lastIndexOf('/') + 1);
            Path filePath = tempDir.resolve("avatars").resolve(filename);
            assertThat(filePath).doesNotExist();
        }

        @Test
        void doesNothingWhenUrlIsNull() {
            storageService.deleteFile(null);
        }

        @Test
        void doesNothingWhenUrlIsBlank() {
            storageService.deleteFile("   ");
        }

        @Test
        void throwsWhenUrlDoesNotMatchBaseUrl() {
            assertThatThrownBy(() -> storageService.deleteFile("http://malicious.com/files/avatars/some.jpg"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file URL");
        }

        @Test
        void throwsWhenUrlStructureIsInvalid() {
            assertThatThrownBy(() -> storageService.deleteFile("http://localhost:8001/files/avatars/sub/extra/file.jpg"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file URL structure");
        }

        @Test
        void throwsWhenFolderInUrlIsInvalid() {
            assertThatThrownBy(() -> storageService.deleteFile("http://localhost:8001/files/../etc/" + java.util.UUID.randomUUID() + ".jpg"))
                    .isInstanceOf(FileStorageException.class);
        }
    }

    @Nested
    class ResolveFilePath {

        @Test
        void resolvesValidPath() throws IOException {
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);
            String url = storageService.uploadFile(file, "avatars");
            String filename = url.substring(url.lastIndexOf('/') + 1);

            Path resolved = storageService.resolveFilePath("avatars", filename);

            assertThat(resolved).isNotNull();
            assertThat(resolved.toString()).contains("avatars");
            assertThat(resolved.toString()).contains(filename);
        }

        @Test
        void throwsWhenFolderIsNull() {
            assertThatThrownBy(() -> storageService.resolveFilePath(null, java.util.UUID.randomUUID() + ".jpg"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Folder name must not be empty");
        }

        @Test
        void throwsWhenFilenameContainsPathTraversal() {
            assertThatThrownBy(() -> storageService.resolveFilePath("avatars", "../secret.jpg"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("path traversal");
        }

        @Test
        void throwsWhenFilenameHasInvalidUuidFormat() {
            assertThatThrownBy(() -> storageService.resolveFilePath("avatars", "notauuid.jpg"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid stored filename format");
        }

        @Test
        void throwsWhenFilenameHasInvalidExtension() {
            assertThatThrownBy(() -> storageService.resolveFilePath("avatars", java.util.UUID.randomUUID() + ".exe"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file extension");
        }

        @Test
        void throwsWhenFilenameIsBlank() {
            assertThatThrownBy(() -> storageService.resolveFilePath("avatars", ""))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Filename must not be empty");
        }

        @Test
        void throwsWhenFilenameHasNoExtension() {
            assertThatThrownBy(() -> storageService.resolveFilePath("avatars", java.util.UUID.randomUUID().toString()))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid stored filename");
        }
    }
}