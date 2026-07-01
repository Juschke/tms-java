package com.translationagency.modules.inquiry.infrastructure;

import com.translationagency.modules.inquiry.domain.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    List<Quote> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
    Optional<Quote> findByQuoteNumber(String quoteNumber);

    @org.springframework.data.jpa.repository.Query(value = "SELECT q FROM Quote q " +
            "LEFT JOIN FETCH q.customer c " +
            "LEFT JOIN FETCH q.inquiry i " +
            "WHERE q.tenant.id = :tenantId AND q.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:quoteNumber IS NULL OR :quoteNumber = '' OR LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :quoteNumber, '%'))) " +
            "AND (:customerName IS NULL OR :customerName = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
            "AND (:status IS NULL OR q.status = :status) " +
            "AND (:minAmount IS NULL OR q.grossAmount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR q.grossAmount <= :maxAmount)",
            countQuery = "SELECT count(q) FROM Quote q " +
            "LEFT JOIN q.customer c " +
            "WHERE q.tenant.id = :tenantId AND q.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:quoteNumber IS NULL OR :quoteNumber = '' OR LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :quoteNumber, '%'))) " +
            "AND (:customerName IS NULL OR :customerName = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) " +
            "AND (:status IS NULL OR q.status = :status) " +
            "AND (:minAmount IS NULL OR q.grossAmount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR q.grossAmount <= :maxAmount)")
    org.springframework.data.domain.Page<Quote> findFiltered(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("quoteNumber") String quoteNumber,
            @org.springframework.data.repository.query.Param("customerName") String customerName,
            @org.springframework.data.repository.query.Param("status") com.translationagency.modules.inquiry.domain.QuoteStatus status,
            @org.springframework.data.repository.query.Param("minAmount") java.math.BigDecimal minAmount,
            @org.springframework.data.repository.query.Param("maxAmount") java.math.BigDecimal maxAmount,
            org.springframework.data.domain.Pageable pageable);
}
