package com.translationagency.modules.billing.infrastructure;

import com.translationagency.modules.billing.domain.VendorInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, UUID> {
    @EntityGraph(attributePaths = { "partner", "order" })
    List<VendorInvoice> findByTenantId(UUID tenantId);

    List<VendorInvoice> findByPartnerId(UUID partnerId);
}
