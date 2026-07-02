package com.translationagency.modules.settings.application;

import com.translationagency.modules.settings.domain.TenantSettings;
import com.translationagency.modules.settings.domain.AccountingScheme;
import com.translationagency.modules.settings.domain.TextTemplate;
import com.translationagency.modules.settings.domain.TextTemplateType;
import com.translationagency.modules.settings.infrastructure.TenantSettingsRepository;
import com.translationagency.modules.settings.infrastructure.TextTemplateRepository;
import com.translationagency.modules.tenant.domain.Tenant;
import com.translationagency.modules.tenant.infrastructure.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SettingsService {

    private final TenantSettingsRepository tenantSettingsRepository;
    private final TextTemplateRepository textTemplateRepository;
    private final TenantRepository tenantRepository;

    public SettingsService(TenantSettingsRepository tenantSettingsRepository,
                           TextTemplateRepository textTemplateRepository,
                           TenantRepository tenantRepository) {
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.textTemplateRepository = textTemplateRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID tenantId) {
        return loadTenant(tenantId);
    }

    public TenantSettings getOrCreateTenantSettings(UUID tenantId) {
        return tenantSettingsRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultTenantSettings(tenantId));
    }

    public Tenant saveTenant(Tenant tenant, String username) {
        tenant.setUpdatedBy(username);
        return tenantRepository.save(tenant);
    }

    public TenantSettings saveTenantSettings(TenantSettings settings, String username) {
        if (settings.getCreatedBy() == null) {
            settings.setCreatedBy(username);
        }
        settings.setUpdatedBy(username);
        return tenantSettingsRepository.save(settings);
    }

    public List<TextTemplate> getTextTemplates(UUID tenantId) {
        ensureDefaultTextTemplates(tenantId);
        return textTemplateRepository.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId);
    }

    public TextTemplate getOrCreateTextTemplate(UUID tenantId, TextTemplateType type) {
        return textTemplateRepository.findByTenantIdAndTemplateTypeAndDeletedAtIsNullOrderByNameAsc(tenantId, type)
                .stream()
                .findFirst()
                .orElseGet(() -> createTemplate(tenantId, type, defaultTemplateName(type), defaultSubject(type), defaultBody(type)));
    }

    public TextTemplate saveTextTemplate(TextTemplate template, UUID tenantId, String username) {
        if (template.getTenant() == null) {
            template.setTenant(loadTenant(tenantId));
        }
        if (template.getCreatedBy() == null) {
            template.setCreatedBy(username);
        }
        template.setUpdatedBy(username);
        return textTemplateRepository.save(template);
    }

    public void deleteTextTemplate(UUID templateId, String username) {
        textTemplateRepository.findById(templateId).ifPresent(template -> {
            template.setDeletedAt(OffsetDateTime.now());
            template.setUpdatedBy(username);
            textTemplateRepository.save(template);
        });
    }

    private TenantSettings createDefaultTenantSettings(UUID tenantId) {
        Tenant tenant = loadTenant(tenantId);
        TenantSettings settings = new TenantSettings();
        settings.setTenant(tenant);
        settings.setCompanyName(tenant.getName());
        settings.setEmailSenderName(tenant.getName());
        settings.setAccountingScheme(AccountingScheme.SKR03);
        settings.setCreatedBy("system");
        settings.setUpdatedBy("system");
        return tenantSettingsRepository.save(settings);
    }

    private void ensureDefaultTextTemplates(UUID tenantId) {
        createDefaultIfMissing(tenantId, TextTemplateType.QUOTE_EMAIL);
        createDefaultIfMissing(tenantId, TextTemplateType.ORDER_CONFIRMATION);
        createDefaultIfMissing(tenantId, TextTemplateType.INVOICE_EMAIL);
        createDefaultIfMissing(tenantId, TextTemplateType.DUNNING_LEVEL_1);
        createDefaultIfMissing(tenantId, TextTemplateType.DUNNING_LEVEL_2);
        createDefaultIfMissing(tenantId, TextTemplateType.DUNNING_LEVEL_3);
        createDefaultIfMissing(tenantId, TextTemplateType.PARTNER_ASSIGNMENT);
    }

    private void createDefaultIfMissing(UUID tenantId, TextTemplateType type) {
        if (textTemplateRepository.findByTenantIdAndTemplateTypeAndDeletedAtIsNullOrderByNameAsc(tenantId, type).isEmpty()) {
            createTemplate(tenantId, type, defaultTemplateName(type), defaultSubject(type), defaultBody(type));
        }
    }

    private String defaultTemplateName(TextTemplateType type) {
        return switch (type) {
            case QUOTE_EMAIL -> "Angebot versenden";
            case ORDER_CONFIRMATION -> "Auftrag bestaetigen";
            case INVOICE_EMAIL -> "Rechnung versenden";
            case DUNNING_LEVEL_1 -> "Zahlungserinnerung";
            case DUNNING_LEVEL_2 -> "Mahnung Stufe 2";
            case DUNNING_LEVEL_3 -> "Letzte Mahnung";
            case PARTNER_ASSIGNMENT -> "Partner beauftragen";
            case GENERAL -> "Allgemeine Vorlage";
        };
    }

    private String defaultSubject(TextTemplateType type) {
        return switch (type) {
            case QUOTE_EMAIL -> "Ihr Angebot {angebotsnummer}";
            case ORDER_CONFIRMATION -> "Auftragsbestaetigung {auftragsnummer}";
            case INVOICE_EMAIL -> "Rechnung {rechnungsnummer}";
            case DUNNING_LEVEL_1 -> "Zahlungserinnerung zu Rechnung {rechnungsnummer}";
            case DUNNING_LEVEL_2 -> "Mahnung zu Rechnung {rechnungsnummer}";
            case DUNNING_LEVEL_3 -> "Letzte Mahnung zu Rechnung {rechnungsnummer}";
            case PARTNER_ASSIGNMENT -> "Neue Beauftragung {auftragsnummer}";
            case GENERAL -> "";
        };
    }

    private String defaultBody(TextTemplateType type) {
        return switch (type) {
            case QUOTE_EMAIL ->
                    "Guten Tag {kunde},\n\nanbei erhalten Sie unser Angebot {angebotsnummer}.\n\nFreundliche Gruesse";
            case ORDER_CONFIRMATION ->
                    "Guten Tag {kunde},\n\nwir bestaetigen Ihren Auftrag {auftragsnummer}.\n\nFreundliche Gruesse";
            case INVOICE_EMAIL ->
                    "Guten Tag {kunde},\n\nanbei erhalten Sie unsere Rechnung {rechnungsnummer} mit Faelligkeit zum {faelligkeit}.\n\nFreundliche Gruesse";
            case DUNNING_LEVEL_1 ->
                    "Guten Tag {kunde},\n\nunsere Rechnung {rechnungsnummer} ist noch offen. Bitte pruefen Sie den Zahlungsausgleich.\n\nFreundliche Gruesse";
            case DUNNING_LEVEL_2 ->
                    "Guten Tag {kunde},\n\nwir erinnern erneut an die offene Rechnung {rechnungsnummer}. Bitte gleichen Sie den Betrag bis zum {faelligkeit} aus.\n\nFreundliche Gruesse";
            case DUNNING_LEVEL_3 ->
                    "Guten Tag {kunde},\n\ndies ist unsere letzte Mahnung zur Rechnung {rechnungsnummer}. Bitte begleichen Sie den offenen Betrag umgehend.\n\nFreundliche Gruesse";
            case PARTNER_ASSIGNMENT ->
                    "Guten Tag {partner},\n\nwir moechten Sie fuer den Auftrag {auftragsnummer} beauftragen.\n\nFreundliche Gruesse";
            case GENERAL -> "";
        };
    }

    private void createDefaultTextTemplates(UUID tenantId) {
        createTemplate(tenantId, TextTemplateType.QUOTE_EMAIL, "Angebot versenden",
                "Ihr Angebot {angebotsnummer}",
                "Guten Tag {kunde},\n\nanbei erhalten Sie unser Angebot {angebotsnummer}.\n\nFreundliche Gruesse");
        createTemplate(tenantId, TextTemplateType.ORDER_CONFIRMATION, "Auftrag bestaetigen",
                "Auftragsbestaetigung {auftragsnummer}",
                "Guten Tag {kunde},\n\nwir bestaetigen Ihren Auftrag {auftragsnummer}.\n\nFreundliche Gruesse");
        createTemplate(tenantId, TextTemplateType.INVOICE_EMAIL, "Rechnung versenden",
                "Rechnung {rechnungsnummer}",
                "Guten Tag {kunde},\n\nanbei erhalten Sie unsere Rechnung {rechnungsnummer} mit Faelligkeit zum {faelligkeit}.\n\nFreundliche Gruesse");
        createTemplate(tenantId, TextTemplateType.DUNNING_LEVEL_1, "Zahlungserinnerung",
                "Zahlungserinnerung zu Rechnung {rechnungsnummer}",
                "Guten Tag {kunde},\n\nunsere Rechnung {rechnungsnummer} ist noch offen. Bitte pruefen Sie den Zahlungsausgleich.\n\nFreundliche Gruesse");
        createTemplate(tenantId, TextTemplateType.DUNNING_LEVEL_2, "Mahnung Stufe 2",
                "Mahnung zu Rechnung {rechnungsnummer}",
                "Guten Tag {kunde},\n\nwir erinnern erneut an die offene Rechnung {rechnungsnummer}. Bitte gleichen Sie den Betrag bis zum {faelligkeit} aus.\n\nFreundliche Gruesse");
        createTemplate(tenantId, TextTemplateType.DUNNING_LEVEL_3, "Letzte Mahnung",
                "Letzte Mahnung zu Rechnung {rechnungsnummer}",
                "Guten Tag {kunde},\n\ndies ist unsere letzte Mahnung zur Rechnung {rechnungsnummer}. Bitte begleichen Sie den offenen Betrag umgehend.\n\nFreundliche Gruesse");
        createTemplate(tenantId, TextTemplateType.PARTNER_ASSIGNMENT, "Partner beauftragen",
                "Neue Beauftragung {auftragsnummer}",
                "Guten Tag {partner},\n\nwir moechten Sie fuer den Auftrag {auftragsnummer} beauftragen.\n\nFreundliche Gruesse");
    }

    private TextTemplate createTemplate(UUID tenantId, TextTemplateType type, String name, String subject, String body) {
        TextTemplate template = new TextTemplate();
        template.setTenant(loadTenant(tenantId));
        template.setTemplateType(type);
        template.setName(name);
        template.setSubject(subject);
        template.setBody(body);
        template.setActive(true);
        template.setCreatedBy("system");
        template.setUpdatedBy("system");
        return textTemplateRepository.save(template);
    }

    private Tenant loadTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
    }
}
