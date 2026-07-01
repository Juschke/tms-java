package com.translationagency.modules.order.infrastructure;

import com.translationagency.modules.order.domain.PartnerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerAssignmentRepository extends JpaRepository<PartnerAssignment, UUID> {
    List<PartnerAssignment> findByOrderId(UUID orderId);
}
