package com.translationagency.shared.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Universelles Custom-Field: eine Checkbox schaltet einen Datums- und einen
 * Uhrzeit-Picker frei. Nach aussen verhaelt es sich wie ein einzelnes Feld
 * vom Typ {@link OffsetDateTime} und laesst sich direkt an einen Binder binden.
 *
 * <ul>
 *   <li>Checkbox an  -> Datum/Uhrzeit editierbar; ohne Wert wird "jetzt" gesetzt.</li>
 *   <li>Checkbox aus -> Wert ist {@code null}, Picker deaktiviert.</li>
 *   <li>Datumsformat ist fest deutsch: {@code dd.MM.yyyy}.</li>
 * </ul>
 */
public class ToggleDateTimeField extends CustomField<OffsetDateTime> {

    private final Checkbox toggle = new Checkbox();
    private final DatePicker datePicker = new DatePicker();
    private final TimePicker timePicker = new TimePicker();

    private final ZoneId zoneId = ZoneId.systemDefault();

    public ToggleDateTimeField(String checkboxLabel) {
        // Label oben ueber der Checkbox – wie bei Date-/TimePicker
        datePicker.setLabel(checkboxLabel);

        // Deutsches Datumsformat dd.MM.yyyy erzwingen
        DatePicker.DatePickerI18n german = new DatePicker.DatePickerI18n();
        german.setDateFormat("dd.MM.yyyy");
        german.setMonthNames(List.of("Januar", "Februar", "März", "April", "Mai", "Juni",
                "Juli", "August", "September", "Oktober", "November", "Dezember"));
        german.setWeekdays(List.of("Sonntag", "Montag", "Dienstag", "Mittwoch",
                "Donnerstag", "Freitag", "Samstag"));
        german.setWeekdaysShort(List.of("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"));
        german.setFirstDayOfWeek(1);
        german.setToday("Heute");
        german.setCancel("Abbrechen");
        datePicker.setI18n(german);
        datePicker.setPlaceholder("TT.MM.JJJJ");

        timePicker.setStep(java.time.Duration.ofMinutes(15));
        timePicker.setPlaceholder("hh:mm");

        setPickersEnabled(false);

        toggle.addValueChangeListener(e -> {
            boolean on = Boolean.TRUE.equals(e.getValue());
            setPickersEnabled(on);
            if (on) {
                if (datePicker.getValue() == null) {
                    datePicker.setValue(LocalDate.now());
                }
                if (timePicker.getValue() == null) {
                    timePicker.setValue(LocalTime.now().withSecond(0).withNano(0));
                }
            } else {
                datePicker.clear();
                timePicker.clear();
            }
            updateValue();
        });
        datePicker.addValueChangeListener(e -> updateValue());
        timePicker.addValueChangeListener(e -> updateValue());

        // Checkbox ohne eigenes Label; sitzt buendig auf Feldhoehe
        toggle.getStyle().set("flex", "0 0 auto");
        toggle.getStyle().set("padding-bottom", "6px");

        // Datum und Uhrzeit gleich breit
        datePicker.setWidthFull();
        timePicker.setWidthFull();

        HorizontalLayout layout = new HorizontalLayout(toggle, datePicker, timePicker);
        layout.setAlignItems(FlexComponent.Alignment.END);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.setFlexGrow(1, datePicker);
        layout.setFlexGrow(1, timePicker);
        add(layout);
    }

    private void setPickersEnabled(boolean enabled) {
        datePicker.setEnabled(enabled);
        timePicker.setEnabled(enabled);
    }

    /** Liefert {@code true}, wenn die Checkbox aktiv ist (z. B. zur Synchronisation eines boolean-Flags). */
    public boolean isChecked() {
        return Boolean.TRUE.equals(toggle.getValue());
    }

    @Override
    protected OffsetDateTime generateModelValue() {
        if (!toggle.getValue() || datePicker.getValue() == null) {
            return null;
        }
        LocalTime time = timePicker.getValue() != null ? timePicker.getValue() : LocalTime.MIDNIGHT;
        LocalDateTime local = LocalDateTime.of(datePicker.getValue(), time);
        return local.atZone(zoneId).toOffsetDateTime();
    }

    @Override
    protected void setPresentationValue(OffsetDateTime value) {
        if (value == null) {
            toggle.setValue(false);
            setPickersEnabled(false);
            datePicker.clear();
            timePicker.clear();
        } else {
            LocalDateTime local = value.atZoneSameInstant(zoneId).toLocalDateTime();
            toggle.setValue(true);
            setPickersEnabled(true);
            datePicker.setValue(local.toLocalDate());
            timePicker.setValue(local.toLocalTime());
        }
    }
}
