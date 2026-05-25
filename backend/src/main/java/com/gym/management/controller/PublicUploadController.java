package com.gym.management.controller;

import com.gym.management.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Sirve imágenes públicas en /uploads; archivos inexistentes → 404 (no 403). */
@RestController
@RequiredArgsConstructor
public class PublicUploadController {

    private final FileStorageService fileStorageService;

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) throws IOException {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.notFound().build();
        }
        Path root = fileStorageService.resolveUploadPath();
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(file);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null) {
            mediaType = MediaType.parseMediaType(contentType);
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(new FileSystemResource(file));
    }
}
