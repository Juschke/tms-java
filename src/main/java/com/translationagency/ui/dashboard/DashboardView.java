package com.translationagency.ui.dashboard;

import com.translationagency.modules.billing.application.BillingService;
import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.inquiry.application.InquiryService;
import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.OrderStatus;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | Translation Management")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final CustomerService customerService;
    private final InquiryService inquiryService;
    private final OrderService orderService;
    private final BillingService billingService;
    private final SecurityService securityService;

    private Tenant tenant;

    public DashboardView(CustomerService customerService, InquiryService inquiryService,
                         OrderService orderService, BillingService billingService,
                         SecurityService securityService) {
        this.customerService = customerService;
        this.inquiryService = inquiryService;
        this.orderService = orderService;
        this.billingService = billingService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.tenant = t);

        add(new H2("Willkommen im Übersetzungs-Portal"));

        if (tenant != null) {
            setupKpiCards();
            setupDeadlinesGrid();
        }
    }

    private void setupKpiCards() {
        HorizontalLayout cardRow = new HorizontalLayout();
        cardRow.setWidthFull();
        cardRow.getStyle().set("gap", "1.5rem");
        cardRow.getStyle().set("flex-wrap", "wrap");

        // Fetch counts
        long customerCount = customerService.getAllCustomers(tenant.getId()).size();
        long inquiryCount = inquiryService.getAllInquiries(tenant.getId()).size();
        
        List<TranslationOrder> orders = orderService.getAllOrders(tenant.getId());
        long activeOrdersCount = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.PAID && o.getStatus() != OrderStatus.ARCHIVED && o.getStatus() != OrderStatus.CANCELLED)
                .count();

        BigDecimal totalRevenue = billingService.getAllInvoices(tenant.getId()).stream()
                .map(inv -> inv.getGrossAmount() != null ? inv.getGrossAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cardRow.add(createKpiCard("Kunden", String.valueOf(customerCount), "Registrierte B2B-Kunden", "var(--lumo-shade-5pct)"));
        cardRow.add(createKpiCard("Offene Anfragen", String.valueOf(inquiryCount), "Aktive Übersetzungsanfragen", "var(--lumo-shade-5pct)"));
        cardRow.add(createKpiCard("Aktive Aufträge", String.valueOf(activeOrdersCount), "Laufende Übersetzungsprojekte", "var(--lumo-shade-5pct)"));
        cardRow.add(createKpiCard("Umsatz (Brutto)", String.format("%.2f €", totalRevenue), "Gesamter Abrechnungsumsatz", "var(--lumo-primary-color-10pct)"));

        add(cardRow);
    }

    private Div createKpiCard(String title, String value, String description, String bgColor) {
        Div card = new Div();
        card.addClassNames("p-l", "border", "border-contrast-10", "flex", "flex-col");
        card.getStyle().set("flex", "1 1 200px");
        card.getStyle().set("min-width", "220px");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("background-color", bgColor);
        card.getStyle().set("box-shadow", "0 4px 6px rgba(0,0,0,0.05)");
        card.getStyle().set("padding", "1.5rem");

        Span titleSpan = new Span(title);
        titleSpan.addClassNames("text-m", "font-semibold");
        titleSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.addClassNames("text-3xl", "font-bold", "my-s");
        valueSpan.getStyle().set("color", "var(--lumo-primary-text-color)");

        Span descSpan = new Span(description);
        descSpan.addClassNames("text-xs");
        descSpan.getStyle().set("color", "var(--lumo-tertiary-text-color)");

        card.add(titleSpan, valueSpan, descSpan);
        return card;
    }

    private void setupDeadlinesGrid() {
        HorizontalLayout gridsLayout = new HorizontalLayout();
        gridsLayout.setWidthFull();
        gridsLayout.getStyle().set("gap", "2rem");
        gridsLayout.getStyle().set("flex-wrap", "wrap");

        // --- Left Grid: Deadlines ---
        VerticalLayout leftLayout = new VerticalLayout();
        leftLayout.setPadding(false);
        leftLayout.getStyle().set("flex", "1 1 450px");
        leftLayout.add(new H3("Anstehende Lieferfristen"));

        Grid<TranslationOrder> orderGrid = new Grid<>(TranslationOrder.class, false);
        orderGrid.setWidthFull();
        orderGrid.setHeight("350px");
        orderGrid.getStyle().set("border-radius", "8px");
        orderGrid.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)");

        orderGrid.addColumn(TranslationOrder::getOrderNumber).setHeader("Auftragsnummer").setAutoWidth(true);
        orderGrid.addColumn(o -> o.getCustomer() != null ? o.getCustomer().getCompanyName() : "-").setHeader("Kunde").setAutoWidth(true);
        orderGrid.addColumn(TranslationOrder::getStatus).setHeader("Status").setAutoWidth(true);
        orderGrid.addColumn(o -> o.getDeliveryDeadline() != null ? o.getDeliveryDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-").setHeader("Frist").setAutoWidth(true);

        List<TranslationOrder> activeOrders = orderService.getAllOrders(tenant.getId()).stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.PAID && o.getStatus() != OrderStatus.ARCHIVED && o.getStatus() != OrderStatus.CANCELLED)
                .sorted((o1, o2) -> {
                    if (o1.getDeliveryDeadline() == null) return 1;
                    if (o2.getDeliveryDeadline() == null) return -1;
                    return o1.getDeliveryDeadline().compareTo(o2.getDeliveryDeadline());
                })
                .limit(10)
                .collect(Collectors.toList());

        orderGrid.setItems(activeOrders);
        orderGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("orders/" + event.getValue().getId().toString()));
            }
        });
        leftLayout.add(orderGrid);

        // --- Right Grid: Recent Invoices ---
        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setPadding(false);
        rightLayout.getStyle().set("flex", "1 1 450px");
        rightLayout.add(new H3("Aktuelle Rechnungen"));

        Grid<com.translationagency.modules.billing.domain.Invoice> invoiceGrid = new Grid<>(com.translationagency.modules.billing.domain.Invoice.class, false);
        invoiceGrid.setWidthFull();
        invoiceGrid.setHeight("350px");
        invoiceGrid.getStyle().set("border-radius", "8px");
        invoiceGrid.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)");

        invoiceGrid.addColumn(com.translationagency.modules.billing.domain.Invoice::getInvoiceNumber).setHeader("Rechnungsnr.").setAutoWidth(true);
        invoiceGrid.addColumn(inv -> inv.getCustomer() != null ? inv.getCustomer().getCompanyName() : "-").setHeader("Kunde").setAutoWidth(true);
        invoiceGrid.addColumn(inv -> inv.getGrossAmount() != null ? String.format("%.2f €", inv.getGrossAmount()) : "0.00 €").setHeader("Betrag").setAutoWidth(true);
        invoiceGrid.addColumn(com.translationagency.modules.billing.domain.Invoice::getStatus).setHeader("Status").setAutoWidth(true);

        List<com.translationagency.modules.billing.domain.Invoice> recentInvoices = billingService.getAllInvoices(tenant.getId()).stream()
                .sorted((i1, i2) -> {
                    if (i1.getIssuedAt() == null) return 1;
                    if (i2.getIssuedAt() == null) return -1;
                    return i2.getIssuedAt().compareTo(i1.getIssuedAt());
                })
                .limit(10)
                .collect(Collectors.toList());

        invoiceGrid.setItems(recentInvoices);
        rightLayout.add(invoiceGrid);

        gridsLayout.add(leftLayout, rightLayout);
        add(gridsLayout);
    }
}
