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
    private final UUID            tenantId;

    private com.vaadin.flow.component.grid.Grid.Column<Customer> numberCol;
    private com.vaadin.flow.component.grid.Grid.Column<Customer> companyCol;
    private com.vaadin.flow.component.grid.Grid.Column<Customer> vatCol;
    private com.vaadin.flow.component.grid.Grid.Column<Customer> cityCol;

    private String filterSearch         = "";
    private String filterCustomerNumber = "";
    private String filterCompanyName    = "";
    private String filterVatId          = "";
    private String filterCity           = "";
    private String filterCountry        = "";

    public CustomerEnterpriseGrid(CustomerService customerService, UUID tenantId,
                                  Consumer<Customer> onEdit, Consumer<Customer> onDelete) {
        this.customerService = customerService;
        this.tenantId        = tenantId;

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

        // Initialize the first data load
        initialize();
    }

    @Override
    protected void configureColumns() {
        numberCol  = addSortableTextColumn(Customer::getCustomerNumber, "Kundennummer", "customerNumber");
        companyCol = addSortableTextColumn(Customer::getCompanyName, "Firma", "companyName");
        vatCol     = addSortableTextColumn(Customer::getVatId, "USt-IdNr.", "vatId");
        cityCol    = addSortableTextColumn(
                c -> c.getBillingAddressCity() != null ? c.getBillingAddressCity() : "–",
                "Ort", "billingAddressCity"
        );
    }

    @Override
    protected void configureFilters(HeaderRow filterRow) {
        addTextFilter(numberCol,  value -> this.filterCustomerNumber = value);
        addTextFilter(companyCol, value -> this.filterCompanyName    = value);
        addTextFilter(vatCol,     value -> this.filterVatId          = value);
        addTextFilter(cityCol,    value -> this.filterCity           = value);
    }

    @Override
    protected Page<Customer> loadPage(Pageable pageable) {
        return customerService.findCustomersFiltered(
                tenantId,
                filterSearch,
                filterCustomerNumber,
                filterCompanyName,
                filterVatId,
                filterCity,
                filterCountry,
                pageable
        );
    }
}
