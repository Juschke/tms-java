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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.binder.Binder;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Consumer;

public class JobDialog extends Dialog {

    private final PricingService pricingService;
    private final PartnerService partnerService;
    private final TranslationOrderItem item;
    private final Consumer<TranslationOrderItem> onSave;

    private final Binder<TranslationOrderItem> binder = new Binder<>(TranslationOrderItem.class);

    private VerticalLayout contentArea;
    private Div specContent, priceContent, vendorContent, feedbackContent, deliveryContent;

    public JobDialog(PricingService pricingService, PartnerService partnerService,
            TranslationOrderItem item, UUID tenantId, Consumer<TranslationOrderItem> onSave) {
        this.pricingService = pricingService;
        this.partnerService = partnerService;
        this.item = item;
        this.onSave = onSave;

        setHeaderTitle(item.getId() == null ? "Neuer Job" : "Job-Details");
        setWidth("1000px");
        setMaxWidth("95vw");
        setHeight("auto");
        setMaxHeight("92vh");

        // UI Components
        ComboBox<Language> sourceLang = new ComboBox<>("Ausgangssprache");
        sourceLang.setItems(pricingService.getAllLanguages());
        sourceLang.setItemLabelGenerator(Language::getName);

        ComboBox<Language> targetLang = new ComboBox<>("Zielsprache");
        targetLang.setItems(pricingService.getAllLanguages());
        targetLang.setItemLabelGenerator(Language::getName);

        ComboBox<String> activity = new ComboBox<>("Tätigkeit");
        activity.setItems("Übersetzen", "Lektorieren", "Dolmetschen", "Beglaubigung");
        activity.setValue("Übersetzen");

        TextField description = new TextField("Job-Name / Beschreibung");
        description.setWidthFull();

        ComboBox<Partner> translator = new ComboBox<>("Übersetzer");
        translator.setItems(partnerService.getAllPartners(tenantId));
        translator.setItemLabelGenerator(Partner::getFullName);
        translator.setWidthFull();

        Button findPartnerBtn = new Button("Suchen & Vergleichen",
                com.vaadin.flow.component.icon.VaadinIcon.SEARCH.create());
        findPartnerBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        findPartnerBtn.addClickListener(e -> {
            new PartnerFinderDialog(
                    partnerService, tenantId,
                    sourceLang.getValue(), targetLang.getValue(),
                    selectedPartner -> translator.setValue(selectedPartner)).open();
        });

        HorizontalLayout translatorLayout = new HorizontalLayout(translator, findPartnerBtn);
        translatorLayout.setDefaultVerticalComponentAlignment(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);
        translatorLayout.setWidthFull();

        ToggleDateTimeField assignedField = new ToggleDateTimeField("Zuteilung am");
        ToggleDateTimeField deadlineField = new ToggleDateTimeField("Liefertermin (Soll)");
        ToggleDateTimeField completedField = new ToggleDateTimeField("Geliefert am (Ist)");
        ToggleDateTimeField cancelledField = new ToggleDateTimeField("Storniert");

        ComboBox<String> cancellationReason = new ComboBox<>("Grund für Reklamation / Storno");
        cancellationReason.setItems("Qualitätsmangel", "Terminverzug", "Kunde hat storniert", "Sonstiges");
        cancellationReason.setAllowCustomValue(true);
        cancellationReason.addCustomValueSetListener(e -> cancellationReason.setValue(e.getDetail()));
        cancellationReason.setWidthFull();

        ComboBox<String> unit = new ComboBox<>("Einheit");
        unit.setItems("WORDS", "PAGES", "FLAT_RATE");

        IntegerField wordCount = new IntegerField("Bruttomenge (Wörter)");
        BigDecimalField pageCount = new BigDecimalField("Bruttomenge (Seiten)");

        BigDecimalField unitPrice = new BigDecimalField("Preis/Einheit (€)");
        BigDecimalField surchargeOrDiscount = new BigDecimalField("Zuschlag / Abzug (€)");
        BigDecimalField totalPriceField = new BigDecimalField("Gesamtpreis (€)");
        totalPriceField.setReadOnly(true);

        Runnable calculateTotal = () -> {
            BigDecimal qty = BigDecimal.ONE;
            if ("WORDS".equals(unit.getValue()) && wordCount.getValue() != null) {
                qty = BigDecimal.valueOf(wordCount.getValue());
            } else if ("PAGES".equals(unit.getValue()) && pageCount.getValue() != null) {
                qty = pageCount.getValue();
            }
            BigDecimal uPrice = unitPrice.getValue() != null ? unitPrice.getValue() : BigDecimal.ZERO;
            BigDecimal extra = surchargeOrDiscount.getValue() != null ? surchargeOrDiscount.getValue()
                    : BigDecimal.ZERO;
            BigDecimal total = qty.multiply(uPrice).add(extra);
            totalPriceField.setValue(total);
        };

        unit.addValueChangeListener(e -> {
            boolean isWords = "WORDS".equals(e.getValue());
            boolean isPages = "PAGES".equals(e.getValue());
            wordCount.setEnabled(isWords);
            pageCount.setEnabled(isPages);
            if (!isWords)
                wordCount.setValue(null);
            if (!isPages)
                pageCount.setValue(null);
            calculateTotal.run();
        });

        wordCount.addValueChangeListener(e -> calculateTotal.run());
        pageCount.addValueChangeListener(e -> calculateTotal.run());
        unitPrice.addValueChangeListener(e -> calculateTotal.run());
        surchargeOrDiscount.addValueChangeListener(e -> calculateTotal.run());

        // Header
        H2 jobTitle = new H2(item.getDescription() != null ? item.getDescription() : "Neuer Job");
        jobTitle.getStyle().set("margin-top", "0").set("color", "var(--lumo-primary-color)");

        // Setup Tabs
        Tabs tabs = new Tabs();
        Tab specTab = new Tab("SPEZIFIKATION");
        Tab priceTab = new Tab("PREISE & ZEITEN");
        Tab vendorTab = new Tab("BEAUFTRAGUNG");
        Tab feedbackTab = new Tab("FEEDBACK & REKLAMATION");
        Tab deliveryTab = new Tab("LIEFERUNG");
        tabs.add(specTab, priceTab, vendorTab, feedbackTab, deliveryTab);

        contentArea = new VerticalLayout();
        contentArea.setPadding(false);

        // Content: Spezifikation
        specContent = new Div();
        FormLayout specForm = new FormLayout(description, activity, sourceLang, targetLang);
        specForm.setColspan(description, 2);
        specContent.add(specForm);

        // Content: Preise
        priceContent = new Div();
        FormLayout priceForm = new FormLayout(unit, unitPrice, wordCount, pageCount, surchargeOrDiscount,
                totalPriceField);
        priceContent.add(priceForm);
        priceContent.setVisible(false);

        // Content: Beauftragung
        vendorContent = new Div();
        FormLayout vendorForm = new FormLayout();
        vendorForm.add(translatorLayout);
        vendorForm.setColspan(translatorLayout, 2);
        vendorForm.add(assignedField, deadlineField);
        vendorContent.add(vendorForm);
        vendorContent.setVisible(false);

        // Content: Feedback
        feedbackContent = new Div();
        FormLayout feedbackForm = new FormLayout(cancelledField, cancellationReason);
        feedbackForm.setColspan(cancelledField, 2);
        feedbackForm.setColspan(cancellationReason, 2);
        feedbackContent.add(feedbackForm);
        feedbackContent.setVisible(false);

        // Content: Lieferung
        deliveryContent = new Div();
        FormLayout deliveryForm = new FormLayout(completedField);
        deliveryContent.add(deliveryForm);
        deliveryContent.setVisible(false);

        contentArea.add(specContent, priceContent, vendorContent, feedbackContent, deliveryContent);

        tabs.addSelectedChangeListener(event -> {
            specContent.setVisible(false);
            priceContent.setVisible(false);
            vendorContent.setVisible(false);
            feedbackContent.setVisible(false);
            deliveryContent.setVisible(false);

            if (event.getSelectedTab().equals(specTab))
                specContent.setVisible(true);
            else if (event.getSelectedTab().equals(priceTab))
                priceContent.setVisible(true);
            else if (event.getSelectedTab().equals(vendorTab))
                vendorContent.setVisible(true);
            else if (event.getSelectedTab().equals(feedbackTab))
                feedbackContent.setVisible(true);
            else if (event.getSelectedTab().equals(deliveryTab))
                deliveryContent.setVisible(true);
        });

        add(jobTitle, tabs, new com.vaadin.flow.component.html.Hr(), contentArea);

        // Binder configuration
        binder.forField(sourceLang).asRequired().bind(TranslationOrderItem::getSourceLanguage,
                TranslationOrderItem::setSourceLanguage);
        binder.forField(targetLang).asRequired().bind(TranslationOrderItem::getTargetLanguage,
                TranslationOrderItem::setTargetLanguage);
        binder.forField(activity).asRequired().bind(TranslationOrderItem::getActivity,
                TranslationOrderItem::setActivity);
        binder.forField(description).asRequired().bind(TranslationOrderItem::getDescription,
                TranslationOrderItem::setDescription);
        binder.bind(translator, TranslationOrderItem::getAssignedPartner, TranslationOrderItem::setAssignedPartner);
        binder.bind(unit, TranslationOrderItem::getUnit, TranslationOrderItem::setUnit);
        binder.bind(wordCount, TranslationOrderItem::getWordCount, TranslationOrderItem::setWordCount);
        binder.bind(pageCount, TranslationOrderItem::getPageCount, TranslationOrderItem::setPageCount);
        binder.bind(unitPrice, TranslationOrderItem::getUnitPrice, TranslationOrderItem::setUnitPrice);
        binder.bind(surchargeOrDiscount, TranslationOrderItem::getSurchargeOrDiscount,
                TranslationOrderItem::setSurchargeOrDiscount);
        binder.bind(cancellationReason, TranslationOrderItem::getCancellationReason,
                TranslationOrderItem::setCancellationReason);

        binder.forField(assignedField).bind(TranslationOrderItem::getAssignedAt, TranslationOrderItem::setAssignedAt);
        binder.forField(deadlineField).bind(TranslationOrderItem::getDeadlineAt, TranslationOrderItem::setDeadlineAt);
        binder.forField(completedField).bind(TranslationOrderItem::getCompletedAt,
                TranslationOrderItem::setCompletedAt);
        binder.forField(cancelledField).bind(TranslationOrderItem::getCancelledAt,
                TranslationOrderItem::setCancelledAt);

        cancellationReason.setEnabled(cancelledField.isChecked());
        cancelledField.addValueChangeListener(e -> cancellationReason.setEnabled(cancelledField.isChecked()));

        binder.readBean(item);
        if (item.getWordCount() != null)
            wordCount.setValue(item.getWordCount());
        if (item.getPageCount() != null)
            pageCount.setValue(item.getPageCount());
        cancellationReason.setEnabled(cancelledField.isChecked());

        calculateTotal.run();

        Button saveBtn = new Button("Übernehmen", e -> {
            try {
                binder.writeBean(item);
                item.setAssigned(assignedField.isChecked());
                item.setHasDeadline(deadlineField.isChecked());
                item.setCompleted(completedField.isChecked());
                item.setCancelled(cancelledField.isChecked());

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
}
