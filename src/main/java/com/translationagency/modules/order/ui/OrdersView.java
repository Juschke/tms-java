package com.translationagency.modules.order.ui;

import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Aufträge | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class OrdersView extends VerticalLayout {

    private final OrderService orderService;
    private final SecurityService securityService;

    private OrderEnterpriseGrid grid;
    private Tenant currentTenant;

    public OrdersView(OrderService orderService, SecurityService securityService) {
        this.orderService = orderService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.currentTenant = t);

        if (currentTenant != null) {
            grid = new OrderEnterpriseGrid(orderService, currentTenant.getId(), null);
        }

        add(createHeaderLayout());
        if (grid != null) {
            add(grid);
            setFlexGrow(1, grid);
        }
    }

    private HorizontalLayout createHeaderLayout() {
        H2 headerTitle = new H2("Auftragsverwaltung");
        headerTitle.addClassNames("m-0", "text-xl");

        HorizontalLayout header = new HorizontalLayout(headerTitle);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        return header;
    }

    private void updateList() {
        if (grid != null) {
            grid.refresh();
        }
    }
}
