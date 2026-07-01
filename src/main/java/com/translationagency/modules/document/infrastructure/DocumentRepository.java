package com.translationagency.modules.document.infrastructure;

import com.translationagency.modules.document.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT d FROM Document d LEFT JOIN FETCH d.versions WHERE d.tenant.id = :tenantId AND d.deletedAt IS NULL")
    List<Document> findByTenantIdAndDeletedAtIsNull(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM Document d LEFT JOIN FETCH d.versions " +
            "WHERE d.tenant.id = :tenantId " +
            "AND d.associatedEntityType = :associatedEntityType " +
            "AND d.associatedEntityId = :associatedEntityId " +
            "AND d.deletedAt IS NULL")
    List<Document> findByTenantIdAndAssociatedEntityTypeAndAssociatedEntityIdAndDeletedAtIsNull(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("associatedEntityType") String associatedEntityType,
            @org.springframework.data.repository.query.Param("associatedEntityId") UUID associatedEntityId);
}
