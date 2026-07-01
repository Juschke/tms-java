package com.translationagency.modules.crm.application;

import com.translationagency.modules.crm.domain.ContactPerson;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.crm.infrastructure.ContactPersonRepository;
import com.translationagency.modules.crm.infrastructure.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ContactPersonRepository contactPersonRepository;

    public CustomerService(CustomerRepository customerRepository, ContactPersonRepository contactPersonRepository) {
        this.customerRepository = customerRepository;
        this.contactPersonRepository = contactPersonRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers(UUID tenantId) {
        return customerRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(UUID tenantId, String query) {
        if (query == null || query.isBlank()) {
            return getAllCustomers(tenantId);
        }
        return customerRepository.findByTenantIdAndCompanyNameContainingIgnoreCaseAndDeletedAtIsNull(tenantId, query);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Customer> findCustomersFiltered(
            UUID tenantId,
            String search,
            String customerNumber,
            String companyName,
            String vatId,
            String city,
            String country,
            org.springframework.data.domain.Pageable pageable) {
        return customerRepository.findFiltered(
                tenantId,
                search != null ? search : "",
                customerNumber != null ? customerNumber : "",
                companyName != null ? companyName : "",
                vatId != null ? vatId : "",
                city != null ? city : "",
                country != null ? country : "",
                pageable
        );
    }

    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public void deleteCustomer(UUID customerId, String deletedBy) {
        customerRepository.findById(customerId).ifPresent(customer -> {
            customer.setDeletedAt(OffsetDateTime.now());
            customer.setUpdatedBy(deletedBy);
            customerRepository.save(customer);
            
            // Soft-Delete aller Ansprechpartner
            customer.getContactPersons().forEach(cp -> {
                cp.setDeletedAt(OffsetDateTime.now());
                cp.setUpdatedBy(deletedBy);
                contactPersonRepository.save(cp);
            });
        });
    }

    @Transactional(readOnly = true)
    public List<ContactPerson> getContactPersons(UUID customerId) {
        return contactPersonRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
    }

    public ContactPerson saveContactPerson(ContactPerson contactPerson) {
        return contactPersonRepository.save(contactPerson);
    }

    public void deleteContactPerson(UUID contactPersonId, String deletedBy) {
        contactPersonRepository.findById(contactPersonId).ifPresent(cp -> {
            cp.setDeletedAt(OffsetDateTime.now());
            cp.setUpdatedBy(deletedBy);
            contactPersonRepository.save(cp);
        });
    }
}
