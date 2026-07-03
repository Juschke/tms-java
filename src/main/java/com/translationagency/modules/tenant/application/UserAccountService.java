package com.translationagency.modules.tenant.application;

import com.translationagency.modules.tenant.domain.Role;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.modules.tenant.domain.UserAccount;
import com.translationagency.modules.tenant.infrastructure.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> getUsers(UUID tenantId) {
        return userAccountRepository.findByTenantIdAndDeletedAtIsNullOrderByUsernameAsc(tenantId);
    }

    public UserAccount saveUser(UserAccount user, Tenant tenant, String plainPassword, String username) {
        if (user.getTenant() == null) {
            user.setTenant(tenant);
        }
        if (user.getRole() == null) {
            user.setRole(Role.CASE_WORKER);
        }
        if (user.getId() == null && (plainPassword == null || plainPassword.isBlank())) {
            throw new IllegalArgumentException("Passwort ist fuer neue Benutzer erforderlich.");
        }
        if (plainPassword != null && !plainPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(plainPassword));
        }
        if (user.getCreatedBy() == null) {
            user.setCreatedBy(username);
        }
        user.setUpdatedBy(username);
        return userAccountRepository.save(user);
    }

    public void deleteUser(UUID userId, String username) {
        userAccountRepository.findById(userId).ifPresent(user -> {
            user.setDeletedAt(OffsetDateTime.now());
            user.setUpdatedBy(username);
            userAccountRepository.save(user);
        });
    }
}
