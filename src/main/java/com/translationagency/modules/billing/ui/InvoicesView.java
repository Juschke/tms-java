package com.translationagency.modules.billing.ui;

import com.translationagency.modules.billing.application.BillingService;
import com.translationagency.modules.billing.application.DunningService;
import com.translationagency.modules.billing.domain.*;
import com.translationagency.modules.communication.application.CommunicationService;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Route(value = "invoices", layout = MainLayout.class)
@PageTitle("Rechnungen | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class InvoicesView extends VerticalLayout {

    private final BillingService billingService;
    private final SecurityService securityService;
    private final PdfService pdfService;
    private final DunningService dunningService;
    private final CommunicationService communicationService;

    private final Tabs tabs = new Tabs();
    private final Div contentContainer = new Div();

    // Tab layouts
    private VerticalLayout invoicesLayout;
    private VerticalLayout dunningLayout;

    // Grids
    private InvoiceEnterpriseGrid invoiceGrid;
    private final Grid<Invoice> overdueGrid = new Grid<>(Invoice.class, false);

    private Tenant currentTenant;

    public InvoicesView(BillingService billingService, SecurityService securityService, PdfService pdfService,
                        DunningService dunningService, CommunicationService communicationService) {
        this.billingService = billingService;
        this.securityService = securityService;
        this.pdfService = pdfService;
        this.dunningService = dunningService;
        this.communicationService = communicationService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.currentTenant = t);

        add(createHeaderLayout());

        createTabs();
        add(tabs, contentContainer);
        contentContainer.setSizeFull();

        if (currentTenant != null) {
            setupTabContents();
        }
    }

    private HorizontalLayout createHeaderLayout() {
        H2 headerTitle = new H2("Rechnungen & Finanzen");
        headerTitle.addClassNames("m-0", "text-xl");

        HorizontalLayout header = new HorizontalLayout(headerTitle);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        return header;
    }

    private void createTabs() {
        Tab invoicesTab = new Tab("Rechnungen");
        Tab dunningTab = new Tab("Mahnwesen");

        tabs.add(invoicesTab, dunningTab);
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(invoicesTab)) {
                contentContainer.add(invoicesLayout);
                updateInvoiceList();
            } else if (event.getSelectedTab().equals(dunningTab)) {
                contentContainer.add(dunningLayout);
                updateOverdueList();
            }
        });
    }

    private void setupTabContents() {
        setupInvoicesTab();
        setupDunningTab();

        contentContainer.removeAll();
        contentContainer.add(invoicesLayout);
        tabs.setSelectedIndex(0);
        updateInvoiceList();
    }

    private void setupInvoicesTab() {
        invoicesLayout = new VerticalLayout();
        invoicesLayout.setSizeFull();
        invoicesLayout.setPadding(false);

        if (currentTenant != null) {
            invoiceGrid = new InvoiceEnterpriseGrid(billingService, pdfService, currentTenant.getId(),
                    this::sendInvoiceEmail, this::openPaymentDialog);
            invoiceGrid.setSizeFull();
            invoicesLayout.add(invoiceGrid);
        }
    }

    private void setupDunningTab() {
        dunningLayout = new VerticalLayout();
        dunningLayout.setSizeFull();
        dunningLayout.setPadding(false);

        overdueGrid.setSizeFull();
        overdueGrid.addColumn(Invoice::getInvoiceNumber).setHeader("Rechnungsnummer").setAutoWidth(true);
        overdueGrid.addColumn(inv -> inv.getCustomer() != null ? inv.getCustomer().getCompanyName() : "-").setHeader("Kunde").setAutoWidth(true);
        overdueGrid.addColumn(inv -> inv.getDueAt() != null ? inv.getDueAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-").setHeader("Fälligkeitsdatum").setAutoWidth(true);
        overdueGrid.addColumn(inv -> {
            if (inv.getDueAt() == null) return "-";
            long days = ChronoUnit.DAYS.between(inv.getDueAt().toLocalDate(), LocalDate.now());
            return days > 0 ? days + " Tage" : "0 Tage";
        }).setHeader("Überfällig seit").setAutoWidth(true);
        overdueGrid.addColumn(Invoice::getReminderLevel).setHeader("Aktuelle Mahnstufe").setAutoWidth(true);

        overdueGrid.addComponentColumn(invoice -> {
            HorizontalLayout actionLayout = new HorizontalLayout();

            int nextLevel = invoice.getReminderLevel() + 1;
            String btnText = nextLevel == 1 ? "Erinnern" : "Mahnung Lvl " + nextLevel;
            Button remindBtn = new Button(btnText, VaadinIcon.BELL.create(), e -> {
                try {
                    DunningLog log = dunningService.sendReminder(invoice, nextLevel);
                    String username = securityService.getAuthenticatedUser()
                            .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                            .orElse("system");
                    communicationService.sendDunningEmail(log.getId(), username);
                    updateOverdueList();
                    Notification.show("Mahnung erfolgreich versendet").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show("Fehler beim Senden: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            remindBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            actionLayout.add(remindBtn);

            // History button
            Button historyBtn = new Button(VaadinIcon.LIST.create(), e -> openDunningHistoryDialog(invoice));
            historyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            historyBtn.setTooltipText("Mahn-Verlauf");
            actionLayout.add(historyBtn);

            return actionLayout;
        }).setHeader("Aktionen").setAutoWidth(true);

        dunningLayout.add(overdueGrid);
    }

    private void updateInvoiceList() {
        if (invoiceGrid != null) {
            invoiceGrid.refresh();
        }
    }

    private void updateOverdueList() {
        if (currentTenant != null) {
            List<Invoice> overdueInvoices = billingService.getAllInvoices(currentTenant.getId()).stream()
                    .filter(inv -> inv.getStatus() != InvoiceStatus.PAID && inv.getStatus() != InvoiceStatus.CANCELLED)
                    .filter(inv -> inv.getDueAt() != null && inv.getDueAt().isBefore(OffsetDateTime.now()))
                    .collect(Collectors.toList());
            overdueGrid.setItems(overdueInvoices);
        }
    }

    private void openDunningHistoryDialog(Invoice invoice) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Mahnverlauf für " + invoice.getInvoiceNumber());
        dialog.setWidth("600px");

        Grid<DunningLog> historyGrid = new Grid<>(DunningLog.class, false);
        historyGrid.addColumn(DunningLog::getLevel).setHeader("Mahnstufe").setAutoWidth(true);
        historyGrid.addColumn(log -> log.getFeeAmount().toString() + " EUR").setHeader("Gebühr").setAutoWidth(true);
        historyGrid.addColumn(log -> log.getSentAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).setHeader("Gesendet am").setAutoWidth(true);
        historyGrid.addComponentColumn(log -> {
            StreamResource pdfResource = new StreamResource("dunning_lvl_" + log.getLevel() + ".pdf", () -> dunningService.downloadDunningPdf(log));
            Anchor downloadLink = new Anchor(pdfResource, "");
            downloadLink.getElement().setAttribute("download", true);
            Button downloadBtn = new Button(VaadinIcon.DOWNLOAD.create());
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            downloadLink.add(downloadBtn);
            return downloadLink;
        }).setHeader("PDF").setAutoWidth(true);

        historyGrid.setItems(dunningService.getDunningLogsForInvoice(invoice.getId()));

        dialog.add(historyGrid);
        Button closeBtn = new Button("Schließen", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private void sendInvoiceEmail(Invoice invoice) {
        try {
            String username = securityService.getAuthenticatedUser()
                    .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                    .orElse("system");
            communicationService.sendInvoiceEmail(invoice.getId(), username);
            updateInvoiceList();
            Notification.show("Rechnung wurde per E-Mail versendet").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification.show("Versand fehlgeschlagen: " + ex.getMessage(), 6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openPaymentDialog(Invoice invoice) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Zahlungseingang für " + invoice.getInvoiceNumber());

        FormLayout formLayout = new FormLayout();
        NumberField amountField = new NumberField("Zahlungsbetrag (EUR)");
        amountField.setValue(invoice.getGrossAmount().doubleValue());

        ComboBox<String> methodCombo = new ComboBox<>("Zahlungsart");
        methodCombo.setItems("BANK_TRANSFER", "CREDIT_CARD", "PAYPAL", "CASH");
        methodCombo.setValue("BANK_TRANSFER");

        TextField refField = new TextField("Transaktions-Referenz");

        formLayout.add(amountField, methodCombo, refField);

        Button saveButton = new Button("Buchen", e -> {
            if (amountField.getValue() == null) {
                Notification.show("Bitte einen Betrag eingeben").addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String username = securityService.getAuthenticatedUser()
                    .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                    .orElse("system");
            billingService.recordPayment(
                    invoice,
                    BigDecimal.valueOf(amountField.getValue()),
                    methodCombo.getValue(),
                    refField.getValue(),
                    username
            );
            dialog.close();
            updateInvoiceList();
            Notification.show("Zahlung erfolgreich verbucht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }
}
