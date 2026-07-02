package com.translationagency.modules.order.ui;

import com.translationagency.modules.order.domain.TranslationOrderItem;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.shared.ui.ToggleDateTimeField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Consumer;

public class TeilauftragDialog extends Dialog {

    private final PricingService pricingService;
    private final PartnerService partnerService;
    private final TranslationOrderItem item;
    private final Consumer<TranslationOrderItem> onSave;

    private final Binder<TranslationOrderItem> binder = new Binder<>(TranslationOrderItem.class);

    public TeilauftragDialog(PricingService pricingService, PartnerService partnerService,
                             TranslationOrderItem item, UUID tenantId, Consumer<TranslationOrderItem> onSave) {
        this.pricingService = pricingService;
        this.partnerService = partnerService;
        this.item = item;
        this.onSave = onSave;

        setHeaderTitle(item.getId() == null ? "Teilauftrag hinzufügen" : "Teilauftrag bearbeiten");
        setWidth("1200px");
        setMaxWidth("95vw");
        setHeight("auto");
        setMaxHeight("92vh");

        // 1. Allgemeine Informationen
        ComboBox<Language> sourceLang = new ComboBox<>("Ausgangssprache");
        sourceLang.setItems(pricingService.getAllLanguages());
        sourceLang.setItemLabelGenerator(Language::getName);

        ComboBox<Language> targetLang = new ComboBox<>("Zielsprache");
        targetLang.setItems(pricingService.getAllLanguages());
        targetLang.setItemLabelGenerator(Language::getName);

        ComboBox<String> activity = new ComboBox<>("Tätigkeit");
        activity.setItems("Übersetzen", "Lektorieren", "Dolmetschen", "Beglaubigung");
        activity.setValue("Übersetzen");

        TextField description = new TextField("Titel / Beschreibung");
        description.setWidthFull();

        // 2. Zuständigkeit
        ComboBox<Partner> translator = new ComboBox<>("Übersetzer");
        translator.setItems(partnerService.getAllPartners(tenantId));
        translator.setItemLabelGenerator(Partner::getFullName);

        // 3. Status & Termine – universelles Checkbox+Datum+Uhrzeit-Feld
        ToggleDateTimeField assignedField = new ToggleDateTimeField("Zuteilung am");
        ToggleDateTimeField deadlineField = new ToggleDateTimeField("Termin");
        ToggleDateTimeField completedField = new ToggleDateTimeField("Erledigt");
        ToggleDateTimeField cancelledField = new ToggleDateTimeField("Storniert");

        ComboBox<String> cancellationReason = new ComboBox<>("Stornogrund");
        cancellationReason.setItems("Kunde hat storniert", "Übersetzer abgesprungen", "Falsche Sprachen", "Sonstiges");
        cancellationReason.setAllowCustomValue(true);
        cancellationReason.addCustomValueSetListener(e -> cancellationReason.setValue(e.getDetail()));
        cancellationReason.setWidthFull();

        // 4. Umfang
        ComboBox<String> unit = new ComboBox<>("Einheit");
        unit.setItems("WORDS", "PAGES", "FLAT_RATE");

        IntegerField wordCount = new IntegerField("Wörter");
        BigDecimalField pageCount = new BigDecimalField("Seitenanzahl");

        // 5. Fakturierung
        BigDecimalField unitPrice = new BigDecimalField("Preis/Zeile bzw. Einheit (€)");
        BigDecimalField surchargeOrDiscount = new BigDecimalField("Zuschlag od. Abzug (€)");
        BigDecimalField totalPriceField = new BigDecimalField("Gesamtpreis (€)");
        totalPriceField.setReadOnly(true);

        ToggleDateTimeField invoiceReceivedField = new ToggleDateTimeField("Rechnungseingang");
        ToggleDateTimeField paidField = new ToggleDateTimeField("Ausgezahlt am");

        // Calculation Logic
        Runnable calculateTotal = () -> {
            BigDecimal qty = BigDecimal.ONE;
            if ("WORDS".equals(unit.getValue()) && wordCount.getValue() != null) {
                qty = BigDecimal.valueOf(wordCount.getValue());
            } else if ("PAGES".equals(unit.getValue()) && pageCount.getValue() != null) {
                qty = pageCount.getValue();
            }
            BigDecimal uPrice = unitPrice.getValue() != null ? unitPrice.getValue() : BigDecimal.ZERO;
            BigDecimal extra = surchargeOrDiscount.getValue() != null ? surchargeOrDiscount.getValue() : BigDecimal.ZERO;
            BigDecimal total = qty.multiply(uPrice).add(extra);
            totalPriceField.setValue(total);
        };

        unit.addValueChangeListener(e -> {
            boolean isWords = "WORDS".equals(e.getValue());
            boolean isPages = "PAGES".equals(e.getValue());
            wordCount.setEnabled(isWords);
            pageCount.setEnabled(isPages);
            if (!isWords) wordCount.setValue(null);
            if (!isPages) pageCount.setValue(null);
            calculateTotal.run();
        });

        wordCount.addValueChangeListener(e -> calculateTotal.run());
        pageCount.addValueChangeListener(e -> calculateTotal.run());
        unitPrice.addValueChangeListener(e -> calculateTotal.run());
        surchargeOrDiscount.addValueChangeListener(e -> calculateTotal.run());

        // ============================================================
        // Layout: dicht gepackt, gegliedert nach Bearbeitungsphasen
        // ============================================================

        // --- 1. AUFTRAG: Was ist zu tun? ---
        FormLayout auftragForm = twoColForm();
        auftragForm.add(sourceLang, targetLang, activity, description);
        auftragForm.setColspan(description, 2);

        // --- 2. UMFANG & PREIS ---
        FormLayout umfangForm = twoColForm();
        umfangForm.add(unit, wordCount, pageCount, unitPrice, surchargeOrDiscount, totalPriceField);

        // --- 3. BEARBEITUNG: Wer & bis wann? ---
        FormLayout bearbeitungForm = twoColForm();
        bearbeitungForm.add(translator);
        bearbeitungForm.setColspan(translator, 2);
        bearbeitungForm.add(assignedField, deadlineField, completedField);
        bearbeitungForm.setColspan(assignedField, 2);
        bearbeitungForm.setColspan(deadlineField, 2);
        bearbeitungForm.setColspan(completedField, 2);

        // --- 4. ABRECHNUNG ---
        FormLayout abrechnungForm = twoColForm();
        abrechnungForm.add(invoiceReceivedField, paidField);
        abrechnungForm.setColspan(invoiceReceivedField, 2);
        abrechnungForm.setColspan(paidField, 2);

        // --- 5. STORNO (nur relevant, wenn storniert) ---
        FormLayout stornoForm = twoColForm();
        stornoForm.add(cancelledField, cancellationReason);
        stornoForm.setColspan(cancelledField, 2);
        stornoForm.setColspan(cancellationReason, 2);

        // Zwei-Spalten-Raster ohne Freiraum-Overhead
        Div grid = new Div(
            section("Auftrag", auftragForm),
            section("Umfang & Preis", umfangForm),
            section("Bearbeitung", bearbeitungForm),
            section("Abrechnung", abrechnungForm),
            section("Storno", stornoForm)
        );
        grid.getStyle().set("display", "grid");
        grid.getStyle().set("grid-template-columns", "minmax(0, 1fr) minmax(0, 1fr)");
        grid.getStyle().set("gap", "var(--lumo-space-m)");
        grid.getStyle().set("width", "100%");
        grid.getStyle().set("box-sizing", "border-box");
        grid.getStyle().set("align-items", "start");

        add(grid);

        // Dezente Audit-Fusszeile
        String created = item.getCreatedAt() != null
                ? item.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "–";
        String updated = item.getUpdatedAt() != null
                ? item.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "–";
        Span auditInfo = new Span("Erstellt: " + created + "  ·  Zuletzt bearbeitet: " + updated);
        auditInfo.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        auditInfo.getStyle().set("color", "var(--lumo-tertiary-text-color)");
        auditInfo.getStyle().set("margin-top", "var(--lumo-space-s)");
        auditInfo.getStyle().set("display", "block");
        add(auditInfo);

        // Binder configuration
        binder.forField(sourceLang).asRequired().bind(TranslationOrderItem::getSourceLanguage, TranslationOrderItem::setSourceLanguage);
        binder.forField(targetLang).asRequired().bind(TranslationOrderItem::getTargetLanguage, TranslationOrderItem::setTargetLanguage);
        binder.forField(activity).asRequired().bind(TranslationOrderItem::getActivity, TranslationOrderItem::setActivity);
        binder.forField(description).asRequired().bind(TranslationOrderItem::getDescription, TranslationOrderItem::setDescription);
        binder.bind(translator, TranslationOrderItem::getAssignedPartner, TranslationOrderItem::setAssignedPartner);
        binder.bind(unit, TranslationOrderItem::getUnit, TranslationOrderItem::setUnit);
        binder.bind(wordCount, TranslationOrderItem::getWordCount, TranslationOrderItem::setWordCount);
        binder.bind(pageCount, TranslationOrderItem::getPageCount, TranslationOrderItem::setPageCount);
        binder.bind(unitPrice, TranslationOrderItem::getUnitPrice, TranslationOrderItem::setUnitPrice);
        binder.bind(surchargeOrDiscount, TranslationOrderItem::getSurchargeOrDiscount, TranslationOrderItem::setSurchargeOrDiscount);
        binder.bind(cancellationReason, TranslationOrderItem::getCancellationReason, TranslationOrderItem::setCancellationReason);

        // Termin-Felder: je eine direkte OffsetDateTime-Bindung (Checkbox steckt im Custom Field)
        binder.forField(assignedField).bind(TranslationOrderItem::getAssignedAt, TranslationOrderItem::setAssignedAt);
        binder.forField(deadlineField).bind(TranslationOrderItem::getDeadlineAt, TranslationOrderItem::setDeadlineAt);
        binder.forField(completedField).bind(TranslationOrderItem::getCompletedAt, TranslationOrderItem::setCompletedAt);
        binder.forField(cancelledField).bind(TranslationOrderItem::getCancelledAt, TranslationOrderItem::setCancelledAt);
        binder.forField(invoiceReceivedField).bind(TranslationOrderItem::getInvoiceReceivedAt, TranslationOrderItem::setInvoiceReceivedAt);
        binder.forField(paidField).bind(TranslationOrderItem::getPaidAt, TranslationOrderItem::setPaidAt);

        // Stornogrund nur bei aktivem Storno-Feld editierbar
        cancellationReason.setEnabled(cancelledField.isChecked());
        cancelledField.addValueChangeListener(e -> cancellationReason.setEnabled(cancelledField.isChecked()));

        // Load values
        binder.readBean(item);
        if (item.getWordCount() != null) wordCount.setValue(item.getWordCount());
        if (item.getPageCount() != null) pageCount.setValue(item.getPageCount());
        cancellationReason.setEnabled(cancelledField.isChecked());

        calculateTotal.run();

        // Actions
        Button saveBtn = new Button("Übernehmen", e -> {
            try {
                binder.writeBean(item);
                // boolean-Flags aus den Custom Fields synchron halten
                item.setAssigned(assignedField.isChecked());
                item.setHasDeadline(deadlineField.isChecked());
                item.setCompleted(completedField.isChecked());
                item.setCancelled(cancelledField.isChecked());
                item.setInvoiceReceived(invoiceReceivedField.isChecked());
                item.setPaid(paidField.isChecked());

                BigDecimal qty = BigDecimal.ONE;
                if ("WORDS".equals(item.getUnit()) && item.getWordCount() != null) {
                    qty = BigDecimal.valueOf(item.getWordCount());
                } else if ("PAGES".equals(item.getUnit()) && item.getPageCount() != null) {
                    qty = item.getPageCount();
                }
                item.setQuantity(qty);
                item.setTotalPrice(totalPriceField.getValue());

                onSave.accept(item);
                close();
            } catch (Exception ex) {
                Notification.show("Fehler beim Validieren: " + ex.getMessage());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Abbrechen", e -> close());
        getFooter().add(cancelBtn, saveBtn);
    }

    /**
     * Dichtes 2-Spalten-Formular mit engem Spacing – Basis jeder Sektion.
     */
    private FormLayout twoColForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setWidthFull();
        form.getStyle().set("box-sizing", "border-box");
        form.getStyle().set("--vaadin-form-layout-column-spacing", "var(--lumo-space-s)");
        form.getStyle().set("--vaadin-form-item-row-spacing", "var(--lumo-space-xs)");
        return form;
    }

    /**
     * Kompakte Sektionskarte: weisser Rahmen (.form-panel) mit blauem
     * Sektionstitel (.section-header). Nutzt bestehende Theme-Klassen.
     */
    private Div section(String titleText, Component content) {
        Div card = new Div();
        card.addClassName("form-panel");
        card.getStyle().set("width", "100%");
        card.getStyle().set("box-sizing", "border-box");
        card.getStyle().set("min-width", "0");
        card.getStyle().set("overflow", "hidden");
        card.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");

        Span header = new Span(titleText);
        header.addClassName("section-header");
        header.getStyle().set("display", "block");

        card.add(header, content);
        return card;
    }
}
