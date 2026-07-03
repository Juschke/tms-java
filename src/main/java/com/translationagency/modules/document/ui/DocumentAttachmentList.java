package com.translationagency.modules.document.ui;

import com.translationagency.modules.crm.domain.Customer;
import com.translationagency.modules.document.application.DocumentService;
import com.translationagency.modules.document.domain.Document;
import com.translationagency.modules.document.domain.DocumentCategory;
import com.translationagency.modules.document.domain.DocumentVersion;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.shared.ui.Confirmations;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class DocumentAttachmentList extends VerticalLayout {

    private final DocumentService documentService;
    private final SecurityService securityService;
    private final Tenant tenant;
    private final Customer customer;
    private final String associatedEntityType;
    private final UUID associatedEntityId;

    private final Grid<Document> grid = new Grid<>(Document.class, false);

    public DocumentAttachmentList(DocumentService documentService, SecurityService securityService,
                                  Tenant tenant, Customer customer, String associatedEntityType, UUID associatedEntityId) {
        this.documentService = documentService;
        this.securityService = securityService;
        this.tenant = tenant;
        this.customer = customer;
        this.associatedEntityType = associatedEntityType;
        this.associatedEntityId = associatedEntityId;

        setSizeFull();
        setSpacing(true);
        setPadding(false);

        add(createUploadLayout());
        add(createGridLayout());

        refresh();
    }

    private HorizontalLayout createUploadLayout() {
        Button uploadBtn = new Button("Dokument hochladen", VaadinIcon.UPLOAD.create());
        uploadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        uploadBtn.addClickListener(e -> openUploadDialog());

        HorizontalLayout layout = new HorizontalLayout(uploadBtn);
        layout.setWidthFull();
        return layout;
    }

    private Grid<Document> createGridLayout() {
        grid.setSizeFull();

        // 1. Nicht übersetzen Checkbox
        grid.addComponentColumn(doc -> {
            Checkbox cb = new Checkbox();
            cb.setValue(doc.isDoNotTranslate());
            cb.addValueChangeListener(e -> {
                doc.setDoNotTranslate(cb.getValue());
                documentService.saveDocument(doc);
                Notification.show("Änderung gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
            return cb;
        }).setHeader("Nicht übersetzen").setAutoWidth(true);

        // 2. Dokumentname
        grid.addColumn(Document::getName).setHeader("Dokumentname").setAutoWidth(true);

        // 3. Dokumenttyp ComboBox
        grid.addComponentColumn(doc -> {
            ComboBox<DocumentCategory> combo = new ComboBox<>();
            combo.setItems(DocumentCategory.values());
            combo.setItemLabelGenerator(cat -> {
                switch (cat) {
                    case SOURCE_FILE: return "Ausgangsdokument";
                    case TRANSLATION: return "Übersetzungsergebnis";
                    case INVOICE: return "Ausgangsrechnung";
                    case QUOTE: return "Angebot";
                    case SUPPORTING: return "Referenzmaterial";
                    default: return "Sonstige";
                }
            });
            combo.setValue(doc.getCategory());
            combo.addValueChangeListener(e -> {
                if (e.getValue() != null && e.getValue() != doc.getCategory()) {
                    doc.setCategory(e.getValue());
                    documentService.saveDocument(doc);
                    Notification.show("Kategorie aktualisiert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            });
            combo.setWidth("180px");
            return combo;
        }).setHeader("Dokumenttyp").setAutoWidth(true);

        // 4. Version
        grid.addColumn(doc -> {
            DocumentVersion version = doc.getLatestVersion();
            return version != null ? version.getVersionNumber() : 1;
        }).setHeader("Version").setAutoWidth(true);

        // 5. Bearbeiter
        grid.addColumn(doc -> {
            DocumentVersion version = doc.getLatestVersion();
            return version != null ? version.getCreatedBy() : "-";
        }).setHeader("Bearbeiter").setAutoWidth(true);

        // 6. Datum
        grid.addColumn(doc -> {
            DocumentVersion version = doc.getLatestVersion();
            if (version == null || version.getCreatedAt() == null) return "-";
            return version.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy (HH:mm)"));
        }).setHeader("Datum").setAutoWidth(true);

        // 7. Pb Daten Checkbox
        grid.addComponentColumn(doc -> {
            Checkbox cb = new Checkbox();
            cb.setValue(doc.isContainsPersonalData());
            cb.addValueChangeListener(e -> {
                doc.setContainsPersonalData(cb.getValue());
                documentService.saveDocument(doc);
                Notification.show("Änderung gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
            return cb;
        }).setHeader("Pb Daten").setAutoWidth(true);

        // 8. Actions
        grid.addComponentColumn(doc -> {
            DocumentVersion version = doc.getLatestVersion();
            if (version == null) return new Span();

            StreamResource resource = new StreamResource(version.getFileName(), () -> {
                try {
                    return documentService.getDocumentContent(version);
                } catch (IOException e) {
                    throw new RuntimeException("Fehler beim Abrufen des Inhalts", e);
                }
            });

            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            Button downloadButton = new Button(VaadinIcon.DOWNLOAD.create());
            downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            downloadLink.add(downloadButton);

            Button versionBtn = new Button(VaadinIcon.UPLOAD.create(), e -> openVersionUploadDialog(doc));
            versionBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            versionBtn.setTooltipText("Neue Version hochladen");

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> Confirmations.delete("Dokument loeschen",
                    "Soll das Dokument " + doc.getName() + " wirklich geloescht werden?",
                    () -> {
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                documentService.deleteDocument(doc.getId(), username);
                Notification.show("Dokument gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refresh();
            }));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            HorizontalLayout actions = new HorizontalLayout(downloadLink, versionBtn, deleteBtn);
            actions.setSpacing(true);
            return actions;
        }).setHeader("Aktionen").setAutoWidth(true);

        return grid;
    }

    public void refresh() {
        if (tenant != null) {
            grid.setItems(documentService.getDocumentsForEntity(tenant.getId(), associatedEntityType, associatedEntityId));
        }
    }

    private void openUploadDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Dokument hochladen");
        dialog.setWidth("450px");

        FormLayout formLayout = new FormLayout();
        TextField displayNameField = new TextField("Anzeigename");
        displayNameField.setRequired(true);

        ComboBox<DocumentCategory> categoryCombo = new ComboBox<>("Dokumentkategorie");
        categoryCombo.setItems(DocumentCategory.values());
        categoryCombo.setItemLabelGenerator(cat -> {
            switch (cat) {
                case SOURCE_FILE: return "Quelldatei";
                case TRANSLATION: return "Übersetzung";
                case INVOICE: return "Rechnung";
                case QUOTE: return "Angebot";
                case SUPPORTING: return "Referenzmaterial";
                default: return "Sonstige";
            }
        });
        categoryCombo.setValue(DocumentCategory.SOURCE_FILE);
        categoryCombo.setRequired(true);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(
                "application/pdf", 
                "application/msword", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain", 
                "application/zip", 
                "image/png", 
                "image/jpeg"
        );
        upload.setMaxFileSize(50 * 1024 * 1024); // 50 MB limit
        upload.setDropLabel(new Span("Ziehe Dateien hierher (max. 50 MB)"));

        final long[] uploadedSize = {0};
        final String[] uploadedMime = {"application/octet-stream"};

        upload.addSucceededListener(event -> {
            displayNameField.setValue(event.getFileName());
            uploadedSize[0] = event.getContentLength();
            uploadedMime[0] = event.getMIMEType();
        });

        formLayout.add(displayNameField, categoryCombo, upload);

        Button saveBtn = new Button("Hochladen", e -> {
            String fileName = buffer.getFileName();
            if (fileName == null || fileName.isEmpty()) {
                Notification.show("Bitte wähle eine Datei aus").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (displayNameField.isEmpty()) {
                Notification.show("Anzeigename ist erforderlich").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try (InputStream fileStream = buffer.getInputStream()) {
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");

                documentService.uploadDocument(
                        tenant,
                        customer,
                        associatedEntityType,
                        associatedEntityId,
                        displayNameField.getValue(),
                        categoryCombo.getValue(),
                        fileName,
                        uploadedSize[0],
                        uploadedMime[0],
                        fileStream,
                        username
                );

                dialog.close();
                Notification.show("Dokument erfolgreich hochgeladen").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refresh();
            } catch (Exception ex) {
                Notification.show("Fehler beim Upload: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Abbrechen", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(formLayout);
        dialog.open();
    }

    private void openVersionUploadDialog(Document document) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neue Version hochladen");
        dialog.setWidth("450px");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "application/zip",
                "image/png",
                "image/jpeg"
        );
        upload.setMaxFileSize(50 * 1024 * 1024);
        upload.setDropLabel(new Span("Ziehe Dateien hierher (max. 50 MB)"));

        final long[] uploadedSize = {0};
        final String[] uploadedMime = {"application/octet-stream"};
        upload.addSucceededListener(event -> {
            uploadedSize[0] = event.getContentLength();
            uploadedMime[0] = event.getMIMEType();
        });

        Button saveBtn = new Button("Version speichern", e -> {
            String fileName = buffer.getFileName();
            if (fileName == null || fileName.isEmpty()) {
                Notification.show("Bitte wÃ¤hle eine Datei aus").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try (InputStream fileStream = buffer.getInputStream()) {
                String username = securityService.getAuthenticatedUser()
                        .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                        .orElse("system");
                documentService.uploadNewVersion(
                        document.getId(),
                        fileName,
                        uploadedSize[0],
                        uploadedMime[0],
                        fileStream,
                        username
                );
                dialog.close();
                Notification.show("Neue Version gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refresh();
            } catch (Exception ex) {
                Notification.show("Fehler beim Upload: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(upload);
        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), saveBtn);
        dialog.open();
    }
}
