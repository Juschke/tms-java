package com.translationagency.modules.inquiry.infrastructure;

import com.translationagency.modules.inquiry.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {
        @org.springframework.data.jpa.repository.Query("SELECT i FROM Inquiry i " +
                        "LEFT JOIN FETCH i.customer " +
                        "LEFT JOIN FETCH i.serviceType " +
                        "LEFT JOIN FETCH i.sourceLanguage " +
                        "LEFT JOIN FETCH i.targetLanguage " +
                        "WHERE i.tenant.id = :tenantId AND i.deletedAt IS NULL")
        List<Inquiry> findByTenantIdAndDeletedAtIsNull(
                        @org.springframework.data.repository.query.Param("tenantId") UUID tenantId);

        @org.springframework.data.jpa.repository.Query(value = "SELECT i FROM Inquiry i " +
                        "LEFT JOIN FETCH i.customer " +
                        "LEFT JOIN FETCH i.serviceType " +
                        "LEFT JOIN FETCH i.sourceLanguage " +
                        "LEFT JOIN FETCH i.targetLanguage " +
                        "WHERE i.tenant.id = :tenantId AND i.deletedAt IS NULL " +
                        "AND (:search IS NULL OR :search = '' OR " +
                        "     LOWER(i.customer.customerNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "     LOWER(i.customer.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "     LOWER(i.notes) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "AND (:customerName IS NULL OR :customerName = '' OR LOWER(i.customer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) "
                        +
                        "AND (:status IS NULL OR i.status = :status) " +
                        "AND (:sourceLanguageId IS NULL OR i.sourceLanguage.id = :sourceLanguageId) " +
                        "AND (:targetLanguageId IS NULL OR i.targetLanguage.id = :targetLanguageId) " +
                        "AND (:isExpress IS NULL OR i.isExpress = :isExpress) " +
                        "AND (:isCertified IS NULL OR i.isCertified = :isCertified)", countQuery = "SELECT count(i) FROM Inquiry i WHERE i.tenant.id = :tenantId AND i.deletedAt IS NULL "
                                        +
                                        "AND (:search IS NULL OR :search = '' OR " +
                                        "     LOWER(i.customer.customerNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                                        +
                                        "     LOWER(i.customer.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                                        "     LOWER(i.notes) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                                        "AND (:customerName IS NULL OR :customerName = '' OR LOWER(i.customer.companyName) LIKE LOWER(CONCAT('%', :customerName, '%'))) "
                                        +
                                        "AND (:status IS NULL OR i.status = :status) " +
                                        "AND (:sourceLanguageId IS NULL OR i.sourceLanguage.id = :sourceLanguageId) " +
                                        "AND (:targetLanguageId IS NULL OR i.targetLanguage.id = :targetLanguageId) " +
                                        "AND (:isExpress IS NULL OR i.isExpress = :isExpress) " +
                                        "AND (:isCertified IS NULL OR i.isCertified = :isCertified)")
        org.springframework.data.domain.Page<Inquiry> findFiltered(
                        @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
                        @org.springframework.data.repository.query.Param("search") String search,
                        @org.springframework.data.repository.query.Param("customerName") String customerName,
                        @org.springframework.data.repository.query.Param("status") com.translationagency.modules.inquiry.domain.InquiryStatus status,
                        @org.springframework.data.repository.query.Param("sourceLanguageId") UUID sourceLanguageId,
                        @org.springframework.data.repository.query.Param("targetLanguageId") UUID targetLanguageId,
                        @org.springframework.data.repository.query.Param("isExpress") Boolean isExpress,
                        @org.springframework.data.repository.query.Param("isCertified") Boolean isCertified,
                        org.springframework.data.domain.Pageable pageable);
}
