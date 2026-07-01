package com.translationagency.modules.crm.infrastructure;

import com.translationagency.modules.crm.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    List<Customer> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
    List<Customer> findByTenantIdAndCompanyNameContainingIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String companyName);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId " +
            "AND c.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.vatId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(c.billingAddressCity) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:customerNumber IS NULL OR :customerNumber = '' OR LOWER(c.customerNumber) LIKE LOWER(CONCAT('%', :customerNumber, '%'))) " +
            "AND (:companyName IS NULL OR :companyName = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :companyName, '%'))) " +
            "AND (:vatId IS NULL OR :vatId = '' OR LOWER(c.vatId) LIKE LOWER(CONCAT('%', :vatId, '%'))) " +
            "AND (:city IS NULL OR :city = '' OR LOWER(c.billingAddressCity) LIKE LOWER(CONCAT('%', :city, '%'))) " +
            "AND (:country IS NULL OR :country = '' OR LOWER(c.billingAddressCountry) LIKE LOWER(CONCAT('%', :country, '%')))")
    org.springframework.data.domain.Page<Customer> findFiltered(
            @org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("customerNumber") String customerNumber,
            @org.springframework.data.repository.query.Param("companyName") String companyName,
            @org.springframework.data.repository.query.Param("vatId") String vatId,
            @org.springframework.data.repository.query.Param("city") String city,
            @org.springframework.data.repository.query.Param("country") String country,
            org.springframework.data.domain.Pageable pageable);
}
