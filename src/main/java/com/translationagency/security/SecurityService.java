package com.translationagency.security;

import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.modules.tenant.domain.UserAccount;
import com.translationagency.modules.tenant.infrastructure.UserAccountRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityService {

    private final AuthenticationContext authenticationContext;
    private final UserAccountRepository userAccountRepository;

    public SecurityService(AuthenticationContext authenticationContext, UserAccountRepository userAccountRepository) {
        this.authenticationContext = authenticationContext;
        this.userAccountRepository = userAccountRepository;
    }

    public Optional<UserDetails> getAuthenticatedUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class);
    }

    public Optional<UserAccount> getAuthenticatedUserAccount() {
        return getAuthenticatedUser()
                .flatMap(userDetails -> userAccountRepository.findByUsername(userDetails.getUsername()));
    }

    public Optional<Tenant> getAuthenticatedTenant() {
        return getAuthenticatedUserAccount().map(UserAccount::getTenant);
    }

    public void logout() {
        authenticationContext.logout();
    }
}

