package com.translationagency.modules.order.ui;

import com.translationagency.modules.billing.application.BillingService;
import com.translationagency.modules.billing.domain.Invoice;
import com.translationagency.modules.billing.domain.Payment;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.document.application.DocumentService;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.modules.document.ui.DocumentAttachmentList;
import com.translationagency.modules.order.application.OrderNoteService;
import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.*;
import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.tenant.domain.UserAccount;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Auftragsdetails | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class OrderDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final OrderService orderService;
    private final PartnerService partnerService;
    private final BillingService billingService;
    private final DocumentService documentService;
    private final OrderNoteService orderNoteService;
    private final SecurityService securityService;
    private final PdfService pdfService;
    private final PricingService pricingService;

    private TranslationOrder order;
    private Tenant tenant;
    private Invoice associatedInvoice;

    private final H2 title = new H2("Auftragsdetails");
    private final Tabs tabs = new Tabs();
    private final Div contentContainer = new Div();

    // Tab Layouts
    private VerticalLayout detailsLayout;
    private VerticalLayout itemsLayout;
    private VerticalLayout partnerLayout;
    private VerticalLayout notesLayout;
    private DocumentAttachmentList filesLayout;
    private VerticalLayout billingLayout;

    public OrderDetailView(OrderService orderService, PartnerService partnerService,
                           BillingService billingService, DocumentService documentService,
                           OrderNoteService orderNoteService, SecurityService securityService, PdfService pdfService,
                           PricingService pricingService) {
        this.orderService = orderService;
        this.partnerService = partnerService;
        this.billingService = billingService;
        this.documentService = documentService;
        this.orderNoteService = orderNoteService;
        this.securityService = securityService;
        this.pdfService = pdfService;
        this.pricingService = pricingService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.tenant = t);

        Button backButton = new Button("Zurück zur Liste", VaadinIcon.ARROW_LEFT.create(), e -> getUI().ifPresent(ui -> ui.navigate("orders")));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(backButton, title);
        header.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        add(header);

        createTabs();
        add(tabs, contentContainer);
        contentContainer.setSizeFull();
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            UUID id = UUID.fromString(parameter);
            orderService.getOrderById(id)
                    .ifPresentOrElse(o -> {
                        this.order = o;
                        this.title.setText("Auftrag: " + o.getOrderNumber());
                        
                        this.associatedInvoice = billingService.getAllInvoices(tenant.getId()).stream()
                                .filter(i -> i.getOrder() != null && i.getOrder().getId().equals(o.getId()))
                                .findFirst()
                                .orElse(null);

                        setupTabContents();
                    }, () -> {
                        Notification.show("Auftrag nicht gefunden", 3000, Notification.Position.MIDDLE);
                        getUI().ifPresent(ui -> ui.navigate("orders"));
                    });
        } catch (IllegalArgumentException e) {
            Notification.show("Ungültige Auftrags-ID", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("orders"));
        }
    }

    private void createTabs() {
        Tab detailsTab = new Tab("Auftragsdaten");
        Tab itemsTab = new Tab("Teilaufträge");
        Tab partnerTab = new Tab("Partnerzuweisung");
        Tab notesTab = new Tab("Notizen");
        Tab filesTab = new Tab("Dokumentenarchiv");
        Tab billingTab = new Tab("Abrechnung & Finanzen");

        tabs.add(detailsTab, itemsTab, partnerTab, notesTab, filesTab, billingTab);
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(detailsTab)) {
                contentContainer.add(detailsLayout);
            } else if (event.getSelectedTab().equals(itemsTab)) {
                contentContainer.add(itemsLayout);
                refreshItemsTab();
            } else if (event.getSelectedTab().equals(partnerTab)) {
                contentContainer.add(partnerLayout);
                refreshPartnerTab();
            } else if (event.getSelectedTab().equals(notesTab)) {
                contentContainer.add(notesLayout);
                refreshNotesTab();
            } else if (event.getSelectedTab().equals(filesTab)) {
                contentContainer.add(filesLayout);
                filesLayout.refresh();
            } else if (event.getSelectedTab().equals(billingTab)) {
                contentContainer.add(billingLayout);
                setupBillingTab();
            }
        });
    }

    private void setupTabContents() {
        setupDetailsTab();
        setupItemsTab();
        setupPartnerTab();
        setupNotesTab();
        setupFilesTab();
        setupBillingTab();

        contentContainer.removeAll();
        contentContainer.add(detailsLayout);
        tabs.setSelectedIndex(0);
    }

    private void setupDetailsTab() {
        detailsLayout = new VerticalLayout();
        detailsLayout.setSizeFull();
        detailsLayout.setPadding(false);
        refreshDetailsTab();
    }

    private void refreshDetailsTab() {
        if (detailsLayout != null && buildDetailsTabSplit()) {
            return;
        }
        if (detailsLayout != null) {
            detailsLayout.removeAll();
            FormLayout form = new FormLayout();
            TextField customerField = new TextField("Kunde");
            customerField.setValue(order.getCustomer().getCompanyName());
            customerField.setReadOnly(true);

            TextField statusField = new TextField("Status");
            statusField.setValue(order.getStatus().name());
            statusField.setReadOnly(true);

            TextField deliveryMethod = new TextField("Lieferart");
            deliveryMethod.setValue(order.getDeliveryMethod());
            deliveryMethod.setReadOnly(true);

            TextField netField = new TextField("Netto-Gesamtbetrag");
            netField.setValue(order.getNetAmount().toString() + " EUR");
            netField.setReadOnly(true);

            TextField vatField = new TextField("MwSt. (" + order.getVatPercent() + "%)");
            vatField.setValue(order.getVatAmount().toString() + " EUR");
            vatField.setReadOnly(true);

            TextField grossField = new TextField("Brutto-Gesamtbetrag");
            grossField.setValue(order.getGrossAmount().toString() + " EUR");
            grossField.setReadOnly(true);

            TextField deadlineField = new TextField("Lieferfrist");
            deadlineField.setValue(order.getDeliveryDeadline() != null ? order.getDeliveryDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-");
            deadlineField.setReadOnly(true);

            form.add(customerField, statusField, deliveryMethod, netField, vatField, grossField, deadlineField);
            detailsLayout.add(form);
        }
    }

    private void setupPartnerTab() {
        partnerLayout = new VerticalLayout();
        partnerLayout.setSizeFull();
        partnerLayout.setPadding(true);
        refreshPartnerTab();
    }

    private void refreshPartnerTab() {
        if (partnerLayout != null && buildPartnerTabContent()) {
            return;
        }
        partnerLayout.removeAll();

        H3 title = new H3("Partnerzuweisungen");
        partnerLayout.add(title);

        Grid<PartnerAssignment> assignmentGrid = new Grid<>(PartnerAssignment.class, false);
        assignmentGrid.addColumn(a -> a.getPartner().getFullName()).setHeader("Partner");
        assignmentGrid.addColumn(a -> a.getFee().toString() + " EUR").setHeader("Honorar");
        assignmentGrid.addColumn(a -> a.getDeadline() != null ? a.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-").setHeader("Frist");
        assignmentGrid.addColumn(a -> a.getStatus().name()).setHeader("Status");

        assignmentGrid.addComponentColumn(assignment -> {
            HorizontalLayout actionLayout = new HorizontalLayout();
            if (assignment.getStatus() == PartnerAssignmentStatus.OFFERED) {
                Button acceptBtn = new Button("Annehmen", e -> {
                    String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                    orderService.updateAssignmentStatus(assignment, PartnerAssignmentStatus.ACCEPTED, username);
                    Notification.show("Zuweisung akzeptiert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshPartnerTab();
                });
                acceptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                actionLayout.add(acceptBtn);
            } else if (assignment.getStatus() == PartnerAssignmentStatus.ACCEPTED || assignment.getStatus() == PartnerAssignmentStatus.IN_PROGRESS) {
                Button submitBtn = new Button("Einreichen", e -> {
                    String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                    orderService.updateAssignmentStatus(assignment, PartnerAssignmentStatus.SUBMITTED, username);
                    Notification.show("Übersetzung eingereicht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshPartnerTab();
                });
                submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                actionLayout.add(submitBtn);
            } else if (assignment.getStatus() == PartnerAssignmentStatus.SUBMITTED) {
                Button approveBtn = new Button("Freigeben", e -> {
                    String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                    orderService.updateAssignmentStatus(assignment, PartnerAssignmentStatus.APPROVED, username);
                    Notification.show("Übersetzung freigegeben und abgenommen").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshPartnerTab();
                });
                approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                actionLayout.add(approveBtn);
            }
            return actionLayout;
        }).setHeader("Aktionen");

        List<PartnerAssignment> assignments = orderService.getAssignmentsForOrder(order.getId());
        assignmentGrid.setItems(assignments);
        partnerLayout.add(assignmentGrid);

        // Assignment Form
        H3 formTitle = new H3("Neuen Partner zuweisen");
        FormLayout assignForm = new FormLayout();

        ComboBox<Partner> partnerCombo = new ComboBox<>("Partner / Übersetzer");
        partnerCombo.setItems(partnerService.getAllPartners(tenant.getId()));
        partnerCombo.setItemLabelGenerator(Partner::getFullName);

        NumberField feeField = new NumberField("Honorar (EUR)");
        DatePicker deadlinePicker = new DatePicker("Frist");

        assignForm.add(partnerCombo, feeField, deadlinePicker);

        Button assignBtn = new Button("Partner zuweisen", VaadinIcon.USER_CHECK.create(), e -> {
            if (partnerCombo.getValue() == null || feeField.getValue() == null || deadlinePicker.getValue() == null) {
                Notification.show("Bitte alle Felder ausfüllen").addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
            OffsetDateTime deadline = OffsetDateTime.of(deadlinePicker.getValue(), LocalTime.MAX, ZoneOffset.UTC);
            
            orderService.assignPartnerToOrder(order, partnerCombo.getValue(), BigDecimal.valueOf(feeField.getValue()), deadline, username);
            Notification.show("Partner erfolgreich zugewiesen").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshPartnerTab();
        });
        assignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        partnerLayout.add(formTitle, assignForm, assignBtn);
    }

    private boolean buildDetailsTabSplit() {
        detailsLayout.removeAll();

        HorizontalLayout split = new HorizontalLayout();
        split.setWidthFull();
        split.setSpacing(true);
        split.setPadding(false);
        split.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.START);

        VerticalLayout customerPanel = createCustomerDetailsPanel();
        VerticalLayout partnerPanel = createPartnerAssignmentPanel();

        customerPanel.getStyle().set("flex", "1 1 0");
        customerPanel.getStyle().set("min-width", "420px");
        partnerPanel.getStyle().set("flex", "1 1 0");
        partnerPanel.getStyle().set("min-width", "420px");

        split.add(customerPanel, partnerPanel);
        detailsLayout.add(split);
        return true;
    }

    private boolean buildPartnerTabContent() {
        partnerLayout.removeAll();
        partnerLayout.add(createPartnerAssignmentPanel());
        return true;
    }

    private VerticalLayout createCustomerDetailsPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setWidthFull();
        panel.setPadding(false);
        panel.setSpacing(true);

        H3 customerTitle = new H3("Kundendaten");
        customerTitle.addClassNames("m-0");

        FormLayout customerForm = new FormLayout();
        customerForm.setWidthFull();
        customerForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2)
        );
        customerForm.add(
                readOnlyField("Kundennummer", order.getCustomer().getCustomerNumber()),
                readOnlyField("Firmenname", order.getCustomer().getCompanyName()),
                readOnlyField("USt-ID", order.getCustomer().getVatId()),
                readOnlyField("Leitweg-ID", order.getCustomer().getLeitwegId()),
                readOnlyField("Strasse", order.getCustomer().getBillingAddressStreet()),
                readOnlyField("PLZ", order.getCustomer().getBillingAddressZip()),
                readOnlyField("Ort", order.getCustomer().getBillingAddressCity()),
                readOnlyField("Land", order.getCustomer().getBillingAddressCountry())
        );

        H3 orderTitle = new H3("Auftragsdaten");
        orderTitle.addClassNames("m-0");

        FormLayout orderForm = new FormLayout();
        orderForm.setWidthFull();
        orderForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2)
        );
        orderForm.add(
                readOnlyField("Auftragsnummer", order.getOrderNumber()),
                readOnlyField("Status", order.getStatus() != null ? order.getStatus().name() : "-"),
                readOnlyField("Lieferart", order.getDeliveryMethod()),
                readOnlyField("Netto-Gesamtbetrag", order.getNetAmount() + " EUR"),
                readOnlyField("MwSt. (" + order.getVatPercent() + "%)", order.getVatAmount() + " EUR"),
                readOnlyField("Brutto-Gesamtbetrag", order.getGrossAmount() + " EUR"),
                readOnlyField("Lieferfrist", order.getDeliveryDeadline() != null
                        ? order.getDeliveryDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        : "-")
        );

        panel.add(customerTitle, customerForm, orderTitle, orderForm);
        return panel;
    }

    private VerticalLayout createPartnerAssignmentPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setWidthFull();
        panel.setPadding(false);
        panel.setSpacing(true);

        H3 title = new H3("Partnerzuweisung");
        title.addClassNames("m-0");
        panel.add(title);

        Grid<PartnerAssignment> assignmentGrid = new Grid<>(PartnerAssignment.class, false);
        assignmentGrid.setWidthFull();
        assignmentGrid.addColumn(a -> a.getPartner() != null ? a.getPartner().getFullName() : "-").setHeader("Partner");
        assignmentGrid.addColumn(a -> a.getFee() != null ? a.getFee().toString() + " EUR" : "-").setHeader("Honorar");
        assignmentGrid.addColumn(a -> a.getDeadline() != null ? a.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-").setHeader("Frist");
        assignmentGrid.addColumn(a -> a.getStatus() != null ? a.getStatus().name() : "-").setHeader("Status");

        assignmentGrid.addComponentColumn(assignment -> {
            HorizontalLayout actionLayout = new HorizontalLayout();
            if (assignment.getStatus() == PartnerAssignmentStatus.OFFERED) {
                Button acceptBtn = new Button("Annehmen", e -> {
                    String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                    orderService.updateAssignmentStatus(assignment, PartnerAssignmentStatus.ACCEPTED, username);
                    Notification.show("Zuweisung akzeptiert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshPartnerTab();
                    refreshDetailsTab();
                });
                acceptBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                actionLayout.add(acceptBtn);
            } else if (assignment.getStatus() == PartnerAssignmentStatus.ACCEPTED || assignment.getStatus() == PartnerAssignmentStatus.IN_PROGRESS) {
                Button submitBtn = new Button("Einreichen", e -> {
                    String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                    orderService.updateAssignmentStatus(assignment, PartnerAssignmentStatus.SUBMITTED, username);
                    Notification.show("Übersetzung eingereicht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshPartnerTab();
                    refreshDetailsTab();
                });
                submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                actionLayout.add(submitBtn);
            } else if (assignment.getStatus() == PartnerAssignmentStatus.SUBMITTED) {
                Button approveBtn = new Button("Freigeben", e -> {
                    String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                    orderService.updateAssignmentStatus(assignment, PartnerAssignmentStatus.APPROVED, username);
                    Notification.show("Übersetzung freigegeben und abgenommen").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshPartnerTab();
                    refreshDetailsTab();
                });
                approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                actionLayout.add(approveBtn);
            }
            return actionLayout;
        }).setHeader("Aktionen");

        List<PartnerAssignment> assignments = orderService.getAssignmentsForOrder(order.getId());
        assignmentGrid.setItems(assignments);
        panel.add(assignmentGrid);

        H3 formTitle = new H3("Neuen Partner zuweisen");
        FormLayout assignForm = new FormLayout();
        assignForm.setWidthFull();
        assignForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2)
        );

        ComboBox<Partner> partnerCombo = new ComboBox<>("Partner / Übersetzer");
        partnerCombo.setItems(partnerService.getAllPartners(tenant.getId()));
        partnerCombo.setItemLabelGenerator(Partner::getFullName);

        NumberField feeField = new NumberField("Honorar (EUR)");
        DatePicker deadlinePicker = new DatePicker("Frist");

        assignForm.add(partnerCombo, feeField, deadlinePicker);

        Button assignBtn = new Button("Partner zuweisen", VaadinIcon.USER_CHECK.create(), e -> {
            if (partnerCombo.getValue() == null || feeField.getValue() == null || deadlinePicker.getValue() == null) {
                Notification.show("Bitte alle Felder ausfüllen").addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
            OffsetDateTime deadline = OffsetDateTime.of(deadlinePicker.getValue(), LocalTime.MAX, ZoneOffset.UTC);

            orderService.assignPartnerToOrder(order, partnerCombo.getValue(), BigDecimal.valueOf(feeField.getValue()), deadline, username);
            Notification.show("Partner erfolgreich zugewiesen").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshPartnerTab();
            refreshDetailsTab();
        });
        assignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        panel.add(formTitle, assignForm, assignBtn);
        return panel;
    }

    private void setupNotesTab() {
        notesLayout = new VerticalLayout();
        notesLayout.setSizeFull();
        notesLayout.setPadding(false);
        notesLayout.setSpacing(true);
        refreshNotesTab();
    }

    private void refreshNotesTab() {
        if (notesLayout == null) {
            return;
        }

        notesLayout.removeAll();

        H3 title = new H3("Mitarbeiter-Notizen");
        title.addClassNames("m-0");

        TextArea noteInput = new TextArea("Neue Notiz");
        noteInput.setWidthFull();
        noteInput.setMinHeight("120px");
        noteInput.setPlaceholder("Interne Notiz fuer das Team...");

        Button saveBtn = new Button("Notiz speichern", VaadinIcon.CHECK.create(), e -> {
            String body = noteInput.getValue() != null ? noteInput.getValue().trim() : "";
            if (body.isEmpty()) {
                Notification.show("Bitte eine Notiz eingeben").addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            UserAccount account = securityService.getAuthenticatedUserAccount().orElse(null);
            String username = account != null ? account.getUsername() : getCurrentUsername();
            String authorName = account != null ? account.getFullName() : username;

            orderNoteService.addNote(order.getId(), body, username, authorName);
            noteInput.clear();
            refreshNotesTab();
            Notification.show("Notiz gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout composer = new HorizontalLayout(noteInput, saveBtn);
        composer.setWidthFull();
        composer.setPadding(false);
        composer.setSpacing(true);
        composer.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        composer.expand(noteInput);

        VerticalLayout noteList = new VerticalLayout();
        noteList.setWidthFull();
        noteList.setPadding(false);
        noteList.setSpacing(true);

        List<OrderNote> notes = orderNoteService.getNotesForOrder(order.getId());
        if (notes.isEmpty()) {
            Span empty = new Span("Noch keine Notizen vorhanden.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            noteList.add(empty);
        } else {
            notes.forEach(note -> noteList.add(buildNoteCard(note)));
        }

        notesLayout.add(title, composer, noteList);
    }

    private Div buildNoteCard(OrderNote note) {
        Div card = new Div();
        card.getStyle().set("border", "1px solid var(--app-panel-border)");
        card.getStyle().set("border-radius", "4px");
        card.getStyle().set("padding", "12px");
        card.getStyle().set("background", "var(--app-panel-bg)");

        Avatar avatar = new Avatar(note.getAuthorName());
        avatar.setName(note.getAuthorName());

        Span author = new Span(note.getAuthorName());
        author.getStyle().set("font-weight", "600");
        author.getStyle().set("display", "block");

        Span time = new Span(note.getCreatedAt() != null
                ? note.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "-");
        time.getStyle().set("font-size", "var(--lumo-font-size-s)");
        time.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout meta = new VerticalLayout(author, time);
        meta.setPadding(false);
        meta.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(avatar, meta);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        header.setPadding(false);
        header.setWidthFull();
        header.expand(meta);

        Span body = new Span(note.getBody());
        body.getStyle().set("white-space", "pre-wrap");
        body.getStyle().set("display", "block");
        body.getStyle().set("margin-top", "8px");

        card.add(header, body);
        return card;
    }

    private String getCurrentUsername() {
        return securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("system");
    }

    private TextField readOnlyField(String label, String value) {
        TextField field = new TextField(label);
        field.setValue(value != null ? value : "-");
        field.setReadOnly(true);
        field.setWidthFull();
        return field;
    }

    private void setupFilesTab() {
        filesLayout = new DocumentAttachmentList(
                documentService,
                securityService,
                tenant,
                order.getCustomer(),
                "ORDER",
                order.getId()
        );
    }

    private void setupBillingTab() {
        if (billingLayout == null) {
            billingLayout = new VerticalLayout();
            billingLayout.setSizeFull();
            billingLayout.setPadding(true);
        }
        billingLayout.removeAll();

        H3 sectionTitle = new H3("Rechnungsstellung und Zahlungen");
        billingLayout.add(sectionTitle);

        if (associatedInvoice == null) {
            Span noInvoiceLabel = new Span("Für diesen Auftrag wurde noch keine Rechnung erstellt.");
            noInvoiceLabel.getStyle().set("font-style", "italic");

            Button createInvoiceBtn = new Button("Rechnung erzeugen", VaadinIcon.FILE_ADD.create(), e -> {
                try {
                    String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                    associatedInvoice = billingService.createInvoiceFromOrder(order, username);
                    Notification.show("Rechnung " + associatedInvoice.getInvoiceNumber() + " erfolgreich erstellt!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    setupBillingTab();
                } catch (Exception ex) {
                    Notification.show("Fehler bei Rechnungsstellung: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            createInvoiceBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            billingLayout.add(noInvoiceLabel, createInvoiceBtn);
        } else {
            // Display Invoice details
            FormLayout invoiceForm = new FormLayout();
            TextField invNum = new TextField("Rechnungsnummer");
            invNum.setValue(associatedInvoice.getInvoiceNumber());
            invNum.setReadOnly(true);

            TextField invStatus = new TextField("Status");
            invStatus.setValue(associatedInvoice.getStatus().name());
            invStatus.setReadOnly(true);

            TextField grossAmt = new TextField("Bruttobetrag");
            grossAmt.setValue(associatedInvoice.getGrossAmount().toString() + " EUR");
            grossAmt.setReadOnly(true);

            invoiceForm.add(invNum, invStatus, grossAmt);

            // PDF Download
            StreamResource pdfResource = new StreamResource(associatedInvoice.getInvoiceNumber() + ".pdf", () -> pdfService.generateInvoicePdf(associatedInvoice));
            Anchor downloadLink = new Anchor(pdfResource, "");
            downloadLink.getElement().setAttribute("download", true);
            Button downloadBtn = new Button("Rechnung PDF herunterladen", VaadinIcon.DOWNLOAD.create());
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            downloadLink.add(downloadBtn);

            billingLayout.add(invoiceForm, downloadLink);

            // Payments section
            H3 paymentsTitle = new H3("Erhaltene Zahlungen");
            Grid<Payment> paymentGrid = new Grid<>(Payment.class, false);
            paymentGrid.addColumn(p -> p.getAmount().toString() + " EUR").setHeader("Betrag");
            paymentGrid.addColumn(p -> p.getPaymentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).setHeader("Zahlungsdatum");
            paymentGrid.addColumn(Payment::getPaymentMethod).setHeader("Zahlungsart");
            paymentGrid.addColumn(Payment::getTransactionReference).setHeader("Referenz");

            paymentGrid.setItems(billingService.getPaymentsForInvoice(associatedInvoice.getId()));
            billingLayout.add(paymentsTitle, paymentGrid);

            // Record Payment Form
            if (associatedInvoice.getStatus() != com.translationagency.modules.billing.domain.InvoiceStatus.PAID) {
                H3 addPaymentTitle = new H3("Zahlungseingang buchen");
                FormLayout paymentForm = new FormLayout();

                NumberField amountField = new NumberField("Zahlungsbetrag (EUR)");
                amountField.setValue(associatedInvoice.getGrossAmount().doubleValue());

                ComboBox<String> methodCombo = new ComboBox<>("Zahlungsart");
                methodCombo.setItems("BANK_TRANSFER", "CREDIT_CARD", "PAYPAL", "CASH");
                methodCombo.setValue("BANK_TRANSFER");

                TextField refField = new TextField("Transaktions-Referenz");

                paymentForm.add(amountField, methodCombo, refField);

                Button recordBtn = new Button("Zahlung buchen", VaadinIcon.MONEY.create(), e -> {
                    if (amountField.getValue() == null) {
                        Notification.show("Bitte Betrag eingeben").addThemeVariants(NotificationVariant.LUMO_WARNING);
                        return;
                    }
                    String username = securityService.getAuthenticatedUser().map(org.springframework.security.core.userdetails.UserDetails::getUsername).orElse("system");
                    billingService.recordPayment(
                            associatedInvoice,
                            BigDecimal.valueOf(amountField.getValue()),
                            methodCombo.getValue(),
                            refField.getValue(),
                            username
                    );
                    Notification.show("Zahlung erfolgreich verbucht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    setupBillingTab();
                });
                recordBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

                billingLayout.add(addPaymentTitle, paymentForm, recordBtn);
            }
        }
    }

    private void setupItemsTab() {
        itemsLayout = new VerticalLayout();
        itemsLayout.setSizeFull();
        itemsLayout.setPadding(true);
        refreshItemsTab();
    }

    private void refreshItemsTab() {
        if (itemsLayout == null) return;
        itemsLayout.removeAll();

        H3 sectionTitle = new H3("Teilaufträge / Positionen");
        Button addItemBtn = new Button("Teilauftrag hinzufügen", VaadinIcon.PLUS.create(), e -> {
            TranslationOrderItem newItem = new TranslationOrderItem();
            newItem.setOrder(order);
            openTeilauftragDialog(newItem);
        });
        addItemBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Grid<TranslationOrderItem> grid = new Grid<>(TranslationOrderItem.class, false);
        grid.setSizeFull();
        grid.addColumn(TranslationOrderItem::getDescription).setHeader("Beschreibung").setAutoWidth(true);
        grid.addColumn(item -> {
            String sl = item.getSourceLanguage() != null ? item.getSourceLanguage().getCode().toUpperCase() : "-";
            String tl = item.getTargetLanguage() != null ? item.getTargetLanguage().getCode().toUpperCase() : "-";
            return sl + " -> " + tl;
        }).setHeader("Sprachen").setAutoWidth(true);
        grid.addColumn(TranslationOrderItem::getActivity).setHeader("Tätigkeit").setAutoWidth(true);
        grid.addColumn(item -> item.getAssignedPartner() != null ? item.getAssignedPartner().getFullName() : "-").setHeader("Übersetzer").setAutoWidth(true);
        grid.addColumn(TranslationOrderItem::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(item -> {
            if ("WORDS".equals(item.getUnit()) && item.getWordCount() != null) {
                return item.getWordCount() + " W.";
            } else if ("PAGES".equals(item.getUnit()) && item.getPageCount() != null) {
                return item.getPageCount() + " S.";
            }
            return item.getQuantity() + " " + item.getUnit();
        }).setHeader("Umfang").setAutoWidth(true);
        grid.addColumn(item -> item.getTotalPrice().toString() + " EUR").setHeader("Gesamtpreis").setAutoWidth(true);

        grid.addComponentColumn(item -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openTeilauftragDialog(item));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                order.getItems().remove(item);
                orderService.recalculateOrderTotals(order);
                orderService.saveOrder(order);
                Notification.show("Teilauftrag gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshItemsTab();
                refreshDetailsTab();
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Aktionen").setAutoWidth(true);

        grid.setItems(order.getItems());
        itemsLayout.add(sectionTitle, addItemBtn, grid);
    }

    private void openTeilauftragDialog(TranslationOrderItem item) {
        TeilauftragDialog dialog = new TeilauftragDialog(
                pricingService,
                partnerService,
                item,
                tenant.getId(),
                savedItem -> {
                    if (!order.getItems().contains(savedItem)) {
                        order.getItems().add(savedItem);
                    }
                    orderService.recalculateOrderTotals(order);
                    orderService.saveOrder(order);
                    Notification.show("Teilauftrag erfolgreich gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshItemsTab();
                    refreshDetailsTab();
                }
        );
        dialog.open();
    }
}
