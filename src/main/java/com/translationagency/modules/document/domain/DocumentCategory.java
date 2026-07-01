package com.translationagency.modules.document.domain;

public enum DocumentCategory {
    SOURCE_FILE,      // Quelldatei zur Übersetzung
    TRANSLATION,      // Übersetzte Zieldatei
    INVOICE,          // Ausgangsrechnung
    QUOTE,            // Angebot
    SUPPORTING,       // Begleitdokumente (z.B. Glossare, TMs)
    OTHER             // Sonstige Dokumente
}
