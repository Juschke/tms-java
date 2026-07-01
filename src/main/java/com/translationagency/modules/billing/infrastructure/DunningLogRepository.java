package com.translationagency.modules.billing.infrastructure;

import com.translationagency.modules.billing.domain.DunningLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DunningLogRepository extends JpaRepository<DunningLog, UUID> {
    List<DunningLog> findByTenantId(UUID tenantId);
    List<DunningLog> findByInvoiceId(UUID invoiceId);
}
