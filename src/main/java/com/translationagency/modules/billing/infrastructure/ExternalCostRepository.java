package com.translationagency.modules.billing.infrastructure;

import com.translationagency.modules.billing.domain.ExternalCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExternalCostRepository extends JpaRepository<ExternalCost, UUID> {
    @EntityGraph(attributePaths = { "order" })
    List<ExternalCost> findByTenantId(UUID tenantId);

    List<ExternalCost> findByOrderId(UUID orderId);
}
