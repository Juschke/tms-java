package com.translationagency.modules.crm.infrastructure;

import com.translationagency.modules.crm.domain.ContactPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactPersonRepository extends JpaRepository<ContactPerson, UUID> {
    List<ContactPerson> findByCustomerIdAndDeletedAtIsNull(UUID customerId);
}
