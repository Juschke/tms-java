package com.translationagency.modules.settings.ui;

import com.translationagency.modules.billing.application.DunningService;
import com.translationagency.modules.billing.domain.DunningSetting;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.pricing.domain.ServiceType;
import com.translationagency.modules.pricing.domain.Currency;
import com.translationagency.modules.pricing.domain.Unit;
import com.translationagency.modules.settings.application.SettingsService;
import com.translationagency.modules.settings.domain.AccountingScheme;
import com.translationagency.modules.settings.domain.TenantSettings;
import com.translationagency.modules.settings.domain.TextTemplate;
import com.translationagency.modules.settings.domain.TextTemplateType;
import com.translationagency.modules.tenant.application.NumberRangeService;
import com.translationagency.modules.tenant.domain.NumberRange;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Einstellungen | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER"})
public class SettingsView extends VerticalLayout {

    private final SettingsService settingsService;
    private final PricingService pricingService;
    private final NumberRangeService numberRangeService;
    private final DunningService dunningService;
    private final SecurityService securityService;

    private Tenant currentTenant;
    private TenantSettings tenantSettings;

    private final Tabs tabs = new Tabs();
    private final Div contentContainer = new Div();

    private VerticalLayout tenantLayout;
    private VerticalLayout masterDataLayout;
    private VerticalLayout numberRangesLayout;
    private VerticalLayout templatesLayout;
    private VerticalLayout dunningSettingsLayout;

    private final Grid<Language> languageGrid = new Grid<>(Language.class, false);
    private final Grid<ServiceType> serviceTypeGrid = new Grid<>(ServiceType.class, false);
    private final Grid<Currency> currencyGrid = new Grid<>(Currency.class, false);
    private final Grid<Unit> unitGrid = new Grid<>(Unit.class, false);
    private final Grid<NumberRange> numberRangeGrid = new Grid<>(NumberRange.class, false);
    private final Grid<TextTemplate> templateGrid = new Grid<>(TextTemplate.class, false);

    public SettingsView(SettingsService settingsService,
                        PricingService pricingService,
                        NumberRangeService numberRangeService,
                        DunningService dunningService,
                        SecurityService securityService) {
        this.settingsService = settingsService;
        this.pricingService = pricingService;
        this.numberRangeService = numberRangeService;
        this.dunningService = dunningService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant()
                .map(Tenant::getId)
                .ifPresent(tenantId -> this.currentTenant = settingsService.getTenant(tenantId));

        H2 headerTitle = new H2("Einstellungen");
        headerTitle.addClassNames("m-0", "text-xl");
        add(headerTitle);

        if (currentTenant == null) {
            add(new Span("Kein Mandant fuer den aktuellen Benutzer gefunden."));
            return;
        }

        tenantSettings = settingsService.getOrCreateTenantSettings(currentTenant.getId());

        createTabs();
        setupTabContents();

        add(tabs, contentContainer);
        contentContainer.setSizeFull();
    }

    private void createTabs() {
        Tab tenantTab = new Tab("Mandant");
        Tab masterDataTab = new Tab("Stammdaten");
        Tab numberRangesTab = new Tab("Nummernkreise");
        Tab templatesTab = new Tab("Textvorlagen");
        Tab dunningSettingsTab = new Tab("Mahneinstellungen");

        tabs.add(tenantTab, masterDataTab, numberRangesTab, templatesTab, dunningSettingsTab);
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(tenantTab)) {
                contentContainer.add(tenantLayout);
            } else if (event.getSelectedTab().equals(masterDataTab)) {
                contentContainer.add(masterDataLayout);
                refreshMasterData();
            } else if (event.getSelectedTab().equals(numberRangesTab)) {
                contentContainer.add(numberRangesLayout);
                refreshNumberRanges();
            } else if (event.getSelectedTab().equals(templatesTab)) {
                contentContainer.add(templatesLayout);
                refreshTextTemplates();
            } else if (event.getSelectedTab().equals(dunningSettingsTab)) {
                contentContainer.add(dunningSettingsLayout);
            }
        });
    }

    private void setupTabContents() {
        setupTenantTab();
        setupMasterDataTab();
        setupNumberRangesTab();
        setupTextTemplatesTab();
        setupDunningSettingsTab();

        contentContainer.removeAll();
        contentContainer.add(tenantLayout);
        tabs.setSelectedIndex(0);
    }

    private void setupTenantTab() {
        tenantLayout = new VerticalLayout();
        tenantLayout.setWidthFull();
        tenantLayout.setPadding(false);

        TextField tenantName = new TextField("Mandantenname");
        TextField subdomain = new TextField("Subdomain");
        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems("ACTIVE", "INACTIVE");

        TextField companyName = new TextField("Firmenname");
        TextField street = new TextField("Strasse");
        TextField zip = new TextField("PLZ");
        TextField city = new TextField("Ort");
        TextField country = new TextField("Land");
        TextField phone = new TextField("Telefon");
        EmailField email = new EmailField("E-Mail");
        TextField website = new TextField("Website");
        TextField taxNumber = new TextField("Steuernummer");
        TextField vatId = new TextField("USt-ID");

        TextField currency = new TextField("Waehrung");
        BigDecimalField vatPercent = new BigDecimalField("MwSt. (%)");
        IntegerField paymentTerms = new IntegerField("Zahlungsziel (Tage)");
        IntegerField quoteValidity = new IntegerField("Angebotsgueltigkeit (Tage)");
        ComboBox<String> deliveryMethod = new ComboBox<>("Standard-Lieferweg");
        deliveryMethod.setItems("EMAIL", "POST", "PICKUP");
        ComboBox<AccountingScheme> accountingScheme = new ComboBox<>("Kontenplan");
        accountingScheme.setItems(AccountingScheme.values());
        accountingScheme.setItemLabelGenerator(AccountingScheme::getLabel);

        TextField emailSenderName = new TextField("E-Mail Absendername");
        EmailField emailSenderAddress = new EmailField("E-Mail Absenderadresse");
        TextField bankName = new TextField("Bank");
        TextField iban = new TextField("IBAN");
        TextField bic = new TextField("BIC");
        TextArea quoteFooter = new TextArea("Angebots-Fusstext");
        TextArea invoiceFooter = new TextArea("Rechnungs-Fusstext");
        TextArea orderFooter = new TextArea("Auftrags-Fusstext");

        tenantName.setValue(valueOrEmpty(currentTenant.getName()));
        subdomain.setValue(valueOrEmpty(currentTenant.getSubdomain()));
        status.setValue(valueOrDefault(currentTenant.getStatus(), "ACTIVE"));
        companyName.setValue(valueOrEmpty(tenantSettings.getCompanyName()));
        street.setValue(valueOrEmpty(tenantSettings.getStreet()));
        zip.setValue(valueOrEmpty(tenantSettings.getZip()));
        city.setValue(valueOrEmpty(tenantSettings.getCity()));
        country.setValue(valueOrDefault(tenantSettings.getCountry(), "DE"));
        phone.setValue(valueOrEmpty(tenantSettings.getPhone()));
        email.setValue(valueOrEmpty(tenantSettings.getEmail()));
        website.setValue(valueOrEmpty(tenantSettings.getWebsite()));
        taxNumber.setValue(valueOrEmpty(tenantSettings.getTaxNumber()));
        vatId.setValue(valueOrEmpty(tenantSettings.getVatId()));
        currency.setValue(valueOrDefault(tenantSettings.getDefaultCurrency(), "EUR"));
        vatPercent.setValue(valueOrDefault(tenantSettings.getVatPercent(), BigDecimal.valueOf(19.00)));
        paymentTerms.setValue(tenantSettings.getDefaultPaymentTermsDays());
        quoteValidity.setValue(tenantSettings.getDefaultQuoteValidityDays());
        deliveryMethod.setValue(valueOrDefault(tenantSettings.getDefaultDeliveryMethod(), "EMAIL"));
        accountingScheme.setValue(valueOrDefault(tenantSettings.getAccountingScheme(), AccountingScheme.SKR03));
        emailSenderName.setValue(valueOrEmpty(tenantSettings.getEmailSenderName()));
        emailSenderAddress.setValue(valueOrEmpty(tenantSettings.getEmailSenderAddress()));
        bankName.setValue(valueOrEmpty(tenantSettings.getBankName()));
        iban.setValue(valueOrEmpty(tenantSettings.getIban()));
        bic.setValue(valueOrEmpty(tenantSettings.getBic()));
        quoteFooter.setValue(valueOrEmpty(tenantSettings.getQuoteFooter()));
        invoiceFooter.setValue(valueOrEmpty(tenantSettings.getInvoiceFooter()));
        orderFooter.setValue(valueOrEmpty(tenantSettings.getOrderFooter()));

        quoteFooter.setMinHeight("120px");
        invoiceFooter.setMinHeight("120px");
        orderFooter.setMinHeight("120px");

        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.add(
                tenantName, subdomain, status,
                companyName, street, zip, city, country,
                phone, email, website, taxNumber, vatId,
                currency, vatPercent, paymentTerms, quoteValidity, deliveryMethod,
                accountingScheme,
                emailSenderName, emailSenderAddress, bankName, iban, bic,
                quoteFooter, invoiceFooter, orderFooter
        );
        form.setColspan(quoteFooter, 2);
        form.setColspan(invoiceFooter, 2);
        form.setColspan(orderFooter, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("720px", 2));

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            if (isBlank(tenantName.getValue()) || isBlank(subdomain.getValue())) {
                Notification.show("Mandantenname und Subdomain sind erforderlich").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            currentTenant.setName(tenantName.getValue().trim());
            currentTenant.setSubdomain(subdomain.getValue().trim());
            currentTenant.setStatus(valueOrDefault(status.getValue(), "ACTIVE"));

            tenantSettings.setCompanyName(companyName.getValue());
            tenantSettings.setStreet(street.getValue());
            tenantSettings.setZip(zip.getValue());
            tenantSettings.setCity(city.getValue());
            tenantSettings.setCountry(country.getValue());
            tenantSettings.setPhone(phone.getValue());
            tenantSettings.setEmail(email.getValue());
            tenantSettings.setWebsite(website.getValue());
            tenantSettings.setTaxNumber(taxNumber.getValue());
            tenantSettings.setVatId(vatId.getValue());
            tenantSettings.setDefaultCurrency(valueOrDefault(currency.getValue(), "EUR").trim().toUpperCase());
            tenantSettings.setVatPercent(valueOrDefault(vatPercent.getValue(), BigDecimal.valueOf(19.00)));
            tenantSettings.setDefaultPaymentTermsDays(valueOrDefault(paymentTerms.getValue(), 14));
            tenantSettings.setDefaultQuoteValidityDays(valueOrDefault(quoteValidity.getValue(), 14));
            tenantSettings.setDefaultDeliveryMethod(valueOrDefault(deliveryMethod.getValue(), "EMAIL"));
            tenantSettings.setAccountingScheme(valueOrDefault(accountingScheme.getValue(), AccountingScheme.SKR03));
            tenantSettings.setEmailSenderName(emailSenderName.getValue());
            tenantSettings.setEmailSenderAddress(emailSenderAddress.getValue());
            tenantSettings.setBankName(bankName.getValue());
            tenantSettings.setIban(iban.getValue());
            tenantSettings.setBic(bic.getValue());
            tenantSettings.setQuoteFooter(quoteFooter.getValue());
            tenantSettings.setInvoiceFooter(invoiceFooter.getValue());
            tenantSettings.setOrderFooter(orderFooter.getValue());

            String username = getCurrentUsername();
            settingsService.saveTenant(currentTenant, username);
            tenantSettings = settingsService.saveTenantSettings(tenantSettings, username);
            Notification.show("Einstellungen gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        tenantLayout.add(form, saveBtn);
    }

    private void setupMasterDataTab() {
        masterDataLayout = new VerticalLayout();
        masterDataLayout.setSizeFull();
        masterDataLayout.setPadding(false);

        VerticalLayout languageSection = new VerticalLayout();
        languageSection.setPadding(false);
        languageSection.setWidthFull();
        Button addLanguageBtn = new Button("Sprache", VaadinIcon.PLUS.create(), e -> openLanguageDialog(new Language()));
        addLanguageBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        languageGrid.setWidthFull();
        languageGrid.addColumn(Language::getCode).setHeader("Code").setAutoWidth(true).setSortable(true);
        languageGrid.addColumn(Language::getName).setHeader("Sprache").setAutoWidth(true).setSortable(true);
        languageGrid.addComponentColumn(language -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openLanguageDialog(language));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Bearbeiten");
            return editBtn;
        }).setHeader("Aktionen").setAutoWidth(true);
        languageSection.add(createSectionHeader("Sprachen", addLanguageBtn), languageGrid);

        VerticalLayout serviceSection = new VerticalLayout();
        serviceSection.setPadding(false);
        serviceSection.setWidthFull();
        Button addServiceBtn = new Button("Leistungsart", VaadinIcon.PLUS.create(), e -> openServiceTypeDialog(new ServiceType()));
        addServiceBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        serviceTypeGrid.setWidthFull();
        serviceTypeGrid.addColumn(ServiceType::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        serviceTypeGrid.addColumn(ServiceType::getDescription).setHeader("Beschreibung").setAutoWidth(true);
        serviceTypeGrid.addComponentColumn(serviceType -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openServiceTypeDialog(serviceType));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Bearbeiten");
            return editBtn;
        }).setHeader("Aktionen").setAutoWidth(true);
        serviceSection.add(createSectionHeader("Leistungsarten", addServiceBtn), serviceTypeGrid);

        HorizontalLayout topSections = new HorizontalLayout(languageSection, serviceSection);
        topSections.setWidthFull();
        topSections.setSpacing(true);
        topSections.setFlexGrow(1, languageSection, serviceSection);

        VerticalLayout currencySection = new VerticalLayout();
        currencySection.setPadding(false);
        currencySection.setWidthFull();
        Button addCurrencyBtn = new Button("Waehrung", VaadinIcon.PLUS.create(), e -> openCurrencyDialog(new Currency()));
        addCurrencyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        currencyGrid.setWidthFull();
        currencyGrid.addColumn(Currency::getCode).setHeader("Code").setAutoWidth(true).setSortable(true);
        currencyGrid.addColumn(Currency::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        currencyGrid.addColumn(Currency::getSymbol).setHeader("Symbol").setAutoWidth(true);
        currencyGrid.addComponentColumn(currency -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openCurrencyDialog(currency));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Bearbeiten");
            return editBtn;
        }).setHeader("Aktionen").setAutoWidth(true);
        currencySection.add(createSectionHeader("Waehrungen", addCurrencyBtn), currencyGrid);

        VerticalLayout unitSection = new VerticalLayout();
        unitSection.setPadding(false);
        unitSection.setWidthFull();
        Button addUnitBtn = new Button("Einheit", VaadinIcon.PLUS.create(), e -> openUnitDialog(new Unit()));
        addUnitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        unitGrid.setWidthFull();
        unitGrid.addColumn(Unit::getCode).setHeader("Code").setAutoWidth(true).setSortable(true);
        unitGrid.addColumn(Unit::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        unitGrid.addComponentColumn(unit -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openUnitDialog(unit));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Bearbeiten");
            return editBtn;
        }).setHeader("Aktionen").setAutoWidth(true);
        unitSection.add(createSectionHeader("Einheiten", addUnitBtn), unitGrid);

        HorizontalLayout bottomSections = new HorizontalLayout(currencySection, unitSection);
        bottomSections.setWidthFull();
        bottomSections.setSpacing(true);
        bottomSections.setFlexGrow(1, currencySection, unitSection);

        masterDataLayout.add(topSections, bottomSections);
        refreshMasterData();
    }

    private void setupNumberRangesTab() {
        numberRangesLayout = new VerticalLayout();
        numberRangesLayout.setSizeFull();
        numberRangesLayout.setPadding(false);

        VerticalLayout numberRangeSection = new VerticalLayout();
        numberRangeSection.setPadding(false);
        numberRangeSection.setWidthFull();
        numberRangeGrid.setWidthFull();
        numberRangeGrid.addColumn(nr -> getEntityTypeLabel(nr.getEntityType())).setHeader("Dokumenttyp").setAutoWidth(true);
        numberRangeGrid.addColumn(NumberRange::getPrefix).setHeader("Praefix").setAutoWidth(true);
        numberRangeGrid.addColumn(NumberRange::getCurrentNumber).setHeader("Zaehlerstand").setAutoWidth(true);
        numberRangeGrid.addColumn(NumberRange::getIncrement).setHeader("Schritt").setAutoWidth(true);
        numberRangeGrid.addColumn(NumberRange::getPadding).setHeader("Stellen").setAutoWidth(true);
        numberRangeGrid.addColumn(nr -> nr.isResetYearly() ? "Ja" : "Nein").setHeader("Jaehrlicher Reset").setAutoWidth(true);
        numberRangeGrid.addColumn(nr -> numberRangeService.previewNextNumber(currentTenant.getId(), nr.getEntityType())).setHeader("Naechste Nummer").setAutoWidth(true);
        numberRangeGrid.addComponentColumn(range -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openNumberRangeDialog(range));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Bearbeiten");
            return editBtn;
        }).setHeader("Aktionen").setAutoWidth(true);
        
        Button addNumberRangeBtn = new Button("Nummernkreis", VaadinIcon.PLUS.create(), e -> openNumberRangeDialog(new NumberRange()));
        addNumberRangeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        numberRangeSection.add(createSectionHeader("Nummernkreise", addNumberRangeBtn), numberRangeGrid);

        numberRangesLayout.add(numberRangeSection);
        refreshNumberRanges();
    }

    private void setupTextTemplatesTab() {
        templatesLayout = new VerticalLayout();
        templatesLayout.setSizeFull();
        templatesLayout.setPadding(false);

        Button addTemplateBtn = new Button("Textvorlage", VaadinIcon.PLUS.create(), e -> openTextTemplateDialog(new TextTemplate()));
        addTemplateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        templateGrid.setSizeFull();
        templateGrid.addColumn(template -> template.getTemplateType() != null ? template.getTemplateType().getLabel() : "-")
                .setHeader("Typ").setAutoWidth(true).setSortable(true);
        templateGrid.addColumn(TextTemplate::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        templateGrid.addColumn(template -> valueOrEmpty(template.getSubject())).setHeader("Betreff").setAutoWidth(true);
        templateGrid.addColumn(template -> template.isActive() ? "Aktiv" : "Inaktiv").setHeader("Status").setAutoWidth(true);
        templateGrid.addColumn(template -> template.getUpdatedAt() != null
                ? template.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "-").setHeader("Geaendert").setAutoWidth(true);
        templateGrid.addComponentColumn(template -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openTextTemplateDialog(template));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Bearbeiten");

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> deleteTextTemplate(template));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.setTooltipText("Loeschen");

            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Aktionen").setAutoWidth(true);

        templatesLayout.add(addTemplateBtn, templateGrid);
        refreshTextTemplates();
    }

    private void setupDunningSettingsTab() {
        dunningSettingsLayout = new VerticalLayout();
        dunningSettingsLayout.setWidthFull();
        dunningSettingsLayout.setPadding(false);

        DunningSetting dunningSettings = dunningService.getDunningSettings(currentTenant.getId());
        TextTemplate level1Template = settingsService.getOrCreateTextTemplate(currentTenant.getId(), TextTemplateType.DUNNING_LEVEL_1);
        TextTemplate level2Template = settingsService.getOrCreateTextTemplate(currentTenant.getId(), TextTemplateType.DUNNING_LEVEL_2);
        TextTemplate level3Template = settingsService.getOrCreateTextTemplate(currentTenant.getId(), TextTemplateType.DUNNING_LEVEL_3);

        Checkbox enabled = new Checkbox("Mahnwesen aktiv");
        BigDecimalField feePerLevel = new BigDecimalField("Mahngebuehr pro Stufe (EUR)");
        IntegerField daysLevel1 = new IntegerField("Tage ueberfaellig Stufe 1");
        IntegerField daysLevel2 = new IntegerField("Tage ueberfaellig Stufe 2");
        IntegerField daysLevel3 = new IntegerField("Tage ueberfaellig Stufe 3");

        enabled.setValue(dunningSettings.isEnabled());
        feePerLevel.setValue(valueOrDefault(dunningSettings.getFeePerLevel(), BigDecimal.valueOf(5.00)));
        daysLevel1.setValue(dunningSettings.getDaysOverdueLevel1());
        daysLevel2.setValue(dunningSettings.getDaysOverdueLevel2());
        daysLevel3.setValue(dunningSettings.getDaysOverdueLevel3());

        FormLayout settingsForm = new FormLayout();
        settingsForm.add(enabled, feePerLevel, daysLevel1, daysLevel2, daysLevel3);
        settingsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("720px", 2));

        TextField subjectLevel1 = new TextField("Betreff Stufe 1");
        TextField subjectLevel2 = new TextField("Betreff Stufe 2");
        TextField subjectLevel3 = new TextField("Betreff Stufe 3");
        TextArea bodyLevel1 = new TextArea("Text Stufe 1");
        TextArea bodyLevel2 = new TextArea("Text Stufe 2");
        TextArea bodyLevel3 = new TextArea("Text Stufe 3");

        subjectLevel1.setValue(valueOrEmpty(level1Template.getSubject()));
        subjectLevel2.setValue(valueOrEmpty(level2Template.getSubject()));
        subjectLevel3.setValue(valueOrEmpty(level3Template.getSubject()));
        bodyLevel1.setValue(valueOrEmpty(level1Template.getBody()));
        bodyLevel2.setValue(valueOrEmpty(level2Template.getBody()));
        bodyLevel3.setValue(valueOrEmpty(level3Template.getBody()));

        bodyLevel1.setMinHeight("180px");
        bodyLevel2.setMinHeight("180px");
        bodyLevel3.setMinHeight("180px");
        bodyLevel1.setPlaceholder("Platzhalter: {kunde}, {rechnungsnummer}, {faelligkeit}");
        bodyLevel2.setPlaceholder("Platzhalter: {kunde}, {rechnungsnummer}, {faelligkeit}");
        bodyLevel3.setPlaceholder("Platzhalter: {kunde}, {rechnungsnummer}, {faelligkeit}");

        FormLayout textForm = new FormLayout();
        textForm.add(subjectLevel1, subjectLevel2, subjectLevel3, bodyLevel1, bodyLevel2, bodyLevel3);
        textForm.setColspan(bodyLevel1, 2);
        textForm.setColspan(bodyLevel2, 2);
        textForm.setColspan(bodyLevel3, 2);
        textForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("760px", 2));

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            dunningSettings.setEnabled(enabled.getValue());
            dunningSettings.setFeePerLevel(valueOrDefault(feePerLevel.getValue(), BigDecimal.ZERO));
            dunningSettings.setDaysOverdueLevel1(valueOrDefault(daysLevel1.getValue(), 3));
            dunningSettings.setDaysOverdueLevel2(valueOrDefault(daysLevel2.getValue(), 10));
            dunningSettings.setDaysOverdueLevel3(valueOrDefault(daysLevel3.getValue(), 20));
            dunningService.saveSettings(dunningSettings);

            String username = getCurrentUsername();
            saveDunningTemplate(level1Template, TextTemplateType.DUNNING_LEVEL_1, "Zahlungserinnerung", subjectLevel1.getValue(), bodyLevel1.getValue(), username);
            saveDunningTemplate(level2Template, TextTemplateType.DUNNING_LEVEL_2, "Mahnung Stufe 2", subjectLevel2.getValue(), bodyLevel2.getValue(), username);
            saveDunningTemplate(level3Template, TextTemplateType.DUNNING_LEVEL_3, "Letzte Mahnung", subjectLevel3.getValue(), bodyLevel3.getValue(), username);

            refreshTextTemplates();
            Notification.show("Mahneinstellungen gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        H3 dunningHeader = new H3("Mahnwesen");
        H3 textHeader = new H3("Mahnungstexte");
        dunningSettingsLayout.add(dunningHeader, settingsForm, textHeader, textForm, saveBtn);
    }

    private void saveDunningTemplate(TextTemplate template, TextTemplateType type, String name, String subject, String body, String username) {
        template.setTemplateType(type);
        template.setName(name);
        template.setSubject(valueOrEmpty(subject));
        template.setBody(valueOrEmpty(body));
        template.setActive(true);
        settingsService.saveTextTemplate(template, currentTenant.getId(), username);
    }

    private HorizontalLayout createSectionHeader(String title, Button button) {
        H3 heading = new H3(title);
        heading.addClassNames("m-0");
        HorizontalLayout header = new HorizontalLayout(heading, button);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(heading);
        return header;
    }

    private void refreshMasterData() {
        languageGrid.setItems(pricingService.getAllLanguages());
        serviceTypeGrid.setItems(pricingService.getAllServiceTypes());
        currencyGrid.setItems(pricingService.getAllCurrencies());
        unitGrid.setItems(pricingService.getAllUnits());
    }

    private void refreshNumberRanges() {
        numberRangeGrid.setItems(numberRangeService.getRangesForTenant(currentTenant.getId()));
    }

    private void refreshTextTemplates() {
        templateGrid.setItems(settingsService.getTextTemplates(currentTenant.getId()));
    }

    private void openLanguageDialog(Language language) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(language.getId() == null ? "Sprache anlegen" : "Sprache bearbeiten");
        dialog.setWidth("420px");

        TextField code = new TextField("Code");
        TextField name = new TextField("Name");
        code.setValue(valueOrEmpty(language.getCode()));
        name.setValue(valueOrEmpty(language.getName()));

        FormLayout form = new FormLayout(code, name);

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            if (isBlank(code.getValue()) || isBlank(name.getValue())) {
                Notification.show("Code und Name sind erforderlich").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (language.getId() == null) {
                language.setId(UUID.randomUUID());
            }
            language.setCode(code.getValue().trim());
            language.setName(name.getValue().trim());
            pricingService.saveLanguage(language);
            dialog.close();
            refreshMasterData();
            Notification.show("Sprache gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), saveBtn);
        dialog.add(form);
        dialog.open();
    }

    private void openServiceTypeDialog(ServiceType serviceType) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(serviceType.getId() == null ? "Leistungsart anlegen" : "Leistungsart bearbeiten");
        dialog.setWidth("520px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Beschreibung");
        name.setValue(valueOrEmpty(serviceType.getName()));
        description.setValue(valueOrEmpty(serviceType.getDescription()));
        description.setMinHeight("120px");

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("520px", 2));

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            if (isBlank(name.getValue())) {
                Notification.show("Name ist erforderlich").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (serviceType.getId() == null) {
                serviceType.setId(UUID.randomUUID());
            }
            serviceType.setName(name.getValue().trim());
            serviceType.setDescription(description.getValue());
            pricingService.saveServiceType(serviceType);
            dialog.close();
            refreshMasterData();
            Notification.show("Leistungsart gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), saveBtn);
        dialog.add(form);
        dialog.open();
    }

    private void openNumberRangeDialog(NumberRange range) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nummernkreis bearbeiten");
        dialog.setWidth("520px");

        TextField entityType = new TextField("Dokumenttyp");
        entityType.setValue(getEntityTypeLabel(range.getEntityType()));
        entityType.setReadOnly(true);

        TextField prefix = new TextField("Praefix");
        IntegerField currentNumber = new IntegerField("Aktueller Zaehlerstand");
        IntegerField increment = new IntegerField("Schrittweite");
        IntegerField padding = new IntegerField("Zaehlerlaenge");
        TextField separator = new TextField("Trennzeichen");
        Checkbox yearBased = new Checkbox("Jahressegment verwenden");
        Checkbox resetYearly = new Checkbox("Jaehrlich zuruecksetzen");

        prefix.setValue(valueOrEmpty(range.getPrefix()));
        currentNumber.setValue(range.getCurrentNumber());
        increment.setValue(range.getIncrement());
        padding.setValue(range.getPadding());
        separator.setValue(valueOrEmpty(range.getSeparator()));
        yearBased.setValue(range.isYearBased());
        resetYearly.setValue(range.isResetYearly());

        FormLayout form = new FormLayout(entityType, prefix, currentNumber, increment, padding, separator, yearBased, resetYearly);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("520px", 2));

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            range.setPrefix(prefix.getValue());
            range.setCurrentNumber(valueOrDefault(currentNumber.getValue(), 0));
            range.setIncrement(valueOrDefault(increment.getValue(), 1));
            range.setPadding(valueOrDefault(padding.getValue(), 4));
            range.setSeparator(separator.getValue());
            range.setYearBased(yearBased.getValue());
            range.setResetYearly(resetYearly.getValue());
            
            if (range.getTenant() == null) {
                range.setTenant(currentTenant);
            }
            if (range.getId() == null) {
                range.setId(UUID.randomUUID());
            }

            numberRangeService.saveRange(range);
            dialog.close();
            refreshNumberRanges();
            Notification.show("Nummernkreis gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), saveBtn);
        dialog.add(form);
        dialog.open();
    }

    private void openCurrencyDialog(Currency currency) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(currency.getId() == null ? "Waehrung anlegen" : "Waehrung bearbeiten");
        dialog.setWidth("420px");

        TextField code = new TextField("Code (z.B. EUR)");
        TextField name = new TextField("Name (z.B. Euro)");
        TextField symbol = new TextField("Symbol (z.B. €)");

        code.setValue(valueOrEmpty(currency.getCode()));
        name.setValue(valueOrEmpty(currency.getName()));
        symbol.setValue(valueOrEmpty(currency.getSymbol()));

        FormLayout form = new FormLayout(code, name, symbol);

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            if (isBlank(code.getValue()) || isBlank(name.getValue())) {
                Notification.show("Code und Name sind erforderlich").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (currency.getId() == null) {
                currency.setId(UUID.randomUUID());
            }
            currency.setCode(code.getValue().trim().toUpperCase());
            currency.setName(name.getValue().trim());
            currency.setSymbol(symbol.getValue().trim());
            pricingService.saveCurrency(currency);
            dialog.close();
            refreshMasterData();
            Notification.show("Waehrung gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), saveBtn);
        dialog.add(form);
        dialog.open();
    }

    private void openUnitDialog(Unit unit) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(unit.getId() == null ? "Einheit anlegen" : "Einheit bearbeiten");
        dialog.setWidth("420px");

        TextField code = new TextField("Code (z.B. WORDS)");
        TextField name = new TextField("Name (z.B. Wörter)");

        code.setValue(valueOrEmpty(unit.getCode()));
        name.setValue(valueOrEmpty(unit.getName()));

        FormLayout form = new FormLayout(code, name);

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            if (isBlank(code.getValue()) || isBlank(name.getValue())) {
                Notification.show("Code und Name sind erforderlich").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (unit.getId() == null) {
                unit.setId(UUID.randomUUID());
            }
            unit.setCode(code.getValue().trim().toUpperCase());
            unit.setName(name.getValue().trim());
            pricingService.saveUnit(unit);
            dialog.close();
            refreshMasterData();
            Notification.show("Einheit gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), saveBtn);
        dialog.add(form);
        dialog.open();
    }

    private void openTextTemplateDialog(TextTemplate template) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(template.getId() == null ? "Textvorlage anlegen" : "Textvorlage bearbeiten");
        dialog.setWidth("720px");

        ComboBox<TextTemplateType> templateType = new ComboBox<>("Typ");
        templateType.setItems(TextTemplateType.values());
        templateType.setItemLabelGenerator(TextTemplateType::getLabel);
        TextField name = new TextField("Name");
        TextField subject = new TextField("Betreff");
        Checkbox active = new Checkbox("Aktiv");
        TextArea body = new TextArea("Inhalt");

        templateType.setValue(template.getTemplateType() != null ? template.getTemplateType() : TextTemplateType.GENERAL);
        name.setValue(valueOrEmpty(template.getName()));
        subject.setValue(valueOrEmpty(template.getSubject()));
        active.setValue(template.getId() == null || template.isActive());
        body.setValue(valueOrEmpty(template.getBody()));
        body.setMinHeight("260px");
        body.setPlaceholder("Platzhalter: {kunde}, {angebotnummer}, {auftragsnummer}, {rechnungsnummer}, {faelligkeit}, {partner}");

        FormLayout form = new FormLayout(templateType, name, subject, active, body);
        form.setColspan(body, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("640px", 2));

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            if (templateType.getValue() == null || isBlank(name.getValue()) || isBlank(body.getValue())) {
                Notification.show("Typ, Name und Inhalt sind erforderlich").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            template.setTemplateType(templateType.getValue());
            template.setName(name.getValue().trim());
            template.setSubject(subject.getValue());
            template.setActive(active.getValue());
            template.setBody(body.getValue());

            settingsService.saveTextTemplate(template, currentTenant.getId(), getCurrentUsername());
            dialog.close();
            refreshTextTemplates();
            Notification.show("Textvorlage gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), saveBtn);
        dialog.add(form);
        dialog.open();
    }

    private void deleteTextTemplate(TextTemplate template) {
        settingsService.deleteTextTemplate(template.getId(), getCurrentUsername());
        refreshTextTemplates();
        Notification.show("Textvorlage geloescht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private String getEntityTypeLabel(String entityType) {
        return switch (entityType) {
            case "offer" -> "Angebote";
            case "order" -> "Auftraege";
            case "invoice" -> "Ausgangsrechnungen";
            case "vendor_invoice" -> "Eingangsrechnungen";
            case "customer" -> "Kunden";
            case "partner" -> "Partner";
            default -> entityType;
        };
    }

    private String getCurrentUsername() {
        return securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("system");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private Integer valueOrDefault(Integer value, Integer defaultValue) {
        return value != null ? value : defaultValue;
    }

    private BigDecimal valueOrDefault(BigDecimal value, BigDecimal defaultValue) {
        return value != null ? value : defaultValue;
    }

    private <T> T valueOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
