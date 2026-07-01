package com.translationagency.modules.order.domain;

public enum PartnerAssignmentStatus {
    OFFERED,         // Dem Partner angeboten
    ACCEPTED,        // Partner hat den Auftrag angenommen und Frist bestätigt
    REJECTED,        // Partner hat abgelehnt
    IN_PROGRESS,     // Partner arbeitet daran
    SUBMITTED,       // Partner hat fertige Übersetzung hochgeladen
    APPROVED,        // Übersetzung vom Manager abgenommen
    DISPUTED         // Korrekturschleife/Reklamation
}
