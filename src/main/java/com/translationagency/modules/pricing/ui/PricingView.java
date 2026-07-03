package com.translationagency.modules.pricing.ui;

import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.pricing.domain.PriceRule;
import com.translationagency.modules.pricing.domain.ServiceType;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.modules.tenant.application.NumberRangeService;
import com.translationagency.modules.tenant.domain.NumberRange;
import com.translationagency.security.SecurityService;
import com.translationagency.shared.ui.Confirmations;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.IntegerField;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.UUID;

@Route(value = "pricing", layout = MainLayout.class)
@PageTitle("Preise & Tarife | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER"})
public class PricingView extends VerticalLayout {

    private final PricingService pricingService;
    private final SecurityService securityService;
    private final NumberRangeService numberRangeService;

    private Tenant currentTenant;
    private final Tabs tabs = new Tabs();
    private final Div contentContainer = new Div();

    // Tab Contents
    private VerticalLayout priceRulesLayout;
    private VerticalLayout languagesLayout;
    private VerticalLayout serviceTypesLayout;
    private VerticalLayout numRangesLayout;

    // Grids
    private final Grid<PriceRule> priceRuleGrid = new Grid<>(PriceRule.class, false);
    private final Grid<Language> languageGrid = new Grid<>(Language.class, false);
    private final Grid<ServiceType> serviceTypeGrid = new Grid<>(ServiceType.class, false);
    private final Grid<NumberRange> numRangeGrid = new Grid<>(NumberRange.class, false);

    public PricingView(PricingService pricingService, SecurityService securityService, NumberRangeService numberRangeService) {
        this.pricingService = pricingService;
        this.securityService = securityService;
        this.numberRangeService = numberRangeService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.currentTenant = t);

        H2 headerTitle = new H2("Tarif- & Preismanagement");
        headerTitle.addClassNames("m-0", "text-xl");
        add(headerTitle);

        createTabs();
        add(tabs, contentContainer);
        contentContainer.setSizeFull();

        if (currentTenant != null) {
            setupTabContents();
        }
    }

    private void createTabs() {
        Tab rulesTab = new Tab("Tarife & Preisregeln");
        Tab langsTab = new Tab("Sprachen");
        Tab servicesTab = new Tab("Leistungsarten");
        Tab numRangesTab = new Tab("Nummernkreise");

        tabs.add(rulesTab, langsTab, servicesTab, numRangesTab);
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(rulesTab)) {
                contentContainer.add(priceRulesLayout);
                refreshPriceRules();
            } else if (event.getSelectedTab().equals(langsTab)) {
                contentContainer.add(languagesLayout);
                refreshLanguages();
            } else if (event.getSelectedTab().equals(servicesTab)) {
                contentContainer.add(serviceTypesLayout);
                refreshServiceTypes();
            } else if (event.getSelectedTab().equals(numRangesTab)) {
                contentContainer.add(numRangesLayout);
                refreshNumRanges();
            }
        });
    }

    private void setupTabContents() {
        setupPriceRulesTab();
        setupLanguagesTab();
        setupServiceTypesTab();
        setupNumRangesTab();

        contentContainer.removeAll();
        contentContainer.add(priceRulesLayout);
        tabs.setSelectedIndex(0);
        refreshPriceRules();
    }

    // --- TAB 1: PRICE RULES ---
    private void setupPriceRulesTab() {
        priceRulesLayout = new VerticalLayout();
        priceRulesLayout.setSizeFull();
        priceRulesLayout.setPadding(true);

        Button addRuleBtn = new Button("Neuer Tarif / Preisregel", VaadinIcon.PLUS.create(), e -> openPriceRuleDialog(new PriceRule()));
        addRuleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        priceRuleGrid.setSizeFull();
        priceRuleGrid.addColumn(r -> r.getSourceLanguage() != null ? r.getSourceLanguage().getName() : "Alle (Global)").setHeader("Quellsprache").setAutoWidth(true);
        priceRuleGrid.addColumn(r -> r.getTargetLanguage() != null ? r.getTargetLanguage().getName() : "Alle (Global)").setHeader("Zielsprache").setAutoWidth(true);
        priceRuleGrid.addColumn(r -> r.getServiceType() != null ? r.getServiceType().getName() : "-").setHeader("Leistungsart").setAutoWidth(true);
        priceRuleGrid.addColumn(r -> r.getRatePerWord() != null ? r.getRatePerWord().toString() + " EUR" : "-").setHeader("Wortpreis").setAutoWidth(true);
        priceRuleGrid.addColumn(r -> r.getRatePerPage() != null ? r.getRatePerPage().toString() + " EUR" : "-").setHeader("Seitenpreis").setAutoWidth(true);
        priceRuleGrid.addColumn(r -> r.getMinimumFee() != null ? r.getMinimumFee().toString() + " EUR" : "-").setHeader("Mindestgebühr").setAutoWidth(true);
        priceRuleGrid.addColumn(r -> r.getExpressSurchargePercent() != null ? r.getExpressSurchargePercent().toString() + "%" : "-").setHeader("Expresszuschlag").setAutoWidth(true);
        priceRuleGrid.addColumn(r -> r.getCertifiedSurcharge() != null ? r.getCertifiedSurcharge().toString() + " EUR" : "-").setHeader("Beglaubigung").setAutoWidth(true);

        priceRuleGrid.addComponentColumn(rule -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openPriceRuleDialog(rule));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> deletePriceRule(rule));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Aktionen").setAutoWidth(true);

        priceRulesLayout.add(addRuleBtn, priceRuleGrid);
    }

    private void refreshPriceRules() {
        if (currentTenant != null) {
            priceRuleGrid.setItems(pricingService.getAllPriceRules(currentTenant.getId()));
        }
    }

    private void deletePriceRule(PriceRule rule) {
        Confirmations.delete("Preisregel loeschen",
                "Soll diese Preisregel wirklich geloescht werden?",
                () -> {
        String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
        pricingService.deletePriceRule(rule.getId(), username);
        Notification.show("Preisregel erfolgreich gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refreshPriceRules();
                });
    }

    private void openPriceRuleDialog(PriceRule rule) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(rule.getId() == null ? "Neue Preisregel anlegen" : "Preisregel bearbeiten");
        dialog.setWidth("550px");

        FormLayout formLayout = new FormLayout();
        ComboBox<Language> sourceLang = new ComboBox<>("Quellsprache (optional)");
        sourceLang.setItems(pricingService.getAllLanguages());
        sourceLang.setItemLabelGenerator(Language::getName);
        sourceLang.setClearButtonVisible(true);

        ComboBox<Language> targetLang = new ComboBox<>("Zielsprache (optional)");
        targetLang.setItems(pricingService.getAllLanguages());
        targetLang.setItemLabelGenerator(Language::getName);
        targetLang.setClearButtonVisible(true);

        ComboBox<ServiceType> serviceType = new ComboBox<>("Leistungsart");
        serviceType.setItems(pricingService.getAllServiceTypes());
        serviceType.setItemLabelGenerator(ServiceType::getName);
        serviceType.setRequired(true);

        BigDecimalField ratePerWord = new BigDecimalField("Wortpreis (EUR)");
        BigDecimalField ratePerPage = new BigDecimalField("Seitenpreis (EUR)");
        BigDecimalField minimumFee = new BigDecimalField("Mindestgebühr (EUR)");
        BigDecimalField expressSurchargePercent = new BigDecimalField("Expresszuschlag (%)");
        BigDecimalField certifiedSurcharge = new BigDecimalField("Beglaubigungszuschlag (EUR)");

        formLayout.add(sourceLang, targetLang, serviceType, ratePerWord, ratePerPage, minimumFee, expressSurchargePercent, certifiedSurcharge);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        Binder<PriceRule> binder = new Binder<>(PriceRule.class);
        binder.bind(sourceLang, PriceRule::getSourceLanguage, PriceRule::setSourceLanguage);
        binder.bind(targetLang, PriceRule::getTargetLanguage, PriceRule::setTargetLanguage);
        binder.forField(serviceType).asRequired("Leistungsart ist erforderlich").bind(PriceRule::getServiceType, PriceRule::setServiceType);
        binder.bind(ratePerWord, PriceRule::getRatePerWord, PriceRule::setRatePerWord);
        binder.bind(ratePerPage, PriceRule::getRatePerPage, PriceRule::setRatePerPage);
        binder.bind(minimumFee, PriceRule::getMinimumFee, PriceRule::setMinimumFee);
        binder.bind(expressSurchargePercent, PriceRule::getExpressSurchargePercent, PriceRule::setExpressSurchargePercent);
        binder.bind(certifiedSurcharge, PriceRule::getCertifiedSurcharge, PriceRule::setCertifiedSurcharge);

        binder.readBean(rule);

        Button saveBtn = new Button("Speichern", e -> {
            try {
                binder.writeBean(rule);
                rule.setTenant(currentTenant);
                String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                if (rule.getId() == null) {
                    rule.setCreatedBy(username);
                }
                rule.setUpdatedBy(username);
                pricingService.savePriceRule(rule);
                dialog.close();
                refreshPriceRules();
                Notification.show("Preisregel erfolgreich gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(formLayout);
        dialog.open();
    }


    // --- TAB 2: LANGUAGES ---
    private void setupLanguagesTab() {
        languagesLayout = new VerticalLayout();
        languagesLayout.setSizeFull();
        languagesLayout.setPadding(true);

        Button addLangBtn = new Button("Neue Sprache hinzufügen", VaadinIcon.PLUS.create(), e -> openLanguageDialog(new Language()));
        addLangBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        languageGrid.setSizeFull();
        languageGrid.addColumn(Language::getCode).setHeader("Ländercode").setAutoWidth(true).setSortable(true);
        languageGrid.addColumn(Language::getName).setHeader("Sprache").setAutoWidth(true).setSortable(true);

        languagesLayout.add(addLangBtn, languageGrid);
    }

    private void refreshLanguages() {
        languageGrid.setItems(pricingService.getAllLanguages());
    }

    private void openLanguageDialog(Language language) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(language.getId() == null ? "Sprache hinzufügen" : "Sprache bearbeiten");

        FormLayout formLayout = new FormLayout();
        TextField code = new TextField("Ländercode (z.B. it, pl)");
        code.setRequired(true);
        TextField name = new TextField("Name (z.B. Italienisch)");
        name.setRequired(true);

        formLayout.add(code, name);

        Binder<Language> binder = new Binder<>(Language.class);
        binder.forField(code).asRequired("Ländercode ist erforderlich").bind(Language::getCode, Language::setCode);
        binder.forField(name).asRequired("Name ist erforderlich").bind(Language::getName, Language::setName);

        binder.readBean(language);

        Button saveBtn = new Button("Speichern", e -> {
            try {
                binder.writeBean(language);
                if (language.getId() == null) {
                    language.setId(UUID.randomUUID());
                }
                pricingService.saveLanguage(language);
                dialog.close();
                refreshLanguages();
                Notification.show("Sprache erfolgreich hinzugefügt").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(formLayout);
        dialog.open();
    }


    // --- TAB 3: SERVICE TYPES ---
    private void setupServiceTypesTab() {
        serviceTypesLayout = new VerticalLayout();
        serviceTypesLayout.setSizeFull();
        serviceTypesLayout.setPadding(true);

        Button addServiceBtn = new Button("Neue Leistungsart hinzufügen", VaadinIcon.PLUS.create(), e -> openServiceTypeDialog(new ServiceType()));
        addServiceBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        serviceTypeGrid.setSizeFull();
        serviceTypeGrid.addColumn(ServiceType::getName).setHeader("Name").setAutoWidth(true).setSortable(true);
        serviceTypeGrid.addColumn(ServiceType::getDescription).setHeader("Beschreibung").setAutoWidth(true);

        serviceTypesLayout.add(addServiceBtn, serviceTypeGrid);
    }

    private void refreshServiceTypes() {
        serviceTypeGrid.setItems(pricingService.getAllServiceTypes());
    }

    private void openServiceTypeDialog(ServiceType serviceType) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(serviceType.getId() == null ? "Leistungsart hinzufügen" : "Leistungsart bearbeiten");

        FormLayout formLayout = new FormLayout();
        TextField name = new TextField("Name");
        name.setRequired(true);
        TextArea description = new TextArea("Beschreibung");

        formLayout.add(name, description);

        Binder<ServiceType> binder = new Binder<>(ServiceType.class);
        binder.forField(name).asRequired("Name ist erforderlich").bind(ServiceType::getName, ServiceType::setName);
        binder.bind(description, ServiceType::getDescription, ServiceType::setDescription);

        binder.readBean(serviceType);

        Button saveBtn = new Button("Speichern", e -> {
            try {
                binder.writeBean(serviceType);
                if (serviceType.getId() == null) {
                    serviceType.setId(UUID.randomUUID());
                }
                pricingService.saveServiceType(serviceType);
                dialog.close();
                refreshServiceTypes();
                Notification.show("Leistungsart erfolgreich hinzugefügt").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(formLayout);
        dialog.open();
    }

    private void setupNumRangesTab() {
        numRangesLayout = new VerticalLayout();
        numRangesLayout.setSizeFull();
        numRangesLayout.setPadding(true);

        numRangeGrid.setSizeFull();
        numRangeGrid.addColumn(nr -> {
            switch (nr.getEntityType()) {
                case "offer": return "Angebote";
                case "order": return "Aufträge";
                case "invoice": return "Ausgangsrechnungen";
                case "vendor_invoice": return "Eingangsrechnungen";
                case "customer": return "Kunden";
                case "partner": return "Partner";
                default: return nr.getEntityType();
            }
        }).setHeader("Dokumenttyp / Entität").setAutoWidth(true);
        numRangeGrid.addColumn(NumberRange::getPrefix).setHeader("Präfix").setAutoWidth(true);
        numRangeGrid.addColumn(NumberRange::getCurrentNumber).setHeader("Zählerstand").setAutoWidth(true);
        numRangeGrid.addColumn(NumberRange::getPadding).setHeader("Stellen (Padding)").setAutoWidth(true);
        numRangeGrid.addColumn(NumberRange::getSeparator).setHeader("Trennzeichen").setAutoWidth(true);
        numRangeGrid.addColumn(nr -> nr.isResetYearly() ? "Ja" : "Nein").setHeader("Jährlicher Reset").setAutoWidth(true);
        numRangeGrid.addColumn(nr -> numberRangeService.previewNextNumber(currentTenant.getId(), nr.getEntityType())).setHeader("Vorschau nächste Nr.").setAutoWidth(true);

        numRangeGrid.addComponentColumn(nr -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openNumRangeDialog(nr));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return editBtn;
        }).setHeader("Aktionen").setAutoWidth(true);

        numRangesLayout.add(numRangeGrid);
    }

    private void refreshNumRanges() {
        if (currentTenant != null) {
            numRangeGrid.setItems(numberRangeService.getRangesForTenant(currentTenant.getId()));
        }
    }

    private void openNumRangeDialog(NumberRange range) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nummernkreis bearbeiten");
        dialog.setWidth("450px");

        FormLayout formLayout = new FormLayout();
        TextField prefix = new TextField("Präfix (z. B. RE{JJJJ})");
        prefix.setPlaceholder("Platzhalter: {JJJJ}, {JJ}, {MM}, {DD}");
        IntegerField currentNumber = new IntegerField("Aktueller Zählerstand");
        IntegerField padding = new IntegerField("Zählerlänge (z. B. 4)");
        TextField separator = new TextField("Trennzeichen");
        Checkbox resetYearly = new Checkbox("Zähler jährlich zurücksetzen");

        formLayout.add(prefix, currentNumber, padding, separator, resetYearly);

        Binder<NumberRange> binder = new Binder<>(NumberRange.class);
        binder.bind(prefix, NumberRange::getPrefix, NumberRange::setPrefix);
        binder.forField(currentNumber).asRequired("Zählerstand erforderlich").bind(NumberRange::getCurrentNumber, NumberRange::setCurrentNumber);
        binder.forField(padding).asRequired("Padding erforderlich").bind(NumberRange::getPadding, NumberRange::setPadding);
        binder.bind(separator, NumberRange::getSeparator, NumberRange::setSeparator);
        binder.bind(resetYearly, NumberRange::isResetYearly, NumberRange::setResetYearly);

        binder.readBean(range);

        Button saveBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            try {
                binder.writeBean(range);
                numberRangeService.saveRange(range);
                dialog.close();
                refreshNumRanges();
                Notification.show("Nummernkreis erfolgreich gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(formLayout);
        dialog.open();
    }
}
