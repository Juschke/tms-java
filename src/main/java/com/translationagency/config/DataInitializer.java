package com.translationagency.config;

import com.translationagency.modules.tenant.domain.Role;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.modules.tenant.domain.UserAccount;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.partner.infrastructure.PartnerRepository;
import com.translationagency.modules.tenant.infrastructure.TenantRepository;
import com.translationagency.modules.tenant.infrastructure.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PartnerRepository partnerRepository;

    public DataInitializer(TenantRepository tenantRepository,
                           UserAccountRepository userAccountRepository,
                           PasswordEncoder passwordEncoder,
                           PartnerRepository partnerRepository) {
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.partnerRepository = partnerRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (tenantRepository.count() == 0) {
            Tenant tenant = new Tenant();
            tenant.setId(UUID.randomUUID());
            tenant.setName("Standard Mandant");
            tenant.setSubdomain("app");
            tenant.setStatus("ACTIVE");
            tenant.setCreatedBy("system");
            tenant.setUpdatedBy("system");
            tenantRepository.save(tenant);

            // Seed a default partner for testing
            Partner partner = new Partner();
            partner.setId(UUID.fromString("b9736502-c94e-41df-a567-cbe24db67f92"));
            partner.setTenant(tenant);
            partner.setCompanyName("Global Translations Inc.");
            partner.setFirstName("John");
            partner.setLastName("Doe");
            partner.setEmail("john.doe@globaltrans.com");
            partner.setPhone("+49 170 1234567");
            partner.setStatus("ACTIVE");
            partner.setCreatedBy("system");
            partner.setUpdatedBy("system");
            partnerRepository.save(partner);

            createUser(tenant, "admin", "admin", "admin@translationagency.com", Role.ADMIN);
            createUser(tenant, "manager", "manager", "manager@translationagency.com", Role.MANAGER);
            createUser(tenant, "worker", "worker", "worker@translationagency.com", Role.CASE_WORKER);
            createUser(tenant, "accounting", "accounting", "accounting@translationagency.com", Role.ACCOUNTING);

            System.out.println("==================================================");
            System.out.println("Seed-Daten erfolgreich angelegt!");
            System.out.println("Standard-Login-Daten:");
            System.out.println("Admin:      admin / admin");
            System.out.println("Manager:    manager / manager");
            System.out.println("CaseWorker: worker / worker");
            System.out.println("Accounting: accounting / accounting");
            System.out.println("==================================================");
        }
    }

    private void createUser(Tenant tenant, String username, String password, String email, Role role) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setTenant(tenant);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedBy("system");
        user.setUpdatedBy("system");
        userAccountRepository.save(user);
    }
}
