package com.user_service.service.impl;

import com.user_service.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalIStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private MultipartFile multipartFile;

    private LocalIStorageService storageService;

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    private static final byte[] PNG_MAGIC  = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x00};
    private static final byte[] WEBP_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00};

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
        void uploadsJpegFileAndReturnsUrl() throws IOException {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(JPEG_MAGIC));

            String url = storageService.uploadFile(multipartFile, "avatars");

            assertThat(url).startsWith("http://localhost:8001/files/avatars/");
            assertThat(url).endsWith(".jpg");
        }

        @Test
        void uploadsPngFileAndReturnsUrl() throws IOException {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(2048L);
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(multipartFile.getOriginalFilename()).thenReturn("image.png");
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(PNG_MAGIC));

            String url = storageService.uploadFile(multipartFile, "documents");

            assertThat(url).startsWith("http://localhost:8001/files/documents/");
            assertThat(url).endsWith(".png");
        }

        @Test
        void uploadsWebpFileAndReturnsUrl() throws IOException {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(512L);
            when(multipartFile.getContentType()).thenReturn("image/webp");
            when(multipartFile.getOriginalFilename()).thenReturn("image.webp");
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(WEBP_MAGIC));

            String url = storageService.uploadFile(multipartFile, "profile");

            assertThat(url).startsWith("http://localhost:8001/files/profile/");
            assertThat(url).endsWith(".webp");
        }

        @Test
        void createsSubfolderIfNotExists() throws IOException {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(JPEG_MAGIC));

            storageService.uploadFile(multipartFile, "new-folder");

            assertThat(Files.isDirectory(tempDir.resolve("new-folder"))).isTrue();
        }

        @Test
        void throwsWhenFileIsEmpty() {
            when(multipartFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        void throwsWhenFileSizeExceedsLimit() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(6 * 1024 * 1024L);

            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("5MB");
        }

        @Test
        void throwsWhenContentTypeIsNotAllowed() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("application/pdf");

            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        void throwsWhenExtensionIsNotAllowed() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getOriginalFilename()).thenReturn("file.gif");

            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file extension");
        }

        @Test
        void throwsWhenMagicBytesMismatchContentType() throws IOException {
            byte[] fakePngBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(multipartFile.getOriginalFilename()).thenReturn("fake.png");
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fakePngBytes));

            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("spoofing");
        }

        @Test
        void throwsWhenFolderContainsPathTraversal() {
            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, "../evil"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("path traversal");
        }

        @Test
        void throwsWhenFolderContainsInvalidCharacters() {
            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, "bad folder!"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid folder name");
        }

        @Test
        void throwsWhenFolderIsBlank() {
            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, ""))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Folder name must not be empty");
        }

        @Test
        void throwsWhenFileHasNoExtension() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getOriginalFilename()).thenReturn("noextension");

            assertThatThrownBy(() -> storageService.uploadFile(multipartFile, "avatars"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("valid extension");
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void deletesExistingFileSuccessfully() throws IOException {
            Path folder = tempDir.resolve("avatars");
            Files.createDirectories(folder);
            String filename = UUID.randomUUID() + ".jpg";
            Path file = folder.resolve(filename);
            Files.createFile(file);

            String fileUrl = "http://localhost:8001/files/avatars/" + filename;
            storageService.deleteFile(fileUrl);

            assertThat(Files.exists(file)).isFalse();
        }

        @Test
        void doesNothingWhenFileUrlIsNull() {
            storageService.deleteFile(null);
        }

        @Test
        void doesNothingWhenFileUrlIsBlank() {
            storageService.deleteFile("   ");
        }

        @Test
        void throwsWhenFileUrlDoesNotMatchBaseUrl() {
            assertThatThrownBy(() -> storageService.deleteFile("http://evil.com/files/avatars/file.jpg"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file URL");
        }

        @Test
        void throwsWhenUrlStructureIsInvalid() {
            assertThatThrownBy(() -> storageService.deleteFile("http://localhost:8001/files/only-one-segment"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file URL structure");
        }

        @Test
        void throwsWhenFolderInUrlContainsPathTraversal() {
            String filename = UUID.randomUUID() + ".jpg";
            assertThatThrownBy(() -> storageService.deleteFile("http://localhost:8001/files/../evil/" + filename))
                    .isInstanceOf(FileStorageException.class);
        }

        @Test
        void doesNotThrowWhenFileDoesNotExist() {
            String filename = UUID.randomUUID() + ".jpg";
            String fileUrl = "http://localhost:8001/files/avatars/" + filename;

            storageService.deleteFile(fileUrl);
        }
    }

    @Nested
    class ResolveFilePath {

        @Test
        void resolvesValidFolderAndFilename() {
            String filename = UUID.randomUUID() + ".jpg";
            Path resolved = storageService.resolveFilePath("avatars", filename);

            assertThat(resolved.toString()).contains("avatars");
            assertThat(resolved.toString()).contains(filename);
        }

        @Test
        void throwsWhenFolderIsInvalidInResolve() {
            String filename = UUID.randomUUID() + ".jpg";

            assertThatThrownBy(() -> storageService.resolveFilePath("../evil", filename))
                    .isInstanceOf(FileStorageException.class);
        }

        @Test
        void throwsWhenFilenameIsInvalidInResolve() {
            assertThatThrownBy(() -> storageService.resolveFilePath("avatars", "not-a-uuid.jpg"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid stored filename format");
        }

        @Test
        void throwsWhenFilenameHasInvalidExtensionInResolve() {
            String filename = UUID.randomUUID() + ".gif";

            assertThatThrownBy(() -> storageService.resolveFilePath("avatars", filename))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid file extension");
        }

        @Test
        void throwsWhenFilenameHasNoExtension() {
            assertThatThrownBy(() -> storageService.resolveFilePath("avatars", "nodot"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Invalid stored filename");
        }
    }
}