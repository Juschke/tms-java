package com.translationagency.modules.pricing.infrastructure;

import com.translationagency.modules.pricing.domain.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceRuleRepository extends JpaRepository<PriceRule, UUID> {
    List<PriceRule> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
    
    Optional<PriceRule> findByTenantIdAndSourceLanguageIdAndTargetLanguageIdAndServiceTypeIdAndDeletedAtIsNull(
            UUID tenantId, UUID sourceLanguageId, UUID targetLanguageId, UUID serviceTypeId);
}
