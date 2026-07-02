package com.translationagency.modules.order.infrastructure;

import com.translationagency.modules.order.domain.PartnerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerAssignmentRepository extends JpaRepository<PartnerAssignment, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT pa FROM PartnerAssignment pa " +
            "LEFT JOIN FETCH pa.partner " +
            "LEFT JOIN FETCH pa.order o " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.id = :orderId")
    List<PartnerAssignment> findByOrderId(@org.springframework.data.repository.query.Param("orderId") UUID orderId);
}
