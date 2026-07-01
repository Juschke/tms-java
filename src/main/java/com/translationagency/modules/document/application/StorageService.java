package com.translationagency.modules.document.application;

import java.io.InputStream;
import java.io.IOException;

public interface StorageService {
    
    /**
     * Speichert eine Datei und gibt den storagePath zurück.
     */
    String storeFile(String logicalPath, InputStream inputStream) throws IOException;

    /**
     * Holt eine Datei als InputStream zurück.
     */
    InputStream retrieveFile(String storagePath) throws IOException;

    /**
     * Löscht eine Datei im Storage.
     */
    void deleteFile(String storagePath) throws IOException;
}
