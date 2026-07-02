package com.translationagency.shared.ui;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Zentraler Helper fuer einheitliches Nutzer-Feedback (B2B-UX).
 *
 * <p>Vereinheitlicht Position, Dauer, Farbe und Icon aller Toast-Meldungen,
 * damit die gesamte Anwendung konsistent kommuniziert. Statt an vielen Stellen
 * {@code Notification.show(...).addThemeVariants(...)} zu wiederholen, ruft man
 * hier {@link #success(String)}, {@link #error(String)} usw. auf.</p>
 */
public final class Notifications {

    private static final int DURATION_SHORT = 3000;
    private static final int DURATION_LONG = 5000;
    private static final Notification.Position POSITION = Notification.Position.BOTTOM_END;

    private Notifications() {
        // Utility-Klasse
    }

    /** Erfolgsmeldung (gruen), kurze Dauer. */
    public static Notification success(String message) {
        return show(message, VaadinIcon.CHECK_CIRCLE, NotificationVariant.LUMO_SUCCESS, DURATION_SHORT);
    }

    /** Fehlermeldung (rot), laengere Dauer, damit der Nutzer sie lesen kann. */
    public static Notification error(String message) {
        return show(message, VaadinIcon.EXCLAMATION_CIRCLE, NotificationVariant.LUMO_ERROR, DURATION_LONG);
    }

    /** Warnung (gelb), mittlere Dauer. */
    public static Notification warning(String message) {
        return show(message, VaadinIcon.WARNING, NotificationVariant.LUMO_WARNING, DURATION_LONG);
    }

    /** Neutrale Info (blau/kontrast), kurze Dauer. */
    public static Notification info(String message) {
        return show(message, VaadinIcon.INFO_CIRCLE, NotificationVariant.LUMO_CONTRAST, DURATION_SHORT);
    }

    private static Notification show(String message, VaadinIcon icon,
                                     NotificationVariant variant, int duration) {
        Notification notification = new Notification();
        notification.addThemeVariants(variant);
        notification.setPosition(POSITION);
        notification.setDuration(duration);

        HorizontalLayout layout = new HorizontalLayout(
                icon.create(),
                new com.vaadin.flow.component.html.Span(message)
        );
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.setPadding(false);

        notification.add(layout);
        notification.open();
        return notification;
    }
}
