package com.translationagency.shared.ui;

import com.vaadin.flow.component.html.Span;

/**
 * Wiederverwendbares farbiges Status-Chip (B2B-UX): macht den Zustand eines
 * Datensatzes auf einen Blick erfassbar, statt nur Rohtext anzuzeigen.
 *
 * <p>Nutzt die im Theme vorhandene CSS-Klasse {@code .status-badge} plus eine
 * semantische Ton-Klasse. Jede View mappt ihren fachlichen Status auf einen
 * {@link Tone}; die Farbgebung bleibt dadurch anwendungsweit konsistent.</p>
 */
public final class StatusBadge {

    /** Semantische Toene – bestimmen die Farbe des Chips. */
    public enum Tone {
        NEUTRAL("neutral"),
        INFO("info"),
        SUCCESS("success"),
        WARNING("warning"),
        DANGER("danger");

        private final String cssClass;

        Tone(String cssClass) {
            this.cssClass = cssClass;
        }

        public String cssClass() {
            return cssClass;
        }
    }

    private StatusBadge() {
        // Utility-Klasse
    }

    /**
     * Erzeugt ein Status-Chip.
     *
     * @param label Anzeigetext (bereits uebersetzt/aufbereitet)
     * @param tone  semantischer Ton fuer die Farbe
     */
    public static Span create(String label, Tone tone) {
        Span badge = new Span(label != null ? label : "–");
        badge.addClassNames("status-badge", "tone-" + tone.cssClass());
        return badge;
    }
}
