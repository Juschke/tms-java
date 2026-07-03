package com.translationagency.modules.inquiry.ui;

import com.translationagency.modules.inquiry.application.InquiryService;
import com.translationagency.modules.inquiry.domain.Inquiry;
import com.translationagency.modules.inquiry.domain.InquiryStatus;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.shared.ui.BaseEnterpriseGrid;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.HeaderRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class InquiryEnterpriseGrid extends BaseEnterpriseGrid<Inquiry> {

    private final InquiryService inquiryService;
    private final PricingService pricingService;
    private final UUID           tenantId;

    private com.vaadin.flow.component.grid.Grid.Column<Inquiry> customerCol;
    private com.vaadin.flow.component.grid.Grid.Column<Inquiry> serviceCol;
    private com.vaadin.flow.component.grid.Grid.Column<Inquiry> statusCol;
    private com.vaadin.flow.component.grid.Grid.Column<Inquiry> sourceLangCol;
    private com.vaadin.flow.component.grid.Grid.Column<Inquiry> targetLangCol;

    private String        filterSearch           = "";
    private String        filterCustomerName     = "";
    private InquiryStatus filterStatus           = null;
    private UUID          filterSourceLanguageId = null;
    private UUID          filterTargetLanguageId = null;
    private Boolean       filterIsExpress        = null;
    private Boolean       filterIsCertified      = null;

    public InquiryEnterpriseGrid(InquiryService inquiryService, PricingService pricingService, UUID tenantId) {
        this.inquiryService = inquiryService;
        this.pricingService = pricingService;
        this.tenantId       = tenantId;

        // Enable general search
        enableGeneralSearch(value -> this.filterSearch = value);

        // Add advanced filters
        ComboBox<Boolean> expressSelect = new ComboBox<>();
        expressSelect.setItems(Boolean.TRUE, Boolean.FALSE);
        expressSelect.setItemLabelGenerator(val -> val ? "Ja" : "Nein");
        expressSelect.setPlaceholder("Alle");
        expressSelect.setClearButtonVisible(true);
        expressSelect.addValueChangeListener(e -> {
            this.filterIsExpress = e.getValue();
            resetToFirstPage();
        });
        addAdvancedFilter(expressSelect, "Express");

        ComboBox<Boolean> certifiedSelect = new ComboBox<>();
        certifiedSelect.setItems(Boolean.TRUE, Boolean.FALSE);
        certifiedSelect.setItemLabelGenerator(val -> val ? "Ja" : "Nein");
        certifiedSelect.setPlaceholder("Alle");
        certifiedSelect.setClearButtonVisible(true);
        certifiedSelect.addValueChangeListener(e -> {
            this.filterIsCertified = e.getValue();
            resetToFirstPage();
        });
        addAdvancedFilter(certifiedSelect, "Beglaubigt");

        // Selection listener to navigate to detail
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("inquiries/detail/" + event.getValue().getId().toString()));
            }
        });

        initialize();
    }

    @Override
    protected void configureColumns() {
        customerCol = addSortableTextColumn(inq -> inq.getCustomer() != null ? inq.getCustomer().getCompanyName() : "–",
                "Kunde", "customer.companyName");

        serviceCol  = addSortableTextColumn(inq -> inq.getServiceType() != null ? inq.getServiceType().getName() : "–",
                "Leistungsart", "serviceType.name");

        sourceLangCol = addSortableTextColumn(inq -> inq.getSourceLanguage() != null ? inq.getSourceLanguage().getName() : "–",
                "Quellsprache", "sourceLanguage.name");

        targetLangCol = addSortableTextColumn(inq -> inq.getTargetLanguage() != null ? inq.getTargetLanguage().getName() : "–",
                "Zielsprache", "targetLanguage.name");

        addSortableTextColumn(inq -> {
            if (inq.getWordCount() != null && inq.getWordCount() > 0) {
                return inq.getWordCount() + " W.";
            } else if (inq.getPageCount() != null && inq.getPageCount() > 0) {
                return inq.getPageCount() + " S.";
            }
            return "–";
        }, "Umfang", "wordCount");

        statusCol = addTextColumn(inq -> {
            if (inq.getStatus() == null) return "–";
            switch (inq.getStatus()) {
                case RECEIVED: return "Eingegangen";
                case IN_CALCULATION: return "Kalkulation";
                case QUOTE_PREPARED: return "Angebot vorbereitet";
                case QUOTE_SENT: return "Angebot gesendet";
                case ACCEPTED: return "Angenommen";
                case REJECTED: return "Abgelehnt";
                case CANCELLED: return "Storniert";
                default: return inq.getStatus().name();
            }
        }, "Status");

        addSortableTextColumn(inq -> inq.getCreatedAt() != null ? inq.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "–",
                "Datum", "createdAt");
    }

    @Override
    protected void configureFilters(HeaderRow filterRow) {
        addTextFilter(customerCol, value -> this.filterCustomerName = value);

        addComboBoxFilter(statusCol, Arrays.asList(InquiryStatus.values()), value -> this.filterStatus = value);

        List<Language> languages = pricingService.getAllLanguages();
        addComboBoxFilter(sourceLangCol, languages, value -> {
            this.filterSourceLanguageId = value != null ? value.getId() : null;
        });
        addComboBoxFilter(targetLangCol, languages, value -> {
            this.filterTargetLanguageId = value != null ? value.getId() : null;
        });
    }

    @Override
    protected Page<Inquiry> loadPage(Pageable pageable) {
        return inquiryService.findInquiriesFiltered(
                tenantId,
                filterSearch,
                filterCustomerName,
                filterStatus,
                filterSourceLanguageId,
                filterTargetLanguageId,
                filterIsExpress,
                filterIsCertified,
                pageable
        );
    }
}
