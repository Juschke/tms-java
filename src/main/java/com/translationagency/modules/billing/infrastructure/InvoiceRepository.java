package com.translationagency.modules.billing.infrastructure;

import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.billing.domain.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query("SELECT i FROM Invoice i " +
            "LEFT JOIN FETCH i.customer " +
            "WHERE i.tenant.id = :tenantId AND i.deletedAt IS NULL")
    List<Invoice> findByTenantIdAndDeletedAtIsNull(@Param("tenantId") UUID tenantId);

    /**
     * Filtered, paginated invoice query.
     *
     * <p>All filter parameters use "show all" semantics when empty/null:
     * <ul>
     *   <li>{@code numberFilter} — empty string skips the LIKE check.</li>
     *   <li>{@code customerFilter} — empty string skips the customer name check.</li>
     *   <li>{@code status} — {@code null} skips the status check.</li>
     * </ul>
     *
     * <p>The {@link Pageable} argument carries page index, page size and sort orders
     * from {@link org.springframework.data.domain.PageRequest}.
     */
    @Query(value = """
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.customer c
            WHERE i.tenant.id = :tenantId
              AND i.deletedAt IS NULL
              AND (:search IS NULL OR :search = '' OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:numberFilter = '' OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :numberFilter, '%')))
              AND (:customerFilter = '' OR (c IS NOT NULL AND LOWER(c.companyName) LIKE LOWER(CONCAT('%', :customerFilter, '%'))))
              AND (:status IS NULL OR i.status = :status)
              AND (:issuedFromEnabled = FALSE OR i.issuedAt >= :issuedFrom)
              AND (:issuedToEnabled = FALSE OR i.issuedAt <= :issuedTo)
            """,
            countQuery = """
            SELECT COUNT(i) FROM Invoice i
            LEFT JOIN i.customer c
            WHERE i.tenant.id = :tenantId
              AND i.deletedAt IS NULL
              AND (:search IS NULL OR :search = '' OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:numberFilter = '' OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :numberFilter, '%')))
              AND (:customerFilter = '' OR (c IS NOT NULL AND LOWER(c.companyName) LIKE LOWER(CONCAT('%', :customerFilter, '%'))))
              AND (:status IS NULL OR i.status = :status)
              AND (:issuedFromEnabled = FALSE OR i.issuedAt >= :issuedFrom)
              AND (:issuedToEnabled = FALSE OR i.issuedAt <= :issuedTo)
            """)
    Page<Invoice> findFiltered(
            @Param("tenantId")       UUID          tenantId,
            @Param("search")         String        search,
            @Param("numberFilter")   String        numberFilter,
            @Param("customerFilter") String        customerFilter,
            @Param("status")         InvoiceStatus status,
            @Param("issuedFromEnabled") boolean issuedFromEnabled,
            @Param("issuedFrom")     java.time.OffsetDateTime issuedFrom,
            @Param("issuedToEnabled") boolean issuedToEnabled,
            @Param("issuedTo")       java.time.OffsetDateTime issuedTo,
            Pageable                               pageable
    );
}
