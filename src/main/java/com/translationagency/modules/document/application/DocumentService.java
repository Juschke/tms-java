package com.translationagency.modules.document.application;

import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.document.domain.Document;
import com.translationagency.modules.document.domain.DocumentCategory;
import com.translationagency.modules.document.domain.DocumentVersion;
import com.translationagency.modules.document.infrastructure.DocumentRepository;
import com.translationagency.modules.document.infrastructure.DocumentVersionRepository;
import com.translationagency.modules.tenant.domain.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final StorageService storageService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentVersionRepository documentVersionRepository,
                           StorageService storageService) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.storageService = storageService;
    }

    public Document uploadDocument(Tenant tenant, Customer customer, String associatedType, UUID associatedId,
                                   String displayName, DocumentCategory category, String originalFileName,
                                   long fileSize, String mimeType, InputStream fileStream, String username) throws IOException {
        
        // 1. Dokumenten-Metadaten anlegen
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTenant(tenant);
        document.setCustomer(customer);
        document.setAssociatedEntityType(associatedType);
        document.setAssociatedEntityId(associatedId);
        document.setName(displayName);
        document.setCategory(category);
        document.setCreatedBy(username);
        document.setUpdatedBy(username);
        documentRepository.save(document);

        // 2. Datei speichern und Hashwert berechnen (DigestInputStream)
        String storageKey = UUID.randomUUID().toString() + "_" + originalFileName;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 Algorithmus nicht gefunden", e);
        }

        try (DigestInputStream dis = new DigestInputStream(fileStream, digest)) {
            storageService.storeFile(storageKey, dis);
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String fileHash = hexString.toString();

        // 3. Dokumentenversion anlegen
        DocumentVersion version = new DocumentVersion();
        version.setId(UUID.randomUUID());
        version.setDocument(document);
        version.setVersionNumber(1);
        version.setFileName(originalFileName);
        version.setFileSize(fileSize);
        version.setMimeType(mimeType);
        version.setFileHash(fileHash);
        version.setStoragePath(storageKey);
        version.setCreatedBy(username);
        documentVersionRepository.save(version);

        document.getVersions().add(version);
        return document;
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsForEntity(UUID tenantId, String type, UUID id) {
        return documentRepository.findByTenantIdAndAssociatedEntityTypeAndAssociatedEntityIdAndDeletedAtIsNull(tenantId, type, id);
    }

    @Transactional(readOnly = true)
    public InputStream getDocumentContent(DocumentVersion version) throws IOException {
        return storageService.retrieveFile(version.getStoragePath());
    }

    public void deleteDocument(UUID documentId, String username) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setDeletedAt(OffsetDateTime.now());
            doc.setUpdatedBy(username);
            documentRepository.save(doc);
        });
    }

    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }
}
