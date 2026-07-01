package com.translationagency.modules.order.infrastructure;

import com.translationagency.modules.order.domain.TranslationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranslationOrderRepository extends JpaRepository<TranslationOrder, UUID> {
    List<TranslationOrder> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT o FROM TranslationOrder o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.quote " +
            "WHERE o.tenant.id = :tenantId AND o.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(o.customer.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:orderNumber IS NULL OR :orderNumber = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
            "AND (:customerName IS NULL OR :customerName = '' OR LOWER(o.customer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
            "AND (:status IS NULL OR o.status = :status)",
            countQuery = "SELECT count(o) FROM TranslationOrder o WHERE o.tenant.id = :tenantId AND o.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(o.customer.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:orderNumber IS NULL OR :orderNumber = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) " +
            "AND (:customerName IS NULL OR :customerName = '' OR LOWER(o.customer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
            "AND (:status IS NULL OR o.status = :status)")
    org.springframework.data.domain.Page<TranslationOrder> findFiltered(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("orderNumber") String orderNumber,
            @org.springframework.data.repository.query.Param("customerName") String customerName,
            @org.springframework.data.repository.query.Param("status") com.translationagency.modules.order.domain.OrderStatus status,
            org.springframework.data.domain.Pageable pageable);
}
