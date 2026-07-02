package com.translationagency.modules.partner.ui;

import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.security.SecurityService;
import com.translationagency.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

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
                    partner -> getUI().ifPresent(ui -> ui.navigate("partners/" + partner.getId().toString())),
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

        HorizontalLayout header = new HorizontalLayout(headerTitle, addPartnerButton);
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

    private void deletePartner(Partner partner) {
        String username = securityService.getAuthenticatedUser()
                .map(org.springframework.security.core.userdetails.UserDetails::getUsername)
                .orElse("system");
        partnerService.deletePartner(partner.getId(), username);
        Notification.show("Partner gelöscht").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        updateList();
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
                Notification.show("Partner erfolgreich gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
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
