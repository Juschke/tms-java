package com.translationagency.modules.inquiry.ui;

import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.document.application.DocumentService;
import com.translationagency.modules.document.application.PdfService;
import com.translationagency.modules.document.ui.DocumentAttachmentList;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import com.translationagency.modules.inquiry.application.InquiryService;
import com.translationagency.modules.order.application.OrderService;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.inquiry.domain.Inquiry;
import com.translationagency.modules.inquiry.domain.InquiryStatus;
import com.translationagency.modules.inquiry.domain.Quote;
import com.translationagency.modules.inquiry.domain.QuoteStatus;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.pricing.domain.PriceRule;
import com.translationagency.modules.pricing.domain.ServiceType;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.UUID;

@Route(value = "inquiries/detail", layout = MainLayout.class)
@PageTitle("Anfrage Details | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class InquiryDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final InquiryService inquiryService;
    private final CustomerService customerService;
    private final PricingService pricingService;
    private final DocumentService documentService;
    private final SecurityService securityService;
    private final PdfService pdfService;
    private final OrderService orderService;

    private Inquiry inquiry;
    private Tenant tenant;
    private Quote associatedQuote;

    private final H2 title = new H2("Anfrage Details");
    private final Tabs tabs = new Tabs();
    private final Div contentContainer = new Div();

    // Tab Contents
    private VerticalLayout detailsLayout;
    private DocumentAttachmentList filesLayout;
    private VerticalLayout calculationLayout;

    // Calculation Form Fields (Overrides)
    private IntegerField wordCountField;
    private IntegerField pageCountField;
    private Checkbox isCertifiedField;
    private Checkbox isExpressField;
    
    // Results Preview
    private final Span netAmountLabel = new Span("- EUR");
    private final Span vatAmountLabel = new Span("- EUR");
    private final Span grossAmountLabel = new Span("- EUR");
    private final TextArea calculationDetails = new TextArea("Kalkulations-Details");

    public InquiryDetailView(InquiryService inquiryService, CustomerService customerService,
                             PricingService pricingService, DocumentService documentService,
                             SecurityService securityService, PdfService pdfService,
                             OrderService orderService) {
        this.inquiryService = inquiryService;
        this.customerService = customerService;
        this.pricingService = pricingService;
        this.documentService = documentService;
        this.securityService = securityService;
        this.pdfService = pdfService;
        this.orderService = orderService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.tenant = t);

        Button backButton = new Button("Zurück zur Liste", VaadinIcon.ARROW_LEFT.create(), e -> getUI().ifPresent(ui -> ui.navigate("inquiries")));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(backButton, title);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        add(header);

        createTabs();
        add(tabs, contentContainer);
        contentContainer.setSizeFull();
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            UUID id = UUID.fromString(parameter);
            inquiryService.getAllInquiries(tenant.getId()).stream()
                    .filter(inq -> inq.getId().equals(id))
                    .findFirst()
                    .ifPresentOrElse(inq -> {
                        this.inquiry = inq;
                        this.title.setText("Anfrage von " + inq.getCustomer().getCompanyName());
                        
                        // Check if a quote already exists for this inquiry
                        this.associatedQuote = inquiryService.getAllQuotes(tenant.getId()).stream()
                                .filter(q -> q.getInquiry() != null && q.getInquiry().getId().equals(inq.getId()))
                                .findFirst()
                                .orElse(null);

                        setupTabContents();
                    }, () -> {
                        Notification.show("Anfrage nicht gefunden", 3000, Notification.Position.MIDDLE);
                        getUI().ifPresent(ui -> ui.navigate("inquiries"));
                    });
        } catch (IllegalArgumentException e) {
            Notification.show("Ungültige Anfrage-ID", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("inquiries"));
        }
    }

    private void createTabs() {
        Tab detailsTab = new Tab("Anfragedaten");
        Tab filesTab = new Tab("Quelldokumente");
        Tab calcTab = new Tab("Kalkulation & Angebot");

        tabs.add(detailsTab, filesTab, calcTab);
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(detailsTab)) {
                contentContainer.add(detailsLayout);
            } else if (event.getSelectedTab().equals(filesTab)) {
                contentContainer.add(filesLayout);
                filesLayout.refresh();
            } else if (event.getSelectedTab().equals(calcTab)) {
                contentContainer.add(calculationLayout);
                performPreviewCalculation();
            }
        });
    }

    private void setupTabContents() {
        setupDetailsTab();
        setupFilesTab();
        setupCalculationTab();

        contentContainer.removeAll();
        contentContainer.add(detailsLayout);
        tabs.setSelectedIndex(0);
    }

    private void setupDetailsTab() {
        detailsLayout = new VerticalLayout();
        detailsLayout.setSizeFull();
        detailsLayout.setPadding(true);

        FormLayout form = new FormLayout();
        TextField customerField = new TextField("Kunde");
        customerField.setValue(inquiry.getCustomer().getCompanyName());
        customerField.setReadOnly(true);

        TextField contactField = new TextField("Ansprechpartner");
        contactField.setValue(inquiry.getContactPerson() != null ? inquiry.getContactPerson().getFullName() : "-");
        contactField.setReadOnly(true);

        TextField serviceField = new TextField("Leistungsart");
        serviceField.setValue(inquiry.getServiceType() != null ? inquiry.getServiceType().getName() : "-");
        serviceField.setReadOnly(true);

        TextField languagesField = new TextField("Sprachkombination");
        String sl = inquiry.getSourceLanguage() != null ? inquiry.getSourceLanguage().getName() : "-";
        String tl = inquiry.getTargetLanguage() != null ? inquiry.getTargetLanguage().getName() : "-";
        languagesField.setValue(sl + " -> " + tl);
        languagesField.setReadOnly(true);

        IntegerField wordCount = new IntegerField("Wortanzahl");
        wordCount.setValue(inquiry.getWordCount());

        IntegerField pageCount = new IntegerField("Seitenanzahl");
        pageCount.setValue(inquiry.getPageCount());

        Checkbox isCertified = new Checkbox("Beglaubigt");
        isCertified.setValue(inquiry.isCertified());

        Checkbox isExpress = new Checkbox("Express");
        isExpress.setValue(inquiry.isExpress());

        TextArea notes = new TextArea("Kunden-Hinweise");
        notes.setValue(inquiry.getNotes() != null ? inquiry.getNotes() : "");

        form.add(customerField, contactField, serviceField, languagesField, wordCount, pageCount, isCertified, isExpress, notes);
        form.setColspan(notes, 2);

        Binder<Inquiry> binder = new Binder<>(Inquiry.class);
        binder.bind(wordCount, Inquiry::getWordCount, Inquiry::setWordCount);
        binder.bind(pageCount, Inquiry::getPageCount, Inquiry::setPageCount);
        binder.bind(isCertified, Inquiry::isCertified, Inquiry::setCertified);
        binder.bind(isExpress, Inquiry::isExpress, Inquiry::setExpress);
        binder.bind(notes, Inquiry::getNotes, Inquiry::setNotes);

        Button saveButton = new Button("Anfrage aktualisieren", e -> {
            try {
                binder.writeBean(inquiry);
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                inquiry.setUpdatedBy(username);
                inquiryService.saveInquiry(inquiry);
                Notification.show("Anfrage erfolgreich aktualisiert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        detailsLayout.add(form, saveButton);
    }

    private void setupFilesTab() {
        filesLayout = new DocumentAttachmentList(
                documentService,
                securityService,
                tenant,
                inquiry.getCustomer(),
                "INQUIRY",
                inquiry.getId()
        );
    }

    private void setupCalculationTab() {
        calculationLayout = new VerticalLayout();
        calculationLayout.setSizeFull();
        calculationLayout.setPadding(true);

        H3 sectionTitle = new H3("Kalkulation & Angebotserstellung");

        FormLayout calculationParams = new FormLayout();
        wordCountField = new IntegerField("Wortanzahl");
        wordCountField.setValue(inquiry.getWordCount() != null ? inquiry.getWordCount() : 0);
        wordCountField.addValueChangeListener(e -> performPreviewCalculation());

        pageCountField = new IntegerField("Seitenanzahl");
        pageCountField.setValue(inquiry.getPageCount() != null ? inquiry.getPageCount() : 0);
        pageCountField.addValueChangeListener(e -> performPreviewCalculation());

        isCertifiedField = new Checkbox("Beglaubigte Übersetzung");
        isCertifiedField.setValue(inquiry.isCertified());
        isCertifiedField.addValueChangeListener(e -> performPreviewCalculation());

        isExpressField = new Checkbox("Expresszuschlag");
        isExpressField.setValue(inquiry.isExpress());
        isExpressField.addValueChangeListener(e -> performPreviewCalculation());

        calculationParams.add(wordCountField, pageCountField, isCertifiedField, isExpressField);

        // Preview Area
        VerticalLayout previewArea = new VerticalLayout();
        previewArea.setSpacing(true);
        previewArea.setPadding(true);
        previewArea.addClassName("bg-contrast-5"); // subtle background box
        previewArea.getStyle().set("border-radius", "var(--lumo-size-s)");

        HorizontalLayout netRow = new HorizontalLayout(new Span("Netto-Gesamtbetrag:"), netAmountLabel);
        netRow.setWidthFull(); netRow.expand(netRow.getChildren().findFirst().get());
        netAmountLabel.getStyle().set("font-weight", "bold");

        HorizontalLayout vatRow = new HorizontalLayout(new Span("MwSt. (19%):"), vatAmountLabel);
        vatRow.setWidthFull(); vatRow.expand(vatRow.getChildren().findFirst().get());

        HorizontalLayout grossRow = new HorizontalLayout(new Span("Brutto-Gesamtbetrag:"), grossAmountLabel);
        grossRow.setWidthFull(); grossRow.expand(grossRow.getChildren().findFirst().get());
        grossAmountLabel.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-text-color)");

        previewArea.add(netRow, vatRow, grossRow);

        calculationDetails.setReadOnly(true);
        calculationDetails.setWidthFull();
        calculationDetails.setHeight("150px");

        // Action Buttons
        Button generateQuoteBtn = new Button("Angebot erzeugen & speichern", VaadinIcon.FILE_TEXT.create());
        generateQuoteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Anchor downloadLink = null;
        if (associatedQuote != null) {
            generateQuoteBtn.setText("Angebot aktualisieren / neu erzeugen");
            generateQuoteBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            
            Span quoteInfo = new Span("Dieses Angebot existiert bereits: " + associatedQuote.getQuoteNumber() + " (Status: " + associatedQuote.getStatus() + ")");
            quoteInfo.getStyle().set("font-weight", "bold").set("color", "var(--lumo-success-text-color)");
            calculationLayout.add(quoteInfo);

            StreamResource pdfResource = new StreamResource(associatedQuote.getQuoteNumber() + ".pdf", () -> pdfService.generateQuotePdf(associatedQuote));
            downloadLink = new Anchor(pdfResource, "");
            downloadLink.getElement().setAttribute("download", true);
            Button downloadBtn = new Button("Angebot PDF herunterladen", VaadinIcon.DOWNLOAD.create());
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            downloadLink.add(downloadBtn);
        }

        generateQuoteBtn.addClickListener(e -> generateQuote());

        HorizontalLayout actionsLayout = new HorizontalLayout(generateQuoteBtn);
        if (downloadLink != null) {
            actionsLayout.add(downloadLink);
            
            if (associatedQuote.getStatus() != QuoteStatus.ACCEPTED) {
                Button createOrderBtn = new Button("Auftrag erstellen", VaadinIcon.CLIPBOARD_TEXT.create());
                createOrderBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
                createOrderBtn.addClickListener(clickEvent -> {
                    try {
                        String username = securityService.getAuthenticatedUser()
                                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                                .orElse("system");
                        TranslationOrder order = orderService.createOrderFromQuote(associatedQuote, username);
                        Notification.show("Auftrag " + order.getOrderNumber() + " erfolgreich erstellt!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        getUI().ifPresent(ui -> ui.navigate("inquiries/detail/" + inquiry.getId().toString()));
                    } catch (Exception ex) {
                        Notification.show("Fehler beim Erstellen des Auftrags: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                actionsLayout.add(createOrderBtn);
            } else {
                Span acceptedInfo = new Span("Auftrag wurde bereits erstellt");
                acceptedInfo.getStyle().set("font-weight", "bold").set("color", "var(--lumo-success-text-color)");
                actionsLayout.add(acceptedInfo);
            }
        }

        calculationLayout.add(sectionTitle, calculationParams, new H3("Kalkulationsvorschau"), previewArea, calculationDetails, actionsLayout);
    }

    private void performPreviewCalculation() {
        if (inquiry == null || inquiry.getServiceType() == null) return;

        PricingService.CalculationResult result = pricingService.calculatePrice(
                tenant.getId(),
                inquiry.getSourceLanguage() != null ? inquiry.getSourceLanguage().getId() : null,
                inquiry.getTargetLanguage() != null ? inquiry.getTargetLanguage().getId() : null,
                inquiry.getServiceType().getId(),
                wordCountField.getValue() != null ? wordCountField.getValue() : 0,
                pageCountField.getValue() != null ? pageCountField.getValue() : 0,
                isCertifiedField.getValue(),
                isExpressField.getValue()
        );

        netAmountLabel.setText(result.netAmount().toString() + " EUR");
        vatAmountLabel.setText(result.vatAmount().toString() + " EUR");
        grossAmountLabel.setText(result.grossAmount().toString() + " EUR");
        calculationDetails.setValue(result.calculationDetails());
    }

    private void generateQuote() {
        try {
            // Update counts in inquiry model
            inquiry.setWordCount(wordCountField.getValue());
            inquiry.setPageCount(pageCountField.getValue());
            inquiry.setCertified(isCertifiedField.getValue());
            inquiry.setExpress(isExpressField.getValue());
            
            String username = securityService.getAuthenticatedUser()
                    .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                    .orElse("system");
            
            inquiry.setUpdatedBy(username);
            inquiryService.saveInquiry(inquiry);

            PricingService.CalculationResult result = pricingService.calculatePrice(
                    tenant.getId(),
                    inquiry.getSourceLanguage() != null ? inquiry.getSourceLanguage().getId() : null,
                    inquiry.getTargetLanguage() != null ? inquiry.getTargetLanguage().getId() : null,
                    inquiry.getServiceType().getId(),
                    inquiry.getWordCount() != null ? inquiry.getWordCount() : 0,
                    inquiry.getPageCount() != null ? inquiry.getPageCount() : 0,
                    inquiry.isCertified(),
                    inquiry.isExpress()
            );

            Quote quote = inquiryService.createQuoteFromInquiry(inquiry, result, username);
            this.associatedQuote = quote;

            Notification.show("Angebot " + quote.getQuoteNumber() + " erfolgreich generiert!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Reload page or navigate to quote list (we don't have QuotesView yet, but we will soon)
            getUI().ifPresent(ui -> ui.navigate("inquiries/detail/" + inquiry.getId().toString()));
        } catch (Exception ex) {
            Notification.show("Fehler bei der Angebotserstellung: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
