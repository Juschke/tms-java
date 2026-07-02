package com.translationagency.modules.crm.ui;

import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.UUID;

@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Kunden | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class CustomersView extends VerticalLayout {

    private final CustomerService customerService;
    private final SecurityService securityService;
    
    private CustomerEnterpriseGrid grid;
    private Tenant currentTenant;

    public CustomersView(CustomerService customerService, SecurityService securityService) {
        this.customerService = customerService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresentOrElse(
                tenant -> this.currentTenant = tenant,
                () -> Notification.show("Kein Mandant gefunden", 3000, Notification.Position.MIDDLE)
        );

        if (currentTenant != null) {
            grid = new CustomerEnterpriseGrid(customerService, currentTenant.getId(),
                    this::openCustomerDialog, this::deleteCustomer);
        }

        add(createHeaderLayout());
        if (grid != null) {
            add(grid);
            setFlexGrow(1, grid);
        }
    }

    private HorizontalLayout createHeaderLayout() {
        H2 headerTitle = new H2("Kundenverwaltung");
        headerTitle.addClassNames("m-0", "text-xl");

        Button addCustomerButton = new Button("Neuer Kunde", VaadinIcon.PLUS.create(), e -> openCustomerDialog(new Customer()));
        addCustomerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(headerTitle, addCustomerButton);
        header.setWidthFull();
        header.expand(headerTitle);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        return header;
    }

    private void updateList() {
        if (grid != null) {
            grid.refresh();
        }
    }

    private void deleteCustomer(Customer customer) {
        String username = securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("system");
        customerService.deleteCustomer(customer.getId(), username);
        Notification notification = Notification.show("Kunde gelöscht", 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        updateList();
    }

    private void openCustomerDialog(Customer customer) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(customer.getId() == null ? "Neuen Kunden anlegen" : "Kunden bearbeiten");

        FormLayout formLayout = new FormLayout();
        TextField customerNumber = new TextField("Kundennummer");
        TextField companyName = new TextField("Firmenname");
        TextField vatId = new TextField("USt-IdNr.");
        TextField leitwegId = new TextField("Leitweg-ID (XRechnung)");
        TextField street = new TextField("Straße");
        TextField zip = new TextField("PLZ");
        TextField city = new TextField("Ort");
        TextField country = new TextField("Land (Ländercode)");

        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.add(customerNumber, companyName, vatId, leitwegId, street, zip, city, country);

        Binder<Customer> binder = new Binder<>(Customer.class);
        binder.forField(customerNumber).asRequired("Kundennummer ist erforderlich").bind(Customer::getCustomerNumber, Customer::setCustomerNumber);
        binder.forField(companyName).asRequired("Firmenname ist erforderlich").bind(Customer::getCompanyName, Customer::setCompanyName);
        binder.bind(vatId, Customer::getVatId, Customer::setVatId);
        binder.bind(leitwegId, Customer::getLeitwegId, Customer::setLeitwegId);
        binder.bind(street, Customer::getBillingAddressStreet, Customer::setBillingAddressStreet);
        binder.bind(zip, Customer::getBillingAddressZip, Customer::setBillingAddressZip);
        binder.bind(city, Customer::getBillingAddressCity, Customer::setBillingAddressCity);
        binder.bind(country, Customer::getBillingAddressCountry, Customer::setBillingAddressCountry);

        binder.readBean(customer);

        Button saveButton = new Button("Speichern", e -> {
            try {
                binder.writeBean(customer);
                customer.setTenant(currentTenant);
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                if (customer.getId() == null) {
                    customer.setCreatedBy(username);
                }
                customer.setUpdatedBy(username);
                customerService.saveCustomer(customer);
                dialog.close();
                updateList();
                Notification n = Notification.show("Kunde erfolgreich gespeichert", 3000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }
}
