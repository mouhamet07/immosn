package sn.immosn.backend.shared.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sn.immosn.backend.shared.service.FileStorageService;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf", "image/png", "image/jpeg"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".pdf", ".png", ".jpg", ".jpeg"
    );
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide ou absent.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Fichier trop volumineux — max 10 Mo.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                "Type de fichier non autorisé. Formats acceptés : PDF, PNG, JPG, JPEG.");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) extension = original.substring(dot).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Extension non autorisée : " + extension);
        }

        String filename = UUID.randomUUID() + extension;
        try {
            Path dir = Paths.get(uploadDir, subfolder).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier stocké : {}/{}/{}", uploadDir, subfolder, filename);
            return "/" + uploadDir + "/" + subfolder + "/" + filename;
        } catch (IOException e) {
            log.error("Erreur stockage fichier", e);
            throw new RuntimeException("Impossible de sauvegarder le fichier.", e);
        }
    }

    @Override
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return;
        try {
            String relative = storedPath.replaceFirst("^/", "");
            Path path = Paths.get(relative).toAbsolutePath().normalize();
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) log.info("Fichier supprimé : {}", path);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier : {}", storedPath, e);
        }
    }
}
