package com.translationagency.modules.crm.ui;

import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.crm.domain.ContactPerson;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.document.application.DocumentService;
import com.translationagency.modules.document.ui.DocumentAttachmentList;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.UUID;

@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Kunde Details | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class CustomerDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final CustomerService customerService;
    private final DocumentService documentService;
    private final SecurityService securityService;

    private Customer customer;
    private Tenant tenant;

    private final H2 title = new H2("Kunde Details");
    private final Tabs tabs = new Tabs();
    private final Div contentContainer = new Div();

    // Tab Contents
    private VerticalLayout masterDataLayout;
    private VerticalLayout contactPersonsLayout;
    private DocumentAttachmentList documentsLayout;

    public CustomerDetailView(CustomerService customerService, DocumentService documentService, SecurityService securityService) {
        this.customerService = customerService;
        this.documentService = documentService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.tenant = t);

        Button backButton = new Button("Zurück zur Liste", VaadinIcon.ARROW_LEFT.create(), e -> getUI().ifPresent(ui -> ui.navigate("customers")));
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
            // Suchen des Kunden über alle Kunden des Mandanten
            customerService.getAllCustomers(tenant.getId()).stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .ifPresentOrElse(c -> {
                        this.customer = c;
                        this.title.setText("Kunde: " + c.getCompanyName());
                        setupTabContents();
                    }, () -> {
                        Notification.show("Kunde nicht gefunden", 3000, Notification.Position.MIDDLE);
                        getUI().ifPresent(ui -> ui.navigate("customers"));
                    });
        } catch (IllegalArgumentException e) {
            Notification.show("Ungültige Kunden-ID", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("customers"));
        }
    }

    private void createTabs() {
        Tab masterDataTab = new Tab("Stammdaten");
        Tab contactPersonsTab = new Tab("Ansprechpartner");
        Tab documentsTab = new Tab("Dokumente");

        tabs.add(masterDataTab, contactPersonsTab, documentsTab);
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(masterDataTab)) {
                contentContainer.add(masterDataLayout);
            } else if (event.getSelectedTab().equals(contactPersonsTab)) {
                contentContainer.add(contactPersonsLayout);
            } else if (event.getSelectedTab().equals(documentsTab)) {
                contentContainer.add(documentsLayout);
                documentsLayout.refresh();
            }
        });
    }

    private void setupTabContents() {
        setupMasterDataTab();
        setupContactPersonsTab();
        setupDocumentsTab();

        contentContainer.removeAll();
        contentContainer.add(masterDataLayout);
        tabs.setSelectedIndex(0);
    }

    private void setupMasterDataTab() {
        masterDataLayout = new VerticalLayout();
        masterDataLayout.setSizeFull();
        masterDataLayout.setPadding(true);

        FormLayout formLayout = new FormLayout();
        TextField customerNumber = new TextField("Kundennummer");
        customerNumber.setReadOnly(true);
        TextField companyName = new TextField("Firmenname");
        TextField vatId = new TextField("USt-IdNr.");
        TextField leitwegId = new TextField("Leitweg-ID");
        TextField street = new TextField("Straße");
        TextField zip = new TextField("PLZ");
        TextField city = new TextField("Ort");
        TextField country = new TextField("Land (Ländercode)");

        formLayout.add(customerNumber, companyName, vatId, leitwegId, street, zip, city, country);

        Binder<Customer> binder = new Binder<>(Customer.class);
        binder.bindInstanceFields(formLayout);
        // Custom binding for nested Address fields if flat on object
        binder.bind(street, Customer::getBillingAddressStreet, Customer::setBillingAddressStreet);
        binder.bind(zip, Customer::getBillingAddressZip, Customer::setBillingAddressZip);
        binder.bind(city, Customer::getBillingAddressCity, Customer::setBillingAddressCity);
        binder.bind(country, Customer::getBillingAddressCountry, Customer::setBillingAddressCountry);

        binder.readBean(customer);

        Button saveButton = new Button("Änderungen speichern", e -> {
            try {
                binder.writeBean(customer);
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                customer.setUpdatedBy(username);
                customerService.saveCustomer(customer);
                title.setText("Kunde: " + customer.getCompanyName());
                Notification.show("Stammdaten erfolgreich aktualisiert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        masterDataLayout.add(formLayout, saveButton);
    }

    private void setupContactPersonsTab() {
        contactPersonsLayout = new VerticalLayout();
        contactPersonsLayout.setSizeFull();
        contactPersonsLayout.setPadding(true);

        Grid<ContactPerson> grid = new Grid<>(ContactPerson.class, false);
        grid.setSizeFull();
        grid.addColumn(ContactPerson::getFullName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(ContactPerson::getEmail).setHeader("E-Mail").setAutoWidth(true);
        grid.addColumn(ContactPerson::getPhone).setHeader("Telefon").setAutoWidth(true);

        grid.addComponentColumn(cp -> {
            Button editButton = new Button(VaadinIcon.EDIT.create(), e -> openContactPersonDialog(cp, grid));
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deleteButton = new Button(VaadinIcon.TRASH.create(), e -> {
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                customerService.deleteContactPerson(cp.getId(), username);
                Notification.show("Ansprechpartner gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                grid.setItems(customerService.getContactPersons(customer.getId()));
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("Aktionen").setAutoWidth(true);

        grid.setItems(customerService.getContactPersons(customer.getId()));

        Button addButton = new Button("Ansprechpartner hinzufügen", VaadinIcon.PLUS.create(), e -> openContactPersonDialog(new ContactPerson(), grid));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        contactPersonsLayout.add(addButton, grid);
    }

    private void setupDocumentsTab() {
        documentsLayout = new DocumentAttachmentList(
                documentService,
                securityService,
                tenant,
                customer,
                "CUSTOMER",
                customer.getId()
        );
    }

    private void openContactPersonDialog(ContactPerson contactPerson, Grid<ContactPerson> grid) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(contactPerson.getId() == null ? "Ansprechpartner hinzufügen" : "Ansprechpartner bearbeiten");

        FormLayout formLayout = new FormLayout();
        TextField firstName = new TextField("Vorname");
        TextField lastName = new TextField("Nachname");
        TextField email = new TextField("E-Mail");
        TextField phone = new TextField("Telefon");

        formLayout.add(firstName, lastName, email, phone);

        Binder<ContactPerson> binder = new Binder<>(ContactPerson.class);
        binder.forField(firstName).asRequired("Vorname ist erforderlich").bind(ContactPerson::getFirstName, ContactPerson::setFirstName);
        binder.forField(lastName).asRequired("Nachname ist erforderlich").bind(ContactPerson::getLastName, ContactPerson::setLastName);
        binder.forField(email).asRequired("E-Mail ist erforderlich").bind(ContactPerson::getEmail, ContactPerson::setEmail);
        binder.bind(phone, ContactPerson::getPhone, ContactPerson::setPhone);

        binder.readBean(contactPerson);

        Button saveButton = new Button("Speichern", e -> {
            try {
                binder.writeBean(contactPerson);
                contactPerson.setCustomer(customer);
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                if (contactPerson.getId() == null) {
                    contactPerson.setCreatedBy(username);
                }
                contactPerson.setUpdatedBy(username);
                customerService.saveContactPerson(contactPerson);
                dialog.close();
                grid.setItems(customerService.getContactPersons(customer.getId()));
                Notification.show("Ansprechpartner gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }
}
