package com.translationagency.modules.order.ui;

import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.order.domain.OrderStatus;
import com.translationagency.shared.ui.BaseEnterpriseGrid;
import com.translationagency.shared.ui.Confirmations;
import com.translationagency.shared.ui.Notifications;
import com.translationagency.shared.ui.StatusBadge;
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
    private final String       currentUsername;

    private com.vaadin.flow.component.grid.Grid.Column<TranslationOrder> numberCol;
    private com.vaadin.flow.component.grid.Grid.Column<TranslationOrder> customerCol;
    private com.vaadin.flow.component.grid.Grid.Column<TranslationOrder> statusCol;

    private String      filterSearch       = "";
    private String      filterOrderNumber  = "";
    private String      filterCustomerName = "";
    private OrderStatus filterStatus       = null;

    public OrderEnterpriseGrid(OrderService orderService, UUID tenantId, String currentUsername,
                               Consumer<TranslationOrder> onDelete) {
        this.orderService    = orderService;
        this.tenantId        = tenantId;
        this.currentUsername = currentUsername != null ? currentUsername : "system";

        setDeleteAction(onDelete);
        enableGeneralSearch(value -> this.filterSearch = value);

        // Zeilenauswahl navigiert in die Detailansicht
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("orders/detail/" + event.getValue().getId().toString()));
            }
        });

        configureRowActions();
        initialize();
    }

    /**
     * Statusabhaengige Kontextmenue-Aktionen (B2B-UX): der Nutzer sieht pro Zeile
     * nur die Aktionen, die im aktuellen Status fachlich moeglich sind.
     */
    private void configureRowActions() {
        addContextMenuAction("👁️  Details öffnen",
                o -> true,
                o -> getUI().ifPresent(ui -> ui.navigate("orders/detail/" + o.getId().toString())));

        addContextMenuAction("📦  Als geliefert markieren",
                orderService::canDeliver,
                this::deliverOrder);

        addContextMenuAction("🚫  Stornieren",
                orderService::canCancel,
                this::cancelOrder);

        addContextMenuAction("♻️  Reaktivieren",
                o -> o.getStatus() == OrderStatus.CANCELLED,
                this::reopenOrder);
    }

    private void deliverOrder(TranslationOrder o) {
        try {
            orderService.markAsDelivered(o.getId(), currentUsername);
            Notifications.success("Auftrag " + o.getOrderNumber() + " als geliefert markiert.");
            refresh();
        } catch (RuntimeException ex) {
            Notifications.error("Aktion fehlgeschlagen: " + ex.getMessage());
        }
    }

    private void cancelOrder(TranslationOrder o) {
        Confirmations.destructive("Auftrag stornieren",
                "Soll der Auftrag " + o.getOrderNumber() + " wirklich storniert werden?",
                "Stornieren",
                () -> {
                    try {
                        orderService.cancelOrder(o.getId(), currentUsername);
                        Notifications.warning("Auftrag " + o.getOrderNumber() + " wurde storniert.");
                        refresh();
                    } catch (RuntimeException ex) {
                        Notifications.error("Stornierung fehlgeschlagen: " + ex.getMessage());
                    }
                });
    }

    private void reopenOrder(TranslationOrder o) {
        try {
            orderService.reopenOrder(o.getId(), currentUsername);
            Notifications.success("Auftrag " + o.getOrderNumber() + " wurde reaktiviert.");
            refresh();
        } catch (RuntimeException ex) {
            Notifications.error("Reaktivierung fehlgeschlagen: " + ex.getMessage());
        }
    }

    @Override
    protected void configureColumns() {
        numberCol   = addSortableTextColumn(TranslationOrder::getOrderNumber, "Auftragsnummer", "orderNumber");
        customerCol = addSortableTextColumn(o -> o.getCustomer() != null ? o.getCustomer().getCompanyName() : "–",
                "Kunde", "customer.companyName");

        addSortableTextColumn(o -> o.getNetAmount() + " €", "Netto", "netAmount");
        addSortableTextColumn(o -> o.getGrossAmount() + " €", "Brutto", "grossAmount");

        statusCol = addComponentColumn(o -> StatusBadge.create(statusLabel(o.getStatus()), statusTone(o.getStatus())),
                "Status");

        addSortableTextColumn(o -> o.getDeliveryDeadline() != null ? o.getDeliveryDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "–",
                "Liefertermin", "deliveryDeadline");

        addSortableTextColumn(o -> o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "–",
                "Erstellt am", "createdAt");
    }

    static String statusLabel(OrderStatus status) {
        if (status == null) return "–";
        switch (status) {
            case CREATED:            return "Erstellt";
            case ASSIGNED:           return "Partner zugewiesen";
            case IN_PROGRESS:        return "In Bearbeitung";
            case QA_READY:           return "QS-Bereit";
            case READY_FOR_DELIVERY: return "Lieferbereit";
            case DELIVERED:          return "Geliefert";
            case INVOICED:           return "Abgerechnet";
            case PAID:               return "Bezahlt";
            case ARCHIVED:           return "Archiviert";
            case CANCELLED:          return "Storniert";
            default:                 return status.name();
        }
    }

    static StatusBadge.Tone statusTone(OrderStatus status) {
        if (status == null) return StatusBadge.Tone.NEUTRAL;
        switch (status) {
            case CREATED:            return StatusBadge.Tone.NEUTRAL;
            case ASSIGNED:
            case IN_PROGRESS:
            case QA_READY:           return StatusBadge.Tone.INFO;
            case READY_FOR_DELIVERY: return StatusBadge.Tone.WARNING;
            case DELIVERED:
            case INVOICED:
            case PAID:
            case ARCHIVED:           return StatusBadge.Tone.SUCCESS;
            case CANCELLED:          return StatusBadge.Tone.DANGER;
            default:                 return StatusBadge.Tone.NEUTRAL;
        }
    }

    @Override
    protected void configureFilters(HeaderRow filterRow) {
        addTextFilter(numberCol,   value -> this.filterOrderNumber = value);
        addTextFilter(customerCol, value -> this.filterCustomerName = value);
        addComboBoxFilter(statusCol, Arrays.asList(OrderStatus.values()),
                OrderEnterpriseGrid::statusLabel, value -> this.filterStatus = value);
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
