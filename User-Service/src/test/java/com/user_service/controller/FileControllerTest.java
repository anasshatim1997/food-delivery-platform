package com.user_service.controller;

import com.user_service.exception.FileStorageException;
import com.user_service.service.impl.LocalIStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private LocalIStorageService localStorageService;

    @Mock
    private Resource resource;

    @InjectMocks
    private FileController fileController;

    @Test
    void serveFile_withFolderAndFilename_returnsOk() throws Exception {
        Path path = Paths.get("uploads/images/test.png");
        when(localStorageService.resolveFilePath("images", "test.png")).thenReturn(path);

        URI uri = URI.create("file:///uploads/images/test.png");
        try (var mocked = mockConstruction(org.springframework.core.io.UrlResource.class, (mock, ctx) -> {
            when(mock.exists()).thenReturn(true);
            when(mock.isReadable()).thenReturn(true);
            when(mock.getFilename()).thenReturn("test.png");
            when(mock.getURL()).thenReturn(uri.toURL());
        })) {
            ResponseEntity<Resource> response = fileController.serveFile("images", "test.png");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void serveFile_withFolderSubfolderAndFilename_returnsOk() throws Exception {
        Path path = Paths.get("uploads/docs/verification/license.pdf");
        when(localStorageService.resolveFilePath("docs/verification", "license.pdf")).thenReturn(path);

        URI uri = URI.create("file:///uploads/docs/verification/license.pdf");
        try (var mocked = mockConstruction(org.springframework.core.io.UrlResource.class, (mock, ctx) -> {
            when(mock.exists()).thenReturn(true);
            when(mock.isReadable()).thenReturn(true);
            when(mock.getFilename()).thenReturn("license.pdf");
            when(mock.getURL()).thenReturn(uri.toURL());
        })) {
            ResponseEntity<Resource> response = fileController.serveFile("docs", "verification", "license.pdf");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void serveFile_withFileStorageException_returnsBadRequest() {
        when(localStorageService.resolveFilePath("images", "test.png"))
                .thenThrow(new FileStorageException("Invalid path"));

        ResponseEntity<Resource> response = fileController.serveFile("images", "test.png");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void serveFile_withSubfolderAndFileStorageException_returnsBadRequest() {
        when(localStorageService.resolveFilePath("docs/verification", "license.pdf"))
                .thenThrow(new FileStorageException("Path traversal detected"));

        ResponseEntity<Resource> response = fileController.serveFile("docs", "verification", "license.pdf");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void serveFile_whenResourceNotReadable_returnsNotFound() throws Exception {
        Path path = Paths.get("uploads/images/missing.png");
        when(localStorageService.resolveFilePath("images", "missing.png")).thenReturn(path);

        URI uri = URI.create("file:///uploads/images/missing.png");
        try (var mocked = mockConstruction(org.springframework.core.io.UrlResource.class, (mock, ctx) -> {
            when(mock.exists()).thenReturn(true);
            when(mock.isReadable()).thenReturn(false);
        })) {
            ResponseEntity<Resource> response = fileController.serveFile("images", "missing.png");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void serveFile_whenResourceDoesNotExist_returnsNotFound() throws Exception {
        Path path = Paths.get("uploads/images/ghost.png");
        when(localStorageService.resolveFilePath("images", "ghost.png")).thenReturn(path);

        URI uri = URI.create("file:///uploads/images/ghost.png");
        try (var mocked = mockConstruction(org.springframework.core.io.UrlResource.class, (mock, ctx) -> {
            when(mock.exists()).thenReturn(false);
        })) {
            ResponseEntity<Resource> response = fileController.serveFile("images", "ghost.png");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void serveFile_delegatesToLocalStorageService() {
        when(localStorageService.resolveFilePath("images", "test.png"))
                .thenThrow(new FileStorageException("fail"));

        fileController.serveFile("images", "test.png");

        verify(localStorageService).resolveFilePath("images", "test.png");
    }

    @Test
    void serveFile_withSubfolder_buildsCombinedPath() {
        when(localStorageService.resolveFilePath("drivers/documents", "id.jpg"))
                .thenThrow(new FileStorageException("fail"));

        fileController.serveFile("drivers", "documents", "id.jpg");

        verify(localStorageService).resolveFilePath("drivers/documents", "id.jpg");
    }
}