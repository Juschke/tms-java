package com.translationagency.modules.order.infrastructure;

import com.translationagency.modules.order.domain.TranslationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranslationOrderRepository extends JpaRepository<TranslationOrder, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT o FROM TranslationOrder o " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.tenant.id = :tenantId AND o.deletedAt IS NULL")
    List<TranslationOrder> findByTenantIdAndDeletedAtIsNull(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM TranslationOrder o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.sourceLanguage " +
            "LEFT JOIN FETCH i.targetLanguage " +
            "LEFT JOIN FETCH i.assignedPartner " +
            "WHERE o.id = :id")
    java.util.Optional<TranslationOrder> findDetailById(@org.springframework.data.repository.query.Param("id") UUID id);

    @org.springframework.data.jpa.repository.Query(value = "SELECT o FROM TranslationOrder o " +
            "LEFT JOIN FETCH o.customer c " +
            "LEFT JOIN FETCH o.quote " +
            "WHERE o.tenant.id = :tenantId AND o.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:orderNumber IS NULL OR :orderNumber = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
            "AND (:customerName IS NULL OR :customerName = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
            "AND (:status IS NULL OR o.status = :status)",
            countQuery = "SELECT count(o) FROM TranslationOrder o " +
            "LEFT JOIN o.customer c " +
            "WHERE o.tenant.id = :tenantId AND o.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:orderNumber IS NULL OR :orderNumber = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
            "AND (:customerName IS NULL OR :customerName = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
            "AND (:status IS NULL OR o.status = :status)")
    org.springframework.data.domain.Page<TranslationOrder> findFiltered(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("orderNumber") String orderNumber,
            @org.springframework.data.repository.query.Param("customerName") String customerName,
            @org.springframework.data.repository.query.Param("status") com.translationagency.modules.order.domain.OrderStatus status,
            org.springframework.data.domain.Pageable pageable);
}
