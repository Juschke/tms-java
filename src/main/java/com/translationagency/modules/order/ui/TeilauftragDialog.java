package com.translationagency.modules.order.ui;

import com.translationagency.modules.order.domain.TranslationOrderItem;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
        setWidth("1000px");

        FormLayout formLayout = new FormLayout();

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

        // 3. Status Sektion
        Checkbox isAssignedCb = new Checkbox("Zuteilung am");
        DateTimePicker assignedAtPicker = new DateTimePicker();
        HorizontalLayout assignedLayout = createToggleDate(isAssignedCb, assignedAtPicker);

        Checkbox hasDeadlineCb = new Checkbox("Termin");
        DateTimePicker deadlineAtPicker = new DateTimePicker();
        HorizontalLayout deadlineLayout = createToggleDate(hasDeadlineCb, deadlineAtPicker);

        Checkbox isCompletedCb = new Checkbox("Erledigt");
        DateTimePicker completedAtPicker = new DateTimePicker();
        HorizontalLayout completedLayout = createToggleDate(isCompletedCb, completedAtPicker);

        Checkbox isCancelledCb = new Checkbox("Storniert");
        DateTimePicker cancelledAtPicker = new DateTimePicker();
        HorizontalLayout cancelledDateLayout = createToggleDate(isCancelledCb, cancelledAtPicker);
        
        ComboBox<String> cancellationReason = new ComboBox<>("Stornogrund");
        cancellationReason.setItems("Kunde hat storniert", "Übersetzer abgesprungen", "Falsche Sprachen", "Sonstiges");
        cancellationReason.setAllowCustomValue(true);
        cancellationReason.addCustomValueSetListener(e -> {
            cancellationReason.setValue(e.getDetail());
        });
        cancellationReason.setEnabled(isCancelledCb.getValue());
        isCancelledCb.addValueChangeListener(e -> cancellationReason.setEnabled(e.getValue()));

        HorizontalLayout cancelledLayout = new HorizontalLayout(cancelledDateLayout, cancellationReason);
        cancelledLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        TextField createdAtField = new TextField("Erstellt");
        createdAtField.setReadOnly(true);
        if (item.getCreatedAt() != null) {
            createdAtField.setValue(item.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }

        TextField updatedAtField = new TextField("Zuletzt bearbeitet");
        updatedAtField.setReadOnly(true);
        if (item.getUpdatedAt() != null) {
            updatedAtField.setValue(item.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }

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

        Checkbox invoiceReceivedCb = new Checkbox("Rechnungseingang");
        DateTimePicker invoiceReceivedAtPicker = new DateTimePicker();
        HorizontalLayout invoiceReceivedLayout = createToggleDate(invoiceReceivedCb, invoiceReceivedAtPicker);

        Checkbox isPaidCb = new Checkbox("Ausgezahlt am");
        DateTimePicker paidAtPicker = new DateTimePicker();
        HorizontalLayout paidLayout = createToggleDate(isPaidCb, paidAtPicker);

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

        // Layout Zusammenbau
        VerticalLayout statusCol = new VerticalLayout(
            new H3("Status"),
            assignedLayout,
            deadlineLayout,
            completedLayout,
            cancelledLayout,
            new HorizontalLayout(createdAtField, updatedAtField)
        );
        statusCol.setPadding(false);

        VerticalLayout rightCol = new VerticalLayout(
            new H3("Zuständigkeit"),
            translator,
            statusCol
        );
        rightCol.setPadding(false);

        VerticalLayout leftCol = new VerticalLayout(
            new H3("Allgemeine Informationen"),
            sourceLang, targetLang, activity, description,
            new H3("Umfang"),
            new HorizontalLayout(unit, wordCount, pageCount),
            new H3("Fakturierung"),
            new HorizontalLayout(unitPrice, surchargeOrDiscount, totalPriceField),
            new HorizontalLayout(invoiceReceivedLayout, paidLayout)
        );
        leftCol.setPadding(false);

        HorizontalLayout mainLayout = new HorizontalLayout(leftCol, rightCol);
        mainLayout.setWidthFull();
        mainLayout.setFlexGrow(1, leftCol);
        mainLayout.setFlexGrow(1, rightCol);

        add(mainLayout);

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

        binder.bind(isAssignedCb, TranslationOrderItem::isAssigned, TranslationOrderItem::setAssigned);
        binder.forField(assignedAtPicker).withConverter(new OffsetDateTimeConverter()).bind(TranslationOrderItem::getAssignedAt, TranslationOrderItem::setAssignedAt);
        
        binder.bind(hasDeadlineCb, TranslationOrderItem::isHasDeadline, TranslationOrderItem::setHasDeadline);
        binder.forField(deadlineAtPicker).withConverter(new OffsetDateTimeConverter()).bind(TranslationOrderItem::getDeadlineAt, TranslationOrderItem::setDeadlineAt);

        binder.bind(isCompletedCb, TranslationOrderItem::isCompleted, TranslationOrderItem::setCompleted);
        binder.forField(completedAtPicker).withConverter(new OffsetDateTimeConverter()).bind(TranslationOrderItem::getCompletedAt, TranslationOrderItem::setCompletedAt);

        binder.bind(isCancelledCb, TranslationOrderItem::isCancelled, TranslationOrderItem::setCancelled);
        binder.forField(cancelledAtPicker).withConverter(new OffsetDateTimeConverter()).bind(TranslationOrderItem::getCancelledAt, TranslationOrderItem::setCancelledAt);

        binder.bind(invoiceReceivedCb, TranslationOrderItem::isInvoiceReceived, TranslationOrderItem::setInvoiceReceived);
        binder.forField(invoiceReceivedAtPicker).withConverter(new OffsetDateTimeConverter()).bind(TranslationOrderItem::getInvoiceReceivedAt, TranslationOrderItem::setInvoiceReceivedAt);

        binder.bind(isPaidCb, TranslationOrderItem::isPaid, TranslationOrderItem::setPaid);
        binder.forField(paidAtPicker).withConverter(new OffsetDateTimeConverter()).bind(TranslationOrderItem::getPaidAt, TranslationOrderItem::setPaidAt);

        // Load values
        binder.readBean(item);
        if (item.getWordCount() != null) wordCount.setValue(item.getWordCount());
        if (item.getPageCount() != null) pageCount.setValue(item.getPageCount());
        
        // Initial state logic for pickers
        assignedAtPicker.setEnabled(item.isAssigned());
        deadlineAtPicker.setEnabled(item.isHasDeadline());
        completedAtPicker.setEnabled(item.isCompleted());
        cancelledAtPicker.setEnabled(item.isCancelled());
        invoiceReceivedAtPicker.setEnabled(item.isInvoiceReceived());
        paidAtPicker.setEnabled(item.isPaid());
        
        calculateTotal.run();

        // Actions
        Button saveBtn = new Button("Übernehmen", e -> {
            try {
                binder.writeBean(item);
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

    private HorizontalLayout createToggleDate(Checkbox cb, DateTimePicker dtp) {
        cb.addValueChangeListener(e -> {
            dtp.setEnabled(e.getValue());
            if (e.getValue() && dtp.getValue() == null) {
                dtp.setValue(LocalDateTime.now());
            } else if (!e.getValue()) {
                dtp.setValue(null);
            }
        });
        HorizontalLayout layout = new HorizontalLayout(cb, dtp);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        return layout;
    }

    private static class OffsetDateTimeConverter implements Converter<LocalDateTime, OffsetDateTime> {
        @Override
        public Result<OffsetDateTime> convertToModel(LocalDateTime localDateTime, ValueContext valueContext) {
            if (localDateTime == null) return Result.ok(null);
            return Result.ok(localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        }

        @Override
        public LocalDateTime convertToPresentation(OffsetDateTime offsetDateTime, ValueContext valueContext) {
            if (offsetDateTime == null) return null;
            return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
    }
}
