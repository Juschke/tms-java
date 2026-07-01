package com.translationagency.modules.document.infrastructure;

import com.translationagency.modules.document.application.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.local-dir:./storage}") String localDir) {
        this.rootLocation = Paths.get(localDir);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String storeFile(String logicalPath, InputStream inputStream) throws IOException {
        Path destinationFile = this.rootLocation.resolve(Paths.get(logicalPath)).normalize().toAbsolutePath();
        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new IOException("Cannot store file outside current directory.");
        }
        Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return logicalPath;
    }

    @Override
    public InputStream retrieveFile(String storagePath) throws IOException {
        Path file = rootLocation.resolve(storagePath).normalize().toAbsolutePath();
        if (!file.getParent().equals(rootLocation.toAbsolutePath())) {
            throw new IOException("Cannot read file outside current directory.");
        }
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + storagePath);
        }
        return Files.newInputStream(file);
    }

    @Override
    public void deleteFile(String storagePath) throws IOException {
        Path file = rootLocation.resolve(storagePath).normalize().toAbsolutePath();
        if (file.getParent().equals(rootLocation.toAbsolutePath())) {
            Files.deleteIfExists(file);
        }
    }
}
