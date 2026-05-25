package com.gym.management.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Formato no permitido. Usa JPG, PNG, WEBP o GIF");
        }

        String extension = extensionFor(contentType);
        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(filename);
            file.transferTo(target);
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la imagen", ex);
        }
    }

    public Path resolveUploadPath() {
        return uploadRoot;
    }

    public boolean isLocalUploadUrl(String url) {
        return url != null && url.startsWith("/uploads/") && !url.contains("..");
    }

    /** URLs externas (http/https) se consideran válidas; solo se comprueba disco en /uploads/… */
    public boolean localUploadExists(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        if (!isLocalUploadUrl(url)) {
            return true;
        }
        String filename = url.substring("/uploads/".length());
        Path file = uploadRoot.resolve(filename).normalize();
        return file.startsWith(uploadRoot) && Files.isRegularFile(file);
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
