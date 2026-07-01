package com.translationagency.modules.billing.domain;

public enum InvoiceStatus {
    DRAFT,           // Entwurf
    ISSUED,          // Festgeschrieben und gesendet
    PARTIALLY_PAID,  // Teilweise bezahlt
    PAID,            // Vollständig bezahlt
    DUNNED,          // Gemahnt
    CANCELLED        // Storniert (Gutschrift/Stornorechnung erzeugt)
}
