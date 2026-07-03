package com.translationagency.ui.dashboard;

import com.translationagency.modules.billing.application.BillingService;
import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.billing.domain.InvoiceStatus;
import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.inquiry.application.InquiryService;
import com.translationagency.modules.inquiry.domain.Inquiry;
import com.translationagency.modules.inquiry.domain.InquiryStatus;
import com.translationagency.modules.inquiry.domain.Quote;
import com.translationagency.modules.inquiry.domain.QuoteStatus;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
            setupTaskCenter();
            setupDeadlinesGrid();
        }
    }

    private void setupKpiCards() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(false);
        row.getStyle().set("gap", "1.5rem");
        row.getStyle().set("flex-wrap", "wrap");

        long customerCount = customerService.getAllCustomers(tenant.getId()).size();
        long inquiryCount = inquiryService.getAllInquiries(tenant.getId()).size();

        List<TranslationOrder> orders = orderService.getAllOrders(tenant.getId());
        long activeOrdersCount = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.PAID && o.getStatus() != OrderStatus.ARCHIVED && o.getStatus() != OrderStatus.CANCELLED)
                .count();

        BigDecimal totalRevenue = billingService.getAllInvoices(tenant.getId()).stream()
                .map(inv -> inv.getGrossAmount() != null ? inv.getGrossAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        row.add(
            createKpiCard("Kunden", String.valueOf(customerCount), "Registrierte B2B-Kunden"),
            createKpiCard("Offene Anfragen", String.valueOf(inquiryCount), "Aktive Übersetzungsanfragen"),
            createKpiCard("Aktive Aufträge", String.valueOf(activeOrdersCount), "Laufende Übersetzungsprojekte"),
            createKpiCard("Umsatz (Brutto)", String.format("%.2f €", totalRevenue), "Gesamter Abrechnungsumsatz")
        );

        add(row);
    }

    private Div createKpiCard(String title, String value, String description) {
        Div card = new Div();
        card.addClassNames("p-m", "border", "border-contrast-10");
        card.getStyle().set("flex", "1 1 220px");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");
        card.getStyle().set("display", "flex");
        card.getStyle().set("flex-direction", "column");
        card.getStyle().set("gap", "0.5rem");

        Span titleSpan = new Span(title);
        titleSpan.addClassNames("text-m", "font-semibold");
        titleSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.addClassNames("text-3xl", "font-bold");
        valueSpan.getStyle().set("color", "var(--lumo-primary-text-color)");

        Span descSpan = new Span(description);
        descSpan.addClassNames("text-xs");
        descSpan.getStyle().set("color", "var(--lumo-tertiary-text-color)");

        card.add(titleSpan, valueSpan, descSpan);
        return card;
    }

    private void setupTaskCenter() {
        H3 taskTitle = new H3("Aufgaben");
        taskTitle.addClassNames("m-0", "mt-l");

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(false);
        row.getStyle().set("gap", "1.5rem");
        row.getStyle().set("flex-wrap", "wrap");

        row.add(
            createTaskCard("Neue Anfragen", createInquiryTasksGrid()),
            createTaskCard("Angebote versenden", createQuoteTasksGrid()),
            createTaskCard("QS und Lieferung", createOrderTasksGrid()),
            createTaskCard("Fällige Rechnungen", createInvoiceTasksGrid())
        );

        add(taskTitle, row);
    }

    private Div createTaskCard(String title, Grid<?> grid) {
        Div card = new Div();
        card.addClassNames("p-m", "border", "border-contrast-10");
        card.getStyle().set("flex", "1 1 320px");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");
        card.getStyle().set("display", "flex");
        card.getStyle().set("flex-direction", "column");
        card.getStyle().set("gap", "1rem");

        H3 heading = new H3(title);
        heading.addClassNames("m-0");

        card.add(heading, grid);
        return card;
    }

    private Grid<Inquiry> createInquiryTasksGrid() {
        Grid<Inquiry> grid = new Grid<>(Inquiry.class, false);
        styleTaskGrid(grid);
        grid.addColumn(inquiry -> inquiry.getCustomer() != null ? inquiry.getCustomer().getCompanyName() : "-")
                .setHeader("Kunde").setAutoWidth(true);
        grid.addColumn(Inquiry::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(inquiry -> formatDate(inquiry.getCreatedAt())).setHeader("Eingang").setAutoWidth(true);
        grid.setItems(inquiryService.getAllInquiries(tenant.getId()).stream()
                .filter(inquiry -> inquiry.getStatus() == InquiryStatus.RECEIVED
                        || inquiry.getStatus() == InquiryStatus.IN_CALCULATION)
                .limit(8)
                .collect(Collectors.toList()));
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("inquiries/detail/" + event.getValue().getId()));
            }
        });
        return grid;
    }

    private Grid<Quote> createQuoteTasksGrid() {
        Grid<Quote> grid = new Grid<>(Quote.class, false);
        styleTaskGrid(grid);
        grid.addColumn(Quote::getQuoteNumber).setHeader("Angebot").setAutoWidth(true);
        grid.addColumn(quote -> quote.getCustomer() != null ? quote.getCustomer().getCompanyName() : "-")
                .setHeader("Kunde").setAutoWidth(true);
        grid.addColumn(quote -> formatMoney(quote.getGrossAmount())).setHeader("Betrag").setAutoWidth(true);
        grid.addColumn(quote -> formatDate(quote.getExpiresAt())).setHeader("Gültig bis").setAutoWidth(true);
        grid.setItems(inquiryService.getAllQuotes(tenant.getId()).stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.DRAFT)
                .limit(8)
                .collect(Collectors.toList()));
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("quotes"));
            }
        });
        return grid;
    }

    private Grid<TranslationOrder> createOrderTasksGrid() {
        Grid<TranslationOrder> grid = new Grid<>(TranslationOrder.class, false);
        styleTaskGrid(grid);
        grid.addColumn(TranslationOrder::getOrderNumber).setHeader("Auftrag").setAutoWidth(true);
        grid.addColumn(order -> order.getCustomer() != null ? order.getCustomer().getCompanyName() : "-")
                .setHeader("Kunde").setAutoWidth(true);
        grid.addColumn(TranslationOrder::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(order -> formatDate(order.getDeliveryDeadline())).setHeader("Frist").setAutoWidth(true);
        grid.setItems(orderService.getAllOrders(tenant.getId()).stream()
                .filter(order -> order.getStatus() == OrderStatus.QA_READY
                        || order.getStatus() == OrderStatus.READY_FOR_DELIVERY)
                .limit(8)
                .collect(Collectors.toList()));
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("orders/detail/" + event.getValue().getId()));
            }
        });
        return grid;
    }

    private Grid<Invoice> createInvoiceTasksGrid() {
        Grid<Invoice> grid = new Grid<>(Invoice.class, false);
        styleTaskGrid(grid);
        grid.addColumn(Invoice::getInvoiceNumber).setHeader("Rechnung").setAutoWidth(true);
        grid.addColumn(invoice -> invoice.getCustomer() != null ? invoice.getCustomer().getCompanyName() : "-")
                .setHeader("Kunde").setAutoWidth(true);
        grid.addColumn(invoice -> formatDate(invoice.getDueAt())).setHeader("Fällig").setAutoWidth(true);
        grid.addColumn(invoice -> formatMoney(invoice.getGrossAmount())).setHeader("Betrag").setAutoWidth(true);
        grid.setItems(billingService.getAllInvoices(tenant.getId()).stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.PAID
                        && invoice.getStatus() != InvoiceStatus.CANCELLED
                        && invoice.getDueAt() != null
                        && invoice.getDueAt().isBefore(OffsetDateTime.now()))
                .limit(8)
                .collect(Collectors.toList()));
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("invoices"));
            }
        });
        return grid;
    }

    private void styleTaskGrid(Grid<?> grid) {
        grid.setWidthFull();
        grid.setHeight("260px");
        grid.getStyle().set("border-radius", "8px");
    }

    private String formatDate(OffsetDateTime date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-";
    }

    private String formatMoney(BigDecimal amount) {
        return amount != null ? String.format("%.2f EUR", amount) : "0.00 EUR";
    }

    private void setupDeadlinesGrid() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(false);
        row.getStyle().set("gap", "1.5rem");
        row.getStyle().set("flex-wrap", "wrap");

        Grid<TranslationOrder> orderGrid = new Grid<>(TranslationOrder.class, false);
        orderGrid.setWidthFull();
        orderGrid.setHeight("350px");
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
                getUI().ifPresent(ui -> ui.navigate("orders/detail/" + event.getValue().getId().toString()));
            }
        });

        Grid<Invoice> invoiceGrid = new Grid<>(Invoice.class, false);
        invoiceGrid.setWidthFull();
        invoiceGrid.setHeight("350px");
        invoiceGrid.addColumn(Invoice::getInvoiceNumber).setHeader("Rechnungsnr.").setAutoWidth(true);
        invoiceGrid.addColumn(inv -> inv.getCustomer() != null ? inv.getCustomer().getCompanyName() : "-").setHeader("Kunde").setAutoWidth(true);
        invoiceGrid.addColumn(inv -> inv.getGrossAmount() != null ? String.format("%.2f €", inv.getGrossAmount()) : "0.00 €").setHeader("Betrag").setAutoWidth(true);
        invoiceGrid.addColumn(Invoice::getStatus).setHeader("Status").setAutoWidth(true);

        List<Invoice> recentInvoices = billingService.getAllInvoices(tenant.getId()).stream()
                .sorted((i1, i2) -> {
                    if (i1.getIssuedAt() == null) return 1;
                    if (i2.getIssuedAt() == null) return -1;
                    return i2.getIssuedAt().compareTo(i1.getIssuedAt());
                })
                .limit(10)
                .collect(Collectors.toList());

        invoiceGrid.setItems(recentInvoices);

        row.add(
            createGridCard("Anstehende Lieferfristen", orderGrid),
            createGridCard("Aktuelle Rechnungen", invoiceGrid)
        );
        add(row);
    }

    private Div createGridCard(String title, Grid<?> grid) {
        Div card = new Div();
        card.addClassNames("p-m", "border", "border-contrast-10");
        card.getStyle().set("flex", "1 1 450px");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");
        card.getStyle().set("display", "flex");
        card.getStyle().set("flex-direction", "column");
        card.getStyle().set("gap", "1rem");

        H3 heading = new H3(title);
        heading.addClassNames("m-0");

        card.add(heading, grid);
        return card;
    }
}
