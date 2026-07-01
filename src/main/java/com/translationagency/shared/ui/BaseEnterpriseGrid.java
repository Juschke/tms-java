package com.translationagency.shared.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.ValueProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstract, reusable Enterprise Grid component for Spring Boot &amp; Vaadin B2B applications.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Per-column text truncation with ellipsis (CSS) and native browser tooltip (title attr)</li>
 *   <li>Sticky filter row: {@code TextField} (lazy) for text cols, {@code ComboBox} for enums</li>
 *   <li>Manual server-side pagination: page-size selector + navigation buttons</li>
 *   <li>Spring Data {@link Pageable} / {@link Sort} integration via {@link #loadPage}</li>
 *   <li>Frozen action column (right-side) with a lazy {@link ContextMenu} per row</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>Extend this class and set generic type {@code T}.</li>
 *   <li>Implement {@link #configureColumns()}, {@link #configureFilters(HeaderRow)},
 *       and {@link #loadPage(Pageable)}.</li>
 *   <li>At the end of your subclass constructor call {@link #initialize()} to trigger
 *       the first data load (important: avoids NPE on uninitialized subclass fields).</li>
 * </ol>
 *
 * @param <T> The entity / DTO type displayed in the grid.
 */
@CssImport("./themes/translation-theme/enterprise-grid.css")
public abstract class BaseEnterpriseGrid<T> extends VerticalLayout {

    // =========================================================
    // Constants
    // =========================================================

    /** CSS class-name prefix used for all generated CSS class names. */
    private static final String P = "egrid";

    /** Key for the reserved action column so it can be identified programmatically. */
    private static final String ACTION_COL_KEY = "__actions__";

    // =========================================================
    // Core Grid
    // =========================================================

    /** The inner Vaadin {@link Grid}. Accessible to subclasses for advanced configuration. */
    protected final Grid<T> grid;

    // =========================================================
    // Pagination State
    // =========================================================

    private int currentPage   = 0;
    private int pageSize      = 25;
    private int totalElements = 0;
    private int totalPages    = 0;

    // =========================================================
    // Sort State (tracks what column/direction the user clicked)
    // =========================================================

    private String         sortProperty  = null;
    private Sort.Direction sortDirection = Sort.Direction.ASC;

    // =========================================================
    // Pagination UI
    // =========================================================

    private Button          firstPageBtn;
    private Button          prevPageBtn;
    private Button          nextPageBtn;
    private Button          lastPageBtn;
    private Span            pageInfoLabel;
    private Span            totalInfoLabel;
    private Select<Integer> pageSizeSelect;

    // =========================================================
    // Filter Row Reference (exposed to subclasses)
    // =========================================================

    /**
     * The header row reserved for filter fields.
     * Available for use inside {@link #configureFilters(HeaderRow)}.
     */
    protected HeaderRow filterRow;

    // =========================================================
    // Sort Property Registry
    // Maps each sortable Column instance → its Spring Data sort property string.
    // Required because Grid.Column has no public getSortProperty() getter.
    // =========================================================

    private final Map<Grid.Column<T>, String> columnSortProperties = new IdentityHashMap<>();

    // =========================================================
    // Filter Panel (General Search & Advanced Filters)
    // =========================================================

    private final VerticalLayout topFilterPanel = new VerticalLayout();
    private final HorizontalLayout generalSearchRow = new HorizontalLayout();
    private final com.vaadin.flow.component.formlayout.FormLayout advancedFiltersContainer = new com.vaadin.flow.component.formlayout.FormLayout();
    private TextField generalSearchField;
    private Button toggleAdvancedBtn;
    private Button resetFiltersBtn;
    private final List<com.vaadin.flow.component.HasValue<?, ?>> filterComponents = new ArrayList<>();

    // =========================================================
    // Row Actions
    // =========================================================

    private Consumer<T>          editAction;
    private Consumer<T>          deleteAction;
    private final List<ActionEntry<T>> extraActions = new ArrayList<>();

    // =========================================================
    // Constructor
    // =========================================================

    /**
     * Constructs the grid layout, defines columns, filter row and pagination bar.
     *
     * <p><strong>Important:</strong> this constructor intentionally does NOT load data.
     * Call {@link #initialize()} at the end of your subclass constructor, after all
     * subclass fields (e.g. services, tenant IDs) have been assigned.</p>
     */
    protected BaseEnterpriseGrid() {
        this.grid = new Grid<>();
        buildTopFilterPanel();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName(P + "-container");

        configureGridDefaults();

        // Let the subclass define its columns first
        configureColumns();

        // Action column is always appended last, frozen to the right
        configureActionColumn();

        // Sort listener translates Vaadin sort orders → Spring Sort
        configureSortListener();

        // Filter row must come AFTER all columns are added
        this.filterRow = grid.appendHeaderRow();
        configureFilters(filterRow);

        HorizontalLayout paginationBar = buildPaginationBar();
        add(topFilterPanel, grid, paginationBar);
        setFlexGrow(1, grid);

        // Do NOT call loadData() here; subclass fields may be null at this point.
    }

    private void buildTopFilterPanel() {
        topFilterPanel.setWidthFull();
        topFilterPanel.setPadding(true);
        topFilterPanel.setSpacing(true);
        topFilterPanel.addClassName(P + "-top-filter-panel");
        topFilterPanel.setVisible(false);

        generalSearchField = new TextField();
        generalSearchField.setPlaceholder("Allgemeine Suche...");
        generalSearchField.setClearButtonVisible(true);
        generalSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        generalSearchField.setValueChangeTimeout(300);
        generalSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        generalSearchField.setWidth("300px");
        generalSearchField.setVisible(false);

        toggleAdvancedBtn = new Button("Erweiterte Filter", VaadinIcon.FILTER.create());
        toggleAdvancedBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        toggleAdvancedBtn.setVisible(false);

        resetFiltersBtn = new Button("Filter zurücksetzen", VaadinIcon.REFRESH.create(), e -> clearAllFilters());
        resetFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetFiltersBtn.setVisible(false);

        generalSearchRow.setWidthFull();
        generalSearchRow.setSpacing(true);
        generalSearchRow.setPadding(false);
        generalSearchRow.setAlignItems(Alignment.CENTER);
        generalSearchRow.add(generalSearchField, toggleAdvancedBtn, resetFiltersBtn);

        advancedFiltersContainer.setWidthFull();
        advancedFiltersContainer.setVisible(false);
        advancedFiltersContainer.setResponsiveSteps(
                new com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep("0", 1),
                new com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep("400px", 2),
                new com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep("800px", 3),
                new com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep("1200px", 4)
        );

        toggleAdvancedBtn.addClickListener(e -> {
            boolean visible = !advancedFiltersContainer.isVisible();
            advancedFiltersContainer.setVisible(visible);
            toggleAdvancedBtn.setIcon(visible ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.FILTER.create());
        });

        topFilterPanel.add(generalSearchRow, advancedFiltersContainer);
    }

    // =========================================================
    // Abstract Contract – Subclasses MUST Implement
    // =========================================================

    /**
     * Define all grid columns here using the helper methods:
     * {@link #addTextColumn}, {@link #addSortableTextColumn}, {@link #addComponentColumn}.
     * Store returned {@link Grid.Column} references for use in {@link #configureFilters}.
     */
    protected abstract void configureColumns();

    /**
     * Attach filter widgets to the {@link HeaderRow}.
     * Use {@link #addTextFilter} or {@link #addComboBoxFilter} helpers.
     *
     * @param filterRow The header row dedicated to filter fields.
     */
    protected abstract void configureFilters(HeaderRow filterRow);

    /**
     * Load one page of data from the backend.
     * Spring Data {@link Pageable} already contains page index, page size and sort.
     *
     * @param pageable  Pagination + sort info from the grid's current state.
     * @return          A {@link Page} containing the items and the total element count.
     */
    protected abstract Page<T> loadPage(Pageable pageable);

    // =========================================================
    // Overridable Hook
    // =========================================================

    /**
     * Populate the per-row action {@link ContextMenu}.
     * The default implementation adds "Bearbeiten" and "Löschen" when actions are set.
     * Override to add extra items or change the defaults entirely.
     *
     * @param menu  The already-opened (empty) context menu – add items here.
     * @param item  The entity that belongs to the clicked row.
     */
    protected void configureContextMenuItems(ContextMenu menu, T item) {
        if (editAction != null) {
            MenuItem editItem = menu.addItem("✏\uFE0F  Bearbeiten", e -> editAction.accept(item));
            editItem.addClassName(P + "-menu-edit");
        }
        if (deleteAction != null) {
            MenuItem deleteItem = menu.addItem("🗑\uFE0F  Löschen", e -> deleteAction.accept(item));
            deleteItem.addClassName(P + "-menu-delete");
        }
        for (ActionEntry<T> entry : extraActions) {
            menu.addItem(entry.label(), e -> entry.action().accept(item));
        }
    }

    // =========================================================
    // Public API
    // =========================================================

    /**
     * Call this at the END of your subclass constructor to trigger the initial data load.
     * Must be called after all service fields and filter state are initialized.
     */
    protected final void initialize() {
        loadData();
    }

    /** Sets the handler invoked when "Bearbeiten" is selected in the context menu. */
    public void setEditAction(Consumer<T> action) {
        this.editAction = action;
    }

    /** Sets the handler invoked when "Löschen" is selected in the context menu. */
    public void setDeleteAction(Consumer<T> action) {
        this.deleteAction = action;
    }

    /**
     * Appends a custom item to every row's context menu.
     *
     * @param label  Display label (emoji + text is fine, e.g. "📄  PDF herunterladen").
     * @param action Called with the row's entity when the item is clicked.
     */
    public void addContextMenuAction(String label, Consumer<T> action) {
        extraActions.add(new ActionEntry<>(label, action));
    }

    /** Reloads the current page from the backend. */
    public void refresh() {
        loadData();
    }

    /** Jumps to page 0 and reloads. Call after changing filter state. */
    public void resetToFirstPage() {
        currentPage = 0;
        loadData();
    }

    // =========================================================
    // Column Helpers – call from configureColumns()
    // =========================================================

    /**
     * Adds a non-sortable column with:
     * <ul>
     *   <li>CSS text truncation (ellipsis)</li>
     *   <li>Native browser tooltip ({@code title} attribute) with the full cell text</li>
     *   <li>Column header tooltip</li>
     *   <li>{@code setResizable(true)}</li>
     * </ul>
     *
     * @param valueProvider  Extracts the display value from the entity.
     * @param header         Column header label.
     * @return The created {@link Grid.Column} for use in filter configuration.
     */
    protected Grid.Column<T> addTextColumn(ValueProvider<T, ?> valueProvider, String header) {
        return grid.addComponentColumn(item -> buildTruncatedCell(valueProvider.apply(item)))
                .setHeader(buildHeaderSpan(header))
                .setResizable(true)
                .setSortable(false);
    }

    /**
     * Like {@link #addTextColumn} but also enables server-side sorting.
     * The {@code sortProperty} string is forwarded to Spring Data's {@link Sort.Order}.
     *
     * @param valueProvider  Extracts the display value from the entity.
     * @param header         Column header label.
     * @param sortProperty   Spring Data sort property, e.g. {@code "customer.companyName"}.
     * @return The created {@link Grid.Column} for use in filter configuration.
     */
    protected Grid.Column<T> addSortableTextColumn(ValueProvider<T, ?> valueProvider,
                                                    String header,
                                                    String sortProperty) {
        Grid.Column<T> col = grid.addComponentColumn(item -> buildTruncatedCell(valueProvider.apply(item)))
                .setHeader(buildHeaderSpan(header))
                .setResizable(true)
                .setSortable(true)
                .setSortProperty(sortProperty);
        // Register the sort property so the sort listener can look it up.
        // We cannot call col.getSortProperty() later — it has no public getter.
        columnSortProperties.put(col, sortProperty);
        return col;
    }

    /**
     * Adds a component column (for status badges, action icons, etc.).
     * No truncation applied – the component is responsible for its own sizing.
     *
     * @param renderer  Produces a Vaadin component for each row.
     * @param header    Column header label.
     * @return The created {@link Grid.Column}.
     */
    protected Grid.Column<T> addComponentColumn(SerializableFunction<T, com.vaadin.flow.component.Component> renderer,
                                                  String header) {
        return grid.addComponentColumn(renderer::apply)
                .setHeader(buildHeaderSpan(header))
                .setResizable(true)
                .setSortable(false);
    }

    // =========================================================
    // Filter Helpers – call from configureFilters()
    // =========================================================

    /**
     * Enables the general search input at the top of the grid.
     * Triggers the callback when the query value changes.
     */
    protected void enableGeneralSearch(Consumer<String> searchCallback) {
        topFilterPanel.setVisible(true);
        generalSearchField.setVisible(true);
        resetFiltersBtn.setVisible(true);
        generalSearchField.addValueChangeListener(e -> {
            searchCallback.accept(e.getValue() != null ? e.getValue().trim() : "");
            resetToFirstPage();
        });
    }

    /**
     * Adds an advanced filter field (e.g. ComboBox, DatePicker, Select) to the top panel.
     * The field is wrapped in a FormLayout with the specified label and automatically registered for reset.
     */
    protected void addAdvancedFilter(com.vaadin.flow.component.Component field, String label) {
        topFilterPanel.setVisible(true);
        toggleAdvancedBtn.setVisible(true);
        resetFiltersBtn.setVisible(true);

        if (field instanceof com.vaadin.flow.component.HasValue<?, ?> hv) {
            registerFilterComponent(hv);
        }

        advancedFiltersContainer.addFormItem(field, label);
    }

    /**
     * Registers a filter component so that it can be automatically cleared by the reset button.
     */
    protected void registerFilterComponent(com.vaadin.flow.component.HasValue<?, ?> component) {
        filterComponents.add(component);
    }

    /**
     * Resets all registered filter components and the general search field.
     */
    protected void clearAllFilters() {
        if (generalSearchField.isVisible()) {
            generalSearchField.clear();
        }
        for (com.vaadin.flow.component.HasValue<?, ?> component : filterComponents) {
            component.clear();
        }
        resetToFirstPage();
    }

    /**
     * Attaches a lazy {@link TextField} to a column's filter cell.
     * Triggers {@code onChange} after 300 ms of typing inactivity and resets to page 0.
     *
     * @param column    The column whose filter cell should receive the text field.
     * @param onChange  Callback that receives the trimmed filter string (may be empty).
     */
    protected void addTextFilter(Grid.Column<T> column, Consumer<String> onChange) {
        TextField field = new TextField();
        field.setPlaceholder("Filtern...");
        field.setClearButtonVisible(true);
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.setValueChangeTimeout(300);
        field.addClassName(P + "-filter-field");
        field.setWidthFull();
        field.addValueChangeListener(e -> {
            onChange.accept(e.getValue() != null ? e.getValue().trim() : "");
            resetToFirstPage();
        });
        registerFilterComponent(field);
        filterRow.getCell(column).setComponent(field);
    }

    /**
     * Attaches a {@link ComboBox} to a column's filter cell.
     * Triggers {@code onChange} when the user picks or clears a value.
     *
     * @param column    The column whose filter cell should receive the combo box.
     * @param items     The selectable items (usually an enum's values list).
     * @param onChange  Callback that receives the selected item, or {@code null} when cleared.
     * @param <E>       The filter value type (usually an enum).
     */
    protected <E> void addComboBoxFilter(Grid.Column<T> column, List<E> items, Consumer<E> onChange) {
        ComboBox<E> box = new ComboBox<>();
        box.setItems(items);
        box.setPlaceholder("Alle");
        box.setClearButtonVisible(true);
        box.addClassName(P + "-filter-field");
        box.setWidthFull();
        box.addValueChangeListener(e -> {
            onChange.accept(e.getValue()); // null = "show all"
            resetToFirstPage();
        });
        registerFilterComponent(box);
        filterRow.getCell(column).setComponent(box);
    }

    // =========================================================
    // Private – Grid Setup
    // =========================================================

    private void configureGridDefaults() {
        grid.setSizeFull();
        grid.addClassName(P);
        // Disable Vaadin's default infinite scroll: we control pages manually.
        grid.setAllRowsVisible(false);
        // Single-column sort is sufficient for most enterprise use cases.
        grid.setMultiSort(false);
    }

    private void configureActionColumn() {
        grid.addComponentColumn(item -> {
            Button btn = new Button(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            btn.addClassName(P + "-action-btn");
            btn.getElement().setAttribute("aria-label", "Zeilenaktionen öffnen");

            ContextMenu menu = new ContextMenu();
            menu.setTarget(btn);
            menu.setOpenOnClick(true);

            // Items are populated lazily on each open to avoid stale references
            // and to keep the DOM lightweight on large data sets.
            menu.addOpenedChangeListener(evt -> {
                if (evt.isOpened()) {
                    menu.removeAll();
                    configureContextMenuItems(menu, item);
                }
            });

            return btn;
        })
        .setKey(ACTION_COL_KEY)
        .setHeader("")
        .setWidth("60px")
        .setFlexGrow(0)
        .setResizable(false)
        .setSortable(false)
        .setFrozenToEnd(true); // Keeps the column visible when scrolling horizontally
    }

    private void configureSortListener() {
        grid.addSortListener(event -> {
            List<GridSortOrder<T>> orders = event.getSortOrder();
            if (!orders.isEmpty()) {
                GridSortOrder<T> first = orders.get(0);
                // Look up the Spring Data sort property from our registry.
                // Grid.Column.getSortProperty() is not a public API in Vaadin 24.
                sortProperty  = columnSortProperties.get(first.getSorted());
                sortDirection = first.getDirection() == SortDirection.ASCENDING
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
            } else {
                sortProperty = null;
            }
            currentPage = 0;
            loadData();
        });
    }

    // =========================================================
    // Private – Data Loading
    // =========================================================

    private void loadData() {
        Sort sort = (sortProperty != null && !sortProperty.isBlank())
                ? Sort.by(sortDirection, sortProperty)
                : Sort.unsorted();

        Pageable pageable = PageRequest.of(currentPage, pageSize, sort);
        Page<T> page      = loadPage(pageable);

        totalElements = (int) page.getTotalElements();
        totalPages    = page.getTotalPages();

        // Edge case: all items on the last page were deleted → go back one page
        if (totalPages > 0 && currentPage >= totalPages) {
            currentPage = totalPages - 1;
            loadData();
            return;
        }

        // Use a ListDataProvider with a no-op sort comparator:
        // the backend already returns sorted data; no in-memory re-sort needed.
        ListDataProvider<T> provider = new ListDataProvider<>(page.getContent());
        provider.setSortComparator((a, b) -> 0);
        grid.setDataProvider(provider);

        updatePaginationUI();
    }

    private void updatePaginationUI() {
        boolean hasData   = totalPages > 0;
        int     displayPage  = hasData ? currentPage + 1 : 0;
        int     displayTotal = hasData ? totalPages : 0;

        pageInfoLabel.setText("Seite " + displayPage + " von " + displayTotal);
        totalInfoLabel.setText(totalElements + " Einträge gesamt");

        firstPageBtn.setEnabled(currentPage > 0);
        prevPageBtn.setEnabled(currentPage > 0);
        nextPageBtn.setEnabled(currentPage < totalPages - 1);
        lastPageBtn.setEnabled(currentPage < totalPages - 1);
        pageSizeSelect.setEnabled(hasData || totalElements == 0);
    }

    // =========================================================
    // Private – Pagination Bar Builder
    // =========================================================

    private HorizontalLayout buildPaginationBar() {
        // ---- Page-size selector ----
        pageSizeSelect = new Select<>();
        pageSizeSelect.setItems(10, 25, 50, 100);
        pageSizeSelect.setValue(pageSize);
        pageSizeSelect.addClassName(P + "-page-size");
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize    = e.getValue();
                currentPage = 0;
                loadData();
            }
        });

        Span sizeLabel = new Span("Einträge pro Seite:");
        sizeLabel.addClassName(P + "-label");

        HorizontalLayout sizeLayout = new HorizontalLayout(sizeLabel, pageSizeSelect);
        sizeLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        sizeLayout.setSpacing(true);
        sizeLayout.setPadding(false);

        // ---- Navigation buttons ----
        firstPageBtn = navBtn(VaadinIcon.ANGLE_DOUBLE_LEFT, "Erste Seite",
                () -> { currentPage = 0; loadData(); });
        prevPageBtn  = navBtn(VaadinIcon.ANGLE_LEFT, "Vorherige Seite",
                () -> { currentPage = Math.max(0, currentPage - 1); loadData(); });
        nextPageBtn  = navBtn(VaadinIcon.ANGLE_RIGHT, "Nächste Seite",
                () -> { currentPage = Math.min(totalPages - 1, currentPage + 1); loadData(); });
        lastPageBtn  = navBtn(VaadinIcon.ANGLE_DOUBLE_RIGHT, "Letzte Seite",
                () -> { currentPage = Math.max(0, totalPages - 1); loadData(); });

        pageInfoLabel = new Span("Seite 1 von 1");
        pageInfoLabel.addClassName(P + "-page-info");

        HorizontalLayout navLayout = new HorizontalLayout(
                firstPageBtn, prevPageBtn, pageInfoLabel, nextPageBtn, lastPageBtn);
        navLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        navLayout.setSpacing(false);
        navLayout.setPadding(false);
        navLayout.addClassName(P + "-nav");

        // ---- Total counter ----
        totalInfoLabel = new Span("0 Einträge gesamt");
        totalInfoLabel.addClassName(P + "-total");

        // ---- Assemble full bar ----
        Div spacer = new Div();
        spacer.getStyle().set("flex", "1");

        HorizontalLayout bar = new HorizontalLayout(sizeLayout, spacer, totalInfoLabel, navLayout);
        bar.addClassName(P + "-pagination-bar");
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        return bar;
    }

    // =========================================================
    // Private – UI Utilities
    // =========================================================

    /**
     * Builds a {@code <div>} for text columns: truncates overflowing text with "…"
     * and exposes the full value as a native browser tooltip via the {@code title} attribute.
     */
    private Div buildTruncatedCell(Object value) {
        String text = value != null ? value.toString() : "";
        Div cell = new Div();
        cell.setText(text);
        cell.addClassName(P + "-cell");
        cell.getElement().setAttribute("title", text);
        return cell;
    }

    /** Builds a column header {@code <span>} with its own tooltip in case the header is truncated. */
    private Span buildHeaderSpan(String header) {
        Span span = new Span(header);
        span.addClassName(P + "-header-span");
        span.getElement().setAttribute("title", header);
        return span;
    }

    /** Creates a compact icon-only navigation button for the pagination bar. */
    private Button navBtn(VaadinIcon icon, String tooltip, Runnable onClick) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        btn.addClassName(P + "-nav-btn");
        btn.setTooltipText(tooltip);
        btn.addClickListener(e -> onClick.run());
        return btn;
    }

    // =========================================================
    // Inner Types
    // =========================================================

    /** Immutable record holding a custom context-menu entry. */
    private record ActionEntry<T>(String label, Consumer<T> action) {}
}
