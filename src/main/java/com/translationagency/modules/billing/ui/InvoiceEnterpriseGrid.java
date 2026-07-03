package com.translationagency.modules.billing.ui;

import com.translationagency.modules.billing.application.BillingService;
import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.billing.domain.InvoiceStatus;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.shared.ui.BaseEnterpriseGrid;
import com.translationagency.shared.ui.StatusBadge;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

public class InvoiceEnterpriseGrid extends BaseEnterpriseGrid<Invoice> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BillingService billingService;
    private final UUID           tenantId;

    private com.vaadin.flow.component.grid.Grid.Column<Invoice> numberCol;
    private com.vaadin.flow.component.grid.Grid.Column<Invoice> customerCol;
    private com.vaadin.flow.component.grid.Grid.Column<Invoice> statusCol;

    private String                   filterSearch   = "";
    private String                   filterNumber   = "";
    private String                   filterCustomer = "";
    private InvoiceStatus            filterStatus   = null;
    private java.time.OffsetDateTime filterIssuedFrom = null;
    private java.time.OffsetDateTime filterIssuedTo   = null;

    public InvoiceEnterpriseGrid(BillingService billingService, PdfService pdfService, UUID tenantId,
                                 Consumer<Invoice> onSendEmail, Consumer<Invoice> onRecordPayment) {
        this.billingService = billingService;
        this.tenantId       = tenantId;

        // Wire PDF Download context menu action
        addContextMenuAction("📄 PDF herunterladen", invoice -> {
            StreamResource pdfResource = new StreamResource(invoice.getInvoiceNumber() + ".pdf", () -> pdfService.generateInvoicePdf(invoice));
            StreamRegistration registration = VaadinSession.getCurrent().getResourceRegistry().registerResource(pdfResource);
            UI.getCurrent().getPage().open(registration.getResourceUri().toString());
        });

        addContextMenuAction("E-Mail versenden",
                invoice -> invoice.getStatus() != InvoiceStatus.PAID
                        && invoice.getStatus() != InvoiceStatus.CANCELLED,
                onSendEmail::accept);

        // Zahlung buchen – nur solange die Rechnung nicht vollstaendig bezahlt/storniert ist
        addContextMenuAction("💸 Zahlung buchen",
                invoice -> invoice.getStatus() != InvoiceStatus.PAID
                        && invoice.getStatus() != InvoiceStatus.CANCELLED,
                onRecordPayment::accept);

        // Enable general search
        enableGeneralSearch(value -> this.filterSearch = value);

        // Add advanced filters for date range
        DatePicker fromPicker = new DatePicker();
        fromPicker.setPlaceholder("Von");
        fromPicker.setClearButtonVisible(true);
        fromPicker.addValueChangeListener(e -> {
            this.filterIssuedFrom = e.getValue() != null ? e.getValue().atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime() : null;
            resetToFirstPage();
        });
        addAdvancedFilter(fromPicker, "Erstellt von");

        DatePicker toPicker = new DatePicker();
        toPicker.setPlaceholder("Bis");
        toPicker.setClearButtonVisible(true);
        toPicker.addValueChangeListener(e -> {
            this.filterIssuedTo = e.getValue() != null ? e.getValue().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime() : null;
            resetToFirstPage();
        });
        addAdvancedFilter(toPicker, "Erstellt bis");

        initialize();
    }

    @Override
    protected void configureColumns() {
        numberCol   = addSortableTextColumn(Invoice::getInvoiceNumber, "Rechnungsnr.", "invoiceNumber");
        customerCol = addSortableTextColumn(inv -> inv.getCustomer() != null ? inv.getCustomer().getCompanyName() : "–",
                "Kunde", "customer.companyName");

        addSortableTextColumn(inv -> String.format("%.2f €", inv.getGrossAmount()), "Brutto", "grossAmount");

        statusCol = addComponentColumn(inv -> StatusBadge.create(statusLabel(inv.getStatus()), statusTone(inv.getStatus())),
                "Status");

        addSortableTextColumn(inv -> inv.getIssuedAt() != null ? inv.getIssuedAt().format(DATE_FMT) : "–",
                "Rechnungsdatum", "issuedAt");
    }

    @Override
    protected void configureFilters(HeaderRow filterRow) {
        addTextFilter(numberCol,   value -> this.filterNumber   = value);
        addTextFilter(customerCol, value -> this.filterCustomer = value);
        addComboBoxFilter(statusCol, Arrays.asList(InvoiceStatus.values()),
                InvoiceEnterpriseGrid::statusLabel, value -> this.filterStatus = value);
    }

    static String statusLabel(InvoiceStatus status) {
        if (status == null) return "–";
        switch (status) {
            case DRAFT:          return "Entwurf";
            case ISSUED:         return "Gesendet";
            case PARTIALLY_PAID: return "Teilweise bezahlt";
            case PAID:           return "Bezahlt";
            case DUNNED:         return "Gemahnt";
            case CANCELLED:      return "Storniert";
            default:             return status.name();
        }
    }

    static StatusBadge.Tone statusTone(InvoiceStatus status) {
        if (status == null) return StatusBadge.Tone.NEUTRAL;
        switch (status) {
            case DRAFT:          return StatusBadge.Tone.NEUTRAL;
            case ISSUED:         return StatusBadge.Tone.INFO;
            case PARTIALLY_PAID: return StatusBadge.Tone.WARNING;
            case PAID:           return StatusBadge.Tone.SUCCESS;
            case DUNNED:         return StatusBadge.Tone.DANGER;
            case CANCELLED:      return StatusBadge.Tone.DANGER;
            default:             return StatusBadge.Tone.NEUTRAL;
        }
    }

    @Override
    protected Page<Invoice> loadPage(Pageable pageable) {
        return billingService.findInvoicesFiltered(
                tenantId,
                filterSearch,
                filterNumber,
                filterCustomer,
                filterStatus,
                filterIssuedFrom,
                filterIssuedTo,
                pageable
        );
    }
}
