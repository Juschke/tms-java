package com.translationagency.modules.settings.domain;

public enum AccountingScheme {
    SKR03("SKR03"),
    SKR04("SKR04");

    private final String label;

    AccountingScheme(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
