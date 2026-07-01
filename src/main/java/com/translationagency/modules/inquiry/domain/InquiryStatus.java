package com.translationagency.modules.inquiry.domain;

public enum InquiryStatus {
    RECEIVED,        // Eingegangen (per E-Mail, Portal, Telefon)
    IN_CALCULATION,  // In der Preiskalkulation
    QUOTE_PREPARED,  // Angebot wurde erstellt
    QUOTE_SENT,      // Angebot an Kunden übermittelt
    ACCEPTED,        // Vom Kunden angenommen (wird zu Order)
    REJECTED,        // Vom Kunden abgelehnt
    CANCELLED        // Storniert
}
