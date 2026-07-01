package com.translationagency.modules.order.ui;

import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.order.domain.OrderStatus;
import com.translationagency.shared.ui.BaseEnterpriseGrid;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.HeaderRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

public class OrderEnterpriseGrid extends BaseEnterpriseGrid<TranslationOrder> {

    private final OrderService orderService;
    private final UUID         tenantId;

    private com.vaadin.flow.component.grid.Grid.Column<TranslationOrder> numberCol;
    private com.vaadin.flow.component.grid.Grid.Column<TranslationOrder> customerCol;
    private com.vaadin.flow.component.grid.Grid.Column<TranslationOrder> statusCol;

    private String      filterSearch       = "";
    private String      filterOrderNumber  = "";
    private String      filterCustomerName = "";
    private OrderStatus filterStatus       = null;

    public OrderEnterpriseGrid(OrderService orderService, UUID tenantId, Consumer<TranslationOrder> onDelete) {
        this.orderService = orderService;
        this.tenantId     = tenantId;

        // Wire delete action
        setDeleteAction(onDelete);

        // Enable general search
        enableGeneralSearch(value -> this.filterSearch = value);

        // Row selection navigation to order detail
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("orders/" + event.getValue().getId().toString()));
            }
        });

        initialize();
    }

    @Override
    protected void configureColumns() {
        numberCol   = addSortableTextColumn(TranslationOrder::getOrderNumber, "Auftragsnummer", "orderNumber");
        customerCol = addSortableTextColumn(o -> o.getCustomer() != null ? o.getCustomer().getCompanyName() : "–",
                "Kunde", "customer.companyName");

        addSortableTextColumn(o -> o.getNetAmount() + " €", "Netto", "netAmount");
        addSortableTextColumn(o -> o.getGrossAmount() + " €", "Brutto", "grossAmount");

        statusCol = addTextColumn(o -> {
            if (o.getStatus() == null) return "–";
            switch (o.getStatus()) {
                case CREATED: return "Erstellt";
                case ASSIGNED: return "Partner zugewiesen";
                case IN_PROGRESS: return "In Bearbeitung";
                case QA_READY: return "QS-Bereit";
                case READY_FOR_DELIVERY: return "Lieferbereit";
                case DELIVERED: return "Geliefert";
                case INVOICED: return "Abgerechnet";
                case PAID: return "Bezahlt";
                case ARCHIVED: return "Archiviert";
                case CANCELLED: return "Storniert";
                default: return o.getStatus().name();
            }
        }, "Status");

        addSortableTextColumn(o -> o.getDeliveryDeadline() != null ? o.getDeliveryDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "–",
                "Liefertermin", "deliveryDeadline");

        addSortableTextColumn(o -> o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "–",
                "Erstellt am", "createdAt");
    }

    @Override
    protected void configureFilters(HeaderRow filterRow) {
        addTextFilter(numberCol,   value -> this.filterOrderNumber = value);
        addTextFilter(customerCol, value -> this.filterCustomerName = value);
        addComboBoxFilter(statusCol, Arrays.asList(OrderStatus.values()), value -> this.filterStatus = value);
    }

    @Override
    protected Page<TranslationOrder> loadPage(Pageable pageable) {
        return orderService.findOrdersFiltered(
                tenantId,
                filterSearch,
                filterOrderNumber,
                filterCustomerName,
                filterStatus,
                pageable
        );
    }
}
