package com.translationagency.ui;

import com.translationagency.modules.billing.ui.InvoicesView;
import com.translationagency.modules.billing.ui.VendorInvoicesView;
import com.translationagency.modules.crm.ui.CustomersView;
import com.translationagency.modules.inquiry.ui.InquiriesView;
import com.translationagency.modules.inquiry.ui.QuotesView;
import com.translationagency.modules.order.ui.OrdersView;
import com.translationagency.modules.partner.ui.PartnersView;
import com.translationagency.modules.pricing.ui.PricingView;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.dashboard.DashboardView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

public class MainLayout extends AppLayout {

    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Übersetzungs-Portal");
        logo.addClassNames("text-l", "m-m");

        Button logout = new Button("Abmelden", VaadinIcon.SIGN_OUT.create(), e -> securityService.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        String username = securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("Gast");

        Span userLabel = new Span(username);
        userLabel.addClassNames("text-s", "text-secondary", "margin-right-m");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, userLabel, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Kunden", CustomersView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem("Anfragen", InquiriesView.class, VaadinIcon.QUESTION.create()));
        nav.addItem(new SideNavItem("Angebote", QuotesView.class, VaadinIcon.FILE_TEXT.create()));
        nav.addItem(new SideNavItem("Aufträge", OrdersView.class, VaadinIcon.TASKS.create()));
        nav.addItem(new SideNavItem("Partner", PartnersView.class, VaadinIcon.HANDSHAKE.create()));
        nav.addItem(new SideNavItem("Rechnungen", InvoicesView.class, VaadinIcon.MONEY.create()));
        nav.addItem(new SideNavItem("Eingangsrechnungen & Kosten", VendorInvoicesView.class, VaadinIcon.RECORDS.create()));
        nav.addItem(new SideNavItem("Preise & Tarife", PricingView.class, VaadinIcon.COINS.create()));

        addToDrawer(nav);
    }
}
