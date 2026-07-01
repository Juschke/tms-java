package com.translationagency.modules.inquiry.ui;

import com.translationagency.modules.inquiry.application.InquiryService;
import com.translationagency.modules.inquiry.domain.Quote;
import com.translationagency.modules.inquiry.domain.QuoteStatus;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.shared.ui.BaseEnterpriseGrid;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.StreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

public class QuoteEnterpriseGrid extends BaseEnterpriseGrid<Quote> {

    private final InquiryService inquiryService;
    private final PdfService     pdfService;
    private final UUID           tenantId;

    private com.vaadin.flow.component.grid.Grid.Column<Quote> numberCol;
    private com.vaadin.flow.component.grid.Grid.Column<Quote> customerCol;
    private com.vaadin.flow.component.grid.Grid.Column<Quote> statusCol;

    private String      filterSearch       = "";
    private String      filterQuoteNumber  = "";
    private String      filterCustomerName = "";
    private QuoteStatus filterStatus       = null;
    private BigDecimal  filterMinAmount    = null;
    private BigDecimal  filterMaxAmount    = null;

    public QuoteEnterpriseGrid(InquiryService inquiryService, PdfService pdfService, UUID tenantId,
                               Consumer<Quote> onConvertToOrder, Consumer<Quote> onDelete) {
        this.inquiryService = inquiryService;
        this.pdfService     = pdfService;
        this.tenantId       = tenantId;

        // Set actions
        setDeleteAction(onDelete);

        addContextMenuAction("📄 PDF herunterladen", quote -> {
            StreamResource pdfResource = new StreamResource(quote.getQuoteNumber() + ".pdf", () -> pdfService.generateQuotePdf(quote));
            com.vaadin.flow.server.StreamRegistration registration = com.vaadin.flow.server.VaadinSession.getCurrent().getResourceRegistry().registerResource(pdfResource);
            UI.getCurrent().getPage().open(registration.getResourceUri().toString());
        });

        addContextMenuAction("📋 Auftrag erstellen", quote -> {
            if (quote.getStatus() != QuoteStatus.ACCEPTED) {
                onConvertToOrder.accept(quote);
            }
        });

        // Enable general search
        enableGeneralSearch(value -> this.filterSearch = value);

        // Add advanced filters for gross amount range
        NumberField minAmountField = new NumberField();
        minAmountField.setPlaceholder("Min (€)");
        minAmountField.setClearButtonVisible(true);
        minAmountField.addValueChangeListener(e -> {
            this.filterMinAmount = e.getValue() != null ? BigDecimal.valueOf(e.getValue()) : null;
            resetToFirstPage();
        });
        addAdvancedFilter(minAmountField, "Min. Betrag (€)");

        NumberField maxAmountField = new NumberField();
        maxAmountField.setPlaceholder("Max (€)");
        maxAmountField.setClearButtonVisible(true);
        maxAmountField.addValueChangeListener(e -> {
            this.filterMaxAmount = e.getValue() != null ? BigDecimal.valueOf(e.getValue()) : null;
            resetToFirstPage();
        });
        addAdvancedFilter(maxAmountField, "Max. Betrag (€)");

        // Row selection navigation to inquiry detail
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null && event.getValue().getInquiry() != null) {
                UUID inquiryId = event.getValue().getInquiry().getId();
                getUI().ifPresent(ui -> ui.navigate("inquiries/" + inquiryId.toString()));
            }
        });

        initialize();
    }

    @Override
    protected void configureColumns() {
        numberCol   = addSortableTextColumn(Quote::getQuoteNumber, "Angebotsnummer", "quoteNumber");
        customerCol = addSortableTextColumn(q -> q.getCustomer() != null ? q.getCustomer().getCompanyName() : "–",
                "Kunde", "customer.companyName");

        addSortableTextColumn(q -> q.getNetAmount() + " €", "Netto", "netAmount");
        addSortableTextColumn(q -> q.getGrossAmount() + " €", "Brutto", "grossAmount");

        statusCol = addTextColumn(q -> {
            if (q.getStatus() == null) return "–";
            switch (q.getStatus()) {
                case DRAFT: return "Entwurf";
                case SENT: return "Gesendet";
                case ACCEPTED: return "Angenommen";
                case REJECTED: return "Abgelehnt";
                case EXPIRED: return "Abgelaufen";
                default: return q.getStatus().name();
            }
        }, "Status");

        addSortableTextColumn(q -> q.getExpiresAt() != null ? q.getExpiresAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "–",
                "Gültig bis", "expiresAt");

        addSortableTextColumn(q -> q.getCreatedAt() != null ? q.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "–",
                "Erstellt am", "createdAt");
    }

    @Override
    protected void configureFilters(HeaderRow filterRow) {
        addTextFilter(numberCol,   value -> this.filterQuoteNumber = value);
        addTextFilter(customerCol, value -> this.filterCustomerName = value);
        addComboBoxFilter(statusCol, Arrays.asList(QuoteStatus.values()), value -> this.filterStatus = value);
    }

    @Override
    protected Page<Quote> loadPage(Pageable pageable) {
        return inquiryService.findQuotesFiltered(
                tenantId,
                filterSearch,
                filterQuoteNumber,
                filterCustomerName,
                filterStatus,
                filterMinAmount,
                filterMaxAmount,
                pageable
        );
    }
}
