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
        cardRow.setSpacing(true);

        // Fetch counts
        long customerCount = customerService.getAllCustomers(tenant.getId()).size();
        long inquiryCount = inquiryService.getAllInquiries(tenant.getId()).size();
        
        List<TranslationOrder> orders = orderService.getAllOrders(tenant.getId());
        long activeOrdersCount = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.PAID && o.getStatus() != OrderStatus.ARCHIVED && o.getStatus() != OrderStatus.CANCELLED)
                .count();

        BigDecimal totalRevenue = billingService.getAllInvoices(tenant.getId()).stream()
                .map(inv -> inv.getGrossAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cardRow.add(createKpiCard("Kunden", String.valueOf(customerCount), "Registrierte B2B-Kunden", "bg-primary-10"));
        cardRow.add(createKpiCard("Offene Anfragen", String.valueOf(inquiryCount), "Aktive Übersetzungsanfragen", "bg-error-10"));
        cardRow.add(createKpiCard("Aktive Aufträge", String.valueOf(activeOrdersCount), "Laufende Übersetzungsprojekte", "bg-success-10"));
        cardRow.add(createKpiCard("Umsatz (Brutto)", totalRevenue.toString() + " EUR", "Gesamter Abrechnungsumsatz", "bg-contrast-10"));

        add(cardRow);
    }

    private Div createKpiCard(String title, String value, String description, String bgClass) {
        Div card = new Div();
        card.addClassNames("p-m", "border", "border-contrast-20", "rounded-l", "flex", "flex-col");
        card.getStyle().set("flex", "1");
        card.getStyle().set("min-width", "200px");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)");

        Span titleSpan = new Span(title);
        titleSpan.addClassNames("text-s", "text-secondary", "font-medium");

        Span valueSpan = new Span(value);
        valueSpan.addClassNames("text-xxl", "font-bold", "my-xs");
        valueSpan.getStyle().set("color", "var(--lumo-primary-color)");

        Span descSpan = new Span(description);
        descSpan.addClassNames("text-xs", "text-tertiary");

        card.add(titleSpan, valueSpan, descSpan);
        return card;
    }

    private void setupDeadlinesGrid() {
        add(new H3("Anstehende Lieferfristen"));

        Grid<TranslationOrder> grid = new Grid<>(TranslationOrder.class, false);
        grid.setWidthFull();
        grid.setHeight("300px");

        grid.addColumn(TranslationOrder::getOrderNumber).setHeader("Auftragsnummer").setAutoWidth(true);
        grid.addColumn(o -> o.getCustomer() != null ? o.getCustomer().getCompanyName() : "-").setHeader("Kunde").setAutoWidth(true);
        grid.addColumn(TranslationOrder::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(o -> o.getDeliveryDeadline() != null ? o.getDeliveryDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-").setHeader("Lieferfrist").setAutoWidth(true);

        // Fetch active orders sorted by deadline
        List<TranslationOrder> activeOrders = orderService.getAllOrders(tenant.getId()).stream()
                .filter(o -> o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.PAID && o.getStatus() != OrderStatus.ARCHIVED && o.getStatus() != OrderStatus.CANCELLED)
                .sorted((o1, o2) -> {
                    if (o1.getDeliveryDeadline() == null) return 1;
                    if (o2.getDeliveryDeadline() == null) return -1;
                    return o1.getDeliveryDeadline().compareTo(o2.getDeliveryDeadline());
                })
                .collect(Collectors.toList());

        grid.setItems(activeOrders);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("orders/" + event.getValue().getId().toString()));
            }
        });

        add(grid);
    }
}
