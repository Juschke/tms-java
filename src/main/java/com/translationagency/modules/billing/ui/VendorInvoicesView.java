package com.translationagency.modules.billing.ui;

import com.translationagency.modules.billing.application.VendorInvoiceService;
import com.translationagency.modules.billing.domain.ExternalCost;
import com.translationagency.modules.billing.domain.VendorInvoice;
import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.tenant.application.NumberRangeService;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Route(value = "vendor-invoices", layout = MainLayout.class)
@PageTitle("Eingangsrechnungen & Fremdkosten | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER"})
public class VendorInvoicesView extends VerticalLayout {

    private final VendorInvoiceService vendorInvoiceService;
    private final PartnerService partnerService;
    private final OrderService orderService;
    private final SecurityService securityService;
    private final NumberRangeService numberRangeService;

    private final Tabs tabs = new Tabs();
    private final Div contentContainer = new Div();

    // Tab Layouts
    private VerticalLayout vendorInvoicesLayout;
    private VerticalLayout externalCostsLayout;

    // Grids
    private final Grid<VendorInvoice> vendorInvoiceGrid = new Grid<>(VendorInvoice.class, false);
    private final Grid<ExternalCost> externalCostGrid = new Grid<>(ExternalCost.class, false);

    private Tenant currentTenant;

    public VendorInvoicesView(VendorInvoiceService vendorInvoiceService,
                              PartnerService partnerService,
                              OrderService orderService,
                              SecurityService securityService,
                              NumberRangeService numberRangeService) {
        this.vendorInvoiceService = vendorInvoiceService;
        this.partnerService = partnerService;
        this.orderService = orderService;
        this.securityService = securityService;
        this.numberRangeService = numberRangeService;

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
        H2 headerTitle = new H2("Eingangsrechnungen & Honorare");
        headerTitle.addClassNames("m-0", "text-xl");

        HorizontalLayout header = new HorizontalLayout(headerTitle);
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        return header;
    }

    private void createTabs() {
        Tab invoicesTab = new Tab("Übersetzer-Rechnungen");
        Tab costsTab = new Tab("Sonstige Fremdkosten");

        tabs.add(invoicesTab, costsTab);
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(invoicesTab)) {
                contentContainer.add(vendorInvoicesLayout);
                refreshVendorInvoices();
            } else if (event.getSelectedTab().equals(costsTab)) {
                contentContainer.add(externalCostsLayout);
                refreshExternalCosts();
            }
        });
    }

    private void setupTabContents() {
        setupVendorInvoicesTab();
        setupExternalCostsTab();

        contentContainer.removeAll();
        contentContainer.add(vendorInvoicesLayout);
        tabs.setSelectedIndex(0);
        refreshVendorInvoices();
    }

    private void setupVendorInvoicesTab() {
        vendorInvoicesLayout = new VerticalLayout();
        vendorInvoicesLayout.setSizeFull();
        vendorInvoicesLayout.setPadding(false);

        Button addInvoiceBtn = new Button("Eingangsrechnung erfassen", VaadinIcon.PLUS.create(), e -> openVendorInvoiceDialog(new VendorInvoice()));
        addInvoiceBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        vendorInvoiceGrid.setSizeFull();
        vendorInvoiceGrid.addColumn(VendorInvoice::getInvoiceNumber).setHeader("Rechnungsnummer").setAutoWidth(true).setSortable(true);
        vendorInvoiceGrid.addColumn(vi -> vi.getPartner() != null ? vi.getPartner().getFullName() : "-").setHeader("Übersetzer / Partner").setAutoWidth(true);
        vendorInvoiceGrid.addColumn(vi -> vi.getOrder() != null ? vi.getOrder().getOrderNumber() : "-").setHeader("Auftrag").setAutoWidth(true);
        vendorInvoiceGrid.addColumn(vi -> vi.getDate() != null ? vi.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-").setHeader("Datum").setAutoWidth(true);
        vendorInvoiceGrid.addColumn(vi -> vi.getDueDate() != null ? vi.getDueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-").setHeader("Fällig am").setAutoWidth(true);
        vendorInvoiceGrid.addColumn(vi -> String.format("%.2f EUR", vi.getAmountNet())).setHeader("Netto").setAutoWidth(true);
        vendorInvoiceGrid.addColumn(vi -> String.format("%.2f EUR", vi.getAmountGross())).setHeader("Brutto").setAutoWidth(true);
        vendorInvoiceGrid.addColumn(VendorInvoice::getStatus).setHeader("Status").setAutoWidth(true);
        vendorInvoiceGrid.addColumn(vi -> String.format("%.2f EUR", vi.getPaidAmount())).setHeader("Bezahlt").setAutoWidth(true);

        vendorInvoiceGrid.addComponentColumn(vi -> {
            HorizontalLayout actionLayout = new HorizontalLayout();

            // Record Payment Button
            if (!"paid".equalsIgnoreCase(vi.getStatus())) {
                Button payBtn = new Button(VaadinIcon.MONEY.create(), e -> openVendorPaymentDialog(vi));
                payBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                payBtn.setTooltipText("Zahlung buchen");
                actionLayout.add(payBtn);
            }

            // Delete Button
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                vendorInvoiceService.deleteVendorInvoice(vi.getId());
                refreshVendorInvoices();
                Notification.show("Eingangsrechnung gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            actionLayout.add(deleteBtn);

            return actionLayout;
        }).setHeader("Aktionen").setAutoWidth(true);

        vendorInvoicesLayout.add(addInvoiceBtn, vendorInvoiceGrid);
    }

    private void setupExternalCostsTab() {
        externalCostsLayout = new VerticalLayout();
        externalCostsLayout.setSizeFull();
        externalCostsLayout.setPadding(false);

        Button addCostBtn = new Button("Fremdkosten erfassen", VaadinIcon.PLUS.create(), e -> openExternalCostDialog(new ExternalCost()));
        addCostBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        externalCostGrid.setSizeFull();
        externalCostGrid.addColumn(ExternalCost::getDescription).setHeader("Beschreibung").setAutoWidth(true).setSortable(true);
        externalCostGrid.addColumn(ExternalCost::getCostType).setHeader("Typ").setAutoWidth(true);
        externalCostGrid.addColumn(ec -> String.format("%.2f EUR", ec.getAmount())).setHeader("Betrag").setAutoWidth(true);
        externalCostGrid.addColumn(ec -> ec.getDate() != null ? ec.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-").setHeader("Datum").setAutoWidth(true);
        externalCostGrid.addColumn(ExternalCost::getSupplier).setHeader("Lieferant").setAutoWidth(true);
        externalCostGrid.addColumn(ec -> ec.getOrder() != null ? ec.getOrder().getOrderNumber() : "-").setHeader("Auftrag / Projekt").setAutoWidth(true);

        externalCostGrid.addComponentColumn(ec -> {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                vendorInvoiceService.deleteExternalCost(ec.getId());
                refreshExternalCosts();
                Notification.show("Kosten-Eintrag gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return deleteBtn;
        }).setHeader("Aktionen").setAutoWidth(true);

        externalCostsLayout.add(addCostBtn, externalCostGrid);
    }

    private void refreshVendorInvoices() {
        if (currentTenant != null) {
            vendorInvoiceGrid.setItems(vendorInvoiceService.getAllVendorInvoices(currentTenant.getId()));
        }
    }

    private void refreshExternalCosts() {
        if (currentTenant != null) {
            externalCostGrid.setItems(vendorInvoiceService.getAllExternalCosts(currentTenant.getId()));
        }
    }

    private void openVendorInvoiceDialog(VendorInvoice invoice) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(invoice.getId() == null ? "Neue Eingangsrechnung erfassen" : "Eingangsrechnung bearbeiten");
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();

        ComboBox<Partner> partnerCombo = new ComboBox<>("Übersetzer / Partner");
        partnerCombo.setItems(partnerService.getAllPartners(currentTenant.getId()));
        partnerCombo.setItemLabelGenerator(Partner::getFullName);
        partnerCombo.setRequired(true);

        ComboBox<TranslationOrder> orderCombo = new ComboBox<>("Zugehöriger Auftrag (Optional)");
        orderCombo.setItems(orderService.getAllOrders(currentTenant.getId()));
        orderCombo.setItemLabelGenerator(TranslationOrder::getOrderNumber);

        TextField invoiceNum = new TextField("Rechnungsnummer");
        invoiceNum.setRequired(true);
        Button generateNumBtn = new Button("Auto-Generieren", e -> {
            invoiceNum.setValue(numberRangeService.getNextNumber(currentTenant.getId(), "vendor_invoice"));
        });
        HorizontalLayout numberLayout = new HorizontalLayout(invoiceNum, generateNumBtn);
        numberLayout.setVerticalComponentAlignment(Alignment.END, generateNumBtn);

        TextField reference = new TextField("Referenz");
        DatePicker date = new DatePicker("Rechnungsdatum");
        date.setValue(LocalDate.now());
        DatePicker dueDate = new DatePicker("Fälligkeitsdatum");
        dueDate.setValue(LocalDate.now().plusDays(14));

        BigDecimalField amountNet = new BigDecimalField("Netto-Betrag (EUR)");
        BigDecimalField taxRate = new BigDecimalField("Umsatzsteuersatz (%)");
        taxRate.setValue(BigDecimal.valueOf(19.00));
        BigDecimalField amountGross = new BigDecimalField("Brutto-Betrag (EUR)");
        amountGross.setReadOnly(true);

        TextArea notes = new TextArea("Notizen");

        // Dynamic tax calculation
        amountNet.addValueChangeListener(e -> calculateGross(amountNet, taxRate, amountGross));
        taxRate.addValueChangeListener(e -> calculateGross(amountNet, taxRate, amountGross));

        formLayout.add(partnerCombo, orderCombo, reference, date, dueDate, amountNet, taxRate, amountGross, notes);
        formLayout.addFormItem(numberLayout, "");

        Binder<VendorInvoice> binder = new Binder<>(VendorInvoice.class);
        binder.forField(partnerCombo).asRequired("Übersetzer ist erforderlich").bind(VendorInvoice::getPartner, VendorInvoice::setPartner);
        binder.bind(orderCombo, VendorInvoice::getOrder, VendorInvoice::setOrder);
        binder.forField(invoiceNum).asRequired("Rechnungsnummer ist erforderlich").bind(VendorInvoice::getInvoiceNumber, VendorInvoice::setInvoiceNumber);
        binder.bind(reference, VendorInvoice::getReference, VendorInvoice::setReference);
        binder.forField(date).asRequired("Datum erforderlich").bind(VendorInvoice::getDate, VendorInvoice::setDate);
        binder.bind(dueDate, VendorInvoice::getDueDate, VendorInvoice::setDueDate);
        binder.forField(amountNet).asRequired("Nettobetrag erforderlich").bind(VendorInvoice::getAmountNet, VendorInvoice::setAmountNet);
        binder.bind(taxRate, VendorInvoice::getTaxRate, VendorInvoice::setTaxRate);
        binder.bind(notes, VendorInvoice::getNotes, VendorInvoice::setNotes);

        binder.readBean(invoice);

        Button saveBtn = new Button("Erfassen", e -> {
            try {
                binder.writeBean(invoice);
                invoice.setTenant(currentTenant);

                // calculate gross and tax
                BigDecimal net = invoice.getAmountNet();
                BigDecimal taxRateVal = invoice.getTaxRate() != null ? invoice.getTaxRate() : BigDecimal.valueOf(19.00);
                BigDecimal tax = net.multiply(taxRateVal).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal gross = net.add(tax);

                invoice.setAmountTax(tax);
                invoice.setAmountGross(gross);

                vendorInvoiceService.saveVendorInvoice(invoice);
                dialog.close();
                refreshVendorInvoices();
                Notification.show("Eingangsrechnung erfolgreich erfasst").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(formLayout);
        dialog.open();
    }

    private void calculateGross(BigDecimalField netField, BigDecimalField taxField, BigDecimalField grossField) {
        BigDecimal net = netField.getValue();
        BigDecimal tax = taxField.getValue();
        if (net != null) {
            BigDecimal taxRateVal = tax != null ? tax : BigDecimal.valueOf(19.00);
            BigDecimal taxAmount = net.multiply(taxRateVal).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            grossField.setValue(net.add(taxAmount));
        }
    }

    private void openVendorPaymentDialog(VendorInvoice invoice) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Zahlung buchen für " + invoice.getInvoiceNumber());

        FormLayout formLayout = new FormLayout();
        NumberField amountField = new NumberField("Zahlungsbetrag (EUR)");
        BigDecimal remaining = invoice.getAmountGross().subtract(invoice.getPaidAmount());
        amountField.setValue(remaining.doubleValue());

        formLayout.add(amountField);

        Button saveButton = new Button("Buchen", e -> {
            if (amountField.getValue() == null) {
                Notification.show("Bitte einen Betrag eingeben").addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            vendorInvoiceService.recordVendorPayment(invoice, BigDecimal.valueOf(amountField.getValue()));
            dialog.close();
            refreshVendorInvoices();
            Notification.show("Zahlung erfolgreich gebucht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }

    private void openExternalCostDialog(ExternalCost cost) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neue Fremdkosten erfassen");
        dialog.setWidth("450px");

        FormLayout formLayout = new FormLayout();
        TextField desc = new TextField("Beschreibung");
        desc.setRequired(true);

        ComboBox<String> typeCombo = new ComboBox<>("Kostentyp");
        typeCombo.setItems("Versand & Porto", "Software & Lizenzen", "Unterauftrag", "Büromaterial", "Reisekosten", "Sonstiges");
        typeCombo.setAllowCustomValue(true);
        typeCombo.setRequired(true);

        BigDecimalField amount = new BigDecimalField("Betrag (EUR)");
        amount.setRequired(true);

        DatePicker date = new DatePicker("Datum");
        date.setValue(LocalDate.now());

        TextField supplier = new TextField("Lieferant / Dienstleister");

        ComboBox<TranslationOrder> orderCombo = new ComboBox<>("Zugehöriger Auftrag (Optional)");
        orderCombo.setItems(orderService.getAllOrders(currentTenant.getId()));
        orderCombo.setItemLabelGenerator(TranslationOrder::getOrderNumber);

        TextArea notes = new TextArea("Notizen");

        formLayout.add(desc, typeCombo, amount, date, supplier, orderCombo, notes);

        Binder<ExternalCost> binder = new Binder<>(ExternalCost.class);
        binder.forField(desc).asRequired("Beschreibung ist erforderlich").bind(ExternalCost::getDescription, ExternalCost::setDescription);
        binder.forField(typeCombo).asRequired("Typ erforderlich").bind(ExternalCost::getCostType, ExternalCost::setCostType);
        binder.forField(amount).asRequired("Betrag erforderlich").bind(ExternalCost::getAmount, ExternalCost::setAmount);
        binder.forField(date).asRequired("Datum erforderlich").bind(ExternalCost::getDate, ExternalCost::setDate);
        binder.bind(supplier, ExternalCost::getSupplier, ExternalCost::setSupplier);
        binder.bind(orderCombo, ExternalCost::getOrder, ExternalCost::setOrder);
        binder.bind(notes, ExternalCost::getNotes, ExternalCost::setNotes);

        binder.readBean(cost);

        Button saveBtn = new Button("Speichern", e -> {
            try {
                binder.writeBean(cost);
                cost.setTenant(currentTenant);
                vendorInvoiceService.saveExternalCost(cost);
                dialog.close();
                refreshExternalCosts();
                Notification.show("Fremdkosten erfolgreich gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(formLayout);
        dialog.open();
    }
}
