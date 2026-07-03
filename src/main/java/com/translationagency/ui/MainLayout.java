package com.translationagency.ui;

import com.translationagency.modules.billing.ui.InvoicesView;
import com.translationagency.modules.billing.ui.VendorInvoicesView;
import com.translationagency.modules.crm.ui.CustomersView;
import com.translationagency.modules.inquiry.ui.InquiriesView;
import com.translationagency.modules.inquiry.ui.QuotesView;
import com.translationagency.modules.order.ui.OrdersView;
import com.translationagency.modules.partner.ui.PartnersView;
import com.translationagency.modules.pricing.ui.PricingView;
import com.translationagency.modules.settings.ui.SettingsView;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.dashboard.DashboardView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);

        // Dashboard
        SideNav dashboardNav = new SideNav();
        dashboardNav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        drawerLayout.add(dashboardNav);

        // Trennlinie
        Hr separator1 = new Hr();
        separator1.addClassNames("my-m");
        drawerLayout.add(separator1);

        // Arbeitsprozess: Anfragen -> Angebote -> Projekte
        H3 workflowTitle = new H3("Arbeitsprozess");
        workflowTitle.addClassNames("text-xs", "text-secondary", "m-0", "px-m", "py-s");
        drawerLayout.add(workflowTitle);

        SideNav workflowNav = new SideNav();
        workflowNav.addItem(new SideNavItem("Anfragen", InquiriesView.class, VaadinIcon.QUESTION.create()));
        workflowNav.addItem(new SideNavItem("Angebote", QuotesView.class, VaadinIcon.FILE_TEXT.create()));
        workflowNav.addItem(new SideNavItem("Projekte", OrdersView.class, VaadinIcon.TASKS.create()));
        drawerLayout.add(workflowNav);

        // Trennlinie
        Hr separator2 = new Hr();
        separator2.addClassNames("my-m");
        drawerLayout.add(separator2);

        // Verwaltung: Kunden & Partner
        H3 managementTitle = new H3("Verwaltung");
        managementTitle.addClassNames("text-xs", "text-secondary", "m-0", "px-m", "py-s");
        drawerLayout.add(managementTitle);

        SideNav managementNav = new SideNav();
        managementNav.addItem(new SideNavItem("Kunden", CustomersView.class, VaadinIcon.USERS.create()));
        managementNav.addItem(new SideNavItem("Partner", PartnersView.class, VaadinIcon.HANDSHAKE.create()));
        drawerLayout.add(managementNav);

        // Trennlinie
        Hr separator3 = new Hr();
        separator3.addClassNames("my-m");
        drawerLayout.add(separator3);

        // Finanzen: Rechnungen & Tarife
        H3 financeTitle = new H3("Finanzen");
        financeTitle.addClassNames("text-xs", "text-secondary", "m-0", "px-m", "py-s");
        drawerLayout.add(financeTitle);

        SideNav financeNav = new SideNav();
        financeNav.addItem(new SideNavItem("Rechnungen", InvoicesView.class, VaadinIcon.MONEY.create()));
        financeNav.addItem(new SideNavItem("Fremdkosten", VendorInvoicesView.class, VaadinIcon.RECORDS.create()));
        financeNav.addItem(new SideNavItem("Preise & Tarife", PricingView.class, VaadinIcon.COINS.create()));
        drawerLayout.add(financeNav);

        // Trennlinie
        Hr separator4 = new Hr();
        separator4.addClassNames("my-m");
        drawerLayout.add(separator4);

        // System
        H3 systemTitle = new H3("System");
        systemTitle.addClassNames("text-xs", "text-secondary", "m-0", "px-m", "py-s");
        drawerLayout.add(systemTitle);

        SideNav systemNav = new SideNav();
        systemNav.addItem(new SideNavItem("Einstellungen", SettingsView.class, VaadinIcon.COG.create()));
        drawerLayout.add(systemNav);

        addToDrawer(drawerLayout);
    }
}
