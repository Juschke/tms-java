package com.translationagency.modules.settings.infrastructure;

import com.translationagency.modules.settings.domain.TextTemplate;
import com.translationagency.modules.settings.domain.TextTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TextTemplateRepository extends JpaRepository<TextTemplate, UUID> {
    List<TextTemplate> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);

    Optional<TextTemplate> findByTenantIdAndTemplateTypeAndDeletedAtIsNull(UUID tenantId, TextTemplateType templateType);

    List<TextTemplate> findByTenantIdAndTemplateTypeAndDeletedAtIsNullOrderByNameAsc(UUID tenantId, TextTemplateType templateType);

    long countByTenantId(UUID tenantId);
}
