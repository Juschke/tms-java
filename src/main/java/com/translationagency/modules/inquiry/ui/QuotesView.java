package com.translationagency.modules.inquiry.ui;

import com.translationagency.modules.inquiry.application.InquiryService;
import com.translationagency.modules.inquiry.domain.Quote;
import com.translationagency.modules.inquiry.domain.QuoteStatus;
import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Route(value = "quotes", layout = MainLayout.class)
@PageTitle("Angebote | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class QuotesView extends VerticalLayout {

    private final InquiryService inquiryService;
    private final OrderService orderService;
    private final PdfService pdfService;
    private final SecurityService securityService;

    private QuoteEnterpriseGrid grid;
    private Tenant currentTenant;

    public QuotesView(InquiryService inquiryService, OrderService orderService,
                      PdfService pdfService, SecurityService securityService) {
        this.inquiryService = inquiryService;
        this.orderService = orderService;
        this.pdfService = pdfService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.currentTenant = t);

        if (currentTenant != null) {
            grid = new QuoteEnterpriseGrid(inquiryService, pdfService, currentTenant.getId(),
                    this::convertToOrder, this::deleteQuote);
        }

        add(createHeaderLayout());
        if (grid != null) {
            add(grid);
            setFlexGrow(1, grid);
        }
    }

    private HorizontalLayout createHeaderLayout() {
        H2 headerTitle = new H2("Angebotsverwaltung");
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

    private void convertToOrder(Quote quote) {
        try {
            String username = securityService.getAuthenticatedUser()
                    .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                    .orElse("system");
            TranslationOrder order = orderService.createOrderFromQuote(quote, username);
            Notification.show("Auftrag " + order.getOrderNumber() + " erfolgreich erstellt!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            updateList();
        } catch (Exception ex) {
            Notification.show("Fehler beim Erstellen des Auftrags: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteQuote(Quote quote) {
        try {
            String username = securityService.getAuthenticatedUser()
                    .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                    .orElse("system");
            inquiryService.deleteQuote(quote.getId(), username);
            Notification.show("Angebot erfolgreich storniert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            updateList();
        } catch (Exception ex) {
            Notification.show("Fehler beim Stornieren des Angebots: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
