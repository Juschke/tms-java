package com.translationagency.modules.partner.ui;

import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.partner.domain.PartnerLanguagePair;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.UUID;

@Route(value = "partners", layout = MainLayout.class)
@PageTitle("Partner Details | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class PartnerDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final PartnerService partnerService;
    private final PricingService pricingService;
    private final SecurityService securityService;

    private Partner partner;
    private Tenant tenant;

    private final H2 title = new H2("Partner Details");
    private final Tabs tabs = new Tabs();
    private final Div contentContainer = new Div();

    // Tab Contents
    private VerticalLayout masterDataLayout;
    private VerticalLayout skillsLayout;
    private VerticalLayout contactLayout;

    // Skills Grid
    private final Grid<PartnerLanguagePair> skillsGrid = new Grid<>(PartnerLanguagePair.class, false);

    public PartnerDetailView(PartnerService partnerService, PricingService pricingService, SecurityService securityService) {
        this.partnerService = partnerService;
        this.pricingService = pricingService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.tenant = t);

        Button backButton = new Button("Zurück zur Liste", VaadinIcon.ARROW_LEFT.create(), e -> getUI().ifPresent(ui -> ui.navigate("partners")));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(backButton, title);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        add(header);

        createTabs();
        add(tabs, contentContainer);
        contentContainer.setSizeFull();
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            UUID id = UUID.fromString(parameter);
            partnerService.getAllPartners(tenant.getId()).stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .ifPresentOrElse(p -> {
                        this.partner = p;
                        this.title.setText("Partner: " + p.getFullName());
                        setupTabContents();
                    }, () -> {
                        Notification.show("Partner nicht gefunden", 3000, Notification.Position.MIDDLE);
                        getUI().ifPresent(ui -> ui.navigate("partners"));
                    });
        } catch (IllegalArgumentException e) {
            Notification.show("Ungültige Partner-ID", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("partners"));
        }
    }

    private void createTabs() {
        Tab masterDataTab = new Tab("Stammdaten");
        Tab skillsTab = new Tab("Sprachprofil");
        Tab contactTab = new Tab("Anschrift & Kontakt");

        tabs.add(masterDataTab, skillsTab, contactTab);
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(masterDataTab)) {
                contentContainer.add(masterDataLayout);
            } else if (event.getSelectedTab().equals(skillsTab)) {
                contentContainer.add(skillsLayout);
                refreshSkills();
            } else if (event.getSelectedTab().equals(contactTab)) {
                contentContainer.add(contactLayout);
            }
        });
    }

    private void setupTabContents() {
        setupMasterDataTab();
        setupSkillsTab();
        setupContactTab();

        contentContainer.removeAll();
        contentContainer.add(masterDataLayout);
        tabs.setSelectedIndex(0);
    }

    // --- TAB 1: MASTER DATA ---
    private void setupMasterDataTab() {
        masterDataLayout = new VerticalLayout();
        masterDataLayout.setSizeFull();
        masterDataLayout.setPadding(true);

        FormLayout formLayout = new FormLayout();
        
        // Firma / Organisation
        TextField companyName = new TextField("Firma");
        TextField organization = new TextField("Organisation");
        TextField organizationUnit = new TextField("Organisationseinheit");

        // Personendaten
        ComboBox<String> salutation = new ComboBox<>("Anrede");
        salutation.setItems("Herr", "Frau", "Divers");
        TextField pTitle = new TextField("Titel");
        TextField firstName = new TextField("Vorname");
        TextField lastName = new TextField("Nachname");
        TextField displayName = new TextField("Anzeigename");

        // Status Checkboxen
        Checkbox isTranslator = new Checkbox("Übersetzer");
        Checkbox isInterpreter = new Checkbox("Dolmetscher");
        Checkbox isActive = new Checkbox("Aktiv");
        Checkbox isRecommended = new Checkbox("Empfehlenswert");

        ComboBox<String> classification = new ComboBox<>("Übersetzerklasse");
        classification.setItems("extern", "intern", "premium");

        formLayout.add(
                new H3("Firma / Organisation"), companyName, organization, organizationUnit,
                new H3("Personendaten"), salutation, pTitle, firstName, lastName, displayName,
                new H3("Status & Einstufung"), isTranslator, isInterpreter, isActive, isRecommended, classification
        );

        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("800px", 3)
        );

        // Grid span headers
        formLayout.setColspan(formLayout.getChildren().filter(c -> c instanceof H3).toArray(H3[]::new)[0], 3);
        formLayout.setColspan(formLayout.getChildren().filter(c -> c instanceof H3).toArray(H3[]::new)[1], 3);
        formLayout.setColspan(formLayout.getChildren().filter(c -> c instanceof H3).toArray(H3[]::new)[2], 3);

        Binder<Partner> binder = new Binder<>(Partner.class);
        binder.bind(companyName, Partner::getCompanyName, Partner::setCompanyName);
        binder.bind(organization, Partner::getOrganization, Partner::setOrganization);
        binder.bind(organizationUnit, Partner::getOrganizationUnit, Partner::setOrganizationUnit);
        binder.bind(salutation, Partner::getSalutation, Partner::setSalutation);
        binder.bind(pTitle, Partner::getTitle, Partner::setTitle);
        binder.bind(firstName, Partner::getFirstName, Partner::setFirstName);
        binder.bind(lastName, Partner::getLastName, Partner::setLastName);
        binder.bind(displayName, Partner::getDisplayName, Partner::setDisplayName);
        binder.bind(isTranslator, Partner::isTranslator, Partner::setTranslator);
        binder.bind(isInterpreter, Partner::isInterpreter, Partner::setInterpreter);
        binder.bind(isActive, Partner::isActive, Partner::setActive);
        binder.bind(isRecommended, Partner::isRecommended, Partner::setRecommended);
        binder.bind(classification, Partner::getClassification, Partner::setClassification);

        binder.readBean(partner);

        Button saveBtn = new Button("Stammdaten speichern", VaadinIcon.CHECK.create(), e -> {
            try {
                binder.writeBean(partner);
                String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                partner.setUpdatedBy(username);
                partnerService.savePartner(partner);
                title.setText("Partner: " + partner.getFullName());
                Notification.show("Stammdaten erfolgreich gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        masterDataLayout.add(formLayout, saveBtn);
    }

    // --- TAB 2: SKILLS / SPRACHPROFIL ---
    private void setupSkillsTab() {
        skillsLayout = new VerticalLayout();
        skillsLayout.setSizeFull();
        skillsLayout.setPadding(true);

        Button addSkillBtn = new Button("Sprachpaar hinzufügen", VaadinIcon.PLUS.create(), e -> openSkillDialog(new PartnerLanguagePair()));
        addSkillBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        skillsGrid.setSizeFull();
        skillsGrid.addColumn(s -> s.getSourceLanguage() != null ? s.getSourceLanguage().getName() : "-").setHeader("Ausgangssprache").setAutoWidth(true);
        skillsGrid.addColumn(s -> s.getTargetLanguage() != null ? s.getTargetLanguage().getName() : "-").setHeader("Zielsprache").setAutoWidth(true);
        skillsGrid.addColumn(PartnerLanguagePair::getActivity).setHeader("Tätigkeit").setAutoWidth(true);
        skillsGrid.addColumn(PartnerLanguagePair::getSubjectArea).setHeader("Fachgebiet").setAutoWidth(true);
        skillsGrid.addColumn(s -> s.getPricePerLine().toString() + " EUR").setHeader("Preis pro Zeile").setAutoWidth(true);

        skillsGrid.addComponentColumn(skill -> {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                partnerService.deleteLanguagePair(skill.getId());
                Notification.show("Sprachpaar gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshSkills();
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return deleteBtn;
        }).setHeader("Aktionen").setAutoWidth(true);

        skillsLayout.add(addSkillBtn, skillsGrid);
    }

    private void refreshSkills() {
        if (partner != null) {
            skillsGrid.setItems(partnerService.getLanguagePairsForPartner(partner.getId()));
        }
    }

    private void openSkillDialog(PartnerLanguagePair pair) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Sprachpaar / Tarif hinzufügen");

        FormLayout formLayout = new FormLayout();
        ComboBox<Language> sourceLang = new ComboBox<>("Ausgangssprache");
        sourceLang.setItems(pricingService.getAllLanguages());
        sourceLang.setItemLabelGenerator(Language::getName);
        sourceLang.setRequired(true);

        ComboBox<Language> targetLang = new ComboBox<>("Zielsprache");
        targetLang.setItems(pricingService.getAllLanguages());
        targetLang.setItemLabelGenerator(Language::getName);
        targetLang.setRequired(true);

        ComboBox<String> activity = new ComboBox<>("Tätigkeit");
        activity.setItems("Übersetzen", "Lektorieren", "Simultan", "Konsekutiv", "Beglaubigte Übersetzung");
        activity.setValue("Übersetzen");
        activity.setRequired(true);

        TextField subjectArea = new TextField("Fachgebiet");
        subjectArea.setPlaceholder("z.B. Philosophie, Medizin");

        BigDecimalField pricePerLine = new BigDecimalField("Preis pro Zeile (EUR)");
        pricePerLine.setRequired(true);

        formLayout.add(sourceLang, targetLang, activity, subjectArea, pricePerLine);

        Binder<PartnerLanguagePair> binder = new Binder<>(PartnerLanguagePair.class);
        binder.forField(sourceLang).asRequired("Ausgangssprache erforderlich").bind(PartnerLanguagePair::getSourceLanguage, PartnerLanguagePair::setSourceLanguage);
        binder.forField(targetLang).asRequired("Zielsprache erforderlich").bind(PartnerLanguagePair::getTargetLanguage, PartnerLanguagePair::setTargetLanguage);
        binder.forField(activity).asRequired("Tätigkeit erforderlich").bind(PartnerLanguagePair::getActivity, PartnerLanguagePair::setActivity);
        binder.bind(subjectArea, PartnerLanguagePair::getSubjectArea, PartnerLanguagePair::setSubjectArea);
        binder.forField(pricePerLine).asRequired("Preis ist erforderlich").bind(PartnerLanguagePair::getPricePerLine, PartnerLanguagePair::setPricePerLine);

        binder.readBean(pair);

        Button saveBtn = new Button("Hinzufügen", e -> {
            try {
                binder.writeBean(pair);
                pair.setPartner(partner);
                partnerService.saveLanguagePair(pair);
                dialog.close();
                refreshSkills();
                Notification.show("Sprachpaar erfolgreich hinzugefügt").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Hinzufügen: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(formLayout);
        dialog.open();
    }

    // --- TAB 3: CONTACT & REMARKS ---
    private void setupContactTab() {
        contactLayout = new VerticalLayout();
        contactLayout.setSizeFull();
        contactLayout.setPadding(true);

        FormLayout formLayout = new FormLayout();
        TextField email = new TextField("E-Mail");
        email.setReadOnly(true); // defined on creation
        TextField phone = new TextField("Telefon");

        TextField street = new TextField("Straße & Hausnummer");
        TextField zip = new TextField("PLZ");
        TextField city = new TextField("Ort");
        TextField country = new TextField("Land");

        TextArea notes = new TextArea("Bemerkungen");
        notes.setWidthFull();
        notes.setHeight("150px");

        formLayout.add(
                new H3("Kontaktkanäle"), email, phone,
                new H3("Anschrift"), street, zip, city, country,
                new H3("Zusätzliches"), notes
        );

        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        formLayout.setColspan(notes, 2);

        Binder<Partner> binder = new Binder<>(Partner.class);
        binder.bind(phone, Partner::getPhone, Partner::setPhone);
        binder.bind(street, Partner::getStreet, Partner::setStreet);
        binder.bind(zip, Partner::getZip, Partner::setZip);
        binder.bind(city, Partner::getCity, Partner::setCity);
        binder.bind(country, Partner::getCountry, Partner::setCountry);
        binder.bind(notes, Partner::getNotes, Partner::setNotes);

        binder.readBean(partner);

        Button saveBtn = new Button("Kontaktdaten speichern", VaadinIcon.CHECK.create(), e -> {
            try {
                binder.writeBean(partner);
                String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                partner.setUpdatedBy(username);
                partnerService.savePartner(partner);
                Notification.show("Kontaktdaten erfolgreich gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        contactLayout.add(formLayout, saveBtn);
    }
}
