package com.translationagency.modules.crm.ui;

import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.shared.ui.Confirmations;
import com.translationagency.shared.util.CsvUtils;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
                    customer -> getUI().ifPresent(ui -> ui.navigate("customers/detail/" + customer.getId().toString())),
                    this::deleteCustomer);
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

        Button addCustomerButton = new Button("Neuer Kunde", VaadinIcon.PLUS.create(),
                e -> getUI().ifPresent(ui -> ui.navigate("customers/create")));
        addCustomerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button importButton = new Button("CSV Import", VaadinIcon.UPLOAD.create(), e -> openCustomerImportDialog());
        importButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(headerTitle, createCustomerExportAnchor(), importButton, addCustomerButton);
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

    private Anchor createCustomerExportAnchor() {
        StreamResource resource = new StreamResource("kunden.csv", () -> CsvUtils.toUtf8Csv(buildCustomerCsv()));
        resource.setContentType("text/csv;charset=utf-8");

        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        Button exportButton = new Button("CSV Export", VaadinIcon.DOWNLOAD.create());
        exportButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        anchor.add(exportButton);
        return anchor;
    }

    private void openCustomerImportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Kunden importieren");
        dialog.setWidth("520px");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv", "text/csv");
        upload.setMaxFiles(1);
        upload.addSucceededListener(event -> {
            try {
                int imported = importCustomers(buffer);
                dialog.close();
                updateList();
                Notification.show(imported + " Kunden importiert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Import fehlgeschlagen: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.add(upload);
        dialog.getFooter().add(new Button("Schliessen", e -> dialog.close()));
        dialog.open();
    }

    private String buildCustomerCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append(CsvUtils.row("customer_number", "company_name", "vat_id", "leitweg_id",
                "street", "zip", "city", "country"));
        customerService.getAllCustomers(currentTenant.getId()).forEach(customer -> csv.append(CsvUtils.row(
                customer.getCustomerNumber(),
                customer.getCompanyName(),
                customer.getVatId(),
                customer.getLeitwegId(),
                customer.getBillingAddressStreet(),
                customer.getBillingAddressZip(),
                customer.getBillingAddressCity(),
                customer.getBillingAddressCountry()
        )));
        return csv.toString();
    }

    private int importCustomers(MemoryBuffer buffer) throws Exception {
        List<Customer> existing = customerService.getAllCustomers(currentTenant.getId());
        String username = securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("system");
        int imported = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(buffer.getInputStream(), StandardCharsets.UTF_8))) {
            boolean firstLine = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (firstLine && line.charAt(0) == 0xFEFF) {
                    line = line.substring(1);
                }

                List<String> columns = CsvUtils.parseLine(line);
                if (firstLine && !columns.isEmpty()
                        && "customer_number".equalsIgnoreCase(columns.get(0).trim())) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String customerNumber = valueAt(columns, 0);
                String companyName = valueAt(columns, 1);
                if (companyName.isBlank()) {
                    continue;
                }

                Customer customer = findCustomerByNumber(existing, customerNumber);
                if (customer == null) {
                    customer = new Customer();
                    customer.setTenant(currentTenant);
                    customer.setCreatedBy(username);
                    if (!customerNumber.isBlank()) {
                        customer.setCustomerNumber(customerNumber);
                    }
                    existing.add(customer);
                }

                customer.setCompanyName(companyName);
                customer.setVatId(valueAt(columns, 2));
                customer.setLeitwegId(valueAt(columns, 3));
                customer.setBillingAddressStreet(valueAt(columns, 4));
                customer.setBillingAddressZip(valueAt(columns, 5));
                customer.setBillingAddressCity(valueAt(columns, 6));
                customer.setBillingAddressCountry(valueAt(columns, 7));
                customer.setUpdatedBy(username);
                customerService.saveCustomer(customer);
                imported++;
            }
        }

        return imported;
    }

    private Customer findCustomerByNumber(List<Customer> customers, String customerNumber) {
        if (customerNumber.isBlank()) {
            return null;
        }
        return customers.stream()
                .filter(customer -> customerNumber.equalsIgnoreCase(valueOrEmpty(customer.getCustomerNumber())))
                .findFirst()
                .orElse(null);
    }

    private void deleteCustomer(Customer customer) {
        Confirmations.delete("Kunde loeschen",
                "Soll der Kunde " + customer.getCompanyName() + " wirklich geloescht werden?",
                () -> {
        String username = securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("system");
        customerService.deleteCustomer(customer.getId(), username);
        Notification notification = Notification.show("Kunde gelöscht", 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        updateList();
                });
    }


    private String valueAt(List<String> values, int index) {
        return index < values.size() && values.get(index) != null ? values.get(index).trim() : "";
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}
