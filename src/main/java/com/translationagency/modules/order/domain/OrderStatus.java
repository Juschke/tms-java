package com.translationagency.modules.order.domain;

public enum OrderStatus {
    CREATED,         // Auftrag angelegt (aus Angebot)
    ASSIGNED,        // Partner oder internem Übersetzer zugewiesen
    IN_PROGRESS,     // In Bearbeitung (Übersetzung läuft)
    QA_READY,        // Übersetzung hochgeladen, Qualitätssicherung läuft
    READY_FOR_DELIVERY, // QS erfolgreich, bereit zur Lieferung
    DELIVERED,       // An Kunden ausgeliefert (digital/postalisch)
    INVOICED,        // Rechnung erstellt
    PAID,            // Rechnung vollständig bezahlt
    ARCHIVED,        // Vorgang abgeschlossen und archiviert
    CANCELLED        // Auftrag storniert
}
