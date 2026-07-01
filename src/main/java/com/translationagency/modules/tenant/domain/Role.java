package com.translationagency.modules.tenant.domain;

public enum Role {
    ADMIN,
    MANAGER,
    CASE_WORKER,
    ACCOUNTING,
    TRANSLATOR_INTERNAL,
    PARTNER,
    CUSTOMER;

    public String getRoleName() {
        return "ROLE_" + this.name();
    }
}
