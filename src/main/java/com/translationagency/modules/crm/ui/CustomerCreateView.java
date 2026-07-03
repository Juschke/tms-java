package com.translationagency.modules.crm.ui;

import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "customers/create", layout = MainLayout.class)
@PageTitle("Neuer Kunde | Translation Management")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class CustomerCreateView extends VerticalLayout {

    private final CustomerService customerService;
    private final SecurityService securityService;
    private Tenant currentTenant;

    public CustomerCreateView(CustomerService customerService, SecurityService securityService) {
        this.customerService = customerService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresentOrElse(
                tenant -> this.currentTenant = tenant,
                () -> Notification.show("Kein Mandant gefunden", 3000, Notification.Position.MIDDLE)
        );

        H2 pageTitle = new H2("Neuen Kunden anlegen");
        pageTitle.addClassNames("m-0", "text-xl");

        HorizontalLayout header = new HorizontalLayout(pageTitle);
        header.setWidthFull();
        header.expand(pageTitle);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        add(header);

        Button backButton = new Button("Zurück zur Liste", VaadinIcon.ARROW_LEFT.create(),
                e -> getUI().ifPresent(ui -> ui.navigate("customers")));
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

        Customer customer = new Customer();
        Binder<Customer> binder = new Binder<>(Customer.class);

        // Firmeninformationen Sektion (2 Spalten)
        H3 companyTitle = new H3("Firmeninformationen");
        companyTitle.addClassNames("m-0", "text-m");
        container.add(companyTitle);

        FormLayout companyForm = new FormLayout();
        companyForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 2)
        );

        TextField companyName = new TextField("Firmenname");
        companyName.setRequiredIndicatorVisible(true);
        TextField customerNumber = new TextField("Kundennummer");
        customerNumber.setReadOnly(true);
        customerNumber.setValue("wird automatisch vergeben");

        binder.forField(companyName).asRequired("Firmenname ist erforderlich")
                .bind(Customer::getCompanyName, Customer::setCompanyName);
        binder.bind(customerNumber, Customer::getCustomerNumber, Customer::setCustomerNumber);

        companyForm.add(companyName, customerNumber);
        container.add(companyForm);

        // Steuerdaten Sektion (2 Spalten)
        H3 taxTitle = new H3("Steuerdaten");
        taxTitle.addClassNames("m-0", "text-m");
        container.add(taxTitle);

        FormLayout taxForm = new FormLayout();
        taxForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 2)
        );

        TextField vatId = new TextField("USt-IdNr.");
        TextField leitwegId = new TextField("Leitweg-ID (XRechnung)");

        binder.bind(vatId, Customer::getVatId, Customer::setVatId);
        binder.bind(leitwegId, Customer::getLeitwegId, Customer::setLeitwegId);

        taxForm.add(vatId, leitwegId);
        container.add(taxForm);

        // Rechnungsadresse Sektion (3 Spalten)
        H3 addressTitle = new H3("Rechnungsadresse");
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
        TextField country = new TextField("Land (Ländercode)");

        binder.bind(street, Customer::getBillingAddressStreet, Customer::setBillingAddressStreet);
        binder.bind(zip, Customer::getBillingAddressZip, Customer::setBillingAddressZip);
        binder.bind(city, Customer::getBillingAddressCity, Customer::setBillingAddressCity);
        binder.bind(country, Customer::getBillingAddressCountry, Customer::setBillingAddressCountry);

        addressForm.add(street, zip, city, country);
        container.add(addressForm);

        // Action Buttons
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button cancelBtn = new Button("Abbrechen", VaadinIcon.CLOSE.create(),
                e -> getUI().ifPresent(ui -> ui.navigate("customers")));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button saveBtn = new Button("Kunde anlegen", VaadinIcon.CHECK.create(), e -> {
            try {
                binder.writeBean(customer);
                customer.setTenant(currentTenant);
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                customer.setCreatedBy(username);
                customer.setUpdatedBy(username);
                customerService.saveCustomer(customer);

                Notification.show("Kunde " + customer.getCompanyName() + " erfolgreich erstellt!")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("customers"));
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        actions.add(cancelBtn, saveBtn);
        container.add(actions);

        binder.readBean(customer);
        return container;
    }
}
