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

        SideNavItem dashboard = new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create());
        dashboard.getElement().setAttribute("title", "Dashboard");
        nav.addItem(dashboard);

        SideNavItem customers = new SideNavItem("Kunden", CustomersView.class, VaadinIcon.USERS.create());
        customers.getElement().setAttribute("title", "Kunden");
        nav.addItem(customers);

        SideNavItem inquiries = new SideNavItem("Anfragen", InquiriesView.class, VaadinIcon.QUESTION.create());
        inquiries.getElement().setAttribute("title", "Anfragen");
        nav.addItem(inquiries);

        SideNavItem quotes = new SideNavItem("Angebote", QuotesView.class, VaadinIcon.FILE_TEXT.create());
        quotes.getElement().setAttribute("title", "Angebote");
        nav.addItem(quotes);

        SideNavItem orders = new SideNavItem("Aufträge", OrdersView.class, VaadinIcon.TASKS.create());
        orders.getElement().setAttribute("title", "Aufträge");
        nav.addItem(orders);

        SideNavItem partners = new SideNavItem("Partner", PartnersView.class, VaadinIcon.HANDSHAKE.create());
        partners.getElement().setAttribute("title", "Partner");
        nav.addItem(partners);

        SideNavItem invoices = new SideNavItem("Rechnungen", InvoicesView.class, VaadinIcon.MONEY.create());
        invoices.getElement().setAttribute("title", "Rechnungen");
        nav.addItem(invoices);

        SideNavItem vendorInvoices = new SideNavItem("Eingangsrechnungen & Kosten", VendorInvoicesView.class, VaadinIcon.RECORDS.create());
        vendorInvoices.getElement().setAttribute("title", "Eingangsrechnungen & Kosten");
        nav.addItem(vendorInvoices);

        SideNavItem pricing = new SideNavItem("Preise & Tarife", PricingView.class, VaadinIcon.COINS.create());
        pricing.getElement().setAttribute("title", "Preise & Tarife");
        nav.addItem(pricing);

        addToDrawer(nav);
    }
}
