package com.translationagency.modules.settings.domain;

public enum TextTemplateType {
    QUOTE_EMAIL("Angebot E-Mail"),
    ORDER_CONFIRMATION("Auftragsbestaetigung"),
    INVOICE_EMAIL("Rechnung E-Mail"),
    DUNNING_LEVEL_1("Mahnung Stufe 1"),
    DUNNING_LEVEL_2("Mahnung Stufe 2"),
    DUNNING_LEVEL_3("Mahnung Stufe 3"),
    PARTNER_ASSIGNMENT("Partner-Beauftragung"),
    GENERAL("Allgemein");

    private final String label;

    TextTemplateType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
