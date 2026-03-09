package com.user_service.controller;

import com.user_service.exception.FileStorageException;
import com.user_service.service.impl.LocalIStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final LocalIStorageService localStorageService;

    @GetMapping("/{folder}/{subfolder}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String folder,
            @PathVariable String subfolder,
            @PathVariable String filename) {
        return serveFromPath(folder + "/" + subfolder, filename);
    }

    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String folder,
            @PathVariable String filename) {
        return serveFromPath(folder, filename);
    }

    private ResponseEntity<Resource> serveFromPath(String folder, String filename) {
        try {
            Path filePath = localStorageService.resolveFilePath(folder, filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                    .body(resource);

        } catch (FileStorageException e) {
            log.warn("Rejected file request for folder={} filename={}: {}", folder, filename, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (MalformedURLException e) {
            log.error("Malformed URL for file: {}/{}", folder, filename);
            return ResponseEntity.notFound().build();
        }
    }
}