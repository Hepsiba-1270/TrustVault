
package com.trustvault.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentStorageService {

    private final Path storageDirectory =
            Paths.get("trustvault-private-storage")
                  .toAbsolutePath()
                  .normalize();

    public DocumentStorageService() throws IOException {
        Files.createDirectories(storageDirectory);
    }

    public String storeFile(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file.");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Invalid filename.");
        }

        if (originalName.contains("/") || originalName.contains("\\")
                || originalName.contains("..")) {
            throw new IllegalArgumentException("Invalid filename.");
        }

        String storageFilename = UUID.randomUUID().toString();

        Path destination = storageDirectory
                .resolve(storageFilename)
                .normalize();

        if (!destination.getParent().equals(storageDirectory)) {
            throw new IllegalArgumentException("Invalid storage path.");
        }

        Files.copy(file.getInputStream(), destination,
                StandardCopyOption.REPLACE_EXISTING);

        return storageFilename;
    }

    public Path getStoragePath(String storageFilename) {

        if (storageFilename == null
                || !storageFilename.matches(
                        "[a-fA-F0-9-]{36}")) {
            throw new IllegalArgumentException("Invalid storage ID.");
        }

        Path destination = storageDirectory
                .resolve(storageFilename)
                .normalize();

        if (!destination.getParent().equals(storageDirectory)) {
            throw new IllegalArgumentException("Invalid storage path.");
        }

        return destination;
    }
}
