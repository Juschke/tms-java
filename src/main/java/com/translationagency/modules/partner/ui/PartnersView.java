package com.translationagency.modules.partner.ui;

import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.shared.ui.Confirmations;
import com.translationagency.shared.util.CsvUtils;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Route(value = "partners", layout = MainLayout.class)
@PageTitle("Partner | Translation Management")
@RolesAllowed({"ADMIN", "MANAGER", "CASE_WORKER"})
public class PartnersView extends VerticalLayout {

    private final PartnerService partnerService;
    private final SecurityService securityService;

    private PartnerEnterpriseGrid grid;
    private Tenant currentTenant;

    public PartnersView(PartnerService partnerService, SecurityService securityService) {
        this.partnerService = partnerService;
        this.securityService = securityService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        securityService.getAuthenticatedTenant().ifPresent(t -> this.currentTenant = t);

        if (currentTenant != null) {
            grid = new PartnerEnterpriseGrid(partnerService, currentTenant.getId(),
                    partner -> getUI().ifPresent(ui -> ui.navigate("partners/detail/" + partner.getId().toString())),
                    this::deletePartner);
        }

        add(createHeaderLayout());
        if (grid != null) {
            add(grid);
            setFlexGrow(1, grid);
        }
    }

    private HorizontalLayout createHeaderLayout() {
        H2 headerTitle = new H2("Partner & Übersetzer");
        headerTitle.addClassNames("m-0", "text-xl");

        Button addPartnerButton = new Button("Neuer Partner", VaadinIcon.PLUS.create(), e -> openPartnerDialog(new Partner()));
        addPartnerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button importButton = new Button("CSV Import", VaadinIcon.UPLOAD.create(), e -> openPartnerImportDialog());
        importButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(headerTitle, createPartnerExportAnchor(), importButton, addPartnerButton);
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

    private Anchor createPartnerExportAnchor() {
        StreamResource resource = new StreamResource("partner.csv", () -> CsvUtils.toUtf8Csv(buildPartnerCsv()));
        resource.setContentType("text/csv;charset=utf-8");

        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        Button exportButton = new Button("CSV Export", VaadinIcon.DOWNLOAD.create());
        exportButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        anchor.add(exportButton);
        return anchor;
    }

    private void openPartnerImportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Partner importieren");
        dialog.setWidth("520px");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv", "text/csv");
        upload.setMaxFiles(1);
        upload.addSucceededListener(event -> {
            try {
                int imported = importPartners(buffer);
                dialog.close();
                updateList();
                Notification.show(imported + " Partner importiert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Import fehlgeschlagen: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.add(upload);
        dialog.getFooter().add(new Button("Schliessen", e -> dialog.close()));
        dialog.open();
    }

    private String buildPartnerCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append(CsvUtils.row("partner_number", "company_name", "first_name", "last_name",
                "email", "phone", "street", "zip", "city", "country", "classification", "active", "recommended"));
        partnerService.getAllPartners(currentTenant.getId()).forEach(partner -> csv.append(CsvUtils.row(
                partner.getPartnerNumber(),
                partner.getCompanyName(),
                partner.getFirstName(),
                partner.getLastName(),
                partner.getEmail(),
                partner.getPhone(),
                partner.getStreet(),
                partner.getZip(),
                partner.getCity(),
                partner.getCountry(),
                partner.getClassification(),
                String.valueOf(partner.isActive()),
                String.valueOf(partner.isRecommended())
        )));
        return csv.toString();
    }

    private int importPartners(MemoryBuffer buffer) throws Exception {
        List<Partner> existing = partnerService.getAllPartners(currentTenant.getId());
        String username = securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("system");
        int imported = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(buffer.getInputStream(), StandardCharsets.UTF_8))) {
            boolean firstLine = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (firstLine && line.charAt(0) == 0xFEFF) {
                    line = line.substring(1);
                }

                List<String> columns = CsvUtils.parseLine(line);
                if (firstLine && !columns.isEmpty()
                        && "partner_number".equalsIgnoreCase(columns.get(0).trim())) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String partnerNumber = valueAt(columns, 0);
                String email = valueAt(columns, 4);
                if (email.isBlank()) {
                    continue;
                }

                Partner partner = findPartner(existing, partnerNumber, email);
                if (partner == null) {
                    partner = new Partner();
                    partner.setTenant(currentTenant);
                    partner.setCreatedBy(username);
                    if (!partnerNumber.isBlank()) {
                        partner.setPartnerNumber(partnerNumber);
                    }
                    existing.add(partner);
                }

                partner.setCompanyName(valueAt(columns, 1));
                partner.setFirstName(valueAt(columns, 2));
                partner.setLastName(valueAt(columns, 3));
                partner.setEmail(email);
                partner.setPhone(valueAt(columns, 5));
                partner.setStreet(valueAt(columns, 6));
                partner.setZip(valueAt(columns, 7));
                partner.setCity(valueAt(columns, 8));
                partner.setCountry(valueAt(columns, 9));
                partner.setClassification(valueOrDefault(valueAt(columns, 10), "extern"));
                boolean active = booleanAt(columns, 11, true);
                partner.setActive(active);
                partner.setStatus(active ? "ACTIVE" : "INACTIVE");
                partner.setRecommended(booleanAt(columns, 12, false));
                partner.setUpdatedBy(username);
                partnerService.savePartner(partner);
                imported++;
            }
        }

        return imported;
    }

    private Partner findPartner(List<Partner> partners, String partnerNumber, String email) {
        return partners.stream()
                .filter(partner -> !partnerNumber.isBlank()
                        && partnerNumber.equalsIgnoreCase(valueOrEmpty(partner.getPartnerNumber())))
                .findFirst()
                .or(() -> partners.stream()
                        .filter(partner -> !email.isBlank()
                                && email.equalsIgnoreCase(valueOrEmpty(partner.getEmail())))
                        .findFirst())
                .orElse(null);
    }

    private void deletePartner(Partner partner) {
        Confirmations.delete("Partner loeschen",
                "Soll der Partner " + partner.getFullName() + " wirklich geloescht werden?",
                () -> {
        String username = securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("system");
        partnerService.deletePartner(partner.getId(), username);
        Notification.show("Partner gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        updateList();
                });
    }

    private void openPartnerDialog(Partner partner) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(partner.getId() == null ? "Neuen Partner anlegen" : "Partner bearbeiten");

        FormLayout formLayout = new FormLayout();
        TextField companyName = new TextField("Firmenname (optional)");
        TextField firstName = new TextField("Vorname");
        TextField lastName = new TextField("Nachname");
        TextField email = new TextField("E-Mail");
        TextField phone = new TextField("Telefon");

        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.add(companyName, firstName, lastName, email, phone);

        Binder<Partner> binder = new Binder<>(Partner.class);
        binder.bind(companyName, Partner::getCompanyName, Partner::setCompanyName);
        binder.forField(firstName).asRequired("Vorname ist erforderlich").bind(Partner::getFirstName, Partner::setFirstName);
        binder.forField(lastName).asRequired("Nachname ist erforderlich").bind(Partner::getLastName, Partner::setLastName);
        binder.forField(email).asRequired("E-Mail ist erforderlich").bind(Partner::getEmail, Partner::setEmail);
        binder.bind(phone, Partner::getPhone, Partner::setPhone);

        binder.readBean(partner);

        Button saveButton = new Button("Speichern", e -> {
            try {
                binder.writeBean(partner);
                partner.setTenant(currentTenant);
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                if (partner.getId() == null) {
                    partner.setCreatedBy(username);
                }
                partner.setUpdatedBy(username);
                partnerService.savePartner(partner);
                dialog.close();
                updateList();
                com.translationagency.shared.ui.Notifications.success(
                        "Partner " + (partner.getPartnerNumber() != null ? partner.getPartnerNumber() + " " : "")
                                + "erfolgreich gespeichert");
            } catch (Exception ex) {
                com.translationagency.shared.ui.Notifications.error("Fehler beim Speichern: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }

    private boolean booleanAt(List<String> values, int index, boolean defaultValue) {
        String value = valueAt(values, index);
        if (value.isBlank()) {
            return defaultValue;
        }
        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("ja")
                || value.equalsIgnoreCase("yes")
                || value.equals("1")
                || value.equalsIgnoreCase("aktiv");
    }

    private String valueAt(List<String> values, int index) {
        return index < values.size() && values.get(index) != null ? values.get(index).trim() : "";
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
