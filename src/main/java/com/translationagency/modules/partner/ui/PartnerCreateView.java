package com.translationagency.modules.partner.ui;

import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "partners/create", layout = MainLayout.class)
@PageTitle("Neuer Partner | Translation Management")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class PartnerCreateView extends VerticalLayout {

    private final PartnerService partnerService;
    private final SecurityService securityService;
    private Tenant currentTenant;

    public PartnerCreateView(PartnerService partnerService, SecurityService securityService) {
        this.partnerService = partnerService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresentOrElse(
                tenant -> this.currentTenant = tenant,
                () -> Notification.show("Kein Mandant gefunden", 3000, Notification.Position.MIDDLE)
        );

        H2 pageTitle = new H2("Neuen Partner anlegen");
        pageTitle.addClassNames("m-0", "text-xl");

        HorizontalLayout header = new HorizontalLayout(pageTitle);
        header.setWidthFull();
        header.expand(pageTitle);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        add(header);

        Button backButton = new Button("Zurück zur Liste", VaadinIcon.ARROW_LEFT.create(),
                e -> getUI().ifPresent(ui -> ui.navigate("partners")));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttonLayout = new HorizontalLayout(backButton);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setPadding(false);
        buttonLayout.setSpacing(true);
        add(buttonLayout);

        VerticalLayout formContainer = createFormContainer();
        add(formContainer);
        setFlexGrow(1, formContainer);
    }

    private VerticalLayout createFormContainer() {
        VerticalLayout container = new VerticalLayout();
        container.setSpacing(true);
        container.setPadding(true);
        container.getStyle().set("max-width", "1400px");

        Partner partner = new Partner();
        Binder<Partner> binder = new Binder<>(Partner.class);

        // Partnernummer (read-only)
        H3 basicTitle = new H3("Partnerdaten");
        basicTitle.addClassNames("m-0", "text-m");
        container.add(basicTitle);

        FormLayout basicForm = new FormLayout();
        basicForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 2)
        );

        TextField partnerNumber = new TextField("Partnernummer");
        partnerNumber.setReadOnly(true);
        partnerNumber.setValue("wird automatisch vergeben");
        binder.bind(partnerNumber, Partner::getPartnerNumber, Partner::setPartnerNumber);

        TextField email = new TextField("E-Mail");
        email.setRequiredIndicatorVisible(true);
        binder.forField(email).asRequired("E-Mail ist erforderlich")
                .bind(Partner::getEmail, Partner::setEmail);

        basicForm.add(partnerNumber, email);
        container.add(basicForm);

        // Firma / Organisation Sektion (3 Spalten) - Basis für alle
        H3 companyTitle = new H3("Organisationsdaten");
        companyTitle.addClassNames("m-0", "text-m");
        container.add(companyTitle);

        FormLayout companyForm = new FormLayout();
        companyForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        TextField companyName = new TextField("Firma");
        TextField organization = new TextField("Organisation");
        TextField organizationUnit = new TextField("Organisationseinheit");

        binder.bind(companyName, Partner::getCompanyName, Partner::setCompanyName);
        binder.bind(organization, Partner::getOrganization, Partner::setOrganization);
        binder.bind(organizationUnit, Partner::getOrganizationUnit, Partner::setOrganizationUnit);

        companyForm.add(companyName, organization, organizationUnit);
        container.add(companyForm);

        // Partner-Typ Sektion (Radio Buttons)
        H3 typeTitle = new H3("Partner-Typ");
        typeTitle.addClassNames("m-0", "text-m");
        container.add(typeTitle);

        RadioButtonGroup<String> partnerTypeGroup = new RadioButtonGroup<>();
        partnerTypeGroup.setItems("Privat", "Firma", "Behörde");
        partnerTypeGroup.setValue("Privat");
        partnerTypeGroup.setLabel("Wählen Sie den Partner-Typ:");
        container.add(partnerTypeGroup);

        // Container für dynamische Felder
        VerticalLayout dynamicFieldsContainer = new VerticalLayout();
        dynamicFieldsContainer.setSpacing(true);
        dynamicFieldsContainer.setPadding(false);
        container.add(dynamicFieldsContainer);

        // Personendaten Sektion (3 Spalten)
        H3 personalTitle = new H3("Personendaten");
        personalTitle.addClassNames("m-0", "text-m");

        FormLayout personalForm = new FormLayout();
        personalForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        ComboBox<String> salutation = new ComboBox<>("Anrede");
        salutation.setItems("Herr", "Frau", "Divers");

        TextField title = new TextField("Titel");
        TextField firstName = new TextField("Vorname");
        firstName.setRequiredIndicatorVisible(true);

        TextField lastName = new TextField("Nachname");
        lastName.setRequiredIndicatorVisible(true);

        TextField displayName = new TextField("Anzeigename");
        TextField phone = new TextField("Telefon");

        binder.bind(salutation, Partner::getSalutation, Partner::setSalutation);
        binder.bind(title, Partner::getTitle, Partner::setTitle);
        binder.forField(firstName).asRequired("Vorname ist erforderlich")
                .bind(Partner::getFirstName, Partner::setFirstName);
        binder.forField(lastName).asRequired("Nachname ist erforderlich")
                .bind(Partner::getLastName, Partner::setLastName);
        binder.bind(displayName, Partner::getDisplayName, Partner::setDisplayName);
        binder.bind(phone, Partner::getPhone, Partner::setPhone);

        personalForm.add(salutation, title, firstName, lastName, displayName, phone);

        // Firma-spezifische Felder (nur bei Typ "Firma")
        H3 firmaDeatilsTitle = new H3("Firma-Details");
        firmaDeatilsTitle.addClassNames("m-0", "text-m");

        FormLayout firmaDetailsForm = new FormLayout();
        firmaDetailsForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 2)
        );

        TextField registrationNumber = new TextField("Handelsregisternummer");
        TextField taxNumber = new TextField("Steuernummer");

        binder.bind(registrationNumber, Partner::getOrganization, Partner::setOrganization);
        binder.bind(taxNumber, Partner::getTitle, Partner::setTitle);

        firmaDetailsForm.add(registrationNumber, taxNumber);

        // Behörden-spezifische Felder (nur bei Typ "Behörde")
        H3 authorityDetailsTitle = new H3("Behörden-Details");
        authorityDetailsTitle.addClassNames("m-0", "text-m");

        FormLayout authorityDetailsForm = new FormLayout();
        authorityDetailsForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 2)
        );

        TextField leitwegId = new TextField("Leitweg-ID (XRechnung)");
        leitwegId.setRequiredIndicatorVisible(true);

        binder.bind(leitwegId, Partner::getOrganization, Partner::setOrganization);

        authorityDetailsForm.add(leitwegId);

        // Listener für Partner-Typ Änderung
        partnerTypeGroup.addValueChangeListener(event -> {
            dynamicFieldsContainer.removeAll();
            String selectedType = event.getValue();

            // Immer Personendaten anzeigen
            dynamicFieldsContainer.add(personalTitle, personalForm);

            // Je nach Typ zusätzliche Felder hinzufügen
            if ("Firma".equals(selectedType)) {
                dynamicFieldsContainer.add(firmaDeatilsTitle, firmaDetailsForm);
            } else if ("Behörde".equals(selectedType)) {
                dynamicFieldsContainer.add(authorityDetailsTitle, authorityDetailsForm);
            }
        });

        // Initial: Privat ist ausgewählt, also nur Personendaten
        dynamicFieldsContainer.add(personalTitle, personalForm);

        // Adresse Sektion (3 Spalten)
        H3 addressTitle = new H3("Anschrift");
        addressTitle.addClassNames("m-0", "text-m");
        container.add(addressTitle);

        FormLayout addressForm = new FormLayout();
        addressForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        TextField street = new TextField("Straße & Hausnummer");
        TextField zip = new TextField("PLZ");
        TextField city = new TextField("Ort");
        TextField country = new TextField("Land");

        binder.bind(street, Partner::getStreet, Partner::setStreet);
        binder.bind(zip, Partner::getZip, Partner::setZip);
        binder.bind(city, Partner::getCity, Partner::setCity);
        binder.bind(country, Partner::getCountry, Partner::setCountry);

        addressForm.add(street, zip, city, country);
        container.add(addressForm);

        // Status & Einstufung Sektion (2 Spalten)
        H3 statusTitle = new H3("Status & Einstufung");
        statusTitle.addClassNames("m-0", "text-m");
        container.add(statusTitle);

        FormLayout statusForm = new FormLayout();
        statusForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 2)
        );

        Checkbox isTranslator = new Checkbox("Übersetzer");
        Checkbox isInterpreter = new Checkbox("Dolmetscher");
        Checkbox isActive = new Checkbox("Aktiv");
        Checkbox isRecommended = new Checkbox("Empfehlenswert");

        ComboBox<String> classification = new ComboBox<>("Übersetzerklasse");
        classification.setItems("extern", "intern", "premium");
        classification.setValue("extern");

        binder.bind(isTranslator, Partner::isTranslator, Partner::setTranslator);
        binder.bind(isInterpreter, Partner::isInterpreter, Partner::setInterpreter);
        binder.bind(isActive, Partner::isActive, Partner::setActive);
        binder.bind(isRecommended, Partner::isRecommended, Partner::setRecommended);
        binder.bind(classification, Partner::getClassification, Partner::setClassification);

        statusForm.add(isTranslator, isInterpreter, isActive, isRecommended, classification);
        container.add(statusForm);

        // Action Buttons
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button cancelBtn = new Button("Abbrechen", VaadinIcon.CLOSE.create(),
                e -> getUI().ifPresent(ui -> ui.navigate("partners")));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button saveBtn = new Button("Partner anlegen", VaadinIcon.CHECK.create(), e -> {
            try {
                binder.writeBean(partner);
                partner.setTenant(currentTenant);
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                partner.setCreatedBy(username);
                partner.setUpdatedBy(username);
                if (!partner.isActive()) {
                    partner.setActive(true);
                }
                partnerService.savePartner(partner);

                Notification.show("Partner " + partner.getFullName() + " erfolgreich erstellt!")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("partners"));
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        actions.add(cancelBtn, saveBtn);
        container.add(actions);

        binder.readBean(partner);
        return container;
    }
}
