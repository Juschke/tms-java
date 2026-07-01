package com.translationagency.modules.partner.infrastructure;

import com.translationagency.modules.partner.domain.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, UUID> {
    List<Partner> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Partner p WHERE p.tenant.id = :tenantId " +
            "AND p.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:firstName IS NULL OR :firstName = '' OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) " +
            "AND (:lastName IS NULL OR :lastName = '' OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) " +
            "AND (:email IS NULL OR :email = '' OR LOWER(p.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
            "AND (:city IS NULL OR :city = '' OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
            "AND (:classification IS NULL OR :classification = '' OR LOWER(p.classification) LIKE LOWER(CONCAT('%', :classification, '%'))) " +
            "AND (:isTranslator IS NULL OR p.isTranslator = :isTranslator) " +
            "AND (:isInterpreter IS NULL OR p.isInterpreter = :isInterpreter) " +
            "AND (:isActive IS NULL OR p.isActive = :isActive)")
    org.springframework.data.domain.Page<Partner> findFiltered(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("firstName") String firstName,
            @org.springframework.data.repository.query.Param("lastName") String lastName,
            @org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("city") String city,
            @org.springframework.data.repository.query.Param("classification") String classification,
            @org.springframework.data.repository.query.Param("isTranslator") Boolean isTranslator,
            @org.springframework.data.repository.query.Param("isInterpreter") Boolean isInterpreter,
            @org.springframework.data.repository.query.Param("isActive") Boolean isActive,
            org.springframework.data.domain.Pageable pageable);
}
