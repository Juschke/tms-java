package com.translationagency.modules.tenant.infrastructure;

import com.translationagency.modules.tenant.domain.NumberRange;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NumberRangeRepository extends JpaRepository<NumberRange, UUID> {

    List<NumberRange> findByTenantId(UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT nr FROM NumberRange nr WHERE nr.tenant.id = :tenantId AND nr.entityType = :entityType")
    Optional<NumberRange> findByTenantIdAndEntityTypeWithLock(@Param("tenantId") UUID tenantId, @Param("entityType") String entityType);

    Optional<NumberRange> findByTenantIdAndEntityType(UUID tenantId, String entityType);
}
