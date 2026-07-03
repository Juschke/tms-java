package com.translationagency.modules.crm.ui;

import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.shared.ui.BaseEnterpriseGrid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.function.Consumer;

public class CustomerEnterpriseGrid extends BaseEnterpriseGrid<Customer> {

    private final CustomerService customerService;
    private final UUID tenantId;

    private com.vaadin.flow.component.grid.Grid.Column<Customer> companyCol;
    private com.vaadin.flow.component.grid.Grid.Column<Customer> numberCol;
    private com.vaadin.flow.component.grid.Grid.Column<Customer> contactCol;
    private com.vaadin.flow.component.grid.Grid.Column<Customer> phoneCol;
    private com.vaadin.flow.component.grid.Grid.Column<Customer> emailCol;
    private com.vaadin.flow.component.grid.Grid.Column<Customer> addressCol;

    private String filterSearch = "";
    private String filterCustomerNumber = "";
    private String filterCompanyName = "";
    private String filterCity = "";
    private String filterCountry = "";
    private String filterContact = "";
    private String filterPhone = "";

    public CustomerEnterpriseGrid(CustomerService customerService, UUID tenantId,
            Consumer<Customer> onEdit, Consumer<Customer> onDelete) {
        this.customerService = customerService;
        this.tenantId = tenantId;

        // Wire edit & delete actions
        setEditAction(onEdit);
        setDeleteAction(onDelete);

        // Enable General Search
        enableGeneralSearch(value -> this.filterSearch = value);

        // Add Advanced Filters
        TextField countryFilter = new TextField();
        countryFilter.setPlaceholder("z.B. DE, AT");
        countryFilter.setClearButtonVisible(true);
        countryFilter.addValueChangeListener(e -> {
            this.filterCountry = e.getValue() != null ? e.getValue().trim() : "";
            resetToFirstPage();
        });
        addAdvancedFilter(countryFilter, "Land (Ländercode)");

        // Row selection navigation
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("customers/detail/" + event.getValue().getId().toString()));
            }
        });

        configureRowActions();

        // Initialize the first data load
        initialize();
    }

    private void configureRowActions() {
        addContextMenuAction("👁️  Details öffnen",
                c -> getUI().ifPresent(ui -> ui.navigate("customers/detail/" + c.getId().toString())));

        addContextMenuAction("📧  E-Mail schreiben", c -> {
            com.vaadin.flow.component.UI.getCurrent().getPage().open("mailto:info@example.com", "_blank"); // Platzhalter
                                                                                                           // für echte
                                                                                                           // Logik
        });
    }

    @Override
    protected void configureColumns() {
        companyCol = addSortableTextColumn(Customer::getCompanyName, "Firma", "companyName");
        numberCol = addSortableTextColumn(Customer::getCustomerNumber, "Kundennummer", "customerNumber");

        contactCol = addSortableTextColumn(c -> {
            var contacts = customerService.getContactPersons(c.getId());
            if (!contacts.isEmpty()) {
                return contacts.get(0).getFullName();
            }
            return "–";
        }, "Ansprechpartner", "contactPerson");

        phoneCol = addSortableTextColumn(c -> {
            var contacts = customerService.getContactPersons(c.getId());
            if (!contacts.isEmpty()) {
                return contacts.get(0).getPhone() != null ? contacts.get(0).getPhone() : "–";
            }
            return "–";
        }, "Telefon", "phone");

        emailCol = addSortableTextColumn(c -> {
            var contacts = customerService.getContactPersons(c.getId());
            if (!contacts.isEmpty()) {
                return contacts.get(0).getEmail() != null ? contacts.get(0).getEmail() : "–";
            }
            return "–";
        }, "E-Mail", "email");

        addressCol = addSortableTextColumn(c -> {
            String street = c.getBillingAddressStreet() != null ? c.getBillingAddressStreet() : "";
            String zip = c.getBillingAddressZip() != null ? c.getBillingAddressZip() : "";
            String city = c.getBillingAddressCity() != null ? c.getBillingAddressCity() : "";

            if (street.isEmpty() && zip.isEmpty() && city.isEmpty()) {
                return "–";
            }

            StringBuilder addr = new StringBuilder();
            if (!street.isEmpty()) addr.append(street);
            if (!zip.isEmpty()) {
                if (addr.length() > 0) addr.append(", ");
                addr.append(zip);
            }
            if (!city.isEmpty()) {
                if (addr.length() > 0) addr.append(" ");
                addr.append(city);
            }
            return addr.toString();
        }, "Adresse", "billingAddressCity");
    }

    @Override
    protected void configureFilters(HeaderRow filterRow) {
        addTextFilter(companyCol, value -> this.filterCompanyName = value);
        addTextFilter(numberCol, value -> this.filterCustomerNumber = value);
        addTextFilter(contactCol, value -> this.filterContact = value);
        addTextFilter(phoneCol, value -> this.filterPhone = value);
        addTextFilter(emailCol, value -> {
            this.filterSearch = value;
            resetToFirstPage();
        });
        addTextFilter(addressCol, value -> this.filterCity = value);
    }

    @Override
    protected Page<Customer> loadPage(Pageable pageable) {
        return customerService.findCustomersFiltered(
                tenantId,
                filterSearch,
                filterCustomerNumber,
                filterCompanyName,
                "",
                filterCity,
                filterCountry,
                pageable);
    }
}
