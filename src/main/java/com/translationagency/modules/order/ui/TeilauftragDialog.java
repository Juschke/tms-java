package com.translationagency.modules.order.ui;

import com.translationagency.modules.order.domain.TranslationOrderItem;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import java.math.BigDecimal;
import java.util.List;
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
        setWidth("800px");

        FormLayout formLayout = new FormLayout();

        // 1. Allgemeine Informationen
        ComboBox<Language> sourceLang = new ComboBox<>("Ausgangssprache");
        sourceLang.setItems(pricingService.getAllLanguages());
        sourceLang.setItemLabelGenerator(Language::getName);
        sourceLang.setRequired(true);

        ComboBox<Language> targetLang = new ComboBox<>("Zielsprache");
        targetLang.setItems(pricingService.getAllLanguages());
        targetLang.setItemLabelGenerator(Language::getName);
        targetLang.setRequired(true);

        ComboBox<String> activity = new ComboBox<>("Tätigkeit");
        activity.setItems("Übersetzen", "Lektorieren", "Dolmetschen", "Beglaubigung");
        activity.setValue("Übersetzen");
        activity.setRequired(true);

        TextField description = new TextField("Titel / Beschreibung");
        description.setRequired(true);

        // 2. Zuständigkeit
        ComboBox<Partner> translator = new ComboBox<>("Zuständiger Übersetzer");
        translator.setItems(partnerService.getAllPartners(tenantId));
        translator.setItemLabelGenerator(Partner::getFullName);

        // 3. Status
        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems("CREATED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        status.setValue("CREATED");

        // 4. Umfang
        ComboBox<String> unit = new ComboBox<>("Einheit");
        unit.setItems("WORDS", "PAGES", "FLAT_RATE");
        unit.setValue("WORDS");

        IntegerField wordCount = new IntegerField("Wortanzahl");
        BigDecimalField pageCount = new BigDecimalField("Seitenanzahl");

        // 5. Fakturierung
        BigDecimalField unitPrice = new BigDecimalField("Preis pro Einheit (EUR)");
        BigDecimalField surchargeOrDiscount = new BigDecimalField("Zuschlag / Abzug (EUR)");
        BigDecimalField totalPriceField = new BigDecimalField("Gesamtpreis (berechnet, EUR)");
        totalPriceField.setReadOnly(true);

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
            if ("WORDS".equals(e.getValue())) {
                wordCount.setEnabled(true);
                pageCount.setEnabled(false);
                pageCount.setValue(null);
            } else if ("PAGES".equals(e.getValue())) {
                wordCount.setEnabled(false);
                wordCount.setValue(null);
                pageCount.setEnabled(true);
            } else {
                wordCount.setEnabled(false);
                pageCount.setEnabled(false);
                wordCount.setValue(null);
                pageCount.setValue(null);
            }
            calculateTotal.run();
        });

        wordCount.addValueChangeListener(e -> calculateTotal.run());
        pageCount.addValueChangeListener(e -> calculateTotal.run());
        unitPrice.addValueChangeListener(e -> calculateTotal.run());
        surchargeOrDiscount.addValueChangeListener(e -> calculateTotal.run());

        formLayout.add(
                new H3("Allgemeine Informationen"), sourceLang, targetLang, activity, description,
                new H3("Zuständigkeit & Status"), translator, status,
                new H3("Umfang & Kalkulation"), unit, wordCount, pageCount, unitPrice, surchargeOrDiscount, totalPriceField
        );

        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("800px", 3)
        );

        // Span headers
        formLayout.setColspan(formLayout.getChildren().filter(c -> c instanceof H3).toArray(H3[]::new)[0], 3);
        formLayout.setColspan(formLayout.getChildren().filter(c -> c instanceof H3).toArray(H3[]::new)[1], 3);
        formLayout.setColspan(formLayout.getChildren().filter(c -> c instanceof H3).toArray(H3[]::new)[2], 3);
        formLayout.setColspan(description, 2);

        // Binder configuration
        binder.forField(sourceLang).asRequired("Ausgangssprache erforderlich").bind(TranslationOrderItem::getSourceLanguage, TranslationOrderItem::setSourceLanguage);
        binder.forField(targetLang).asRequired("Zielsprache erforderlich").bind(TranslationOrderItem::getTargetLanguage, TranslationOrderItem::setTargetLanguage);
        binder.forField(activity).asRequired("Tätigkeit erforderlich").bind(TranslationOrderItem::getActivity, TranslationOrderItem::setActivity);
        binder.forField(description).asRequired("Beschreibung erforderlich").bind(TranslationOrderItem::getDescription, TranslationOrderItem::setDescription);
        binder.bind(translator, TranslationOrderItem::getAssignedPartner, TranslationOrderItem::setAssignedPartner);
        binder.bind(status, TranslationOrderItem::getStatus, TranslationOrderItem::setStatus);
        binder.bind(unit, TranslationOrderItem::getUnit, TranslationOrderItem::setUnit);
        binder.bind(wordCount, TranslationOrderItem::getWordCount, TranslationOrderItem::setWordCount);
        binder.bind(pageCount, TranslationOrderItem::getPageCount, TranslationOrderItem::setPageCount);
        binder.bind(unitPrice, TranslationOrderItem::getUnitPrice, TranslationOrderItem::setUnitPrice);
        binder.bind(surchargeOrDiscount, TranslationOrderItem::getSurchargeOrDiscount, TranslationOrderItem::setSurchargeOrDiscount);

        // Load values
        binder.readBean(item);
        if (item.getWordCount() != null) {
            wordCount.setValue(item.getWordCount());
        }
        if (item.getPageCount() != null) {
            pageCount.setValue(item.getPageCount());
        }
        calculateTotal.run();

        // Actions
        Button saveBtn = new Button("Übernehmen", e -> {
            try {
                binder.writeBean(item);
                
                // Set the calculated values to quantity/totalPrice for base columns compatibility
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
                Notification.show("Fehler beim Validieren der Eingabe: " + ex.getMessage());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> close());
        getFooter().add(cancelBtn, saveBtn);
        add(formLayout);
    }
}
