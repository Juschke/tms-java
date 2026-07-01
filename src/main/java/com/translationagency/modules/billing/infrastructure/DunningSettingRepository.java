package com.translationagency.modules.billing.infrastructure;

import com.translationagency.modules.billing.domain.DunningSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DunningSettingRepository extends JpaRepository<DunningSetting, UUID> {
    Optional<DunningSetting> findByTenantId(UUID tenantId);
}
