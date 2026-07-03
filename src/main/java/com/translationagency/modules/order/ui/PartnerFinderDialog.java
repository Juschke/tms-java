package com.translationagency.modules.order.ui;

import com.translationagency.modules.partner.application.PartnerService;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.partner.domain.PartnerLanguagePair;
import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.shared.ui.Notifications;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PartnerFinderDialog extends Dialog {

    public PartnerFinderDialog(PartnerService partnerService, UUID tenantId,
            Language sourceLang, Language targetLang,
            Consumer<Partner> onSelect) {

        setHeaderTitle("🔍 Passenden Übersetzer finden");
        setWidth("800px");
        setMaxWidth("95vw");

        if (sourceLang == null || targetLang == null) {
            add(new Span("Bitte wählen Sie zuerst Ausgangs- und Zielsprache im Teilauftrag aus."));
            Button cancel = new Button("Schließen", e -> close());
            getFooter().add(cancel);
            return;
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);

        H3 filterInfo = new H3("Gefiltert nach: " + sourceLang.getName() + " ➔ " + targetLang.getName());
        filterInfo.getStyle()
                .set("margin-top", "0")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-secondary-text-color)");

        Grid<Partner> grid = new Grid<>(Partner.class, false);
        grid.addColumn(Partner::getFullName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(p -> p.isRecommended() ? "⭐ Empfohlen" : "").setHeader("Status").setAutoWidth(true);

        grid.addColumn(p -> {
            List<PartnerLanguagePair> pairs = partnerService.getLanguagePairsForPartner(p.getId());
            for (PartnerLanguagePair pair : pairs) {
                if (pair.getSourceLanguage().getId().equals(sourceLang.getId())
                        && pair.getTargetLanguage().getId().equals(targetLang.getId())) {
                    if (pair.getPricePerLine() != null && pair.getPricePerLine().compareTo(BigDecimal.ZERO) > 0) {
                        return pair.getPricePerLine() + " € / Zeile";
                    }
                }
            }
            return "-";
        }).setHeader("Hinterlegter Preis").setAutoWidth(true);

        grid.addComponentColumn(p -> {
            Button selectBtn = new Button("Beauftragen", VaadinIcon.CHECK.create());
            selectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            selectBtn.addClickListener(e -> {
                onSelect.accept(p);
                Notifications.success(p.getFullName() + " wurde ausgewählt.");
                close();
            });
            return selectBtn;
        }).setHeader("Aktion").setAutoWidth(true);

        List<Partner> matchingPartners = partnerService.findPartnersByLanguagePair(tenantId, sourceLang.getId(),
                targetLang.getId());
        grid.setItems(matchingPartners);

        if (matchingPartners.isEmpty()) {
            layout.add(filterInfo, new Span("Keine Übersetzer für diese Sprachkombination gefunden."));
            grid.setVisible(false);
        } else {
            layout.add(filterInfo, grid);
        }

        add(layout);

        Button cancelBtn = new Button("Abbrechen", e -> close());
        getFooter().add(cancelBtn);
    }
}
