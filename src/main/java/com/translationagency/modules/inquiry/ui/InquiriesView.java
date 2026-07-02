package com.translationagency.modules.inquiry.ui;

import com.translationagency.modules.crm.application.CustomerService;
import com.translationagency.modules.crm.domain.ContactPerson;
import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.inquiry.application.InquiryService;
import com.translationagency.modules.inquiry.domain.Inquiry;
import com.translationagency.modules.inquiry.domain.InquiryStatus;
import com.translationagency.modules.pricing.application.PricingService;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.pricing.domain.ServiceType;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

@Route(value = "inquiries", layout = MainLayout.class)
@PageTitle("Anfragen | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class InquiriesView extends VerticalLayout {

    private final InquiryService inquiryService;
    private final CustomerService customerService;
    private final PricingService pricingService;
    private final SecurityService securityService;

    private InquiryEnterpriseGrid grid;
    private Tenant currentTenant;

    public InquiriesView(InquiryService inquiryService, CustomerService customerService,
                         PricingService pricingService, SecurityService securityService) {
        this.inquiryService = inquiryService;
        this.customerService = customerService;
        this.pricingService = pricingService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.currentTenant = t);

        if (currentTenant != null) {
            grid = new InquiryEnterpriseGrid(inquiryService, pricingService, currentTenant.getId());
        }

        add(createHeaderLayout());
        if (grid != null) {
            add(grid);
            setFlexGrow(1, grid);
        }
    }

    private HorizontalLayout createHeaderLayout() {
        H2 headerTitle = new H2("Kundenanfragen");
        headerTitle.addClassNames("m-0", "text-xl");

        Button addInquiryButton = new Button("Neue Anfrage", VaadinIcon.PLUS.create(), e -> openInquiryDialog(new Inquiry()));
        addInquiryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(headerTitle, addInquiryButton);
        header.setWidthFull();
        header.expand(headerTitle);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        return header;
    }

    private void updateList() {
        if (grid != null) {
            grid.refresh();
        }
    }

    private void openInquiryDialog(Inquiry inquiry) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neue Anfrage erfassen");
        dialog.setWidth("600px");

        FormLayout formLayout = new FormLayout();

        ComboBox<Customer> customerCombo = new ComboBox<>("Kunde");
        customerCombo.setItems(customerService.getAllCustomers(currentTenant.getId()));
        customerCombo.setItemLabelGenerator(Customer::getCompanyName);
        customerCombo.setRequired(true);

        ComboBox<ContactPerson> contactCombo = new ComboBox<>("Ansprechpartner");
        contactCombo.setItemLabelGenerator(ContactPerson::getFullName);
        customerCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                contactCombo.setItems(customerService.getContactPersons(e.getValue().getId()));
            } else {
                contactCombo.setItems(Collections.emptyList());
            }
        });

        ComboBox<Language> sourceLang = new ComboBox<>("Quellsprache");
        sourceLang.setItems(pricingService.getAllLanguages());
        sourceLang.setItemLabelGenerator(Language::getName);

        ComboBox<Language> targetLang = new ComboBox<>("Zielsprache");
        targetLang.setItems(pricingService.getAllLanguages());
        targetLang.setItemLabelGenerator(Language::getName);

        ComboBox<ServiceType> serviceTypeCombo = new ComboBox<>("Dienstleistung");
        serviceTypeCombo.setItems(pricingService.getAllServiceTypes());
        serviceTypeCombo.setItemLabelGenerator(ServiceType::getName);
        serviceTypeCombo.setRequired(true);

        IntegerField wordCount = new IntegerField("Wortanzahl");
        IntegerField pageCount = new IntegerField("Seitenanzahl");

        Checkbox isCertified = new Checkbox("Beglaubigte Übersetzung");
        Checkbox isExpress = new Checkbox("Expressauftrag (Zuschlag)");

        ComboBox<String> deliveryMethod = new ComboBox<>("Lieferart");
        deliveryMethod.setItems("EMAIL", "POST", "PICKUP");
        deliveryMethod.setValue("EMAIL");

        TextArea notes = new TextArea("Hinweise / Details");
        notes.setPlaceholder("z.B. Besondere Fachterminologie...");

        formLayout.add(customerCombo, contactCombo, sourceLang, targetLang, serviceTypeCombo, wordCount, pageCount, isCertified, isExpress, deliveryMethod, notes);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        formLayout.setColspan(notes, 2);

        Binder<Inquiry> binder = new Binder<>(Inquiry.class);
        binder.forField(customerCombo).asRequired("Kunde ist erforderlich").bind(Inquiry::getCustomer, Inquiry::setCustomer);
        binder.bind(contactCombo, Inquiry::getContactPerson, Inquiry::setContactPerson);
        binder.bind(sourceLang, Inquiry::getSourceLanguage, Inquiry::setSourceLanguage);
        binder.bind(targetLang, Inquiry::getTargetLanguage, Inquiry::setTargetLanguage);
        binder.forField(serviceTypeCombo).asRequired("Dienstleistung ist erforderlich").bind(Inquiry::getServiceType, Inquiry::setServiceType);
        binder.bind(wordCount, Inquiry::getWordCount, Inquiry::setWordCount);
        binder.bind(pageCount, Inquiry::getPageCount, Inquiry::setPageCount);
        binder.bind(isCertified, Inquiry::isCertified, Inquiry::setCertified);
        binder.bind(isExpress, Inquiry::isExpress, Inquiry::setExpress);
        binder.bind(deliveryMethod, Inquiry::getDeliveryMethod, Inquiry::setDeliveryMethod);
        binder.bind(notes, Inquiry::getNotes, Inquiry::setNotes);

        binder.readBean(inquiry);

        Button saveButton = new Button("Anfrage anlegen", e -> {
            try {
                binder.writeBean(inquiry);
                inquiry.setTenant(currentTenant);
                inquiry.setStatus(InquiryStatus.RECEIVED);
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                inquiry.setCreatedBy(username);
                inquiry.setUpdatedBy(username);
                inquiryService.saveInquiry(inquiry);
                dialog.close();
                updateList();
                Notification.show("Anfrage erfolgreich erfasst").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }
}
