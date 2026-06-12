package sn.immosn.backend.shared.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, String subfolder);
    void delete(String storedPath);
}
