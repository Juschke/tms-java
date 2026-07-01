package com.translationagency.modules.pricing.infrastructure;

import com.translationagency.modules.pricing.domain.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceRuleRepository extends JpaRepository<PriceRule, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT r FROM PriceRule r " +
            "LEFT JOIN FETCH r.sourceLanguage " +
            "LEFT JOIN FETCH r.targetLanguage " +
            "LEFT JOIN FETCH r.serviceType " +
            "WHERE r.tenant.id = :tenantId AND r.deletedAt IS NULL")
    List<PriceRule> findByTenantIdAndDeletedAtIsNull(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId);
    
    Optional<PriceRule> findByTenantIdAndSourceLanguageIdAndTargetLanguageIdAndServiceTypeIdAndDeletedAtIsNull(
            UUID tenantId, UUID sourceLanguageId, UUID targetLanguageId, UUID serviceTypeId);
}
