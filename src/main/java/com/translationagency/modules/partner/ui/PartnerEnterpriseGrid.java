package com.translationagency.modules.partner.ui;

import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.shared.ui.BaseEnterpriseGrid;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.HeaderRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.function.Consumer;

public class PartnerEnterpriseGrid extends BaseEnterpriseGrid<Partner> {

    private final PartnerService partnerService;
    private final UUID           tenantId;

    private com.vaadin.flow.component.grid.Grid.Column<Partner> firstNameCol;
    private com.vaadin.flow.component.grid.Grid.Column<Partner> lastNameCol;
    private com.vaadin.flow.component.grid.Grid.Column<Partner> emailCol;
    private com.vaadin.flow.component.grid.Grid.Column<Partner> classCol;

    private String  filterSearch         = "";
    private String  filterFirstName      = "";
    private String  filterLastName       = "";
    private String  filterEmail          = "";
    private String  filterCity           = "";
    private String  filterClassification = "";
    private Boolean filterIsTranslator   = null;
    private Boolean filterIsInterpreter  = null;
    private Boolean filterIsActive       = null;

    public PartnerEnterpriseGrid(PartnerService partnerService, UUID tenantId,
                                 Consumer<Partner> onEdit, Consumer<Partner> onDelete) {
        this.partnerService = partnerService;
        this.tenantId       = tenantId;

        // Wire edit & delete actions
        setEditAction(onEdit);
        setDeleteAction(onDelete);

        // Enable General Search
        enableGeneralSearch(value -> this.filterSearch = value);

        // Add Advanced Filters (Translator / Interpreter / Active)
        ComboBox<Boolean> transSelect = new ComboBox<>();
        transSelect.setItems(Boolean.TRUE, Boolean.FALSE);
        transSelect.setItemLabelGenerator(val -> val ? "Ja" : "Nein");
        transSelect.setPlaceholder("Alle");
        transSelect.setClearButtonVisible(true);
        transSelect.addValueChangeListener(e -> {
            this.filterIsTranslator = e.getValue();
            resetToFirstPage();
        });
        addAdvancedFilter(transSelect, "Übersetzer");

        ComboBox<Boolean> interpSelect = new ComboBox<>();
        interpSelect.setItems(Boolean.TRUE, Boolean.FALSE);
        interpSelect.setItemLabelGenerator(val -> val ? "Ja" : "Nein");
        interpSelect.setPlaceholder("Alle");
        interpSelect.setClearButtonVisible(true);
        interpSelect.addValueChangeListener(e -> {
            this.filterIsInterpreter = e.getValue();
            resetToFirstPage();
        });
        addAdvancedFilter(interpSelect, "Dolmetscher");

        ComboBox<Boolean> activeSelect = new ComboBox<>();
        activeSelect.setItems(Boolean.TRUE, Boolean.FALSE);
        activeSelect.setItemLabelGenerator(val -> val ? "Ja" : "Nein");
        activeSelect.setPlaceholder("Alle");
        activeSelect.setClearButtonVisible(true);
        activeSelect.addValueChangeListener(e -> {
            this.filterIsActive = e.getValue();
            resetToFirstPage();
        });
        addAdvancedFilter(activeSelect, "Aktiv");

        // Row selection navigation
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate("partners/" + event.getValue().getId().toString()));
            }
        });

        // Initialize the first data load
        initialize();
    }

    @Override
    protected void configureColumns() {
        firstNameCol = addSortableTextColumn(Partner::getFirstName, "Vorname", "firstName");
        lastNameCol  = addSortableTextColumn(Partner::getLastName, "Nachname", "lastName");
        emailCol     = addSortableTextColumn(Partner::getEmail, "E-Mail", "email");
        classCol     = addSortableTextColumn(Partner::getClassification, "Einstufung", "classification");
    }

    @Override
    protected void configureFilters(HeaderRow filterRow) {
        addTextFilter(firstNameCol, value -> this.filterFirstName = value);
        addTextFilter(lastNameCol,  value -> this.filterLastName  = value);
        addTextFilter(emailCol,     value -> this.filterEmail     = value);
        addTextFilter(classCol,     value -> this.filterClassification = value);
    }

    @Override
    protected Page<Partner> loadPage(Pageable pageable) {
        return partnerService.findPartnersFiltered(
                tenantId,
                filterSearch,
                filterFirstName,
                filterLastName,
                filterEmail,
                filterCity,
                filterClassification,
                filterIsTranslator,
                filterIsInterpreter,
                filterIsActive,
                pageable
        );
    }
}
